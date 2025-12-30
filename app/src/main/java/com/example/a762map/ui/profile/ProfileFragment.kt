package com.example.a762map.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.a762map.R
import com.example.a762map.databinding.FragmentProfileBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Properties
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var db: AppDbHelper
    private lateinit var session: SessionManager

    // ========= 一键救援：收件人邮箱配置（你必须改成真实可用） =========
    private val ADMIN_EMAIL = "2463763422@qq.com"

    // 建议新建“救援专用邮箱”，并使用“应用专用密码/授权码”
    // Gmail: smtp.gmail.com:587 (STARTTLS)
    // QQ: smtp.qq.com:587 (STARTTLS) 或 465(SSL)
    private val SMTP_HOST = "smtp.qq.com"
    private val SMTP_PORT = "587"
    private val SENDER_EMAIL = "2696973787@qq.com"
    private val SENDER_APP_PASSWORD = "gtwqdumehxkldgdf"
    // =====================================================

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentProfileBinding.bind(view)

        db = AppDbHelper(requireContext())
        session = SessionManager(requireContext())

        setupClicks()
        refreshUI()
    }

    override fun onResume() {
        super.onResume()
        refreshUI()
    }

    private fun setupClicks() {

        // ① 用户卡：游客 -> 登录；登录 -> 用户详情/修改信息
        binding.cardUser.setOnClickListener {
            if (!session.isLoggedIn()) {
                goLogin("请先登录")
            } else {
                startActivity(Intent(requireContext(), UserDetailActivity::class.java))
            }
        }

        // 头像单独点击（登录后可做更换头像）
        binding.imgAvatar.setOnClickListener {
            if (!session.isLoggedIn()) {
                goLogin("请先登录")
            } else {
                startActivity(Intent(requireContext(), EditProfileActivity::class.java))
            }
        }

        // ② 消息通知
        binding.cardMessage.setOnClickListener {
            if (!session.isLoggedIn()) {
                goLogin("请先登录后查看消息")
                return@setOnClickListener
            }
            Toast.makeText(requireContext(), "消息通知页面尚未接入（请创建 MessageListActivity）", Toast.LENGTH_SHORT).show()
        }

        // ③ 系统设置
        binding.cardSetting.setOnClickListener {
            Toast.makeText(requireContext(), "系统设置页面尚未接入（请创建 SettingsActivity）", Toast.LENGTH_SHORT).show()
        }

        // ④ 足迹：游客拦截
        binding.cardFootprint.setOnClickListener {
            if (!session.isLoggedIn()) {
                goLogin("请登录！")
            } else {
                startActivity(Intent(requireContext(), FootprintActivity::class.java))
            }
        }

        // ⑤ 年度报告：游客拦截
        binding.cardReport.setOnClickListener {
            if (!session.isLoggedIn()) {
                goLogin("请登录！")
            } else {
                startActivity(Intent(requireContext(), YearReportActivity::class.java))
            }
        }

        // ⑥ VIP 专属：改为“一键救援”
        // 卡片点击：如果你想卡片本身也触发救援，可保留；我这里让卡片点击只提示
        binding.cardVip.setOnClickListener {
            if (!session.isLoggedIn()) {
                goLogin("请登录！")
                return@setOnClickListener
            }
            val u = db.getUserById(session.getUserId()) ?: run {
                session.logout()
                refreshUI()
                return@setOnClickListener
            }
            if (u.role != "vip") {
                Toast.makeText(requireContext(), "请开启VIP！", Toast.LENGTH_SHORT).show()
                startActivity(Intent(requireContext(), RechargeActivity::class.java))
            } else {
                Toast.makeText(requireContext(), "VIP 专属：点击“一键救援”按钮发送救援信息", Toast.LENGTH_SHORT).show()
            }
        }

        // ✅ 一键救援按钮（必须在 fragment_profile.xml 里存在 btnRescue）
        binding.btnRescue.setOnClickListener {
            if (!session.isLoggedIn()) {
                goLogin("请先登录！")
                return@setOnClickListener
            }

            val u = db.getUserById(session.getUserId())
            if (u == null) {
                session.logout()
                refreshUI()
                return@setOnClickListener
            }

            if (u.role != "vip") {
                Toast.makeText(requireContext(), "请开启VIP后使用一键救援！", Toast.LENGTH_SHORT).show()
                startActivity(Intent(requireContext(), RechargeActivity::class.java))
                return@setOnClickListener
            }

            // 开始发送
            binding.tvRescueStatus.text = "状态：发送中…"
            binding.btnRescue.isEnabled = false

            lifecycleScope.launch {
                val ok = withContext(Dispatchers.IO) {
                    sendRescueEmail(
                        userName = u.username,
                        userPhone = u.phone,
                        userEmail = u.email ?: "",
                        userId = u.id.toString(),
                        totalMileage = u.totalMileage,
                        yearMileage = u.yearMileage,
                        navCount = u.navCount,
                        vipLevel = u.vipLevel
                    )
                }

                binding.btnRescue.isEnabled = true
                binding.tvRescueStatus.text = if (ok) "状态：发送成功 ✅" else "状态：发送失败 ❌（检查网络/邮箱配置）"
                Toast.makeText(requireContext(), if (ok) "救援信息发送成功" else "救援信息发送失败", Toast.LENGTH_SHORT).show()
            }
        }

        // 充值（普通用户可见）
        binding.btnRecharge.setOnClickListener {
            if (!session.isLoggedIn()) {
                goLogin("请登录！")
            } else {
                startActivity(Intent(requireContext(), RechargeActivity::class.java))
            }
        }

        // 修改个人信息（普通/VIP 可见）
        binding.btnEditProfile.setOnClickListener {
            if (!session.isLoggedIn()) {
                goLogin("请登录！")
            } else {
                startActivity(Intent(requireContext(), EditProfileActivity::class.java))
            }
        }

        // 管理员：用户管理
        binding.btnAdminManage.setOnClickListener {
            if (!session.isLoggedIn()) {
                goLogin("请登录！")
                return@setOnClickListener
            }
            startActivity(Intent(requireContext(), AdminUserManageActivity::class.java))
        }

        // 退出登录
        binding.btnLogout.setOnClickListener {
            session.logout()
            Toast.makeText(requireContext(), "已退出登录", Toast.LENGTH_SHORT).show()
            refreshUI()
        }
    }

    private fun refreshUI() {
        if (!session.isLoggedIn()) {
            // ===== 游客状态 =====
            binding.tvUsername.text = "游客"
            binding.tvPhone.text = "点击登录"
            binding.tvTotalMileage.text = "--"
            binding.tvNavCount.text = "导航次数\n--"
            binding.tvYearMileage.text = "年度里程\n--"

            binding.imgAvatar.setImageResource(R.drawable.ic_avatar_default)

            binding.vipBadge.visibility = View.GONE

            binding.btnRecharge.visibility = View.GONE
            binding.btnEditProfile.visibility = View.GONE
            binding.btnAdminManage.visibility = View.GONE
            binding.btnLogout.visibility = View.GONE

            // 救援区：游客隐藏按钮/状态（或你也可以显示但点击就提示登录）
            binding.btnRescue.visibility = View.GONE
            binding.tvRescueStatus.visibility = View.GONE
            return
        }

        val uid = session.getUserId()
        val u = db.getUserById(uid) ?: run {
            session.logout()
            refreshUI()
            return
        }

        // ===== 已登录状态 =====
        binding.tvUsername.text = u.username
        binding.tvPhone.text = u.phone.maskPhone()
        binding.tvTotalMileage.text = "${format1(u.totalMileage)} km"
        binding.tvNavCount.text = "导航次数\n${u.navCount}次"
        binding.tvYearMileage.text = "年度里程\n${format1(u.yearMileage)} km"

        binding.imgAvatar.setImageResource(R.drawable.ic_avatar_default)

        binding.btnLogout.visibility = View.VISIBLE

        when (u.role) {
            "normal" -> {
                binding.vipBadge.visibility = View.GONE
                binding.btnRecharge.visibility = View.VISIBLE
                binding.btnEditProfile.visibility = View.VISIBLE
                binding.btnAdminManage.visibility = View.GONE

                // 普通用户不显示救援按钮
                binding.btnRescue.visibility = View.GONE
                binding.tvRescueStatus.visibility = View.GONE
            }

            "vip" -> {
                binding.vipBadge.visibility = View.VISIBLE
                binding.vipBadge.text = "VIP Lv.${u.vipLevel}"

                binding.btnRecharge.visibility = View.GONE
                binding.btnEditProfile.visibility = View.VISIBLE
                binding.btnAdminManage.visibility = View.GONE

                // VIP 显示救援按钮
                binding.btnRescue.visibility = View.VISIBLE
                binding.tvRescueStatus.visibility = View.VISIBLE
                binding.tvRescueStatus.text = "状态：未发送"
                binding.btnRescue.isEnabled = true
            }

            "admin" -> {
                binding.vipBadge.visibility = View.VISIBLE
                binding.vipBadge.text = "管理员"

                binding.btnRecharge.visibility = View.GONE
                binding.btnEditProfile.visibility = View.GONE
                binding.btnAdminManage.visibility = View.VISIBLE

                // 管理员不需要救援按钮（你也可以改成可用）
                binding.btnRescue.visibility = View.GONE
                binding.tvRescueStatus.visibility = View.GONE
            }

            else -> {
                binding.vipBadge.visibility = View.GONE
                binding.btnRecharge.visibility = View.GONE
                binding.btnEditProfile.visibility = View.GONE
                binding.btnAdminManage.visibility = View.GONE

                binding.btnRescue.visibility = View.GONE
                binding.tvRescueStatus.visibility = View.GONE
            }
        }
    }

    private fun goLogin(msg: String) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
        startActivity(Intent(requireContext(), LoginActivity::class.java))
    }

    private fun String.maskPhone(): String {
        if (length < 7) return this
        return substring(0, 3) + "****" + substring(length - 4)
    }

    private fun format1(v: Double): String {
        return String.format("%.1f", v)
    }

    /**
     * ✅ SMTP 实时发送救援邮件：收件人=【用户邮箱 + 管理员邮箱】
     * 返回 true/false 用于 UI 显示发送成功/失败
     */
    private fun sendRescueEmail(
        userName: String,
        userPhone: String,
        userEmail: String,
        userId: String,
        totalMileage: Double,
        yearMileage: Double,
        navCount: Int,
        vipLevel: Int
    ): Boolean {
        return try {
            if (userEmail.isBlank()) return false

            val now = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            val subject = "【A762Map 一键救援】紧急求助 - $userName(${userPhone.maskPhone()})"

            val body = """
                【A762Map 一键救援系统 - 紧急求助邮件】
                
                发送时间：$now
                
                【用户身份信息】
                - 用户ID：$userId
                - 用户名：$userName
                - 手机号：$userPhone
                - 用户邮箱：$userEmail
                - 账号类型：VIP Lv.$vipLevel
                
                【使用统计（辅助判断是否异常）】
                - 总里程：${format1(totalMileage)} km
                - 本年里程：${format1(yearMileage)} km
                - 本年导航次数：$navCount 次
                
                【可能的紧急情况（系统自动提示）】
                1) 用户可能迷路/失联/遇到突发身体不适；
                2) 可能发生交通事故或被困；
                3) 设备异常导致无法继续导航；
                
                【建议处置】
                - 管理员：请立即拨打用户电话确认情况；
                - 若无法联系：建议联系紧急联系人/学校安保/报警；
                - 建议保留此邮件作为救援记录；
                
                —— A762Map 救援系统
            """.trimIndent()

            val props = Properties().apply {
                put("mail.smtp.auth", "true")
                put("mail.smtp.starttls.enable", "true")
                put("mail.smtp.host", SMTP_HOST)
                put("mail.smtp.port", SMTP_PORT)
            }

            val mailSession = Session.getInstance(props, object : Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication {
                    return PasswordAuthentication(SENDER_EMAIL, SENDER_APP_PASSWORD)
                }
            })

            val message = MimeMessage(mailSession).apply {
                setFrom(InternetAddress(SENDER_EMAIL))
                setRecipients(
                    Message.RecipientType.TO,
                    InternetAddress.parse("$userEmail,$ADMIN_EMAIL")
                )
                this.subject = subject
                setText(body)
            }

            Transport.send(message)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
