package com.feri.healthydiet.ui.menuscan

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.feri.healthydiet.databinding.FragmentMenuScanBinding
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import android.util.Log

class MenuScanFragment : Fragment() {

    private var _binding: FragmentMenuScanBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MenuScanViewModel by viewModel()

    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private lateinit var cameraExecutor: ExecutorService

    private var currentPhotoUri: Uri? = null

    private val TAG = "MenuScanFragment"

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startCamera()
        } else {
            Toast.makeText(context, "Camera permission is required", Toast.LENGTH_SHORT).show()
        }
    }

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            processMenuImage(it)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMenuScanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cameraExecutor = Executors.newSingleThreadExecutor()

        binding.btnCapture.setOnClickListener {
            if (allPermissionsGranted()) {
                takePhoto()
            } else {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }

        binding.btnUpload.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        // Observe view model states
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when {
                    state.isLoading -> binding.progressBar.visibility = View.VISIBLE
                    state.error != null -> {
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(context, state.error, Toast.LENGTH_LONG).show()
                    }
                    state.analysisResult != null -> {
                        binding.progressBar.visibility = View.GONE
                        navigateToResults(state.analysisResult)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (allPermissionsGranted()) {
            startCamera()
        }
    }

    override fun onPause() {
        super.onPause()
        // Oprește camera când fragmentul nu este vizibil
        shutdownCamera()
    }

    private fun shutdownCamera() {
        try {
            // Eliberează resursele camerei
            cameraProvider?.unbindAll()
            camera = null
        } catch (e: Exception) {
            Log.e(TAG, "Error shutting down camera: ${e.message}")
        }
    }

    private fun startCamera() {
        try {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

            cameraProviderFuture.addListener({
                try {
                    // Obține furnizorul de cameră
                    cameraProvider = cameraProviderFuture.get()

                    // Unbind înainte de a configura din nou camera
                    cameraProvider?.unbindAll()

                    // Set up the preview use case
                    val preview = Preview.Builder()
                        .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                        .build()

                    preview.setSurfaceProvider(binding.viewFinder.surfaceProvider)

                    // Set up the capture use case
                    imageCapture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                        .build()

                    // Specify the camera (usually the back camera)
                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    // Bind use cases to camera
                    camera = cameraProvider?.bindToLifecycle(
                        viewLifecycleOwner,
                        cameraSelector,
                        preview,
                        imageCapture
                    )

                    // Setează parametri suplimentari pentru cameră
                    setupCameraControls()

                } catch (e: Exception) {
                    Log.e(TAG, "Error starting camera: ${e.message}")
                    Toast.makeText(context, "Failed to start camera: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }, ContextCompat.getMainExecutor(requireContext()))
        } catch (e: Exception) {
            Log.e(TAG, "Camera initialization failed: ${e.message}")
            Toast.makeText(context, "Camera initialization failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupCameraControls() {
        camera?.cameraControl?.let { cameraControl ->
            try {
                // Setare AutoFocus continuu
                cameraControl.setLinearZoom(0f) // Resetează zoom-ul

                // Dacă ai nevoie de control suplimentar, adaugă aici
            } catch (e: Exception) {
                Log.e(TAG, "Error setting camera controls: ${e.message}")
            }
        }
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        try {
            // Creează un nume de fișier bazat pe timestamp
            val name = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.US)
                .format(System.currentTimeMillis())
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            }

            val outputOptions = ImageCapture.OutputFileOptions.Builder(
                requireContext().contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            ).build()

            // Configurează captarea imaginii
            imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(requireContext()),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        output.savedUri?.let {
                            currentPhotoUri = it
                            Log.d(TAG, "Photo saved: $it")
                            processMenuImage(it)
                        }
                    }

                    override fun onError(exc: ImageCaptureException) {
                        Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                        Toast.makeText(
                            context,
                            "Photo capture failed: ${exc.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error taking photo: ${e.message}", e)
            Toast.makeText(context, "Failed to take photo: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun processMenuImage(uri: Uri) {
        viewModel.analyzeMenuImage(uri, requireContext())
    }

    private fun navigateToResults(result: MenuAnalysisResult) {
        try {
            val action = MenuScanFragmentDirections.actionMenuScanFragmentToResultsFragment(result)
            findNavController().navigate(action)
        } catch (e: Exception) {
            Log.e(TAG, "Navigation error: ${e.message}", e)
            Toast.makeText(context, "Error navigating to results: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun allPermissionsGranted() = ContextCompat.checkSelfPermission(
        requireContext(), Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor.shutdown()
        shutdownCamera()
        _binding = null
    }
}