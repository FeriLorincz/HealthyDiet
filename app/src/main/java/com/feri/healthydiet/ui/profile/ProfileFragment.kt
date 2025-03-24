package com.feri.healthydiet.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.feri.healthydiet.databinding.FragmentProfileBinding
import org.koin.androidx.viewmodel.ext.android.viewModel

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeViewModel()
        setupListeners()

        viewModel.loadUserProfile()
    }

    private fun observeViewModel() {
        viewModel.userProfile.observe(viewLifecycleOwner) { profile ->
            profile?.let {
                binding.etName.setText(it.name)
                binding.etEmail.setText(it.email)

                // Health conditions
                binding.cbDiabetes.isChecked = it.healthProfile.hasDiabetes
                binding.cbLiverSteatosis.isChecked = it.healthProfile.hasLiverSteatosis
                binding.cbHypertension.isChecked = it.healthProfile.hasHypertension
                binding.cbHighCholesterol.isChecked = it.healthProfile.hasHighCholesterol
                binding.cbCeliac.isChecked = it.healthProfile.hasCeliac

                // Custom conditions
                val customConditions = it.healthProfile.customConditions.joinToString("\n")
                binding.etCustomConditions.setText(customConditions)
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.saveSuccess.observe(viewLifecycleOwner) { saved ->
            if (saved) {
                Toast.makeText(context, "Profile saved successfully", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupListeners() {
        binding.btnSave.setOnClickListener {
            val name = binding.etName.text.toString()
            val email = binding.etEmail.text.toString()

            // Health conditions
            val hasDiabetes = binding.cbDiabetes.isChecked
            val hasLiverSteatosis = binding.cbLiverSteatosis.isChecked
            val hasHypertension = binding.cbHypertension.isChecked
            val hasHighCholesterol = binding.cbHighCholesterol.isChecked
            val hasCeliac = binding.cbCeliac.isChecked

            // Custom conditions (separated by new lines)
            val customConditionsText = binding.etCustomConditions.text.toString()
            val customConditions = customConditionsText
                .split("\n")
                .filter { it.isNotBlank() }

            viewModel.saveUserProfile(
                name = name,
                email = email,
                hasDiabetes = hasDiabetes,
                hasLiverSteatosis = hasLiverSteatosis,
                hasHypertension = hasHypertension,
                hasHighCholesterol = hasHighCholesterol,
                hasCeliac = hasCeliac,
                customConditions = customConditions
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}