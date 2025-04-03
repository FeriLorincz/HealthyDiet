package com.feri.healthydiet.ui.auth

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.feri.healthydiet.R
import com.feri.healthydiet.databinding.FragmentSplashBinding
import com.google.firebase.auth.FirebaseAuth
import org.koin.androidx.viewmodel.ext.android.viewModel

class SplashFragment : Fragment() {

    private var _binding: FragmentSplashBinding? = null
    private val binding get() = _binding!!

    private val authViewModel: AuthViewModel by viewModel()
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSplashBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Așteaptă 1.5 secunde înainte de a verifica starea autentificării
        handler.postDelayed({
            checkAuthState()
        }, 1500)
    }

    private fun checkAuthState() {
        val currentUser = FirebaseAuth.getInstance().currentUser

        if (currentUser != null) {
            // Utilizatorul este autentificat, navigăm la Dashboard
            findNavController().navigate(R.id.action_splashFragment_to_dashboardFragment)
        } else {
            // Utilizatorul nu este autentificat, navigăm la Login
            findNavController().navigate(R.id.action_splashFragment_to_loginFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacksAndMessages(null)
        _binding = null
    }
}