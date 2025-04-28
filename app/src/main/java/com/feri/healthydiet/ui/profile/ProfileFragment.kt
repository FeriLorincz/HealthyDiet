package com.feri.healthydiet.ui.profile

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.feri.healthydiet.R
import com.feri.healthydiet.databinding.FragmentProfileBinding
import com.feri.healthydiet.ui.auth.AuthViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileViewModel by viewModel()
    private val authViewModel: AuthViewModel by viewModel()

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
        Log.d("ProfileFragment", "onViewCreated - loading user profile")
        viewModel.loadUserProfile()  // Asigură-te că această linie există
        observeViewModel()
        setupListeners()
    }

    private fun observeViewModel() {
        viewModel.userProfile.observe(viewLifecycleOwner) { profile ->
            profile?.let {
                Log.d("ProfileFragment", "Received profile update: name=${it.name}, hasDiabetes=${it.healthProfile.hasDiabetes}, customConditions=${it.healthProfile.customConditions}")
                binding.etName.setText(it.name)
                binding.etEmail.setText(it.email)

                // Health conditions
                binding.cbDiabetes.isChecked = it.healthProfile.hasDiabetes
                binding.cbLiverSteatosis.isChecked = it.healthProfile.hasLiverSteatosis
                binding.cbHypertension.isChecked = it.healthProfile.hasHypertension
                binding.cbHighCholesterol.isChecked = it.healthProfile.hasHighCholesterol
                binding.cbCeliac.isChecked = it.healthProfile.hasCeliac

                // Custom conditions - separă cu newline
                val customConditions = if (it.healthProfile.customConditions.isEmpty()) {
                    ""
                } else {
                    it.healthProfile.customConditions.joinToString("\n")
                }
                binding.etCustomConditions.setText(customConditions)
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.saveSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                Toast.makeText(context, "Profile saved successfully", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Failed to save profile", Toast.LENGTH_SHORT).show()
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

            // Custom conditions (separate by newlines)
            val customConditionsText = binding.etCustomConditions.text.toString()
            val customConditions = if (customConditionsText.isNotEmpty()) {
                // Încearcă diferite separatoare
                if (customConditionsText.contains("\n")) {
                    customConditionsText.split("\n")
                } else {
                    // Dacă nu conține newline, încearcă alt separator sau consideră tot textul ca o condiție
                    listOf(customConditionsText)
                }
            } else {
                emptyList()
            }.filter { it.isNotBlank() }

            Log.d("ProfileFragment", "Custom conditions: $customConditions")

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

        // Adăugăm acțiunea de logout
        binding.btnLogout.setOnClickListener {
            // Asigură-te că și deconectarea funcționează corect
            val authViewModel: AuthViewModel by viewModel()
            authViewModel.logout()
            findNavController().navigate(R.id.action_profileFragment_to_loginFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}