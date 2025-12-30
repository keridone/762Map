package com.example.a762map.ui.map

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.amap.api.location.AMapLocation
import com.amap.api.location.AMapLocationClient
import com.amap.api.location.AMapLocationClientOption
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.MapView
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.Marker
import com.amap.api.maps.model.MarkerOptions
import com.amap.api.maps.model.MyLocationStyle
import com.amap.api.services.core.LatLonPoint
import com.example.a762map.R
import com.example.a762map.data.search.AMapSearchService

class MapFragment : Fragment() {

    private lateinit var mapView: MapView
    private var aMap: AMap? = null

    private lateinit var suggestionAdapter: SuggestionAdapter

    private var locationClient: AMapLocationClient? = null
    private var lastKnownLocation: AMapLocation? = null
    private var suppressSuggestions = false
    private var searchTextWatcher: TextWatcher? = null
    private val permissionRequestCode = 1001

    // 防抖
    private val debounceMs = 300L
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private var pendingSearch: Runnable? = null
    private var lastQuery: String = ""

    // 搜索服务
    private lateinit var searchService: AMapSearchService
    private var currentCity: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root = inflater.inflate(R.layout.fragment_map, container, false)

        searchService = AMapSearchService(requireContext().applicationContext)

        mapView = root.findViewById(R.id.amap_view)
        mapView.onCreate(savedInstanceState)

        setupMap()
        setupSearchUi(root)
        ensureLocationPermissionThenStart()

        return root
    }

    private fun setupMap() {
        aMap = mapView.map ?: return

        val ui = aMap!!.uiSettings
        ui.isZoomControlsEnabled = true
        ui.isCompassEnabled = true
        ui.isScaleControlsEnabled = true
        ui.isMyLocationButtonEnabled = true

        // Marker 点击事件
        aMap!!.setOnMarkerClickListener { marker ->
            showPlacePreview(marker)
            true
        }
    }

    private fun setupSearchUi(root: View) {
        val et = root.findViewById<android.widget.EditText>(R.id.et_search)
        val btnClear = root.findViewById<android.widget.ImageButton>(R.id.btn_clear)
        val rv = root.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rv_suggestions)

        suggestionAdapter = SuggestionAdapter { item ->
            // ===== 新增：如果输入提示没有坐标，用 POI 搜索兜底 =====
            if (item.lat.isNaN() || item.lng.isNaN()) {
                suppressSuggestions = true
                rv.visibility = View.GONE
                suggestionAdapter.submitList(emptyList())
                et.clearFocus()
                hideKeyboard()

                searchPoiThenLocate(item.title)

                handler.postDelayed({ suppressSuggestions = false }, 350)
                return@SuggestionAdapter
            }
            // ===== 关键：进入“抑制联想”状态，先把列表强制关掉 =====
            suppressSuggestions = true
            pendingSearch?.let { handler.removeCallbacks(it) }
            pendingSearch = null

            rv.visibility = View.GONE
            suggestionAdapter.submitList(emptyList())

            // 关键：清焦点 + 收键盘（避免保持搜索态）
            et.clearFocus()
            hideKeyboard()

            // ===== 关键：临时移除 TextWatcher，避免 setText 触发 afterTextChanged 又把列表打开 =====
            searchTextWatcher?.let { et.removeTextChangedListener(it) }
            et.setText(item.title)
            et.setSelection(et.text.length)
            searchTextWatcher?.let { et.addTextChangedListener(it) }

            // 地图动作：打点 + 移动镜头
            val latLng = com.amap.api.maps.model.LatLng(item.lat, item.lng)
            aMap?.clear()
            aMap?.addMarker(
                com.amap.api.maps.model.MarkerOptions()
                    .position(latLng)
                    .title(item.title)
                    .snippet(item.subtitle)
            )
            aMap?.animateCamera(com.amap.api.maps.CameraUpdateFactory.newLatLngZoom(latLng, 17f))

            // 给一个很短的延迟再解除抑制，防止“刚点完又弹出”
            handler.postDelayed({ suppressSuggestions = false }, 350)
        }

        rv.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())
        rv.adapter = suggestionAdapter

        // 保存 watcher 引用，便于上面临时 remove/add
        searchTextWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

            override fun afterTextChanged(s: android.text.Editable?) {
                // ===== 关键：如果处于抑制状态，不要再打开建议 =====
                if (suppressSuggestions) return

                val q = s?.toString().orEmpty().trim()
                btnClear.visibility = if (q.isEmpty()) View.GONE else View.VISIBLE

                if (q.isEmpty()) {
                    rv.visibility = View.GONE
                    suggestionAdapter.submitList(emptyList())
                    pendingSearch?.let { handler.removeCallbacks(it) }
                    pendingSearch = null
                    return
                }

                // 这里继续走你原本的“防抖 + inputTips”逻辑
                pendingSearch?.let { handler.removeCallbacks(it) }
                pendingSearch = Runnable {
                    // 记录本次查询，用于回调时防止旧结果覆盖
                    lastQuery = q

                    // 如果此时仍处于抑制状态，不请求也不显示
                    if (suppressSuggestions) return@Runnable

                    // 发起输入提示请求
                    requestInputTips(q, rv)
                }
                handler.postDelayed(pendingSearch!!, debounceMs)
            }
        }

        et.addTextChangedListener(searchTextWatcher)

        btnClear.setOnClickListener {
            suppressSuggestions = true
            pendingSearch?.let { handler.removeCallbacks(it) }
            pendingSearch = null

            rv.visibility = View.GONE
            suggestionAdapter.submitList(emptyList())

            searchTextWatcher?.let { et.removeTextChangedListener(it) }
            et.setText("")
            searchTextWatcher?.let { et.addTextChangedListener(it) }

            et.requestFocus()
            suppressSuggestions = false
        }
    }


    private fun requestInputTips(query: String, rv: androidx.recyclerview.widget.RecyclerView) {
        // city 为空时也可请求，但有城市会更准
        searchService.inputTips(query, currentCity) { tips ->
            // 异步返回时，避免覆盖最新输入
            if (lastQuery != query) return@inputTips
            if (!isAdded) return@inputTips

            val items = tips
                .filter { !it.name.isNullOrBlank() }
                .map { tip ->
                    SuggestionItem(
                        title = tip.name ?: "",
                        subtitle = tip.district ?: "",
                        lat = tip.point?.latitude ?: Double.NaN,
                        lng = tip.point?.longitude ?: Double.NaN,
                        poiId = tip.poiID ?: ""
                    )
                }

            // 关键：如果刚点了建议（抑制状态），不要把列表重新显示出来
            if (suppressSuggestions) return@inputTips

            rv.visibility = if (items.isEmpty()) View.GONE else View.VISIBLE
            suggestionAdapter.submitList(items)
        }
    }


    private fun searchPoiThenLocate(keyword: String) {
        val around = lastKnownLocation?.let { loc ->
            LatLonPoint(loc.latitude, loc.longitude)
        }

        searchService.searchFirstPoi(keyword, currentCity, around) { lat, lng, title, snippet ->
            if (!isAdded) return@searchFirstPoi

            if (lat != null && lng != null) {
                locateTo(title ?: keyword, snippet ?: "", lat, lng)
            } else {
                // 找不到则不做动作；你如果需要可以 Toast 提示
                // Toast.makeText(requireContext(), "未找到地点：$keyword", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun locateTo(title: String, subtitle: String, lat: Double, lng: Double) {
        val latLng = LatLng(lat, lng)
        aMap?.clear()
        aMap?.addMarker(
            MarkerOptions()
                .position(latLng)
                .title(title)
                .snippet(subtitle)
        )
        aMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17f))
    }

    private fun showPlacePreview(marker: Marker) {
        val title = marker.title ?: "未命名地点"
        val lat = marker.position.latitude
        val lng = marker.position.longitude

        val args = Bundle().apply {
            putString("title", title)
            putDouble("lat", lat)
            putDouble("lng", lng)
        }
        findNavController().navigate(R.id.action_mapFragment_to_placeDetailFragment, args)
    }

    private fun ensureLocationPermissionThenStart() {
        val need = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        val granted = need.all {
            ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
        }

        if (granted) {
            startLocationAndBlueDot()
        } else {
            requestPermissions(need, permissionRequestCode)
        }
    }

    private fun startLocationAndBlueDot() {
        // 1) 开启地图蓝点
        val style = MyLocationStyle()
        style.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE_NO_CENTER)
        style.interval(2000)

        aMap?.myLocationStyle = style
        aMap?.uiSettings?.isMyLocationButtonEnabled = true
        aMap?.isMyLocationEnabled = true

        // 2) 获取一次定位并移动镜头
        locationClient?.onDestroy()
        locationClient = AMapLocationClient(requireContext().applicationContext)

        val option = AMapLocationClientOption().apply {
            locationMode = AMapLocationClientOption.AMapLocationMode.Hight_Accuracy
            isOnceLocation = true
            isNeedAddress = true
        }

        locationClient?.setLocationOption(option)
        locationClient?.setLocationListener { loc ->
            if (loc != null && loc.errorCode == 0) {
                lastKnownLocation = loc
                currentCity = loc.city // 用于输入提示/POI搜索更准

                val latLng = LatLng(loc.latitude, loc.longitude)
                aMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16f))
            }
        }
        locationClient?.startLocation()
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(InputMethodManager::class.java)
        imm?.hideSoftInputFromWindow(view?.windowToken, 0)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == permissionRequestCode) {
            val ok = grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            if (ok) startLocationAndBlueDot()
        }
    }

    // MapView 生命周期管理（必须）
    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onDestroyView() {
        // 清理防抖任务，避免销毁后回调更新 UI
        pendingSearch?.let { handler.removeCallbacks(it) }
        pendingSearch = null

        super.onDestroyView()
        locationClient?.onDestroy()
        locationClient = null
        mapView.onDestroy()
        aMap = null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }
}
