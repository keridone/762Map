package com.example.a762map.ui.profile

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.a762map.R

class UserDetailActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_detail)
        title = "用户信息"
    }
}
