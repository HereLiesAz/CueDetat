package com.hereliesaz.cuedetat.domain

/**
 * What Lock View ([CueDetatState.isViewLocked]) holds still: every event through which the
 * sensors move the table. Manual gestures, sliders, the rotation swiper and scans are not here,
 * so they keep working while locked.
 */
object SensorLock {

    /** In AR_ACTIVE the live ARCore camera projection is not an IMU control:
     * it must continue to track the real table even when Lock View is selected.
     * All other event classification remains unchanged.
     */
    fun blocks(event: MainScreenEvent, cameraMode: CameraMode): Boolean =
        !(cameraMode == CameraMode.AR_ACTIVE && event is MainScreenEvent.ArTableMatrixUpdated) &&
            blocks(event)

    /** True if [event] is sensor-driven and must be dropped while the view is locked. */
    fun blocks(event: MainScreenEvent): Boolean = when (event) {
        // Compass heading (SystemReducer.followCompass) and phone tilt (pitch matrices).
        is MainScreenEvent.FullOrientationChanged,
        // Felt-fit pull and remembered orientation (MainViewModel.tableSnapSideEffects).
        is MainScreenEvent.TableFitUpdated,
        is MainScreenEvent.ApplyTablePose,
        // Vision / AR / depth tracking.
        is MainScreenEvent.UpdateArPose,
        is MainScreenEvent.ArTableMatrixUpdated,
        is MainScreenEvent.ArCameraPoseUpdated,
        is MainScreenEvent.DepthPlaneUpdated -> true
        else -> false
    }
}
