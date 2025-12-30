package com.example.a762map.ui.profile

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.a762map.databinding.ActivityAdminUserManageBinding

class AdminUserManageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminUserManageBinding
    private lateinit var db: AppDbHelper
    private lateinit var session: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminUserManageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = AppDbHelper(this)
        session = SessionManager(this)

        val me = db.getUserById(session.getUserId())
        if (me?.role != "admin") {
            toast("无权限")
            finish()
            return
        }

        fun refresh() {
            val role = when {
                binding.rbNormal.isChecked -> "normal"
                binding.rbVip.isChecked -> "vip"
                else -> null
            }

            val list = db.queryUsers(role)
            binding.tvList.text = buildString {
                append("共 ${list.size} 人\n\n")
                list.forEach { u ->
                    append("ID=${u.id}  ${u.username}  ${u.phone}  ${u.role}  vipLv=${u.vipLevel}  年里程=${u.yearMileage}  导航=${u.navCount}\n")
                }
            }
        }

        binding.rgFilter.setOnCheckedChangeListener { _, _ -> refresh() }

        // 新增
        binding.btnAdd.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val phone = binding.etPhone.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val pwd = binding.etPwd.text.toString().trim()
            val role = binding.etRole.text.toString().trim().ifEmpty { "normal" }
            val vipLv = binding.etVipLevel.text.toString().trim().toIntOrNull() ?: 0

            if (name.isEmpty() || phone.isEmpty() || email.isEmpty() || pwd.isEmpty()) {
                toast("请完整填写")
                return@setOnClickListener
            }

            val id = try {
                db.adminAddUser(
                    User(
                        username = name,
                        phone = phone,
                        email = email,
                        password = pwd,
                        avatarRes = android.R.drawable.sym_def_app_icon,
                        avatarUri = null,
                        role = role,
                        vipLevel = vipLv,
                        totalMileage = 0.0,
                        yearMileage = 0.0,
                        navCount = 0,
                        litCities = 0
                    )
                )
            } catch (_: Exception) {
                -1L
            }

            toast(if (id > 0) "新增成功 id=$id" else "新增失败：手机号/邮箱可能重复")
            refresh()
        }

        // 修改（按ID）
        binding.btnUpdate.setOnClickListener {
            val id = binding.etId.text.toString().trim().toLongOrNull()
            if (id == null) { toast("请输入ID"); return@setOnClickListener }

            val old = db.getUserById(id)
            if (old == null) { toast("用户不存在"); return@setOnClickListener }

            val newName = binding.etName.text.toString().trim().ifEmpty { old.username }
            val newPhone = binding.etPhone.text.toString().trim().ifEmpty { old.phone }
            val newEmail = binding.etEmail.text.toString().trim().ifEmpty { old.email }
            val newPwd = binding.etPwd.text.toString().trim().ifEmpty { old.password }
            val newRole = binding.etRole.text.toString().trim().ifEmpty { old.role }
            val newVip = binding.etVipLevel.text.toString().trim().toIntOrNull() ?: old.vipLevel

            val ok = try {
                db.adminUpdateUser(
                    old.copy(
                        username = newName,
                        phone = newPhone,
                        email = newEmail,
                        password = newPwd,
                        role = newRole,
                        vipLevel = newVip
                    )
                )
            } catch (_: Exception) {
                false
            }

            toast(if (ok) "修改成功" else "修改失败（可能手机号/邮箱重复）")
            refresh()
        }

        // 删除（按ID）
        binding.btnDelete.setOnClickListener {
            val id = binding.etId.text.toString().trim().toLongOrNull()
            if (id == null) { toast("请输入ID"); return@setOnClickListener }

            val ok = db.adminDeleteUser(id)
            toast(if (ok) "删除成功" else "删除失败")
            refresh()
        }

        // 批量删除 VIP
        binding.btnDeleteAllVip.setOnClickListener {
            val count = db.adminDeleteAllVip()
            toast("已删除 VIP 用户：$count 个")
            refresh()
        }

        refresh()
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
