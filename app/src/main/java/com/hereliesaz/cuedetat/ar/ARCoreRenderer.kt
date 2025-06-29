package com.hereliesaz.cuedetat.ar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.xr.compose.ARScene
import androidx.xr.compose.AnchorNode
import androidx.xr.compose.rememberARState
import androidx.xr.compose.core.Anchor
import com.google.xr.arcore.Config
import com.google.xr.arcore.Plane
import com.hereliesaz.cuedetat.ar.renderables.Table
import com.hereliesaz.cuedetat.ar.rendering.TableNode
import com.hereliesaz.cuedetat.ui.MainViewModel

/**
 * This is the main composable for the AR experience, built with the official
 * Google Jetpack XR library. It replaces the old GLSurfaceView.Renderer paradigm.
 *
 * @param modifier The modifier to be applied to the scene.
 * @param viewModel The ViewModel that holds the application's state.
 */
@Composable
fun ARCoreRenderer(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = viewModel()
) {
    // ARState manages the underlying ARCore session and frame lifecycle.
    val arState = rememberARState()

    // Observe the state from the ViewModel.
    // This allows the UI to reactively update when the state changes.
    val anchors by viewModel.anchors.collectAsState()
    val tablePlaced by viewModel.tablePlaced.collectAsState()

    // ARScene is the root container for your AR content.
    ARScene(
        modifier = modifier,
        arState = arState,
        onSessionConfig = { session, config ->
            // Configure the ARCore session. This is called once.
            config.planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
            config.lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
        },
        onTap = { hitResult ->
            // Handle user taps in the 3D scene.
            // We only allow placing the table once.
            if (!tablePlaced) {
                // Ensure the tap is on a valid, tracking plane.
                val trackable = hitResult.trackable
                if (trackable is Plane && trackable.trackingState == Plane.TrackingState.TRACKING) {
                    viewModel.onPlaneTap(hitResult.createAnchor())
                }
            }
        }
    ) {
        // The content of this lambda is a declarative description of your 3D scene.
        // It will be re-composed whenever the state it depends on changes.
        // This replaces the imperative onDrawFrame() loop.

        // Iterate through the anchors managed by the ViewModel and render them.
        for (appAnchor in anchors) {
            AnchorNode(anchor = appAnchor.arAnchor) {
                // This is where you place your 3D objects relative to the anchor.
                // You would have custom composables like TableNode, BallNode, etc.

                // Example: If the anchor represents the table, render the table.
                if (appAnchor.isTable) {
                    // Assuming TableNode is a @Composable function you've created
                    // that knows how to render your table model.
                    TableNode()
                }

                // Other objects like balls, cue sticks, and lines would be rendered
                // here as well, driven by the state in your ViewModel.
            }
        }
    }
}

// Note: You will need to create a simple data class to help manage your anchors in the ViewModel,
// for example:
// data class AppAnchor(val arAnchor: Anchor, val isTable: Boolean = false)