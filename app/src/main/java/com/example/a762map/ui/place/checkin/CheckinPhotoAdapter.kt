package com.example.a762map.ui.place.checkin

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.a762map.R
import com.example.a762map.data.checkin.CheckinPhoto
import java.io.File

class CheckinPhotoAdapter(
    private val onClick: (CheckinPhoto) -> Unit
) : ListAdapter<CheckinPhoto, CheckinPhotoAdapter.VH>(DIFF) {

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val iv: ImageView = itemView.findViewById(R.id.iv_checkin_thumb)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_checkin_photo, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        holder.itemView.setOnClickListener { onClick(item) }

        val f = File(item.filePath)
        if (f.exists()) {
            // 简单缩略图解码（可用；后续想优化可换 Coil/Glide 或加采样）
            val bmp = BitmapFactory.decodeFile(f.absolutePath)
            holder.iv.setImageBitmap(bmp)
        } else {
            holder.iv.setImageResource(android.R.drawable.ic_menu_report_image)
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<CheckinPhoto>() {
            override fun areItemsTheSame(oldItem: CheckinPhoto, newItem: CheckinPhoto) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: CheckinPhoto, newItem: CheckinPhoto) =
                oldItem == newItem
        }
    }
}
