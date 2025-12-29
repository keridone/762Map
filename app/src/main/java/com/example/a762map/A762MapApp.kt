package com.example.a762map

import android.app.Application
import com.amap.api.maps.MapsInitializer

class A762MapApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // 默认：你已在隐私政策中告知并获得同意
        val isAgree = true

        // 注意：必须在使用SDK任意能力之前调用
        // 常见签名：updatePrivacyShow(Context, boolean, boolean) / updatePrivacyAgree(Context, boolean)
        MapsInitializer.updatePrivacyShow(this, true, isAgree)
        MapsInitializer.updatePrivacyAgree(this, isAgree)

        // 若你需要动态设置Key，也可以使用 MapsInitializer.setApiKey(...)（可选）
        // MapsInitializer.setApiKey("你的key")  // 一般用 Manifest meta-data 即可
    }
}
