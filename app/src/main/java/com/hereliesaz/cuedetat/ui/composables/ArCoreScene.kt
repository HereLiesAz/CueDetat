// app/src/main/java/com/hereliesaz/cuedetat/ui/composables/ArCoreScene.kt
package com.hereliesaz.cuedetat.ui.composables

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.ar.core.Session
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.SceneView
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.hereliesaz.cuedetat.BallNode
import com.hereliesaz.cuedetat.view.state.OverlayState

@Composable
fun ArCoreScene(
    modifier: Modifier = Modifier,
    arSession: Session,
    uiState: OverlayState
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            SceneView(context).apply {
                scene.camera.farClipPlane = 60f // Render objects further away
            }
        },
        update = { sceneView ->
            try {
                // Manually update the session and get the current frame
                val frame = arSession.update()

                // Find or create the main anchor attached to the camera
                var anchorNode = sceneView.scene.findByName("cameraAnchor") as? AnchorNode
                if (anchorNode == null) {
                    val cameraPose = frame.camera.pose
                    val anchor = arSession.createAnchor(cameraPose)
                    anchorNode = AnchorNode(anchor).apply {
                        name = "cameraAnchor"
                        setParent(sceneView.scene)
                    }
                }

                // Update or add the actual cue ball if it exists
                val existingCueBallNode = anchorNode.findByName("actualCueBall") as? BallNode
                if (uiState.actualCueBall != null && uiState.isBankingMode) { // Only show in banking mode
                    val ballPosition = Vector3(
                        uiState.actualCueBall.logicalPosition.x,
                        0f,
                        uiState.actualCueBall.logicalPosition.y
                    )
                    if (existingCueBallNode == null) {
                        val newBallNode = BallNode(sceneView.context).apply {
                            name = "actualCueBall"
                            localPosition = ballPosition
                            setParent(anchorNode)
                        }
                    } else {
                        existingCueBallNode.localPosition = ballPosition
                    }
                } else {
                    existingCueBallNode?.let {
                        anchorNode.removeChild(it)
                        it.renderable = null
                    }
                }

                // TODO: Implement 3D Protractor "Ghost Ball" and Shot Line
                // A 3D node representing the protractor unit should be created and updated here.
                // val existingProtractorNode = anchorNode.findByName("protractor")
                // if (existingProtractorNode == null) {
                //     // Create and add a new 3D node for the protractor, position it logically.
                // } else {
                //     // Update its position based on uiState.protractorUnit.logicalPosition
                // }
                //
                // Apply rotation from the UI state to the 3D node
                // val rotationY = uiState.protractorUnit.rotationDegrees
                // existingProtractorNode.localRotation = Quaternion.axisAngle(Vector3(0f, 1f, 0f), -rotationY)
                //
                // A 3D line or cylinder would also need to be rendered from the protractor node
                // to represent the shot guide line.

            } catch (e: Exception) {
                Log.e("ArCoreScene", "Failed to update AR scene", e)
                return@AndroidView
            }
        }
    )
}