package com.example.a762map.ui.map

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.a762map.R

class SuggestionAdapter(
    private val onClick: (SuggestionItem) -> Unit
) : RecyclerView.Adapter<SuggestionAdapter.VH>() {

    private val data = mutableListOf<SuggestionItem>()

    fun submitList(newList: List<SuggestionItem>) {
        data.clear()
        data.addAll(newList)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_suggestion, parent, false)
        return VH(v, onClick)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(data[position])
    }

    override fun getItemCount(): Int = data.size

    class VH(itemView: View, val onClick: (SuggestionItem) -> Unit) :
        RecyclerView.ViewHolder(itemView) {
        private val tvTitle: TextView = itemView.findViewById(R.id.tv_title)
        private val tvSubtitle: TextView = itemView.findViewById(R.id.tv_subtitle)

        fun bind(item: SuggestionItem) {
            tvTitle.text = item.title
            tvSubtitle.text = item.subtitle
            itemView.setOnClickListener { onClick(item) }
        }
    }
}
