package com.example.a762map.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.a762map.databinding.ActivityRegisterBinding

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnRegister.setOnClickListener {
            val u = binding.etNewUsername.text?.toString()?.trim().orEmpty()
            val p = binding.etNewPassword.text?.toString()?.trim().orEmpty()

            if (u.isBlank() || p.isBlank()) {
                Toast.makeText(this, "请输入账号和密码", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 测试用：不落库，直接提示成功并返回
            Toast.makeText(this, "注册成功（测试页，不保存）", Toast.LENGTH_SHORT).show()
            finish()
        }

        binding.btnBack.setOnClickListener { finish() }
    }
}
