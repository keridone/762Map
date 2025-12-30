package com.example.a762map.ui.profile

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.amap.api.location.AMapLocation
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

    private lateinit var locationProvider: RescueLocationProvider

    // ========= 一键救援：收件人邮箱配置（你必须改成真实可用） =========
    private val ADMIN_EMAIL = "2463763422@qq.com"
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
        locationProvider = RescueLocationProvider(requireContext().applicationContext)

        setupClicks()
        refreshUI()
    }

    override fun onResume() {
        super.onResume()
        refreshUI()
    }

    private fun setupClicks() {

        binding.cardUser.setOnClickListener {
            if (!session.isLoggedIn()) {
                goLogin("请先登录")
            } else {
                startActivity(Intent(requireContext(), UserDetailActivity::class.java))
            }
        }

        binding.imgAvatar.setOnClickListener {
            if (!session.isLoggedIn()) {
                goLogin("请先登录")
            } else {
                startActivity(Intent(requireContext(), EditProfileActivity::class.java))
            }
        }

        binding.cardMessage.setOnClickListener {
            if (!session.isLoggedIn()) {
                goLogin("请先登录后查看消息")
                return@setOnClickListener
            }
            Toast.makeText(requireContext(), "消息通知页面尚未接入（请创建 MessageListActivity）", Toast.LENGTH_SHORT).show()
        }

        binding.cardSetting.setOnClickListener {
            Toast.makeText(requireContext(), "系统设置页面尚未接入（请创建 SettingsActivity）", Toast.LENGTH_SHORT).show()
        }

        binding.cardFootprint.setOnClickListener {
            if (!session.isLoggedIn()) {
                goLogin("请登录！")
            } else {
                startActivity(Intent(requireContext(), FootprintActivity::class.java))
            }
        }

        binding.cardReport.setOnClickListener {
            if (!session.isLoggedIn()) {
                goLogin("请登录！")
            } else {
                startActivity(Intent(requireContext(), YearReportActivity::class.java))
            }
        }

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

        // ✅ 一键救援按钮：发送邮件 + 附带当前位置
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

            binding.tvRescueStatus.text = "状态：发送中…"
            binding.btnRescue.isEnabled = false

            lifecycleScope.launch {
                // 1) 先取定位（若无权限/失败则为 null）
                val loc: AMapLocation? = withContext(Dispatchers.IO) { getCurrentLocationIfPermitted() }

                // 2) 再发邮件（IO线程）
                val ok = withContext(Dispatchers.IO) {
                    sendRescueEmail(
                        userName = u.username,
                        userPhone = u.phone,
                        userEmail = u.email ?: "",
                        userId = u.id.toString(),
                        totalMileage = u.totalMileage,
                        yearMileage = u.yearMileage,
                        navCount = u.navCount,
                        vipLevel = u.vipLevel,
                        location = loc
                    )
                }

                binding.btnRescue.isEnabled = true
                binding.tvRescueStatus.text = if (ok) "状态：发送成功 ✅" else "状态：发送失败 ❌（检查网络/邮箱配置/定位权限）"
                Toast.makeText(requireContext(), if (ok) "救援信息发送成功" else "救援信息发送失败", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnRecharge.setOnClickListener {
            if (!session.isLoggedIn()) {
                goLogin("请登录！")
            } else {
                startActivity(Intent(requireContext(), RechargeActivity::class.java))
            }
        }

        binding.btnEditProfile.setOnClickListener {
            if (!session.isLoggedIn()) {
                goLogin("请登录！")
            } else {
                startActivity(Intent(requireContext(), EditProfileActivity::class.java))
            }
        }

        binding.btnAdminManage.setOnClickListener {
            if (!session.isLoggedIn()) {
                goLogin("请登录！")
                return@setOnClickListener
            }
            startActivity(Intent(requireContext(), AdminUserManageActivity::class.java))
        }

        binding.btnLogout.setOnClickListener {
            session.logout()
            Toast.makeText(requireContext(), "已退出登录", Toast.LENGTH_SHORT).show()
            refreshUI()
        }
    }

    private fun refreshUI() {
        if (!session.isLoggedIn()) {
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

                binding.btnRescue.visibility = View.GONE
                binding.tvRescueStatus.visibility = View.GONE
            }

            "vip" -> {
                binding.vipBadge.visibility = View.VISIBLE
                binding.vipBadge.text = "VIP Lv.${u.vipLevel}"

                binding.btnRecharge.visibility = View.GONE
                binding.btnEditProfile.visibility = View.VISIBLE
                binding.btnAdminManage.visibility = View.GONE

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

    private fun format1(v: Double): String = String.format("%.1f", v)

    /**
     * 获取当前定位：无权限 -> null；失败 -> null
     */
    private suspend fun getCurrentLocationIfPermitted(): AMapLocation? {
        val fine = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!fine && !coarse) return null

        return locationProvider.getOnceLocation()
    }

    /**
     * SMTP 实时发送救援邮件：收件人=【用户邮箱 + 管理员邮箱】
     * 增强：包含当前位置（若获取失败则写明“未能获取”）
     */
    private fun sendRescueEmail(
        userName: String,
        userPhone: String,
        userEmail: String,
        userId: String,
        totalMileage: Double,
        yearMileage: Double,
        navCount: Int,
        vipLevel: Int,
        location: AMapLocation?
    ): Boolean {
        return try {
            if (userEmail.isBlank()) return false

            val now = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            val subject = "【A762Map 一键救援】紧急求助 - $userName(${userPhone.maskPhone()})"

            val locationBlock = buildLocationBlock(location)

            val body = """
                【A762Map 一键救援系统 - 紧急求助邮件】
                
                发送时间：$now
                
                【用户身份信息】
                - 用户ID：$userId
                - 用户名：$userName
                - 手机号：$userPhone
                - 用户邮箱：$userEmail
                - 账号类型：VIP Lv.$vipLevel
                
                【当前位置（系统自动获取）】
                $locationBlock
                
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

    private fun buildLocationBlock(loc: AMapLocation?): String {
        if (loc == null) {
            return "- 结果：未能获取当前位置（无权限/定位失败/超时）"
        }

        val lat = loc.latitude
        val lng = loc.longitude
        val acc = loc.accuracy
        val provider = loc.provider ?: "unknown"
        val addr = listOfNotNull(
            loc.country, loc.province, loc.city, loc.district, loc.street, loc.streetNum, loc.aoiName
        ).joinToString("")

        val time = try {
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(loc.time))
        } catch (_: Exception) {
            "unknown"
        }

        // 高德 Web 打开链接（仅文本；便于救援人员复制）
        val amapLink = "https://uri.amap.com/marker?position=$lng,$lat&name=${java.net.URLEncoder.encode("求助位置", "UTF-8")}"

        return """
            - 坐标：$lat, $lng
            - 精度：${acc}m
            - 来源：$provider
            - 时间：$time
            - 地址：${if (addr.isBlank()) "（无地址信息）" else addr}
            - 链接：$amapLink
        """.trimIndent()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
