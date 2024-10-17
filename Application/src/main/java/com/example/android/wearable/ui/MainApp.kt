/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.android.wearable.ui

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.CameraController.IMAGE_CAPTURE
import androidx.camera.view.CameraController.IMAGE_ANALYSIS
import androidx.camera.view.LifecycleCameraController
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.SendAndArchive
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.graphics.decodeBitmap
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.android.wearable.ui.components.CameraPreview
import com.example.android.wearable.ui.components.PhotoBottomSheetContent
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.launch

/**
 * The UI affording the actions the user can take, along with a list of the events and the image
 * to be sent to the wearable devices.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(
    clientDataViewModel: ClientDataViewModel,
    isCameraSupported: Boolean,
    sendPhoto: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val scaffoldState = rememberBottomSheetScaffoldState()

    val cameraSelector = remember {
        mutableStateOf(CameraSelector.DEFAULT_FRONT_CAMERA)
    }

    val context = LocalContext.current

    val imageCapture = remember {
        ImageCapture.Builder().build()
    }

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = 0.dp,
        sheetContent = {
            PhotoBottomSheetContent(
                bitmaps = listOf(clientDataViewModel.image),
                modifier = Modifier
                    .fillMaxWidth()
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            CameraPreview(
                modifier = Modifier
                    .fillMaxSize(),
                cameraSelector = cameraSelector.value,
                imageCapture
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(64.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {

                IconButton(
                    onClick = {
                        scope.launch {
                            cameraSelector.value =
                                if (cameraSelector.value == CameraSelector.DEFAULT_BACK_CAMERA) {
                                    CameraSelector.DEFAULT_FRONT_CAMERA
                                } else
                                    CameraSelector.DEFAULT_BACK_CAMERA
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Cameraswitch,
                        contentDescription = "Switch camera",
                        modifier = Modifier.size(128.dp),
                        tint = Color.White
                    )
                }

                IconButton(
                    onClick = {
                        scope.launch {
                            if (!isCameraSupported) return@launch

                            captureImage(imageCapture, context) {
                                Toast.makeText(context, "Sending image", Toast.LENGTH_LONG).show()
                                clientDataViewModel.onPictureTaken(it)
                                sendPhoto()
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = "Take photo",
                        modifier = Modifier
                            .size(128.dp),
                        tint = Color.White
                    )
                }
            }
        }
    }
}

private fun captureImage(
    imageCapture: ImageCapture,
    context: Context,
    onPhotoTaken: (Bitmap) -> Unit
) {
    val name = "CameraxImage.jpeg"
    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, name)
        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Image")
        }
    }
    val outputOptions = ImageCapture.OutputFileOptions
        .Builder(
            context.contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        )
        .build()
    imageCapture.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                outputFileResults
                    .savedUri
                    ?.run {
                        ImageDecoder.createSource(context.contentResolver, this)
                            .decodeBitmap { _, source ->
                                ImageDecoder.decodeBitmap(source)
                            }
                    }?.let {
                        onPhotoTaken(it)
                    }
            }

            override fun onError(exception: ImageCaptureException) {
                Log.d(javaClass.simpleName, "Failed $exception")
            }
        })
}
