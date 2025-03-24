package com.feri.healthydiet.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.feri.healthydiet.data.model.AnalysisType
import com.feri.healthydiet.databinding.FragmentHistoryDetailBinding
import com.feri.healthydiet.ui.foodanalyzer.FoodAnalysis
import com.feri.healthydiet.ui.menuscan.FoodRecommendationAdapter
import com.feri.healthydiet.ui.menuscan.MenuAnalysisResult
import com.google.gson.Gson
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryDetailFragment : Fragment() {

    private var _binding: FragmentHistoryDetailBinding? = null
    private val binding get() = _binding!!

    private val args by lazy { HistoryDetailFragmentArgs.fromBundle(requireArguments()) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHistoryDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val historyItem = args.historyItem

        binding.tvTitle.text = historyItem.name
        val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        binding.tvDate.text = dateFormat.format(Date(historyItem.createdAt))

        when (historyItem.type) {
            AnalysisType.FOOD_ITEM -> displayFoodAnalysis(historyItem.content)
            AnalysisType.MENU -> displayMenuAnalysis(historyItem.content)
        }
    }

    private fun displayFoodAnalysis(content: String) {
        try {
            val analysis = Gson().fromJson(content, FoodAnalysis::class.java)

            binding.layoutFoodAnalysis.visibility = View.VISIBLE
            binding.layoutMenuAnalysis.visibility = View.GONE

            binding.tvCategory.text = "Category: ${analysis.category}"
            binding.tvProtein.text = "${analysis.protein}g"
            binding.tvCarbs.text = "${analysis.carbs}g"
            binding.tvFats.text = "${analysis.fats}g"

            binding.linearHealthImpacts.removeAllViews()
            for (impact in analysis.healthImpacts) {
                val impactView = layoutInflater.inflate(
                    android.R.layout.simple_list_item_1,
                    binding.linearHealthImpacts,
                    false
                )
                impactView.findViewById<TextView>(android.R.id.text1).text = "• $impact"
                binding.linearHealthImpacts.addView(impactView)
            }

        } catch (e: Exception) {
            binding.tvError.visibility = View.VISIBLE
            binding.tvError.text = "Error displaying analysis: ${e.message}"
        }
    }

    private fun displayMenuAnalysis(content: String) {
        try {
            val analysis = Gson().fromJson(content, MenuAnalysisResult::class.java)

            binding.layoutFoodAnalysis.visibility = View.GONE
            binding.layoutMenuAnalysis.visibility = View.VISIBLE

            binding.rvRecommended.adapter = FoodRecommendationAdapter(analysis.recommended)
            binding.rvAvoid.adapter = FoodRecommendationAdapter(analysis.avoid)
            binding.rvModerate.adapter = FoodRecommendationAdapter(analysis.moderate)

        } catch (e: Exception) {
            binding.tvError.visibility = View.VISIBLE
            binding.tvError.text = "Error displaying analysis: ${e.message}"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}