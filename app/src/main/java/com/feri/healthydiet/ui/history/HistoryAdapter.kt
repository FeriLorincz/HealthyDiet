package com.feri.healthydiet.ui.history

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.feri.healthydiet.data.model.AnalysisHistory
import com.feri.healthydiet.data.model.AnalysisType
import com.feri.healthydiet.databinding.ItemHistoryBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryAdapter(
    private val onItemClick: (AnalysisHistory) -> Unit
) : ListAdapter<AnalysisHistory, HistoryAdapter.ViewHolder>(HistoryDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHistoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), onItemClick)
    }

    class ViewHolder(
        private val binding: ItemHistoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: AnalysisHistory, onItemClick: (AnalysisHistory) -> Unit) {
            binding.tvItemName.text = item.name
            binding.tvItemType.text = when (item.type) {
                AnalysisType.MENU -> "Menu Analysis"
                AnalysisType.FOOD_ITEM -> "Food Item Analysis"
            }

            val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
            binding.tvDate.text = dateFormat.format(Date(item.createdAt))

            binding.root.setOnClickListener {
                onItemClick(item)
            }
        }
    }
}

class HistoryDiffCallback : DiffUtil.ItemCallback<AnalysisHistory>() {
    override fun areItemsTheSame(oldItem: AnalysisHistory, newItem: AnalysisHistory): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: AnalysisHistory, newItem: AnalysisHistory): Boolean {
        return oldItem == newItem
    }
}