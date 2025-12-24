package com.example.a762map.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.a762map.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnLogin.setOnClickListener {
            val username = binding.etUsername.text?.toString()?.trim().orEmpty()
            val password = binding.etPassword.text?.toString()?.trim().orEmpty()

            if (username.isEmpty()) {
                binding.tilUsername.error = "请输入账号"
                return@setOnClickListener
            } else {
                binding.tilUsername.error = null
            }

            if (password.isEmpty()) {
                binding.tilPassword.error = "请输入密码"
                return@setOnClickListener
            } else {
                binding.tilPassword.error = null
            }

            // 测试用：固定账号密码
            // 账号：admin  密码：123456
            val ok = (username == "admin" && password == "123456")
            if (ok) {
                Toast.makeText(this, "登录成功", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            } else {
                Toast.makeText(this, "账号或密码错误（测试：admin/123456）", Toast.LENGTH_SHORT).show()
            }
        }

        binding.tvGoRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
}
