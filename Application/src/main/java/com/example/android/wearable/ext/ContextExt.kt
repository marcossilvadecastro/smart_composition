package com.example.android.wearable.ext

import android.content.Context
import android.preference.PreferenceManager
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.example.android.wearable.datalayer.R
import com.google.mlkit.vision.objects.ObjectDetectorOptionsBase.DetectorMode
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

suspend fun Context.getCameraProvider(): ProcessCameraProvider =
    suspendCoroutine { continuation ->
        ProcessCameraProvider.getInstance(this).also { cameraProvider ->
            cameraProvider.addListener({
                continuation.resume(cameraProvider.get())
            }, ContextCompat.getMainExecutor(this))
        }
    }

fun Context.getObjectDetectorOptions(
    @DetectorMode mode: Int
): ObjectDetectorOptions {
    val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)

    val enableMultipleObjects =
        sharedPreferences.getBoolean(
            getString(R.string.pref_key_live_preview_object_detector_enable_multiple_objects),
            false
        )
    val enableClassification =
        sharedPreferences.getBoolean(
            getString(R.string.pref_key_still_image_object_detector_enable_multiple_objects),
            true
        )

    val builder =
        ObjectDetectorOptions.Builder().setDetectorMode(mode)
    if (enableMultipleObjects) {
        builder.enableMultipleObjects()
    }
    if (enableClassification) {
        builder.enableClassification()
    }
    return builder.build()
}

fun Context.getCameraXTargetResolution(lensFacing: Int): Size? {
    val prefKey: String =
        if (lensFacing == CameraSelector.LENS_FACING_BACK)
            getString(R.string.pref_key_camerax_rear_camera_target_resolution)
        else
            getString(R.string.pref_key_camerax_front_camera_target_resolution)
    val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
    return try {
        Size.parseSize(sharedPreferences.getString(prefKey, null))
    } catch (e: Exception) {
        null
    }
}
