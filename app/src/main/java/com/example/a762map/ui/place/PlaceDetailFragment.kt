package com.example.a762map.ui.place

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.a762map.R

class PlaceDetailFragment : Fragment(R.layout.fragment_place_detail) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val title = requireArguments().getString("title").orEmpty()
        val lat = requireArguments().getDouble("lat")
        val lng = requireArguments().getDouble("lng")

        view.findViewById<TextView>(R.id.tv_detail).text =
            "地点详情（占位）\n\n标题：$title\n坐标：$lat, $lng\n\n后续：展示详细信息、导航入口、收藏等"

        view.findViewById<Button>(R.id.btn_navigate).setOnClickListener {
            val navController = findNavController()
            val handle = navController.previousBackStackEntry?.savedStateHandle

            // ✅ 使用基础类型回传，避免 SavedStateHandle 崩溃
            handle?.set("nav_title", title)
            handle?.set("nav_lat", lat)
            handle?.set("nav_lng", lng)

            // 返回地图页
            navController.popBackStack()
        }
    }
}
