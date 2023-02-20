package com.example.floatingb_test

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.navigation.ui.AppBarConfiguration
import com.example.floatingb_test.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding


    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            checkPermissionThenStart()
        } else {
            binding.root.showSnackbar(
                getString(R.string.notification_blocked),
                actionMessage = getString(R.string.settings)
            ) { openSettings() }
        }
    }

    private val getResult =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            if (Settings.canDrawOverlays(this)) {
                controlService()
            } else {
                binding.root.showSnackbar(
                    getString(R.string.overlay_permission_not_granted),
                    actionMessage = getString(R.string.settings)
                ) { launchOverlayPermissionScreen() }
            }

        }

    override fun onStop() {
        super.onStop()
        checkPermissionThenStart()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        binding.startFab.setOnClickListener {
            when {
                ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    checkPermissionThenStart()
                }

                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    binding.root.showSnackbar(
                        getString(R.string.notification_blocked),
                        actionMessage = getString(R.string.settings)
                    ) { openSettings() }
                }

                else -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        requestPermissionLauncher.launch(
                            Manifest.permission.POST_NOTIFICATIONS
                        )
                    }
                }
            }
        }

        binding.stopFab.setOnClickListener {
            controlService(stopService = true)
        }
    }

    private fun openSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val uri: Uri = Uri.fromParts("package", packageName, null)
        intent.data = uri
        startActivity(intent)
    }

    private fun checkPermissionThenStart() {
        if (!Settings.canDrawOverlays(this)) {
            launchOverlayPermissionScreen()
        } else {
            controlService()
        }
    }

    private fun launchOverlayPermissionScreen() {
        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
        intent.data = Uri.fromParts("package", packageName, null)
        getResult.launch(intent)
    }

    private fun controlService(stopService: Boolean = false) {
        val intent = Intent(this, BubbleService::class.java)
        if (stopService) stopService(intent) else startService(intent)
    }


}