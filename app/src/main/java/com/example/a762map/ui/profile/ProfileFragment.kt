package com.example.a762map.ui.profile

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.a762map.R

class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private lateinit var tvUsername: TextView
    private lateinit var tvPhone: TextView
    private lateinit var tvTotalMileage: TextView
    private lateinit var tvNavCount: TextView
    private lateinit var tvYearMileage: TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 绑定控件
        tvUsername = view.findViewById(R.id.tvUsername)
        tvPhone = view.findViewById(R.id.tvPhone)
        tvTotalMileage = view.findViewById(R.id.tvTotalMileage)
        tvNavCount = view.findViewById(R.id.tvNavCount)
        tvYearMileage = view.findViewById(R.id.tvYearMileage)

        val cardUser = view.findViewById<View>(R.id.cardUser)
        val cardFoot = view.findViewById<View>(R.id.cardFootprint)
        val cardReport = view.findViewById<View>(R.id.cardReport)
        val cardVip = view.findViewById<View>(R.id.cardVip)

        // 1) 刷新页面显示（根据当前用户状态）
        render()

        // 2) 点击：用户信息白框 -> 跳转子页面
        cardUser.setOnClickListener {
            startActivity(Intent(requireContext(), UserDetailActivity::class.java))
        }

        // 3) 点击：足迹
        cardFoot.setOnClickListener {
            if (UserSession.isGuest(requireContext())) {
                toast("请登录！")
            } else {
                startActivity(Intent(requireContext(), FootprintActivity::class.java))
            }
        }

        // 4) 点击：年度报告
        cardReport.setOnClickListener {
            if (UserSession.isGuest(requireContext())) {
                toast("请登录！")
            } else {
                startActivity(Intent(requireContext(), YearReportActivity::class.java))
            }
        }

        // 5) 点击：VIP专属导出PDF
        cardVip.setOnClickListener {
            if (UserSession.isVip(requireContext())) {
                startActivity(Intent(requireContext(), ExportPdfActivity::class.java))
            } else {
                toast("请开启VIP！")
            }
        }

        // ✅（可选）你为了调试“三个用户切换”
        // 长按用户名可切换：游客 -> 普通 -> VIP
        tvUsername.setOnLongClickListener {
            UserSession.cycleRole(requireContext())
            render()
            true
        }
    }

    private fun render() {
        val ctx = requireContext()
        val role = UserSession.getRole(ctx)

        if (role == Role.GUEST) {
            tvUsername.text = "用户"
            tvPhone.text = "游客（未登录）"
            tvTotalMileage.text = "--"
            tvNavCount.text = "导航次数\n--"
            tvYearMileage.text = "年度里程\n--"
        } else {
            // 这里用模拟数据，你接入 Room 后从数据库读即可
            val u = UserSession.getUser(ctx)
            tvUsername.text = if (role == Role.VIP) "${u.username}（VIP）" else u.username
            tvPhone.text = maskPhone(u.phone)

            tvTotalMileage.text = "${u.totalMileageKm}km"
            tvNavCount.text = "导航次数\n${u.navCount}次"
            tvYearMileage.text = "年度里程\n${u.yearMileageKm}km"
        }
    }

    private fun maskPhone(phone: String): String {
        if (phone.length < 7) return phone
        return phone.substring(0, 3) + "****" + phone.substring(phone.length - 4)
    }

    private fun toast(msg: String) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
    }
}

/** ====== 下面是“游客/普通/VIP”切换的最小会话层（你之后可替换为 Room） ====== */
enum class Role { GUEST, NORMAL, VIP }

data class SimpleUser(
    val username: String,
    val phone: String,
    val totalMileageKm: Double,
    val yearMileageKm: Int,
    val navCount: Int
)

object UserSession {
    private const val SP = "a762_session"
    private const val KEY_ROLE = "role"

    fun getRole(ctx: Context): Role {
        val sp = ctx.getSharedPreferences(SP, Context.MODE_PRIVATE)
        val v = sp.getString(KEY_ROLE, Role.GUEST.name) ?: Role.GUEST.name
        return runCatching { Role.valueOf(v) }.getOrDefault(Role.GUEST)
    }

    fun isGuest(ctx: Context) = getRole(ctx) == Role.GUEST
    fun isVip(ctx: Context) = getRole(ctx) == Role.VIP

    fun cycleRole(ctx: Context) {
        val next = when (getRole(ctx)) {
            Role.GUEST -> Role.NORMAL
            Role.NORMAL -> Role.VIP
            Role.VIP -> Role.GUEST
        }
        ctx.getSharedPreferences(SP, Context.MODE_PRIVATE)
            .edit().putString(KEY_ROLE, next.name).apply()
        Toast.makeText(ctx, "已切换为：${next.name}", Toast.LENGTH_SHORT).show()
    }

    fun getUser(ctx: Context): SimpleUser {
        // 先给你一套模拟用户数据（后面替换成 Room 查询）
        return when (getRole(ctx)) {
            Role.VIP -> SimpleUser("许飞扬", "18512345678", 9.7, 2, 3)
            Role.NORMAL -> SimpleUser("张扬", "18598765432", 4.2, 1, 1)
            Role.GUEST -> SimpleUser("用户", "00000000000", 0.0, 0, 0)
        }
    }
}
