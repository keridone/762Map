package com.example.a762map.data.search

import android.content.Context
import com.amap.api.services.core.AMapException
import com.amap.api.services.core.LatLonPoint
import com.amap.api.services.help.Inputtips
import com.amap.api.services.help.InputtipsQuery
import com.amap.api.services.help.Tip
import com.amap.api.services.poisearch.PoiSearch
import com.amap.api.services.poisearch.PoiSearch.Query

class AMapSearchService(private val appContext: Context) {

    /**
     * 输入提示（联想）
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

        val query = InputtipsQuery(keyword, city).apply {
            // 只有 city 有值才限制城市；否则不限制，避免搜不到
            cityLimit = !city.isNullOrBlank()
        }

        val inputTips = Inputtips(appContext, query)
        inputTips.setInputtipsListener { tipList, rCode ->
            android.util.Log.d(
                "MapSearch",
                "inputTips keyword=$keyword city=$city cityLimit=${query.cityLimit} rCode=$rCode size=${tipList?.size ?: 0}"
            )

            tipList?.take(5)?.forEach { t ->
                android.util.Log.d(
                    "MapSearch",
                    "tip name=${t.name} district=${t.district} point=${t.point} poiId=${t.poiID}"
                )
            }

            if (rCode == AMapException.CODE_AMAP_SUCCESS && !tipList.isNullOrEmpty()) {
                callback(tipList)
            } else {
                callback(emptyList())
            }
        }

        inputTips.requestInputtipsAsyn()
    }


    /**
     * POI 搜索兜底：当 Tip 没有坐标时，取第一个 POI 的位置
     */
    fun searchFirstPoi(
        keyword: String,
        city: String?,
        around: LatLonPoint?,
        callback: (lat: Double?, lng: Double?, title: String?, snippet: String?) -> Unit
    ) {
        val query = Query(keyword, "", city).apply {
            pageNum = 1
            pageSize = 10
        }

        val poiSearch = PoiSearch(appContext, query)

        // 如果有当前位置，优先用“周边搜索”提高命中
        if (around != null) {
            poiSearch.bound = PoiSearch.SearchBound(around, 3000) // 3km 可按需调整
        }

        poiSearch.setOnPoiSearchListener(object : PoiSearch.OnPoiSearchListener {
            override fun onPoiSearched(result: com.amap.api.services.poisearch.PoiResult?, rCode: Int) {
                if (rCode == AMapException.CODE_AMAP_SUCCESS && result != null) {
                    val first = result.pois?.firstOrNull()
                    val p = first?.latLonPoint
                    if (p != null) {
                        callback(
                            p.latitude,
                            p.longitude,
                            first.title,
                            first.snippet
                        )
                        return
                    }
                }
                callback(null, null, null, null)
            }

            override fun onPoiItemSearched(item: com.amap.api.services.core.PoiItem?, rCode: Int) {
                // 本需求不使用单个 poiId 查询，可留空
            }
        })

        poiSearch.searchPOIAsyn()
    }
}
