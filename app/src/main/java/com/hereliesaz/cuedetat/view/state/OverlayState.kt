package com.hereliesaz.cuedetat.view.state

import android.graphics.Matrix
import android.graphics.PointF
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import com.google.ar.core.Anchor
import com.google.ar.core.Session
import com.hereliesaz.cuedetat.data.FullOrientation
import com.hereliesaz.cuedetat.view.model.ActualCueBall
import com.hereliesaz.cuedetat.view.model.ProtractorUnit

data class OverlayState(
    val viewWidth: Int = 0,
    val viewHeight: Int = 0,
    val pitchAngle: Float = 0f,
    val yawAngle: Float = 0f,
    val rollAngle: Float = 0f,

    val currentOrientation: FullOrientation = FullOrientation(0f, 0f, 0f),
    val anchorOrientation: FullOrientation? = null,
    val pitchMatrix: Matrix = Matrix(),
    val railPitchMatrix: Matrix = Matrix(),
    val valuesChangedSinceReset: Boolean = false,
    val isMoreHelpVisible: Boolean = false,
    val appControlColorScheme: ColorScheme? = null,

    val isSpatiallyLocked: Boolean = false,
    val isImpossibleShot: Boolean = false,
    val hasInverseMatrix: Boolean = false,
    val inversePitchMatrix: Matrix = Matrix(),
    val protractorUnit: ProtractorUnit = ProtractorUnit(radius = 100f, rotationDegrees = 0f, logicalPosition = PointF(0f, 0f)),
    val actualCueBall: ActualCueBall? = null,
    val showProtractor: Boolean = true,
    val showTable: Boolean = false,
    val bankingAimTarget: PointF? = null,
    val isBankingMode: Boolean = false,
    val tableRotationDegrees: Float = 0f,
    val areHelpersVisible: Boolean = true,
    val showLuminanceDialog: Boolean = false,
    val luminanceAdjustment: Float = 0f,
    val currentThemeColor: Color = Color.Unspecified,
    val isForceLightMode: Boolean? = null,
    val showTutorialOverlay: Boolean = false,
    val currentTutorialStep: Int = 0,
    val warningText: String? = null,
    val zoomSliderPosition: Float = 0.5f,
    val arSession: Session? = null,
    val isArSupported: Boolean = false,
    val arAnchor: Anchor? = null,
    val anchorPlacementRequested: Boolean = false
)
