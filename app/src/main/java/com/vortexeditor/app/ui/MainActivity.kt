package com.vortexeditor.app.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.vortexeditor.app.databinding.ActivityMainBinding
import com.vortexeditor.app.ui.editor.EditorActivity

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_AUDIO,
            Manifest.permission.READ_MEDIA_IMAGES
        )
    } else {
        arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            openVideoGallery()
        } else {
            Toast.makeText(this, "Permissions required to access media", Toast.LENGTH_SHORT).show()
        }
    }

    private val videoPickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { openEditor(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
    }

    private fun setupUI() {
        binding.btnNewProject.setOnClickListener {
            checkPermissionsAndPickVideo()
        }

        binding.btnOpenProject.setOnClickListener {
            // TODO: Implement project loading
            Toast.makeText(this, "Coming soon!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkPermissionsAndPickVideo() {
        val notGranted = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (notGranted.isEmpty()) {
            openVideoGallery()
        } else {
            permissionLauncher.launch(notGranted.toTypedArray())
        }
    }

    private fun openVideoGallery() {
        videoPickerLauncher.launch("video/*")
    }

    private fun openEditor(videoUri: Uri) {
        val intent = Intent(this, EditorActivity::class.java).apply {
            putExtra(EditorActivity.EXTRA_VIDEO_URI, videoUri.toString())
        }
        startActivity(intent)
    }
}
