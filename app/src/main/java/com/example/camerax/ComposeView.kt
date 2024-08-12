package com.example.camerax

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.Executors


@Composable
fun SnapchatLikeCamera() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // State to manage camera setup
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var preview by remember { mutableStateOf<Preview?>(null) }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var capturedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isCapturing by remember { mutableStateOf(false) }

    // Executor for CameraX operations
    val cameraExecutor = Executors.newSingleThreadExecutor()
    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    var cameraControl: CameraControl? = remember { null }
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

    Box(modifier = Modifier.fillMaxSize()) {

        // Camera preview
        AndroidView(modifier = Modifier.fillMaxSize(), factory = { ctx ->
            previewView = PreviewView(ctx)

            val imageCaptureBuilder = ImageCapture.Builder().build().also {
                imageCapture = it
            }

            cameraProviderFuture.addListener({
                cameraProvider = cameraProviderFuture.get()

                preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView!!.surfaceProvider)
                }

                imageCapture = ImageCapture.Builder().build()

                if (!isCapturing) {
                    cameraProvider?.bindToLifecycle(
                        lifecycleOwner, cameraSelector, preview, imageCapture
                    )
                }

            }, ContextCompat.getMainExecutor(ctx))

            previewView!!
        })

        // Capture Button
        Button(
            onClick = {
                if (!isCapturing) {
                    isCapturing = true
                    cameraProvider!!.unbind(preview)
                    capturePhoto(context, previewView!!, imageCapture) { uri ->
                        capturedImageUri = uri
                        isCapturing = false
                        cameraProvider?.bindToLifecycle(
                            lifecycleOwner, cameraSelector, preview, imageCapture
                        )
                    }
                }
            }, modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        ) {
            Text("Capture")
        }

        // Overlay captured image
        capturedImageUri?.let { uri ->
            uri.toBitmap(context)?.let { bitmap ->
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            // Close Button
            IconButton(
                onClick = { capturedImageUri = null },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
            }
        }

        // Loading Indicator
        if (isCapturing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color.White)
            }
        }
    }
}

private fun capturePhoto(
    context: Context,
    previewView: PreviewView,
    imageCapture: ImageCapture?,
    onImageCaptured: (Uri) -> Unit
) {
    val photoFile = File(
        context.getExternalFilesDir(null), "${System.currentTimeMillis()}.jpg"
    )


    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

    previewView.bitmap?.let { bitmap ->
        val bytes = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
        val path: String = MediaStore.Images.Media.insertImage(
            context.contentResolver,
            bitmap,
            "Title",
            null
        )
        onImageCaptured(Uri.parse(path))
    }

/*
    imageCapture?.takePicture(outputOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                val savedUri = Uri.fromFile(photoFile)
                onImageCaptured(savedUri)
            }

            override fun onError(exception: ImageCaptureException) {
                Toast.makeText(
                    context, "Photo capture failed: ${exception.message}", Toast.LENGTH_SHORT
                ).show()
                onImageCaptured(Uri.EMPTY)
            }
        })*/
}

private fun Uri.toBitmap(context: Context): Bitmap? {
    return try {
        if (Build.VERSION.SDK_INT < 28) {
            @Suppress("DEPRECATION") MediaStore.Images.Media.getBitmap(
                context.contentResolver, this
            )
        } else {
            val source = ImageDecoder.createSource(context.contentResolver, this)
            ImageDecoder.decodeBitmap(source)
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
