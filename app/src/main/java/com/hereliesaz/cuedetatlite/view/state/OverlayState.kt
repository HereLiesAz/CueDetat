package com.hereliesaz.cuedetatlite.view.state

import android.graphics.Matrix
import android.graphics.PointF
import com.hereliesaz.cuedetatlite.view.model.TableModel
import com.hereliesaz.cuedetatlite.view.model.LogicalPlane

data class OverlayState(
    // Core state
    val isProtractorMode: Boolean = true,
    val isDarkMode: Boolean = false,
    val isJumpShot: Boolean = false,
    val showProtractorCueBall: Boolean = true,
    val showActualCueBall: Boolean = true,
    val zoomSliderPosition: Float = 0.5f,

    // Spatial state
    val currentOrientation: FullOrientation = FullOrientation(),

    // Logical objects
    val protractorUnit: LogicalPlane.ProtractorUnit = LogicalPlane.ProtractorUnit(),
    val actualCueBall: LogicalPlane.ActualCueBall = LogicalPlane.ActualCueBall(),
    val bankingAimTarget: PointF = PointF(540f, 500f),
    val tableRotationDegrees: Float = 0f,

    // Derived state (calculated by UseCases)
    val pitchMatrix: Matrix = Matrix(),
    val railPitchMatrix: Matrix = Matrix(),
    val screenState: ScreenState = ScreenState(0, 0),
    val tableModel: TableModel? = null,
    val bankingPath: List<PointF> = emptyList()
) {
    data class FullOrientation(
        val pitch: Float = 0f,
        val roll: Float = 0f,
        val yaw: Float = 0f
    )
}

