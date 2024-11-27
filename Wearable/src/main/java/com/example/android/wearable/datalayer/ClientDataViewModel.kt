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
package com.example.android.wearable.datalayer

import android.annotation.SuppressLint
import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.wearable.Asset
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.CapabilityInfo
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

import android.util.Log
import androidx.compose.runtime.mutableIntStateOf
import com.example.android.wearable.datalayer.data.toDirection
import com.example.android.wearable.datalayer.data.toZLevel

class ClientDataViewModel(
    application: Application
) :
    AndroidViewModel(application),
    DataClient.OnDataChangedListener,
    MessageClient.OnMessageReceivedListener,
    CapabilityClient.OnCapabilityChangedListener {

    private val _events = mutableStateListOf<Event>()

    /**
     * The list of events from the clients.
     */
    val events: List<Event> = _events

    /**
     * The currently received image (if any), available to display.
     */
    var image by mutableStateOf<Bitmap?>(null)
        private set

    /**
     * The currently received image (if any), available to display.
     */
    private var rotationDegrees by mutableIntStateOf(0)

    private var loadPhotoJob: Job = Job().apply { complete() }

    @SuppressLint("VisibleForTests")
    override fun onDataChanged(dataEvents: DataEventBuffer) {

        // Do additional work for specific events
        dataEvents.forEach { dataEvent ->
            Log.d(TAG, "onDataChanged path: ${dataEvent.dataItem.uri.path}")
            when (dataEvent.type) {
                DataEvent.TYPE_CHANGED -> {
                    when (dataEvent.dataItem.uri.path) {
                        DataLayerListenerService.IMAGE_PATH -> {
                            loadPhotoJob.cancel()
                            loadPhotoJob = viewModelScope.launch {
                                val item = DataMapItem.fromDataItem(dataEvent.dataItem).dataMap

                                rotationDegrees = item.getInt(DataLayerListenerService.IMAGE_ROTATION_KEY)

                                image = loadBitmap(
                                    item.getAsset(DataLayerListenerService.IMAGE_KEY)
                                )
                            }
                        }
                    }
                }
            }
            when (dataEvent.dataItem.uri.path) {
                DataLayerListenerService.COORDINATE_PATH -> {
                    _events.add(
                        Event(
                            title = DataMapItem
                                .fromDataItem(dataEvent.dataItem)
                                .dataMap
                                .getInt(DataLayerListenerService.DIRECTION_KEY)
                                .toDirection()
                                .toString()
                                .lowercase(),
                            text = DataMapItem
                                .fromDataItem(dataEvent.dataItem)
                                .dataMap
                                .getInt(DataLayerListenerService.Z_LEVEL_KEY)
                                .toZLevel()
                                .toString()
                                .lowercase()
                        )
                    )
                }
            }
        }
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
//        _events.add(
//            Event(
//                title =  R.string.message,
//                text = messageEvent.toString()
//            )
//        )
    }

    override fun onCapabilityChanged(capabilityInfo: CapabilityInfo) {
//        _events.add(
//            Event(
//                title =  R.string.capability_changed,
//                text = capabilityInfo.toString()
//            )
//        )
    }

    fun Bitmap.rotate(degrees: Float): Bitmap {
        Log.d(javaClass.simpleName, "Roation degrees: $degrees")
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
    }

    private suspend fun loadBitmap(asset: Asset?): Bitmap? {
        if (asset == null) return null
        val response =
            Wearable.getDataClient(getApplication<Application>()).getFdForAsset(asset).await()
        return response.inputStream.use { inputStream ->
            withContext(Dispatchers.IO) {
                val bitmap = BitmapFactory.decodeStream(inputStream)
                bitmap.rotate(rotationDegrees.toFloat())
            }
        }
    }

    companion object {
        private const val TAG = "DataLayerService"
    }
}

/**
 * A data holder describing a client event.
 */
data class Event(
    val title: String,
    val text: String
)
