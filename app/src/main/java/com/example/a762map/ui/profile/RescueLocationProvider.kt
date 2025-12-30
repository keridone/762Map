package com.example.a762map.ui.profile

import android.content.Context
import com.amap.api.location.AMapLocation
import com.amap.api.location.AMapLocationClient
import com.amap.api.location.AMapLocationClientOption
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * 获取一次当前位置（含地址信息）。
 * - 使用高德定位 SDK
 * - isOnceLocation = true
 * - isNeedAddress = true
 */
class RescueLocationProvider(private val context: Context) {

    suspend fun getOnceLocation(timeoutMs: Long = 8000L): AMapLocation? {
        return suspendCancellableCoroutine { cont ->
            val appCtx = context.applicationContext
            val client = AMapLocationClient(appCtx)

            val option = AMapLocationClientOption().apply {
                locationMode = AMapLocationClientOption.AMapLocationMode.Hight_Accuracy
                isOnceLocation = true
                isNeedAddress = true
                httpTimeOut = timeoutMs
            }

            client.setLocationOption(option)

            client.setLocationListener { loc ->
                try {
                    client.stopLocation()
                    client.onDestroy()
                } catch (_: Exception) {}

                if (!cont.isActive) return@setLocationListener
                if (loc != null && loc.errorCode == 0) cont.resume(loc) else cont.resume(null)
            }

            try {
                client.startLocation()
            } catch (_: Exception) {
                try { client.onDestroy() } catch (_: Exception) {}
                if (cont.isActive) cont.resume(null)
            }

            cont.invokeOnCancellation {
                try {
                    client.stopLocation()
                    client.onDestroy()
                } catch (_: Exception) {}
            }
        }
    }
}
