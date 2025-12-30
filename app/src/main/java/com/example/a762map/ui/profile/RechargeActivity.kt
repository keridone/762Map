package com.example.a762map.ui.profile

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.a762map.databinding.ActivityRechargeBinding

class RechargeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRechargeBinding
    private lateinit var db: AppDbHelper
    private lateinit var session: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRechargeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = AppDbHelper(this)
        session = SessionManager(this)

        binding.btnPay10.setOnClickListener {
            val uid = session.getUserId()
            if (uid <= 0) {
                toast("请先登录"); finish(); return@setOnClickListener
            }
            val ok = db.upgradeToVip(uid, vipLevel = 1)
            if (ok) {
                toast("充值成功，已升级 VIP！")
                finish()
            } else toast("充值失败")
        }
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
