package com.feri.healthydiet.ui.menuscan

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.feri.healthydiet.databinding.FragmentResultsBinding
import com.feri.healthydiet.util.VoiceAssistantHelper
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class ResultsFragment : Fragment() {
    private var _binding: FragmentResultsBinding? = null
    private val binding get() = _binding!!

    private val args: ResultsFragmentArgs by navArgs()
    private lateinit var voiceAssistantHelper: VoiceAssistantHelper
    private var isSpeaking = false

    // Adaugă viewModel
    private val viewModel: ResultsViewModel by viewModel()

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

        // Verifică dacă există o eroare și afișează butonul de reîncercare dacă e cazul
        if (result.recommended.size == 1 &&
            (result.recommended[0].name.startsWith("Error") ||
                    result.recommended[0].name.startsWith("Could not extract"))) {
            binding.btnRetry.visibility = View.VISIBLE
            binding.btnRetry.setOnClickListener {
                findNavController().popBackStack()
            }
        } else {
            binding.btnRetry.visibility = View.GONE
        }

        // Setează rezultatul în ViewModel pentru a-l salva ulterior
        viewModel.setResult(result)  // Elimină verificarea isInitialized

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
            // Salvează în istoric direct fără verificare
            viewModel.saveToHistory()  // Elimină verificarea isInitialized
            Toast.makeText(context, "Analysis saved to history", Toast.LENGTH_SHORT).show()
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