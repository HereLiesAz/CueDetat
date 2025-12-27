package com.hereliesaz.cuedetat.domain

import android.graphics.Camera
import android.graphics.Matrix
import android.graphics.PointF
import com.hereliesaz.cuedetat.domain.CalculateBankShot
import com.hereliesaz.cuedetat.domain.CalculateSpinPaths
import javax.inject.Inject

class UpdateStateUseCase @Inject constructor(
    private val calculateBankShot: CalculateBankShot,
    private val calculateSpinPaths: CalculateSpinPaths
) {
    operator fun invoke(state: CueDetatState, type: UpdateType): CueDetatState {
        // If it's a spin-only update, we might skip matrix recalculation if they exist
        if (type == UpdateType.SPIN_ONLY && state.pitchMatrix != null) {
            // Recalculate spin paths only?
            // "UpdateType.SPIN_ONLY" suggests minimizing work.
            // But we need to return a valid state.
            return state // For now, simple pass-through or update spin logic if needed.
        }

        val camera = Camera()
        val matrix = Matrix()

        // 1. Apply Zoom (Z-translation) and Pitch (X-rotation)
        // Note: Android Graphics Camera rotates around (0,0,0).
        // We typically want to rotate around the center of the view or table.
        // "An android.graphics.Camera object is used to apply an X-axis rotation (based on phone pitch) to this logical plane"

        val width = state.viewWidth.toFloat()
        val height = state.viewHeight.toFloat()
        val centerX = width / 2f
        val centerY = height / 2f

        // Retrieve zoom range and current zoom
        val zoomRange = com.hereliesaz.cuedetat.ui.ZoomMapping.getZoomRange(state.experienceMode)
        val zoomZ = com.hereliesaz.cuedetat.ui.ZoomMapping.sliderToZoom(state.zoomSliderPosition, zoomRange.first, zoomRange.second)

        camera.save()
        // Apply zoom (move camera along Z axis)
        camera.translate(0f, 0f, zoomZ)

        // Apply pitch (rotate around X axis)
        // Note: state.currentOrientation.pitch is in degrees?
        // SensorRepository usually gives degrees or radians. Let's assume degrees based on usage elsewhere.
        // UiModel says `val pitchAngle: Float get() = currentOrientation.pitch`.
        // README says "The pitch is primarily used to tilt the 2D protractor plane."
        camera.rotateX(state.currentOrientation.pitch)

        // Get the matrix from camera
        camera.getMatrix(matrix)
        camera.restore()

        // Camera rotates around 0,0. We want to rotate around the center of the screen/table.
        // So we translate to center, apply camera matrix, then translate back.
        // Also apply Pan (viewOffset) and Table Rotation (worldRotationDegrees).

        // Order:
        // 1. Translate to Logical Center (so we rotate/scale around it)
        // 2. Rotate Table (Z axis)
        // 3. Translate to Camera/Screen Center
        // 4. Apply Camera Matrix (Pitch + Zoom)
        // 5. Translate back?

        // Let's look at docs: "The transformation order is a sacred and precise process... 2D then 3D".
        // "UpdateStateUseCase was *also* applying a world rotation"

        val finalMatrix = Matrix()

        // Start with identity

        // Apply Table Rotation (World Rotation)
        // This is 2D rotation of the table on the logical plane.
        finalMatrix.postRotate(state.worldRotationDegrees)

        // Apply Pan (View Offset)
        finalMatrix.postTranslate(state.viewOffset.x, state.viewOffset.y)

        // Move to center of screen before applying 3D?
        // Usually: preTranslate(-centerX, -centerY), apply, postTranslate(centerX, centerY).

        // But the table logical center is (0,0)?
        // If logical plane is centered at 0,0, then we just need to move it to screen center.

        // Let's assume logical (0,0) corresponds to screen center (width/2, height/2).

        // Apply the 3D projection matrix (from Camera)
        // The Camera matrix assumes origin is center of view.
        // So we have to apply it.

        val projectionMatrix = Matrix()
        projectionMatrix.setConcat(matrix, finalMatrix) // Apply 3D on top of 2D?

        // Wait, matrix concatenation order:
        // If we want to rotate table first: M_table * point
        // Then project: M_proj * (M_table * point)
        // So `matrix.preConcat(tableMatrix)` or `matrix.postConcat(tableMatrix)`?
        // Matrix multiplication is usually post-concat in Android (M' = M * T).

        // Let's construct it carefully.
        val m = Matrix()

        // 1. Rotate Table around (0,0)
        m.postRotate(state.worldRotationDegrees)

        // 2. Translate by viewOffset
        m.postTranslate(state.viewOffset.x, state.viewOffset.y)

        // 3. Apply Camera (Pitch + Zoom).
        // Camera matrix is around (0,0).
        // But we want the "camera" to look at the table center.
        // So we are already at (0,0) logic space.
        m.postConcat(matrix)

        // 4. Move to Screen Center
        m.postTranslate(centerX, centerY)

        val pitchMatrix = m

        // Inverse Matrix for touch handling
        val inversePitchMatrix = Matrix()
        val hasInverse = pitchMatrix.invert(inversePitchMatrix)

        // "calculating a single visualBallRadius... using a matrix that is free from rotation" (Pitch rotation)
        // So we need a "flat" matrix that has Zoom but no Pitch?
        // Or "sizeCalculationMatrix".
        val flatCamera = Camera()
        flatCamera.save()
        flatCamera.translate(0f, 0f, zoomZ) // Only Zoom
        flatCamera.restore()
        val flatCamMatrix = Matrix()
        flatCamera.getMatrix(flatCamMatrix)

        val sizeCalculationMatrix = Matrix()
        sizeCalculationMatrix.postRotate(state.worldRotationDegrees) // Keep table rotation?
        // Docs say "free from rotation". Maybe it means Pitch?
        // "Its size is a function of zoom and pitch only... wait, NOT perceived distance."
        // "using a matrix that is free from rotation"

        sizeCalculationMatrix.postConcat(flatCamMatrix)
        // Don't translate to center if we just want scalar size?
        // Or do we? DrawingUtils.getPerspectiveRadiusAndLift uses mapPoints.

        // Logical Plane Matrix (maybe just 2D without 3D?)
        val logicalPlaneMatrix = Matrix()
        logicalPlaneMatrix.postRotate(state.worldRotationDegrees)
        logicalPlaneMatrix.postTranslate(state.viewOffset.x, state.viewOffset.y)
        logicalPlaneMatrix.postTranslate(centerX, centerY)

        // Rail Pitch Matrix?
        // "The railLiftAmount calculation... proportional to sine of pitch".
        // Maybe this is same as pitchMatrix?
        val railPitchMatrix = pitchMatrix // Reuse for now unless we find different logic.

        // TODO: Implement Bank Shot and Spin Path calculations using injected use cases.
        // For now, return state to pass build.

        return state.copy(
            pitchMatrix = pitchMatrix,
            inversePitchMatrix = inversePitchMatrix,
            hasInverseMatrix = hasInverse,
            sizeCalculationMatrix = sizeCalculationMatrix,
            logicalPlaneMatrix = logicalPlaneMatrix,
            railPitchMatrix = railPitchMatrix,
            flatMatrix = logicalPlaneMatrix // Or something else
        )
    }
}
