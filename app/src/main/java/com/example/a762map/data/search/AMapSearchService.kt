package com.example.a762map.data.search

import android.content.Context
import com.amap.api.services.core.LatLonPoint
import com.amap.api.services.help.Inputtips
import com.amap.api.services.help.InputtipsQuery
import com.amap.api.services.help.Inputtips.InputtipsListener
import com.amap.api.services.help.Tip
import com.amap.api.services.poisearch.PoiResult
import com.amap.api.services.poisearch.PoiSearch
import com.amap.api.services.route.DriveRouteResult
import com.amap.api.services.route.RouteSearch

class AMapSearchService(
    private val appContext: Context
) {

    /**
     * 输入提示（搜索联想）
     */
    fun inputTips(
        keyword: String,
        city: String?,
        callback: (List<Tip>) -> Unit
    ) {
        if (keyword.isBlank()) {
            callback(emptyList())
            return
        }

        // ✅ 1. 使用 InputtipsQuery
        val query = InputtipsQuery(keyword, city ?: "").apply {
            // 可选配置（不写也行）
            cityLimit = true      // 限制在当前城市
        }

        // ✅ 2. 正确构造 Inputtips
        val inputTips = Inputtips(appContext, query)

        // ✅ 3. 设置监听并异步请求
        inputTips.setInputtipsListener { tips, rCode ->
            if (rCode == 1000) {
                callback(tips ?: emptyList())
            } else {
                callback(emptyList())
            }
        }

        inputTips.requestInputtipsAsyn()
    }


    /**
     * POI 搜索：取第一条结果（用于 Tip 没坐标时兜底）
     */
    fun searchFirstPoi(
        keyword: String,
        city: String?,
        around: LatLonPoint?,
        callback: (lat: Double?, lng: Double?, title: String?, snippet: String?) -> Unit
    ) {
        if (keyword.isBlank()) {
            callback(null, null, null, null)
            return
        }

        val query = PoiSearch.Query(keyword, "", city ?: "").apply {
            pageSize = 10
            pageNum = 0
        }

        val poiSearch = PoiSearch(appContext, query)

        // 有当前位置则做周边检索更准
        if (around != null) {
            poiSearch.bound = PoiSearch.SearchBound(around, 30000) // 30km
        }

        poiSearch.setOnPoiSearchListener(object : PoiSearch.OnPoiSearchListener {
            override fun onPoiSearched(result: PoiResult?, rCode: Int) {
                val first = result?.pois?.firstOrNull()
                val point = first?.latLonPoint
                if (point != null) {
                    callback(point.latitude, point.longitude, first.title, first.snippet)
                } else {
                    callback(null, null, null, null)
                }
            }

            override fun onPoiItemSearched(poiItem: com.amap.api.services.core.PoiItem?, rCode: Int) {
                // 不使用
            }
        })

        poiSearch.searchPOIAsyn()
    }

    /**
     * 驾车路径规划：返回用于绘制的折线点集合
     */
    fun driveRoute(
        start: LatLonPoint,
        end: LatLonPoint,
        callback: (List<LatLonPoint>) -> Unit
    ) {
        val routeSearch = RouteSearch(appContext)

        val fromAndTo = RouteSearch.FromAndTo(start, end)
        val query = RouteSearch.DriveRouteQuery(
            fromAndTo,
            0,      // 默认策略；后续可扩展
            null,
            null,
            ""
        )

        routeSearch.setRouteSearchListener(object : RouteSearch.OnRouteSearchListener {
            override fun onDriveRouteSearched(result: DriveRouteResult?, rCode: Int) {
                val path = result?.paths?.firstOrNull()
                if (path == null) {
                    callback(emptyList())
                    return
                }

                val points = mutableListOf<LatLonPoint>()
                val steps = path.steps ?: emptyList()
                for (step in steps) {
                    val poly = step.polyline ?: continue
                    points.addAll(poly)
                }
                callback(points)
            }

            override fun onBusRouteSearched(result: com.amap.api.services.route.BusRouteResult?, rCode: Int) = Unit
            override fun onWalkRouteSearched(result: com.amap.api.services.route.WalkRouteResult?, rCode: Int) = Unit
            override fun onRideRouteSearched(result: com.amap.api.services.route.RideRouteResult?, rCode: Int) = Unit
        })

        routeSearch.calculateDriveRouteAsyn(query)
    }
}
