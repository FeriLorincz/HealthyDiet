package com.feri.healthydiet.ui.menuscan

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.feri.healthydiet.databinding.ItemFoodRecommendationBinding

class FoodRecommendationAdapter(
    private val items: List<FoodRecommendation>
) : RecyclerView.Adapter<FoodRecommendationAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemFoodRecommendationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    class ViewHolder(
        private val binding: ItemFoodRecommendationBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: FoodRecommendation) {
            binding.tvFoodName.text = item.name
            binding.tvReason.text = item.reason
        }
    }
}