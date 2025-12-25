package com.example.a762map.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.a762map.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.tvMain.text = "登录成功：这里是 762Map 主页面占位xfytest"
    }
}
