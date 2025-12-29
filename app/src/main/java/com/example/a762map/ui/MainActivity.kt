package com.example.a762map.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.updatePadding
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.a762map.R
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav)
        val navHostView =
            findViewById<androidx.fragment.app.FragmentContainerView>(R.id.nav_host_fragment)

        // 关键：让 Fragment 内容区避开 BottomNavigationView
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
}
