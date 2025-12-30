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

        binding.tvGoRegister.setOnClickListener {
            binding.registerPanel.visibility =
                if (binding.registerPanel.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }

        binding.btnLogin.setOnClickListener {
            val acc = binding.etAccount.text.toString().trim()
            val pwd = binding.etPassword.text.toString().trim()
            if (acc.isEmpty() || pwd.isEmpty()) {
                toast("请输入账号和密码"); return@setOnClickListener
            }

            val user = if (binding.rbPhone.isChecked) db.loginByPhone(acc, pwd) else db.loginByEmail(acc, pwd)
            if (user == null) {
                toast("账号或密码错误"); return@setOnClickListener
            }

            // admin 特判：username=admin 且 password=123456（你要求）
            // 实际上我们已经 seed 为 role=admin，这里直接按 role 走即可
            session.saveLogin(user.id)
            toast("登录成功：${user.role}")
            finish() // 回到 MainActivity -> ProfileFragment onResume 刷新
        }

        binding.btnRegister.setOnClickListener {
            val name = binding.etRegUsername.text.toString().trim()
            val phone = binding.etRegPhone.text.toString().trim()
            val email = binding.etRegEmail.text.toString().trim()
            val pwd = binding.etRegPassword.text.toString().trim()
            if (name.isEmpty() || phone.isEmpty() || email.isEmpty() || pwd.isEmpty()) {
                toast("请完整填写注册信息"); return@setOnClickListener
            }
            val id = try {
                db.registerUser(name, phone, email, pwd)
            } catch (e: Exception) {
                toast("注册失败：手机号/邮箱可能已存在")
                return@setOnClickListener
            }
            if (id > 0) {
                session.saveLogin(id)
                toast("注册成功并已登录")
                finish()
            } else toast("注册失败")
        }
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
