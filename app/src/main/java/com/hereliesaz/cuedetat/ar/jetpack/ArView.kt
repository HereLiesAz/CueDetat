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
package com.hereliesaz.cuedetat.ar.jetpack

import android.opengl.GLSurfaceView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.ar.core.Session
import com.hereliesaz.cuedetat.MainActivity
import com.hereliesaz.cuedetat.ar.jetpack.helpers.DisplayRotationHelper

@Composable
fun ArView(session: Session, modifier: Modifier = Modifier) {
    val glSurfaceView = GLSurfaceView(androidx.compose.ui.platform.LocalContext.current)
    val renderer = ArRenderer(
        activity = androidx.compose.ui.platform.LocalContext.current as MainActivity,
        session = session,
        displayRotationHelper = DisplayRotationHelper(androidx.compose.ui.platform.LocalContext.current as MainActivity)
    )

    glSurfaceView.setEGLContextClientVersion(2)
    glSurfaceView.setRenderer(renderer)
    glSurfaceView.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY

    AndroidView({ glSurfaceView }, modifier = modifier)
}
