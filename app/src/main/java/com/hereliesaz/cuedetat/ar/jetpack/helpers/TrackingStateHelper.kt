/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hereliesaz.cuedetat.ar.jetpack.helpers

import android.app.Activity
import android.widget.TextView
import com.google.ar.core.Camera
import com.google.ar.core.TrackingState

/**
 * Helper to show tracking state.
 */
class TrackingStateHelper(private val activity: Activity) {
    private var previousTrackingState: TrackingState? = null

    /**
     * Get the current tracking state, and update the tracking state TV if the tracking state has changed.
     */
    fun update(camera: Camera) {
        val currentTrackingState = camera.trackingState
        if (currentTrackingState == previousTrackingState) {
            return
        }
        previousTrackingState = currentTrackingState
        activity.runOnUiThread {
            when (currentTrackingState) {
                TrackingState.PAUSED -> {
                    // Show a message that the camera is paused.
                }
                TrackingState.STOPPED -> {
                    // Show a message that tracking has stopped.
                }
                TrackingState.TRACKING -> {
                    // Hide all messages.
                }
            }
        }
    }
}
