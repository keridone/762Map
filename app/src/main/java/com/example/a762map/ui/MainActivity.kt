package com.example.a762map.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.updatePadding
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.a762map.R
import com.google.android.material.bottomnavigation.BottomNavigationView

import android.content.pm.PackageManager
import android.util.Log
import com.amap.api.location.AMapLocationClient
import com.amap.api.maps.MapsInitializer
import java.security.MessageDigest

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        /* ===============================
         * 1️⃣ 高德 SDK 隐私合规初始化（必须最先）
         * =============================== */
        MapsInitializer.updatePrivacyShow(this, true, true)
        MapsInitializer.updatePrivacyAgree(this, true)

        AMapLocationClient.updatePrivacyShow(this, true, true)
        AMapLocationClient.updatePrivacyAgree(this, true)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        /* ===============================
         * 2️⃣ 打印当前应用 SHA1（用于高德控制台校验）
         * =============================== */
        logAppSha1()

        /* ===============================
         * 3️⃣ 你原有的导航 / BottomNav 逻辑
         * =============================== */
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav)
        val navHostView =
            findViewById<androidx.fragment.app.FragmentContainerView>(R.id.nav_host_fragment)

        // 让 Fragment 内容区避开 BottomNavigationView
        bottomNav.post {
            navHostView.updatePadding(
                left = navHostView.paddingLeft,
                top = navHostView.paddingTop,
                right = navHostView.paddingRight,
                bottom = bottomNav.height
            )
        }

        bottomNav.setupWithNavController(navController)
    }

    private fun logAppSha1() {
        try {
            val pkg = packageName
            val pm = packageManager
            val pi = pm.getPackageInfo(pkg, PackageManager.GET_SIGNING_CERTIFICATES)

            val signers = pi.signingInfo?.apkContentsSigners
                ?: pi.signingInfo?.signingCertificateHistory

            if (signers == null || signers.isEmpty()) {
                Log.e("MapSearch", "No signatures found for pkg=$pkg")
                return
            }

            val md = MessageDigest.getInstance("SHA1")
            val sha1 = md.digest(signers[0].toByteArray())
                .joinToString(":") { "%02X".format(it) }

            Log.d("MapSearch", "APP pkg=$pkg SHA1=$sha1")
        } catch (e: Exception) {
            Log.e("MapSearch", "Failed to get SHA1", e)
        }
    }
}
