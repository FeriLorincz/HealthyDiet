package com.feri.healthydiet.ui.foodanalyzer

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.feri.healthydiet.databinding.FragmentFoodAnalyzerBinding
import com.feri.healthydiet.util.SpeechRecognitionHelper
import com.feri.healthydiet.util.VoiceAssistantHelper
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class FoodAnalyzerFragment : Fragment() {

    private var _binding: FragmentFoodAnalyzerBinding? = null
    private val binding get() = _binding!!

    private val viewModel: FoodAnalyzerViewModel by viewModel()
    private lateinit var voiceAssistantHelper: VoiceAssistantHelper
    private var isSpeaking = false

    private lateinit var speechRecognizer: SpeechRecognitionHelper
    private lateinit var speechRecognizerLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        voiceAssistantHelper = VoiceAssistantHelper(requireContext())
        speechRecognizer = SpeechRecognitionHelper()

        speechRecognizerLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                val spokenText = speechRecognizer.processResult(results)

                spokenText?.let {
                    binding.etFoodName.setText(it)
                    viewModel.analyzeFoodItem(it)
                }
            }
        }

        lifecycleScope.launch {
            voiceAssistantHelper.initialize()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFoodAnalyzerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnAnalyze.setOnClickListener {
            val foodName = binding.etFoodName.text.toString()
            if (foodName.isNotBlank()) {
                viewModel.analyzeFoodItem(foodName)
            } else {
                Toast.makeText(context, "Please enter a food item", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnSpeak.setOnClickListener {
            if (!isSpeaking) {
                speakAnalysisResult()
            } else {
                voiceAssistantHelper.stop()
                binding.btnSpeak.text = "Read Analysis"
                isSpeaking = false
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE

                state.error?.let {
                    Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                }

                state.foodAnalysis?.let { analysis ->
                    binding.tvCategory.text = "Category: ${analysis.category}"
                    binding.tvProtein.text = "${analysis.protein}g"
                    binding.tvCarbs.text = "${analysis.carbs}g"
                    binding.tvFats.text = "${analysis.fats}g"

                    binding.linearHealthImpacts.removeAllViews()
                    analysis.healthImpacts.forEach { impact ->
                        val impactView = layoutInflater.inflate(
                            android.R.layout.simple_list_item_1,
                            binding.linearHealthImpacts,
                            false
                        )
                        impactView.findViewById<android.widget.TextView>(android.R.id.text1).text = "• $impact"
                        binding.linearHealthImpacts.addView(impactView)
                    }

                    binding.cardResults.visibility = View.VISIBLE
                }
            }
        }

        binding.btnVoiceInput.setOnClickListener {
            try {
                speechRecognizerLauncher.launch(speechRecognizer.createRecognizeIntent())
            } catch (e: Exception) {
                Toast.makeText(context, "Speech recognition not available", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun speakAnalysisResult() {
        viewModel.uiState.value.foodAnalysis?.let { analysis ->
            lifecycleScope.launch {
                binding.btnSpeak.text = "Stop Reading"
                isSpeaking = true

                val textToSpeak = "This food is categorized as ${analysis.category}. " +
                        "It contains approximately ${analysis.protein} of protein, " +
                        "${analysis.carbs} of carbohydrates, and ${analysis.fats} of fats. " +
                        "Health impacts include: ${analysis.healthImpacts.joinToString(". ")}"

                voiceAssistantHelper.speak(textToSpeak)

                binding.btnSpeak.text = "Read Analysis"
                isSpeaking = false
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        voiceAssistantHelper.shutdown()
        _binding = null
    }
}