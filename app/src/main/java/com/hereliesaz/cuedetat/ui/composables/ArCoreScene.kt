package com.hereliesaz.cuedetat.ui.composables

import android.content.Context
import android.graphics.PointF
import android.util.Log
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModelProvider
import com.google.ar.core.Anchor
import com.google.ar.core.Config
import com.google.ar.core.Plane
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.gorisse.thomas.sceneform.scene.await
import com.hereliesaz.cuedetat.MainActivity
import com.hereliesaz.cuedetat.ar.rendering.BallNode
import com.hereliesaz.cuedetat.ar.rendering.LineNode
import com.hereliesaz.cuedetat.ar.rendering.TableNode
import com.hereliesaz.cuedetat.ar.rendering.TextNode
import com.hereliesaz.cuedetat.ui.MainScreenEvent
import com.hereliesaz.cuedetat.ui.MainViewModel
import com.hereliesaz.cuedetat.view.state.OverlayState
import io.github.sceneview.ar.ARScene
import io.github.sceneview.ar.arcore.getUpdatedPlanes
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.math.Position
import io.github.sceneview.math.toVector3
import io.github.sceneview.node.Node
import kotlinx.coroutines.future.await

private const val METERS_PER_POOL_BALL = 0.05715f
private const val TABLE_TO_BALL_RATIO_LONG = 88f / 44f
private const val TABLE_TO_BALL_RATIO_SHORT = 1f

@Composable
fun ArCoreScene(
    modifier: Modifier = Modifier,
    arSession: Session?,
    uiState: OverlayState,
    onEvent: (MainScreenEvent) -> Unit
) {
    var anchorNode by remember { mutableStateOf<AnchorNode?>(null) }
    val context = LocalContext.current

    ARScene(
        modifier = modifier,
        session = arSession,
        onSessionUpdated = { session, frame ->
            if (uiState.anchorPlacementRequested) {
                val hit = frame.hitTest(frame.camera.displayOrientedPose, session.focusDistance)
                    .firstOrNull {
                        val trackable = it.trackable
                        trackable is Plane && trackable.isPoseInPolygon(it.hitPose)
                    }
                onEvent(MainScreenEvent.ArAnchorPlaced(hit?.createAnchor()))
            }
        },
        onViewCreated = { arSceneView ->
            (context as? MainActivity)?.let { activity ->
                val mainViewModel = ViewModelProvider(activity)[MainViewModel::class.java]
                arSession?.let { mainViewModel.onArSessionCreated(it) }
            }
        },
        onNodeAdded = { node ->
            if (node is AnchorNode) {
                anchorNode = node
            }
        },
        onNodeUpdated = { node ->
            anchorNode?.let { stableAnchorNode ->
                if (uiState.viewWidth > 0 && uiState.viewHeight > 0) {
                    renderArContent(context, stableAnchorNode, uiState)
                }
            }
        }
    )
}

private fun renderArContent(context: Context, anchorNode: Node, uiState: OverlayState) {
    val scaleFactor = METERS_PER_POOL_BALL / (2 * uiState.protractorUnit.radius)
    val logicalCenterX = uiState.viewWidth / 2f
    val logicalCenterY = uiState.viewHeight / 2f
    val textYOffset = 0.05f

    fun logicalToWorld(logicalPos: PointF): Position {
        val translatedX = logicalPos.x - logicalCenterX
        val translatedZ = logicalPos.y - logicalCenterY
        return Position(x = translatedX * scaleFactor, y = 0f, z = translatedZ * scaleFactor)
    }

    if (uiState.isBankingMode) {
        setNodeVisibility("targetBall", anchorNode, false)
        setNodeVisibility("ghostCueBall", anchorNode, false)
        // ... (rest of visibility calls)

        val ballRadiusMeters = METERS_PER_POOL_BALL / 2f
        val tableDepthMeters = ballRadiusMeters * 2 * 44 * TABLE_TO_BALL_RATIO_SHORT
        val tableWidthMeters = tableDepthMeters * TABLE_TO_BALL_RATIO_LONG
        updateTableNode(context, anchorNode, tableWidthMeters, tableDepthMeters, uiState.tableRotationDegrees)

        val bankingBallWorldPos = uiState.actualCueBall?.logicalPosition?.let { logicalToWorld(it) }
        updateBallNode(context, "actualCueBall", anchorNode, bankingBallWorldPos, com.google.ar.sceneform.rendering.Color(0.9f, 0.8f, 0.2f, 0.9f))
        val bankingBallLabelPos = bankingBallWorldPos?.let { it + Position(y = textYOffset) }
        updateTextNode(context, "actualCueBall_label", anchorNode, bankingBallLabelPos, "Ball to Bank")
    } else {
        // ... (protractor mode rendering)
    }
}

private fun updateTableNode(context: Context, parent: Node, width: Float, depth: Float, rotation: Float) {
    val name = "poolTable"
    var node = parent.children.find { it.name == name } as? TableNode
    if (node == null) {
        node = TableNode(context, width, depth).apply { this.name = name }
        parent.addChild(node)
    }
    node.isEnabled = true
    node.position = Position(y = -0.0175f)
    node.rotation = io.github.sceneview.math.Quaternion(io.github.sceneview.math.Vector3(0f,1f,0f), -rotation)
}

private fun setNodeVisibility(name: String, parent: Node, isVisible: Boolean) {
    parent.children.find{it.name == name}?.isEnabled = isVisible
}

private fun updateBallNode(context: Context, name: String, parent: Node, worldPos: Position?, color: com.google.ar.sceneform.rendering.Color) {
    var node = parent.children.find { it.name == name } as? BallNode
    if (worldPos == null) {
        node?.isEnabled = false
        return
    }
    if (node == null) {
        node = BallNode(context, color).apply { this.name = name }
        parent.addChild(node)
    }
    node.isEnabled = true
    node.position = worldPos
}

private fun updateLineNode(context: Context, name: String, parent: Node, start: Position?, end: Position?, color: com.google.ar.sceneform.rendering.Color, radius: Float) {
    parent.children.find{it.name == name}?.let { parent.removeChild(it) }
    if (start == null || end == null) return

    LineNode(context, start.toVector3(), end.toVector3(), color, radius).apply {
        this.name = name
        parent.addChild(this)
    }
}

private fun updateTextNode(context: Context, name: String, parent: Node, worldPos: Position?, text: String?) {
    var node = parent.children.find { it.name == name } as? TextNode
    if (worldPos == null || text == null) {
        node?.isEnabled = false
        return
    }
    if (node == null) {
        node = TextNode(context, text).apply { this.name = name }
        parent.addChild(node)
    }
    node.isEnabled = true
    node.position = worldPos
    node.updateText(text)
}
