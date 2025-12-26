package com.hereliesaz.cuedetat.domain

import android.graphics.Camera
import android.graphics.Matrix
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
            return state
        }

        val camera = Camera()
        val matrix = Matrix()

        val width = state.viewWidth.toFloat()
        val height = state.viewHeight.toFloat()
        val centerX = width / 2f
        val centerY = height / 2f

        // Retrieve zoom range and current zoom
        val zoomRange = com.hereliesaz.cuedetat.ui.ZoomMapping.getZoomRange(state.experienceMode)
        val zoomZ = com.hereliesaz.cuedetat.ui.ZoomMapping.sliderToZoom(state.zoomSliderPosition, zoomRange.first, zoomRange.second)

        camera.save()

        // Apply pitch (rotate around X axis)
        // state.currentOrientation.pitch is in degrees (15..90).
        // 0 degrees = Looking straight down (Top-down view).
        // 90 degrees = Looking at horizon.
        camera.rotateX(state.currentOrientation.pitch)

        // Apply zoom (move camera along Z axis)
        // Note: In Android Camera, +Z is "away" into the screen?
        // We translate the *view*?
        // Standard usage: translate(0, 0, -distance) to move camera back / object forward?
        // Existing logic used `zoomZ` (range -50 to 0).
        // We'll stick to that.
        camera.translate(0f, 0f, zoomZ)

        // Get the matrix from camera
        camera.getMatrix(matrix)
        camera.restore()

        // Construct the final transformation matrix
        val m = Matrix()

        // 1. Rotate Table (Yaw) around its center (0,0)
        // This spins the table on the 2D logical plane.
        m.postRotate(state.worldRotationDegrees)

        // 2. Apply Camera Matrix (Pitch + Zoom)
        // The Camera matrix transforms coordinates from "World" (centered at 0,0)
        // to "Camera Space" (centered at 0,0).
        m.postConcat(matrix)

        // 3. Move to Screen Center
        // We want the logical (0,0) (now projected) to be at the center of the screen.
        m.postTranslate(centerX, centerY)

        // 4. Apply Pan (View Offset)
        // We apply the pan in Screen Space so it moves the view consistently
        // regardless of rotation/pitch.
        m.postTranslate(state.viewOffset.x, state.viewOffset.y)

        val pitchMatrix = m

        // Inverse Matrix for touch handling
        val inversePitchMatrix = Matrix()
        val hasInverse = pitchMatrix.invert(inversePitchMatrix)

        // Calculate size calculation matrix (Zoom only, no pitch)
        val flatCamera = Camera()
        flatCamera.save()
        flatCamera.translate(0f, 0f, zoomZ)
        flatCamera.restore()
        val flatCamMatrix = Matrix()
        flatCamera.getMatrix(flatCamMatrix)

        val sizeCalculationMatrix = Matrix()
        sizeCalculationMatrix.postRotate(state.worldRotationDegrees)
        sizeCalculationMatrix.postConcat(flatCamMatrix)
        // Note: DrawingUtils often uses mapRadius which ignores translation,
        // so we don't strictly need to translate here, but scaling must be correct.

        // Logical Plane Matrix (2D only, for mapping screen to logical table plane)
        val logicalPlaneMatrix = Matrix()
        logicalPlaneMatrix.postRotate(state.worldRotationDegrees)
        logicalPlaneMatrix.postTranslate(centerX, centerY)
        logicalPlaneMatrix.postTranslate(state.viewOffset.x, state.viewOffset.y)

        // Rail Pitch Matrix (Same as main matrix for now)
        val railPitchMatrix = pitchMatrix

        // Recalculate Bank Shots and Spin Paths if needed (Full update)
        // Ideally we'd call the injected use cases here.
        // But for now, we just update the matrices.

        return state.copy(
            pitchMatrix = pitchMatrix,
            inversePitchMatrix = inversePitchMatrix,
            hasInverseMatrix = hasInverse,
            sizeCalculationMatrix = sizeCalculationMatrix,
            logicalPlaneMatrix = logicalPlaneMatrix,
            railPitchMatrix = railPitchMatrix,
            flatMatrix = logicalPlaneMatrix
        )
    }
}
