package com.example.android.wearable.ui.components

import android.util.Log
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.android.wearable.ext.getCameraProvider
import com.example.android.wearable.ext.getCameraXTargetResolution
import com.example.android.wearable.ext.getObjectDetectorOptions
import com.example.android.wearable.ml.vision.GraphicOverlay
import com.example.android.wearable.ml.vision.objectdetector.ObjectDetectorProcessor
import com.google.mlkit.common.MlKitException
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions

@OptIn(ExperimentalGetImage::class)
@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    cameraSelector: CameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current

    val graphicOverlay = remember {
        GraphicOverlay(
            context,
            null
        )
    }

    val preview = remember {
        Preview.Builder().build()
    }

    val previewView = remember {
        PreviewView(context).apply {
            addView(
                graphicOverlay
            )
        }
    }

    LaunchedEffect(cameraSelector) {
        val cameraProvider = context.getCameraProvider()
        cameraProvider.unbindAll()
        preview.setSurfaceProvider(previewView.surfaceProvider)

        val objectDetectorOptions = context.getObjectDetectorOptions(
            ObjectDetectorOptions.STREAM_MODE
        )
        val imageProcessor = ObjectDetectorProcessor(context, objectDetectorOptions)

        val builder = ImageAnalysis.Builder()

        val currentLensFace = if (cameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }

        val targetResolution = context.getCameraXTargetResolution(currentLensFace)
        if (targetResolution != null) {
            builder.setTargetResolution(targetResolution)
        }
        val analysisUseCase = builder.build()
        analysisUseCase.setAnalyzer(
            ContextCompat.getMainExecutor(context)
        ) { imageProxy: ImageProxy ->
            val isImageFlipped = currentLensFace == CameraSelector.LENS_FACING_FRONT
            val rotationDegrees = imageProxy.imageInfo.rotationDegrees
            if (rotationDegrees == 0 || rotationDegrees == 180) {
                graphicOverlay.setImageSourceInfo(
                    imageProxy.width,
                    imageProxy.height,
                    isImageFlipped
                )
            } else {
                graphicOverlay.setImageSourceInfo(
                    imageProxy.height,
                    imageProxy.width,
                    isImageFlipped
                )
            }
            try {
                imageProcessor.processImageProxy(imageProxy, graphicOverlay)
            } catch (e: MlKitException) {
                Log.e("CameraPreview", "Failed to process image. Error: " + e.localizedMessage)
                Toast.makeText(context, e.localizedMessage, Toast.LENGTH_SHORT).show()
            }
        }
        cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, analysisUseCase)
    }


    AndroidView(
        factory = {
            previewView
        },
        modifier = modifier
    )
}
