package com.example.camerax

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import com.example.BaseActivity

// MainActivity inherits from BaseActivity to utilize its camera permission handling logic
class MainActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Collect the camera permission state as a Compose state to automatically update the UI upon change
            val permissionGranted = isCameraPermissionGranted.collectAsState().value
            if (permissionGranted) {
                // If permission is granted, display the camera preview
                SnapchatLikeCamera()
            } else {
                // If permission is not granted, display a button to request camera permission
                handleCameraPermission()
            }
        }
    }
}