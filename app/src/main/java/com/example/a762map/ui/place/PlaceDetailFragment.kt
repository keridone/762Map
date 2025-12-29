package com.example.a762map.ui.place

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.a762map.R

class PlaceDetailFragment : Fragment(R.layout.fragment_place_detail) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val title = requireArguments().getString("title").orEmpty()
        val lat = requireArguments().getDouble("lat")
        val lng = requireArguments().getDouble("lng")

        view.findViewById<TextView>(R.id.tv_detail).text =
            "地点详情（占位）\n\n标题：$title\n坐标：$lat, $lng\n\n后续：展示详细信息、导航入口、收藏等"
    }
}
