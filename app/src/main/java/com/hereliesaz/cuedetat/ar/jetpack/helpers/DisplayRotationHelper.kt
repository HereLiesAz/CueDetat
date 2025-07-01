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
import android.hardware.display.DisplayManager
import android.view.Display
import android.view.Surface
import android.view.WindowManager
import com.google.ar.core.Session

/**
 * Helper to track the display rotations. In particular, the 180 degree rotations are not notified
 * by the onSurfaceChanged() callback, and thus must be tracked manually.
 */
class DisplayRotationHelper(private val activity: Activity) : DisplayManager.DisplayListener {
    private var viewportChanged = false
    private var viewportWidth = 0
    private var viewportHeight = 0
    private val display: Display = activity.display!!

    /**
     * Registers the display listener. Should be called from [Activity.onResume].
     */
    fun onResume() {
        activity.getSystemService(DisplayManager::class.java).registerDisplayListener(this, null)
    }

    /**
     * Unregisters the display listener. Should be called from [Activity.onPause].
     */
    fun onPause() {
        activity.getSystemService(DisplayManager::class.java).unregisterDisplayListener(this)
    }

    /**
     * Records a change in surface dimensions. This will be later used by
     * [.updateSessionIfNeeded]. Should be called from [ ].
     *
     * @param width the new width of the surface.
     * @param height the new height of the surface.
     */
    fun onSurfaceChanged(width: Int, height: Int) {
        viewportWidth = width
        viewportHeight = height
        viewportChanged = true
    }

    /**
     * Updates the session display geometry if a change was posted either by
     * [ ][.onSurfaceChanged] or by [.onDisplayChanged]. This function should be called
     * constantly during the render loop.
     *
     * @param session the [Session] object to update if necessary.
     */
    fun updateSessionIfNeeded(session: Session) {
        if (viewportChanged) {
            val displayRotation = display.rotation
            session.setDisplayGeometry(displayRotation, viewportWidth, viewportHeight)
            viewportChanged = false
        }
    }

    override fun onDisplayAdded(displayId: Int) {}
    override fun onDisplayRemoved(displayId: Int) {}
    override fun onDisplayChanged(displayId: Int) {
        viewportChanged = true
    }
}
