package com.example.a762map.ui.profile

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.a762map.R
import com.example.a762map.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val homeFragment = HomeFragment()
    private val profileFragment = ProfileFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ✅ 正确初始化 ViewBinding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 默认显示首页
        if (savedInstanceState == null) {
            showHome()
        }

        // 底部导航点击
        binding.tabHome.setOnClickListener { showHome() }
        binding.tabProfile.setOnClickListener { showProfile() }
    }

    private fun showHome() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.contentContainer, homeFragment)
            .commitAllowingStateLoss()

        setTabSelected(isHome = true)
    }

    private fun showProfile() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.contentContainer, profileFragment)
            .commitAllowingStateLoss()

        setTabSelected(isHome = false)
    }

    private fun setTabSelected(isHome: Boolean) {
        val selectedColor = ContextCompat.getColor(this, android.R.color.black)
        val normalColor = ContextCompat.getColor(this, android.R.color.darker_gray)

        binding.tvHome.setTextColor(if (isHome) selectedColor else normalColor)
        binding.tvProfile.setTextColor(if (isHome) normalColor else selectedColor)

        // 图标也给个轻微差异：选中用黑色滤镜（最简单）
        binding.ivHome.setColorFilter(if (isHome) selectedColor else normalColor)
        binding.ivProfile.setColorFilter(if (isHome) normalColor else selectedColor)
    }
}
