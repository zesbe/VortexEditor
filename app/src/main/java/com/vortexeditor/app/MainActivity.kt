package com.vortexeditor.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.vortexeditor.app.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    // File picker launcher
    private val pickVideoLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { handleVideoSelected(it) }
    }

    // Permission launcher
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            openVideoPicker()
        } else {
            Toast.makeText(this, "Permission required to access videos", Toast.LENGTH_SHORT).show()
        }
    }

    // Camera launcher
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.CaptureVideo()
    ) { success ->
        if (success) {
            tempVideoUri?.let { handleVideoSelected(it) }
        }
    }

    private var tempVideoUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupClickListeners()
        setupRecentProjects()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "VortexEditor"
    }

    private fun setupClickListeners() {
        // New Project button
        binding.btnNewProject.setOnClickListener {
            checkPermissionAndOpenPicker()
        }

        // Import Video card
        binding.cardImportVideo.setOnClickListener {
            checkPermissionAndOpenPicker()
        }

        // Record Video card
        binding.cardRecordVideo.setOnClickListener {
            openCamera()
        }
    }

    private fun setupRecentProjects() {
        binding.rvRecentProjects.layoutManager = LinearLayoutManager(
            this, 
            LinearLayoutManager.HORIZONTAL, 
            false
        )
        // TODO: Load recent projects from database
        // For now, show empty state
        binding.emptyState.visibility = android.view.View.VISIBLE
    }

    private fun checkPermissionAndOpenPicker() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        val allGranted = permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

        if (allGranted) {
            openVideoPicker()
        } else {
            permissionLauncher.launch(permissions)
        }
    }

    private fun openVideoPicker() {
        try {
            pickVideoLauncher.launch("video/*")
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error opening file picker: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun openCamera() {
        val hasCameraPermission = ContextCompat.checkSelfPermission(
            this, 
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasCameraPermission) {
            permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA))
            return
        }

        try {
            // Create temp file for video
            val contentValues = android.content.ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "VortexEditor_${System.currentTimeMillis()}.mp4")
                put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            }
            
            tempVideoUri = contentResolver.insert(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )
            
            tempVideoUri?.let { uri ->
                cameraLauncher.launch(uri)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error opening camera: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun handleVideoSelected(uri: Uri) {
        try {
            // Get file info
            val cursor = contentResolver.query(uri, null, null, null, null)
            var fileName = "Unknown"
            var fileSize = 0L
            
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                    val sizeIndex = it.getColumnIndex(MediaStore.MediaColumns.SIZE)
                    
                    if (nameIndex >= 0) fileName = it.getString(nameIndex) ?: "Unknown"
                    if (sizeIndex >= 0) fileSize = it.getLong(sizeIndex)
                }
            }

            // Open editor activity
            val intent = Intent(this, EditorActivity::class.java).apply {
                putExtra("video_uri", uri.toString())
                putExtra("video_name", fileName)
                putExtra("video_size", fileSize)
            }
            startActivity(intent)
            
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error loading video: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
