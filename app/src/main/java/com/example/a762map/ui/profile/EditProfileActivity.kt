package com.example.a762map.ui.profile

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.a762map.databinding.ActivityEditProfileBinding

class EditProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditProfileBinding
    private lateinit var db: AppDbHelper
    private lateinit var session: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = AppDbHelper(this)
        session = SessionManager(this)

        val uid = session.getUserId()
        val u = db.getUserById(uid)
        if (u == null) {
            toast("请先登录")
            finish()
            return
        }

        // 回显
        binding.etUsername.setText(u.username)
        binding.etPhone.setText(u.phone)
        binding.etEmail.setText(u.email)
        binding.etPassword.setText(u.password)

        binding.btnSave.setOnClickListener {
            val name = binding.etUsername.text.toString().trim()
            val phone = binding.etPhone.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val pwd = binding.etPassword.text.toString().trim()

            if (name.isEmpty() || phone.isEmpty() || email.isEmpty() || pwd.isEmpty()) {
                toast("请完整填写")
                return@setOnClickListener
            }

            val ok = try {
                db.updateUserSelf(uid, name, phone, email, pwd)
            } catch (ex: Exception) {
                false
            }

            if (ok) {
                toast("保存成功")
                finish()
            } else {
                toast("保存失败：手机号/邮箱可能重复")
            }
        }
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
