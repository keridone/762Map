package com.example.a762map.ui.profile

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.a762map.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var db: AppDbHelper
    private lateinit var session: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = AppDbHelper(this)
        session = SessionManager(this)

        // 切换注册面板显示
        binding.tvGoRegister.setOnClickListener {
            binding.registerPanel.visibility =
                if (binding.registerPanel.visibility == View.VISIBLE)
                    View.GONE
                else
                    View.VISIBLE
        }

        // ===== 登录 =====
        binding.btnLogin.setOnClickListener {
            val phone = binding.etAccount.text.toString().trim()
            val pwd = binding.etPassword.text.toString().trim()

            if (phone.isEmpty() || pwd.isEmpty()) {
                toast("请输入手机号和密码")
                return@setOnClickListener
            }

            val user = db.loginByPhone(phone, pwd)
            if (user == null) {
                toast("账号或密码错误")
                return@setOnClickListener
            }

            session.saveLogin(user.id)
            toast("登录成功：${user.role}")
            finish()   // 回到 MainActivity -> ProfileFragment 自动刷新
        }

        // ===== 注册 =====
        binding.btnRegister.setOnClickListener {
            val name = binding.etRegUsername.text.toString().trim()
            val phone = binding.etRegPhone.text.toString().trim()
            val email = binding.etRegEmail.text.toString().trim()
            val pwd = binding.etRegPassword.text.toString().trim()

            if (name.isEmpty() || phone.isEmpty() || email.isEmpty() || pwd.isEmpty()) {
                toast("请完整填写注册信息")
                return@setOnClickListener
            }

            try {
                val id = db.writableDatabase.insert(
                    AppDbHelper.T_USER,
                    null,
                    android.content.ContentValues().apply {
                        put("username", name)
                        put("phone", phone)
                        put("email", email)
                        put("password", pwd)
                        put("avatarRes", android.R.drawable.sym_def_app_icon)
                        put("role", "normal")
                        put("vipLevel", 0)
                        put("totalMileage", 0.0)
                        put("yearMileage", 0.0)
                        put("navCount", 0)
                        put("litCities", 0)
                    }
                )

                if (id > 0) {
                    session.saveLogin(id)
                    toast("注册成功，已自动登录")
                    finish()
                } else {
                    toast("注册失败")
                }
            } catch (e: Exception) {
                toast("注册失败：手机号或邮箱已存在")
            }
        }
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}
