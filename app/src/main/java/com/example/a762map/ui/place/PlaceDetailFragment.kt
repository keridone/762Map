package com.example.a762map.ui.place

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.a762map.R
import com.example.a762map.data.checkin.ImageStore
import com.example.a762map.data.checkin.PlaceCheckinRepository
import com.example.a762map.ui.place.checkin.CheckinPhotoAdapter

class PlaceDetailFragment : Fragment(R.layout.fragment_place_detail) {

    private lateinit var repo: PlaceCheckinRepository
    private lateinit var imageStore: ImageStore

    private lateinit var adapter: CheckinPhotoAdapter

    private var placeTitle: String = ""
    private var placeLat: Double = Double.NaN
    private var placeLng: Double = Double.NaN
    private val placeKey: String
        get() = "${placeLat},${placeLng}"

    // 拍照输出 Uri（FileProvider）
    private var pendingCameraOutputUri: Uri? = null

    // 相册选择（单张；你后续想多选可改为 OpenMultipleDocuments）
    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri == null) return@registerForActivityResult
            val ctx = context ?: return@registerForActivityResult

            val savedPath = imageStore.copyFromUriToCheckins(uri)
            if (savedPath != null) {
                repo.insert(placeKey, placeTitle, savedPath)
                refreshCheckins()
            }
        }

    // 相机权限
    private val requestCameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                launchCamera()
            }
        }

    // 拍照（TakePicture 会把图片写入我们提供的 Uri）
    private val takePictureLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { ok: Boolean ->
            if (!ok) return@registerForActivityResult
            val uri = pendingCameraOutputUri ?: return@registerForActivityResult

            val path = imageStore.resolvePathFromUri(uri)
            if (path != null) {
                repo.insert(placeKey, placeTitle, path)
                refreshCheckins()
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        placeTitle = requireArguments().getString("title").orEmpty()
        placeLat = requireArguments().getDouble("lat")
        placeLng = requireArguments().getDouble("lng")

        repo = PlaceCheckinRepository(requireContext().applicationContext)
        imageStore = ImageStore(requireContext().applicationContext)

        // 顶部占位文本
        view.findViewById<TextView>(R.id.tv_detail).text =
            "地点详情\n\n标题：$placeTitle\n坐标：$placeLat, $placeLng"

        // 导航按钮（你现有逻辑保持不变）
        view.findViewById<Button>(R.id.btn_navigate).setOnClickListener {
            val navController = findNavController()
            val handle = navController.previousBackStackEntry?.savedStateHandle

            handle?.set("nav_title", placeTitle)
            handle?.set("nav_lat", placeLat)
            handle?.set("nav_lng", placeLng)

            navController.popBackStack()
        }

        // 打卡栏：RecyclerView
        val rv = view.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rv_checkins)
        adapter = CheckinPhotoAdapter { photo ->
            // 第一版：点击缩略图仅用系统查看器打开
            val uri = imageStore.filePathToContentUri(photo.filePath)
            if (uri != null) {
                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "image/*")
                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(intent)
            }
        }
        rv.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        rv.adapter = adapter

        // 打卡栏：按钮
        view.findViewById<ImageButton>(R.id.btn_checkin_pick).setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        view.findViewById<ImageButton>(R.id.btn_checkin_camera).setOnClickListener {
            ensureCameraPermissionThenLaunch()
        }

        // 初次加载
        refreshCheckins()
    }

    private fun refreshCheckins() {
        val list = repo.list(placeKey)
        adapter.submitList(list)

        // 空态提示
        val tvEmpty = view?.findViewById<TextView>(R.id.tv_checkin_empty)
        tvEmpty?.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun ensureCameraPermissionThenLaunch() {
        val granted = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (granted) {
            launchCamera()
        } else {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun launchCamera() {
        val outputUri = imageStore.createNewCheckinImageUri()
        if (outputUri == null) return

        pendingCameraOutputUri = outputUri
        takePictureLauncher.launch(outputUri)
    }
}
