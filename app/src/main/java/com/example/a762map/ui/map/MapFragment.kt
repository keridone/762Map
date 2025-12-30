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
import androidx.recyclerview.widget.RecyclerView
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
import com.amap.api.maps.model.Polyline
import com.amap.api.maps.model.PolylineOptions
import com.amap.api.services.core.LatLonPoint
import com.example.a762map.R
import com.example.a762map.data.search.AMapSearchService
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MapFragment : Fragment() {

    private lateinit var mapView: MapView
    private var aMap: AMap? = null

    private lateinit var suggestionAdapter: SuggestionAdapter

    private var locationClient: AMapLocationClient? = null
    private var lastKnownLocation: AMapLocation? = null

    // ====== 关键：抑制建议列表被“拉回” ======
    private var suppressSuggestions: Boolean = false
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

    // ===== UI =====
    private var fabEndNav: FloatingActionButton? = null

    // ===== Overlay 分离管理 =====
    private var searchMarker: Marker? = null                 // 搜索定位结果 Marker（仅 1 个）
    private var navDestinationMarker: Marker? = null          // 导航目的地 Marker
    private var navPolyline: Polyline? = null                 // 导航路线

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root = inflater.inflate(R.layout.fragment_map, container, false)

        searchService = AMapSearchService(requireContext().applicationContext)

        mapView = root.findViewById(R.id.amap_view)
        mapView.onCreate(savedInstanceState)

        fabEndNav = root.findViewById(R.id.fab_end_nav)
        fabEndNav?.setOnClickListener { clearNavOverlay() }
        fabEndNav?.post {
            val fab = fabEndNav ?: return@post

            // ===== 在这里改数值即可（像素级）=====
            val density = resources.displayMetrics.density

            val offsetXdp = 6f     // 距离左边的微调（dp，正数向右）
            val offsetYdp = 96f      // 垂直方向微调（dp，正数向下）

            fab.translationX = offsetXdp * density
            fab.translationY = offsetYdp * density
        }

        updateNavUiState()

        setupMap()
        setupSearchUi(root)
        observeNavRequest()
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

        aMap!!.setOnMarkerClickListener { marker ->
            showPlacePreview(marker)
            true
        }

        // 点击地图空白处：统一收起建议列表（而不是只隐藏RV）
        aMap!!.setOnMapClickListener {
            collapseSuggestions()
        }
    }

    // ====== 统一收起建议列表（解决“返回地图后列表又出现”的问题）======
    private fun collapseSuggestions() {
        val root = view ?: return
        val rv = root.findViewById<RecyclerView>(R.id.rv_suggestions)
        val et = root.findViewById<android.widget.EditText>(R.id.et_search)

        suppressSuggestions = true

        pendingSearch?.let { handler.removeCallbacks(it) }
        pendingSearch = null

        rv.visibility = View.GONE
        suggestionAdapter.submitList(emptyList())

        et.clearFocus()
        hideKeyboard()

        // 给“在路上的回调”留窗口，避免又把列表拉出来
        handler.postDelayed({ suppressSuggestions = false }, 400L)
    }

    // ====== Overlay clear 方法 ======
    private fun clearSearchOverlay() {
        searchMarker?.remove()
        searchMarker = null
    }

    private fun clearNavOverlay() {
        navPolyline?.remove()
        navPolyline = null

        navDestinationMarker?.remove()
        navDestinationMarker = null

        updateNavUiState()
    }

    private fun updateNavUiState() {
        val hasNav = (navPolyline != null) || (navDestinationMarker != null)
        fabEndNav?.visibility = if (hasNav) View.VISIBLE else View.GONE
    }

    private fun observeNavRequest() {
        val navController = findNavController()
        val entry = navController.currentBackStackEntry ?: return

        entry.savedStateHandle
            .getLiveData<Double>("nav_lat")
            .observe(viewLifecycleOwner) { lat ->

                val lng = entry.savedStateHandle.get<Double>("nav_lng") ?: return@observe
                val title = entry.savedStateHandle.get<String>("nav_title") ?: "目的地"

                // ✅ 消费事件（防止旋转/回退重复触发）
                entry.savedStateHandle.remove<Double>("nav_lat")
                entry.savedStateHandle.remove<Double>("nav_lng")
                entry.savedStateHandle.remove<String>("nav_title")

                if (lat.isNaN() || lng.isNaN()) return@observe

                // 返回地图并开始导航时，也强制收起推荐列表
                collapseSuggestions()

                startNavigationTo(
                    NavRequest(
                        title = title,
                        lat = lat,
                        lng = lng
                    )
                )
            }
    }

    private fun startNavigationTo(req: NavRequest) {
        clearNavOverlay()

        val dest = LatLng(req.lat, req.lng)

        navDestinationMarker = aMap?.addMarker(
            MarkerOptions().position(dest).title(req.title).snippet("目的地")
        )
        updateNavUiState()

        val loc = lastKnownLocation
        if (loc == null) {
            aMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(dest, 16f))
            return
        }

        val start = LatLonPoint(loc.latitude, loc.longitude)
        val end = LatLonPoint(req.lat, req.lng)

        aMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(dest, 15f))

        searchService.driveRoute(start = start, end = end) { points ->
            if (!isAdded) return@driveRoute
            if (points.isEmpty()) return@driveRoute

            val latLngs = points.map { LatLng(it.latitude, it.longitude) }

            navPolyline?.remove()
            navPolyline = aMap?.addPolyline(
                PolylineOptions()
                    .addAll(latLngs)
                    .width(12f)
                    .geodesic(true)
            )

            updateNavUiState()
        }
    }

    private fun setupSearchUi(root: View) {
        val et = root.findViewById<android.widget.EditText>(R.id.et_search)
        val btnClear = root.findViewById<android.widget.ImageButton>(R.id.btn_clear)
        val rv = root.findViewById<RecyclerView>(R.id.rv_suggestions)

        suggestionAdapter = SuggestionAdapter { item ->
            // ====== 关键：阻断 setText() 引发的 watcher + 异步回调“拉回列表” ======
            suppressSuggestions = true

            // 取消可能已排队的防抖任务
            pendingSearch?.let { handler.removeCallbacks(it) }
            pendingSearch = null

            // 临时移除 watcher，避免 et.setText 触发 afterTextChanged
            searchTextWatcher?.let { et.removeTextChangedListener(it) }

            // 立即隐藏列表并清空，防止闪现
            rv.visibility = View.GONE
            suggestionAdapter.submitList(emptyList())

            // 设置文本（此时 watcher 已移除，不会触发搜索）
            et.setText(item.title)
            et.setSelection(et.text.length)

            // 退出输入态
            et.clearFocus()
            hideKeyboard()

            // 恢复 watcher
            searchTextWatcher?.let { et.addTextChangedListener(it) }

            // 给“在路上的回调”一个时间窗，避免再次显示列表
            handler.postDelayed({ suppressSuggestions = false }, 500L)

            // 定位逻辑
            if (item.hasPoint()) {
                locateSearchResultTo(item.title, item.subtitle, item.lat, item.lng)
            } else {
                searchPoiThenLocate(item.title)
            }
        }

        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = suggestionAdapter

        searchTextWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

            override fun afterTextChanged(s: Editable?) {
                if (suppressSuggestions) return

                val q = s?.toString().orEmpty().trim()
                btnClear.visibility = if (q.isEmpty()) View.GONE else View.VISIBLE

                pendingSearch?.let { handler.removeCallbacks(it) }

                if (q.isEmpty()) {
                    lastQuery = ""
                    rv.visibility = View.GONE
                    suggestionAdapter.submitList(emptyList())
                    return
                }

                val task = Runnable {
                    lastQuery = q
                    requestInputTips(q, rv)
                }
                pendingSearch = task
                handler.postDelayed(task, debounceMs)
            }
        }
        et.addTextChangedListener(searchTextWatcher)

        btnClear.setOnClickListener {
            suppressSuggestions = false
            et.setText("")
            rv.visibility = View.GONE
            suggestionAdapter.submitList(emptyList())
        }
    }

    private fun requestInputTips(query: String, rv: RecyclerView) {
        searchService.inputTips(query, currentCity) { tips ->
            if (suppressSuggestions) return@inputTips
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
                locateSearchResultTo(title ?: keyword, snippet ?: "", lat, lng)
            }
        }
    }

    private fun locateSearchResultTo(title: String, subtitle: String, lat: Double, lng: Double) {
        val latLng = LatLng(lat, lng)

        clearSearchOverlay()

        searchMarker = aMap?.addMarker(
            MarkerOptions()
                .position(latLng)
                .title(title)
                .snippet(subtitle)
        )

        aMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17f))
    }

    private fun showPlacePreview(marker: Marker) {
        // 跳转详情前统一收起建议列表，避免返回时残留状态
        collapseSuggestions()

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
        val style = MyLocationStyle()
        style.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE_NO_CENTER)
        style.interval(2000)

        aMap?.myLocationStyle = style
        aMap?.uiSettings?.isMyLocationButtonEnabled = true
        aMap?.isMyLocationEnabled = true

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
                currentCity = loc.city

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

    override fun onResume() {
        super.onResume()
        mapView.onResume()

        // 从详情页/其他页面返回地图时，强制收起建议列表，避免“弹回”
        collapseSuggestions()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onDestroyView() {
        pendingSearch?.let { handler.removeCallbacks(it) }
        pendingSearch = null

        clearSearchOverlay()
        clearNavOverlay()

        super.onDestroyView()
        locationClient?.onDestroy()
        locationClient = null
        mapView.onDestroy()
        aMap = null
        fabEndNav = null
        searchTextWatcher = null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }
}
