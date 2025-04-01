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
            Log.d(TAG, "Camera permission granted, starting camera")
            startCamera()
        } else {
            Log.e(TAG, "Camera permission denied")
            Toast.makeText(context, "Camera permission is required", Toast.LENGTH_SHORT).show()
        }
    }

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            Log.d(TAG, "Image selected from gallery: $it")
            processMenuImage(it)
        } ?: run {
            Log.e(TAG, "No image selected from gallery")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d(TAG, "onCreateView: Creating view")
        _binding = FragmentMenuScanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated: Setting up UI components")

        cameraExecutor = Executors.newSingleThreadExecutor()
        Log.d(TAG, "onViewCreated: Camera executor initialized")

        binding.btnCapture.setOnClickListener {
            Log.d(TAG, "Capture button clicked")
            if (allPermissionsGranted()) {
                Log.d(TAG, "All permissions granted, taking photo")
                takePhoto()
            } else {
                Log.d(TAG, "Camera permission not granted, requesting")
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }

        binding.btnUpload.setOnClickListener {
            Log.d(TAG, "Upload button clicked")
            selectImageFromGallery()
        }

        // Observe view model states
        viewLifecycleOwner.lifecycleScope.launch {
            Log.d(TAG, "Setting up UI state observer")
            viewModel.uiState.collect { state ->
                when {
                    state.isLoading -> {
                        Log.d(TAG, "UI State: Loading")
                        binding.progressBar.visibility = View.VISIBLE
                    }
                    state.error != null -> {
                        Log.e(TAG, "UI State: Error - ${state.error}")
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(context, state.error, Toast.LENGTH_LONG).show()
                    }
                    state.analysisResult != null -> {
                        Log.d(TAG, "UI State: Success - Analysis result received")
                        binding.progressBar.visibility = View.GONE
                        navigateToResults(state.analysisResult)
                    }
                }
            }
        }
    }

    private fun selectImageFromGallery() {
        try {
            Log.d(TAG, "selectImageFromGallery: Launching image picker")
            pickImageLauncher.launch("image/*")
        } catch (e: Exception) {
            Log.e(TAG, "Error launching image picker: ${e.message}", e)
            Toast.makeText(context, "Failed to open gallery: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume: Fragment resumed")
        if (allPermissionsGranted()) {
            Log.d(TAG, "onResume: Starting camera (permissions granted)")
            startCamera()
        } else {
            Log.d(TAG, "onResume: Not starting camera (permissions not granted)")
        }
    }

    override fun onPause() {
        Log.d(TAG, "onPause: Fragment paused")
        super.onPause()
        // Oprește camera când fragmentul nu este vizibil
        shutdownCamera()
    }

    private fun shutdownCamera() {
        try {
            Log.d(TAG, "shutdownCamera: Releasing camera resources")
            // Eliberează resursele camerei
            cameraProvider?.unbindAll()
            camera = null
        } catch (e: Exception) {
            Log.e(TAG, "Error shutting down camera: ${e.message}")
        }
    }

    private fun startCamera() {
        try {
            // Verifică dacă binding-ul este nul (fragmentul poate fi detașat)
            if (_binding == null) {
                Log.e(TAG, "startCamera: Cannot start camera because binding is null")
                return
            }
            Log.d(TAG, "startCamera: Starting camera initialization")
            val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

            cameraProviderFuture.addListener({
                try {
                    // Verifică din nou dacă binding-ul este nul (poate a devenit nul între timp)
                    if (_binding == null) {
                        Log.e(TAG, "startCamera: Binding became null during camera initialization")
                        return@addListener
                    }
                    // Obține furnizorul de cameră
                    cameraProvider = cameraProviderFuture.get()
                    Log.d(TAG, "startCamera: Got camera provider")

                    // Unbind înainte de a configura din nou camera
                    cameraProvider?.unbindAll()
                    Log.d(TAG, "startCamera: Unbound previous uses")

                    // Verifică permisiunile la acest punct
                    if (!allPermissionsGranted()) {
                        Log.e(TAG, "startCamera: Camera permissions not granted")
                        Toast.makeText(context, "Camera permissions not granted", Toast.LENGTH_SHORT).show()
                        return@addListener
                    }

                    // Set up the preview use case
                    val preview = Preview.Builder()
                        .build()
                    Log.d(TAG, "startCamera: Created preview")

                    preview.setSurfaceProvider(binding.viewFinder.surfaceProvider)
                    Log.d(TAG, "startCamera: Set surface provider")

                    // Set up the capture use case
                    imageCapture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build()
                    Log.d(TAG, "startCamera: Set up image capture")

                    // Specify the camera (usually the back camera)
                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                    Log.d(TAG, "startCamera: Selected back camera")

                    try {
                        // Bind use cases to camera
                        camera = cameraProvider?.bindToLifecycle(
                            viewLifecycleOwner,
                            cameraSelector,
                            preview,
                            imageCapture
                        )
                        Log.d(TAG, "startCamera: Camera bound to lifecycle")

                        // Setează parametri suplimentari pentru cameră
                        setupCameraControls()
                        Log.d(TAG, "startCamera: Camera setup complete")
                    } catch (e: Exception) {
                        Log.e(TAG, "startCamera: Use case binding failed", e)
                        Toast.makeText(context, "Camera binding failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "startCamera: Camera initialization error", e)
                    if (isAdded() && context != null) {
                        Toast.makeText(context, "Failed to start camera: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }, ContextCompat.getMainExecutor(requireContext()))
        } catch (e: Exception) {
            Log.e(TAG, "Camera initialization failed", e)
            if (isAdded() && context != null) {
                Toast.makeText(
                    context,
                    "Camera initialization failed: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun setupCameraControls() {
        camera?.cameraControl?.let { cameraControl ->
            try {
                Log.d(TAG, "setupCameraControls: Setting up camera controls")
                // Setare AutoFocus continuu
                cameraControl.setLinearZoom(0f) // Resetează zoom-ul
                Log.d(TAG, "setupCameraControls: Reset zoom")

                // Dacă ai nevoie de control suplimentar, adaugă aici
            } catch (e: Exception) {
                Log.e(TAG, "Error setting camera controls: ${e.message}")
            }
        }
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: run {
            Log.e(TAG, "takePhoto: imageCapture is null")
            Toast.makeText(context, "Camera not initialized", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            Log.d(TAG, "takePhoto: Taking picture")
            // Creează un nume de fișier bazat pe timestamp
            val name = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.US)
                .format(System.currentTimeMillis())
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            }
            Log.d(TAG, "takePhoto: Created file name: $name")

            val outputOptions = ImageCapture.OutputFileOptions.Builder(
                requireContext().contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            ).build()
            Log.d(TAG, "takePhoto: Created output options")

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
                        } ?: run {
                            Log.e(TAG, "savedUri is null")
                            Toast.makeText(context, "Failed to save photo", Toast.LENGTH_SHORT).show()
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
            Log.d(TAG, "takePhoto: Picture capture initiated")
        } catch (e: Exception) {
            Log.e(TAG, "Error taking photo: ${e.message}", e)
            Toast.makeText(context, "Failed to take photo: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun processMenuImage(uri: Uri) {
        Log.d(TAG, "processMenuImage: Processing image URI: $uri")
        viewModel.analyzeMenuImage(uri, requireContext())
    }

    private fun navigateToResults(result: MenuAnalysisResult) {
        try {
            Log.d(TAG, "navigateToResults: Navigating to results screen")
            val action = MenuScanFragmentDirections.actionMenuScanFragmentToResultsFragment(result)
            findNavController().navigate(action)
            Log.d(TAG, "navigateToResults: Navigation successful")
        } catch (e: Exception) {
            Log.e(TAG, "Navigation error: ${e.message}", e)
            Toast.makeText(context, "Error navigating to results: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun allPermissionsGranted(): Boolean {
        val cameraPermissionGranted = ContextCompat.checkSelfPermission(
            requireContext(), Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        Log.d(TAG, "allPermissionsGranted: Camera permission is ${if (cameraPermissionGranted) "granted" else "not granted"}")
        return cameraPermissionGranted
    }

    override fun onDestroyView() {
        Log.d(TAG, "onDestroyView: Fragment view destroyed")
        super.onDestroyView()
        cameraExecutor.shutdown()
        shutdownCamera()
        _binding = null
    }
}