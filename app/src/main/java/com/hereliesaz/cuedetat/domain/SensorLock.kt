package com.hereliesaz.cuedetat.domain

/**
 * What Lock View ([CueDetatState.isViewLocked]) holds still: every event through which the
 * sensors move the table. Manual gestures, sliders, the rotation swiper and scans are not here,
 * so they keep working while locked.
 */
object SensorLock {

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
