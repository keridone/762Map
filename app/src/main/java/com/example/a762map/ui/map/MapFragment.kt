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
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.amap.api.location.AMapLocationClient
import com.amap.api.location.AMapLocationClientOption
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.MapView
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.Marker
import com.amap.api.maps.model.MarkerOptions
import com.amap.api.maps.model.MyLocationStyle
import com.example.a762map.R

class MapFragment : Fragment() {

    private lateinit var mapView: MapView
    private var aMap: AMap? = null

    private lateinit var suggestionAdapter: SuggestionAdapter

    private var locationClient: AMapLocationClient? = null

    private val permissionRequestCode = 1001

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root = inflater.inflate(R.layout.fragment_map, container, false)

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

        // === 高德地图自带 UI 控件 ===
        ui.isZoomControlsEnabled = true      // 缩放按钮（+ / -）
        ui.isCompassEnabled = true           // 指南针
        ui.isScaleControlsEnabled = true     // 比例尺
        ui.isMyLocationButtonEnabled = true  // 定位按钮（你之前已开启）

        // 可选：把缩放按钮挪到右中，避免被底部导航栏遮挡
        // ui.zoomPosition = AMapOptions.ZOOM_POSITION_RIGHT_CENTER

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
            // 选择建议：打点 + 移动镜头 + 收起建议
            rv.visibility = View.GONE
            et.setText(item.title)
            et.setSelection(et.text.length)
            hideKeyboard()

            val latLng = LatLng(item.lat, item.lng)
            aMap?.clear()
            aMap?.addMarker(MarkerOptions().position(latLng).title(item.title).snippet(item.subtitle))
            aMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17f))
        }

        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = suggestionAdapter

        et.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

            override fun afterTextChanged(s: Editable?) {
                val q = s?.toString().orEmpty().trim()
                btnClear.visibility = if (q.isEmpty()) View.GONE else View.VISIBLE

                // 这里先用“本地假数据”模拟联想；后续你接入高德 POI 搜索/你自己的后台即可
                if (q.isEmpty()) {
                    rv.visibility = View.GONE
                    suggestionAdapter.submitList(emptyList())
                } else {
                    rv.visibility = View.VISIBLE
                    suggestionAdapter.submitList(fakeSuggestions(q))
                }
            }
        })

        btnClear.setOnClickListener {
            et.setText("")
        }
    }

    private fun fakeSuggestions(q: String): List<SuggestionItem> {
        // 用于占位：后续替换为 AMap 输入提示/POI 搜索
        return listOf(
            SuggestionItem("$q（示例地点A）", "点击后打点并居中", 39.9087, 116.3975),
            SuggestionItem("$q（示例地点B）", "后续可进详情页", 31.2304, 121.4737),
            SuggestionItem("$q（示例地点C）", "后续可做路线规划", 23.1291, 113.2644),
        )
    }

    private fun showPlacePreview(marker: Marker) {
        val title = marker.title ?: "未命名地点"
        val lat = marker.position.latitude
        val lng = marker.position.longitude

        // 简化：直接跳转详情页（你也可以改为 BottomSheet 预览 + “查看详情”按钮）
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

        // 2) 使用定位 SDK 获取一次定位并移动镜头（占位：后续你可以持续更新）
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
