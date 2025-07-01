package com.hereliesaz.cuedetat.ar.scene

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.xr.scenecore.Scene
import androidx.xr.scenecore.ar.ARScene
import com.google.ar.core.Anchor
import com.google.ar.core.HitResult

@Composable
fun BilliardsScene(
    modifier: Modifier = Modifier,
    onSceneCreated: (ARScene) -> Unit,
    onTap: (HitResult) -> Unit
) {
    val scene = remember { Scene() }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            ARScene(context, scene, Lifecycle.Event.ON_RESUME).apply {
                onSceneCreated(this)
                onTapListener = { hitResult, _ ->
                    onTap(hitResult)
                }
            }
        }
    )
}
