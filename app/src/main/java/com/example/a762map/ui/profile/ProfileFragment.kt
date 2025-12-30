package com.example.a762map.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.a762map.R
import com.example.a762map.databinding.FragmentProfileBinding

class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var db: AppDbHelper
    private lateinit var session: SessionManager

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentProfileBinding.bind(view)

        db = AppDbHelper(requireContext())
        session = SessionManager(requireContext())

        setupClicks()
    }

    override fun onResume() {
        super.onResume()
        refreshUI()
    }

    private fun setupClicks() {
        // 头像/用户卡
        binding.cardUser.setOnClickListener {
            if (!session.isLoggedIn()) {
                goLogin("请登录！")
            } else {
                startActivity(Intent(requireContext(), UserDetailActivity::class.java))
            }
        }

        binding.cardFootprint.setOnClickListener {
            if (!session.isLoggedIn()) goLogin("请登录！")
            else startActivity(Intent(requireContext(), FootprintActivity::class.java))
        }

        binding.cardReport.setOnClickListener {
            if (!session.isLoggedIn()) goLogin("请登录！")
            else startActivity(Intent(requireContext(), YearReportActivity::class.java))
        }

        binding.cardVip.setOnClickListener {
            if (!session.isLoggedIn()) {
                goLogin("请登录！")
                return@setOnClickListener
            }
            val u = db.getUserById(session.getUserId()) ?: return@setOnClickListener
            if (u.role != "vip") {
                Toast.makeText(requireContext(), "请开启VIP！", Toast.LENGTH_SHORT).show()
                // 普通用户点击VIP区 -> 去充值
                startActivity(Intent(requireContext(), RechargeActivity::class.java))
            } else {
                startActivity(Intent(requireContext(), ExportPdfActivity::class.java))
            }
        }

        // 你需要在 fragment_profile.xml 增加两个按钮：
        // btnEditProfile, btnLogout, 以及 admin按钮 btnAdminManage, 普通用户充值按钮 btnRecharge
        binding.btnLogout.setOnClickListener {
            session.logout()
            Toast.makeText(requireContext(), "已退出登录", Toast.LENGTH_SHORT).show()
            refreshUI()
        }

        binding.btnEditProfile.setOnClickListener {
            if (!session.isLoggedIn()) goLogin("请登录！")
            else startActivity(Intent(requireContext(), EditProfileActivity::class.java))
        }

        binding.btnRecharge.setOnClickListener {
            if (!session.isLoggedIn()) goLogin("请登录！")
            else startActivity(Intent(requireContext(), RechargeActivity::class.java))
        }

        binding.btnAdminManage.setOnClickListener {
            if (!session.isLoggedIn()) goLogin("请登录！")
            else startActivity(Intent(requireContext(), AdminUserManageActivity::class.java))
        }
    }

    private fun refreshUI() {
        if (!session.isLoggedIn()) {
            // 游客 UI
            binding.tvUsername.text = "游客"
            binding.tvPhone.text = "点击登录"
            binding.tvTotalMileage.text = "--"
            binding.tvNavCount.text = "导航次数\n--"
            binding.tvYearMileage.text = "年度里程\n--"

            binding.vipBadge.visibility = View.GONE
            binding.btnRecharge.visibility = View.GONE
            binding.btnEditProfile.visibility = View.GONE
            binding.btnLogout.visibility = View.GONE
            binding.btnAdminManage.visibility = View.GONE
            return
        }

        val u = db.getUserById(session.getUserId()) ?: run {
            session.logout()
            refreshUI()
            return
        }

        binding.tvUsername.text = u.username
        binding.tvPhone.text = u.phone.maskPhone()
        binding.tvTotalMileage.text = "${u.totalMileage}km"
        binding.tvNavCount.text = "导航次数\n${u.navCount}次"
        binding.tvYearMileage.text = "年度里程\n${u.yearMileage}km"

        binding.btnLogout.visibility = View.VISIBLE

        when (u.role) {
            "normal" -> {
                binding.vipBadge.visibility = View.GONE
                binding.btnRecharge.visibility = View.VISIBLE
                binding.btnEditProfile.visibility = View.VISIBLE
                binding.btnAdminManage.visibility = View.GONE
            }
            "vip" -> {
                binding.vipBadge.visibility = View.VISIBLE
                binding.vipBadge.text = "VIP Lv.${u.vipLevel}"
                binding.btnRecharge.visibility = View.GONE
                binding.btnEditProfile.visibility = View.VISIBLE
                binding.btnAdminManage.visibility = View.GONE
            }
            "admin" -> {
                binding.vipBadge.visibility = View.VISIBLE
                binding.vipBadge.text = "管理员"
                binding.btnRecharge.visibility = View.GONE
                binding.btnEditProfile.visibility = View.GONE
                binding.btnAdminManage.visibility = View.VISIBLE
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
