package com.feri.healthydiet.ui.menuscan

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.feri.healthydiet.databinding.FragmentResultsBinding
import com.feri.healthydiet.util.VoiceAssistantHelper
import kotlinx.coroutines.launch

class ResultsFragment : Fragment() {
    private var _binding: FragmentResultsBinding? = null
    private val binding get() = _binding!!

    private val args: ResultsFragmentArgs by navArgs()
    private lateinit var voiceAssistantHelper: VoiceAssistantHelper
    private var isSpeaking = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        voiceAssistantHelper = VoiceAssistantHelper(requireContext())
        lifecycleScope.launch {
            voiceAssistantHelper.initialize()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentResultsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val result = args.analysisResult

        // Set up the recommended items
        binding.rvRecommended.adapter = FoodRecommendationAdapter(result.recommended)

        // Set up the avoid items
        binding.rvAvoid.adapter = FoodRecommendationAdapter(result.avoid)

        // Set up the moderate items
        binding.rvModerate.adapter = FoodRecommendationAdapter(result.moderate)

        binding.btnReadResults.setOnClickListener {
            if (!isSpeaking) {
                speakResults()
            } else {
                voiceAssistantHelper.stop()
                binding.btnReadResults.text = "Read Results"
                isSpeaking = false
            }
        }

        binding.btnSaveToHistory.setOnClickListener {
            // Save to history implementation
            findNavController().popBackStack()
        }
    }

    private fun speakResults() {
        val result = args.analysisResult
        lifecycleScope.launch {
            binding.btnReadResults.text = "Stop Reading"
            isSpeaking = true

            val recommendedText = "Recommended foods: " +
                    result.recommended.joinToString(", ") { it.name }

            val avoidText = "Foods to avoid: " +
                    result.avoid.joinToString(", ") { it.name }

            val moderateText = "Foods to consume in moderation: " +
                    result.moderate.joinToString(", ") { it.name }

            val fullText = "$recommendedText. $avoidText. $moderateText."

            voiceAssistantHelper.speak(fullText)

            binding.btnReadResults.text = "Read Results"
            isSpeaking = false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        voiceAssistantHelper.shutdown()
        _binding = null
    }
}