package com.hereliesaz.cuedetat.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.PointF
import android.util.AttributeSet
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration // Import ViewConfiguration
import androidx.compose.material3.ColorScheme
import com.hereliesaz.cuedetat.config.AppConfig
import com.hereliesaz.cuedetat.state.AppPaints
import com.hereliesaz.cuedetat.state.AppState
import com.hereliesaz.cuedetat.state.AppState.SelectionMode
import com.hereliesaz.cuedetat.drawing.DrawingCoordinator
import com.hereliesaz.cuedetat.view.gesture.GestureHandler
import com.hereliesaz.cuedetat.tracking.ball_detector.Ball
import com.hereliesaz.cuedetat.system.CameraManager
import kotlin.math.hypot
import kotlin.math.min

class MainOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val TAG = AppConfig.TAG + "_MainOverlayView"

    interface AppStateListener {
        fun onZoomChanged(newZoomFactor: Float)
        fun onRotationChanged(newRotationAngle: Float)
        fun onUserInteraction()
        fun onCueBallSelected(ballId: String?)
        fun onTargetBallSelected(ballId: String?)
        fun onSelectionModeChanged(mode: SelectionMode)
    }

    var listener: AppStateListener? = null

    private val config = AppConfig
    private val appState = AppState(config)
    private val appPaints = AppPaints(context, config)

    private lateinit var gestureHandler: GestureHandler
    private lateinit var drawingCoordinator: DrawingCoordinator

    // Gesture detection for taps
    private lateinit var gestureDetector: GestureDetector
    private var touchSlop: Int = 0 // Threshold for detecting a drag vs. a tap

    // State for dragging ghost balls
    private var isDraggingConfirmed = false // Whether a drag gesture has been confirmed (moved beyond touch slop)
    private var initialDraggedBall: Ball? = null // Store the ball identified at ACTION_DOWN
    private var dragOffsetX: Float = 0f // Touch point relative to ball center X
    private var dragOffsetY: Float = 0f // Touch point relative to ball center Y
    private var initialTouchDownX: Float = 0f // Initial touch X for drag threshold
    private var initialTouchDownY: Float = 0f // Initial touch Y for drag threshold

    private var cameraManagerRef: CameraManager? = null
    private var areComponentsInitialized = false

    init {
        // Get the system-defined touch slop
        val vc = ViewConfiguration.get(context)
        touchSlop = vc.scaledTouchSlop
    }

    fun setCameraManager(cameraManager: CameraManager) {
        this.cameraManagerRef = cameraManager
        Log.d(TAG, "setCameraManager: CameraManager reference received.")
        if (!areComponentsInitialized && width > 0 && height > 0) {
            initializeComponents()
        }
    }

    private fun initializeComponents() {
        Log.d(TAG, "initializeComponents executing full setup. areComponentsInitialized was $areComponentsInitialized, width=$width, height=$height")
        if (areComponentsInitialized) {
            Log.w(TAG, "initializeComponents: Already initialized. Skipping.")
            return
        }
        if (cameraManagerRef == null || width == 0 || height == 0) {
            Log.w(TAG, "initializeComponents: Pre-conditions not met (cameraManagerRef=${cameraManagerRef != null}, width=$width, height=$height). Skipping.")
            return
        }

        gestureHandler = GestureHandler(
            context, appState, config, listener,
            onZoomChangedByGesture = { newZoomFactor ->
                setZoomFactorInternal(newZoomFactor, true)
            },
            onRotationChangedByGesture = { newRotationAngle ->
                setProtractorRotationAngleInternal(newRotationAngle, true)
            }
        )

        drawingCoordinator = DrawingCoordinator(
            appState, appPaints, config,
            viewWidthProvider = { width },
            viewHeightProvider = { height }
        )

        gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapUp(e: MotionEvent): Boolean {
                // If a drag was confirmed, this method should not be called by the overall onTouchEvent logic.
                // If we reach here, it means it's a tap, either on a ball or empty space.
                Log.d(TAG, "onSingleTapUp detected at (${e.x}, ${e.y}). Calling handleSingleTap.")
                handleSingleTap(e.x, e.y)
                return true // Consume all single taps handled here
            }

            override fun onDown(e: MotionEvent): Boolean {
                // We return true here to ensure GestureDetector processes the full gesture (for taps, scrolls, long presses).
                // Our custom drag logic in onTouchEvent handles the consumption for drag starts.
                return true
            }

            override fun onScroll(
                e1: MotionEvent?,
                e2: MotionEvent,
                distanceX: Float,
                distanceY: Float
            ): Boolean {
                // This method is called by GestureDetector for scrolling motions.
                // If we have an initial touch on a ball and haven't confirmed a drag yet,
                // check if this scroll signifies a drag.
                if (initialDraggedBall != null && !isDraggingConfirmed) {
                    val dx = e2.x - initialTouchDownX
                    val dy = e2.y - initialTouchDownY
                    if (hypot(dx.toDouble(), dy.toDouble()) > touchSlop) {
                        isDraggingConfirmed = true // Confirm drag here
                        Log.d(TAG, "onScroll: Drag confirmed due to movement exceeding touchSlop.")
                    }
                }
                // Return false to allow our main onTouchEvent to continue processing for pan/zoom and ball drag updates.
                return false
            }
        })

        areComponentsInitialized = true
        Log.d(TAG, "MainOverlayView components initialized. areComponentsInitialized is now $areComponentsInitialized")
    }


    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        Log.d(TAG, "onSizeChanged: w=$w, h=$h. Calling appState.initialize.")
        if (w > 0 && h > 0) {
            appState.initialize(w, h)
            if (!areComponentsInitialized && cameraManagerRef != null) {
                initializeComponents()
            }
            invalidate()
        }
    }

    fun applyMaterialYouColors(colorScheme: ColorScheme) {
        if (!areComponentsInitialized) {
            Log.w(TAG, "applyMaterialYouColors called before components initialized. Skipping.")
            return
        }
        appPaints.applyMaterialYouColors(colorScheme)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!areComponentsInitialized || !appState.isInitialized) {
            Log.w(TAG, "onDraw returning early: areComponentsInitialized=$areComponentsInitialized, appState.isInitialized=${appState.isInitialized}")
            return
        }
        drawingCoordinator.onDraw(canvas)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!areComponentsInitialized || !appState.isInitialized) {
            Log.w(TAG, "onTouchEvent returning early: components not initialized.")
            return false
        }

        // Pass to GestureDetector first for tap detection and to trigger onScroll for drag confirmation.
        // We capture its return value to potentially consume the event if GestureDetector handles it,
        // but we prioritize our custom drag and pan/zoom logic.
        val gestureDetectorConsumed = gestureDetector.onTouchEvent(event)

        val x = event.x
        val y = event.y

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                // Reset drag state for new touch sequence.
                isDraggingConfirmed = false
                initialDraggedBall = null // Ensure it's null before potential assignment
                dragOffsetX = 0f
                dragOffsetY = 0f
                initialTouchDownX = x
                initialTouchDownY = y

                // Check if tap is on selected cue ball for potential drag
                appState.selectedCueBall?.let { ball ->
                    // Use the logicalBallRadius scaled by zoomFactor for hit test, as overlays are now scaled by it.
                    val effectiveRadius = appState.logicalBallRadius * appState.zoomFactor
                    if (hypot(x - ball.x, y - ball.y) <= effectiveRadius + config.DRAG_TOUCH_TOLERANCE) {
                        initialDraggedBall = ball
                        dragOffsetX = x - ball.x
                        dragOffsetY = y - ball.y
                        listener?.onUserInteraction()
                        Log.d(TAG, "ACTION_DOWN: Cue ball hit for potential drag.")
                        return true // Consume DOWN to track subsequent MOVE/UP events.
                    }
                }

                // Check if tap is on selected target ball (only if not already decided to drag cue)
                if (initialDraggedBall == null) { // Only if cue ball was not touched
                    appState.selectedTargetBall?.let { ball ->
                        // Use the logicalBallRadius scaled by zoomFactor for hit test, as overlays are now scaled by it.
                        val effectiveRadius = appState.logicalBallRadius * appState.zoomFactor
                        if (hypot(x - ball.x, y - ball.y) <= effectiveRadius + config.DRAG_TOUCH_TOLERANCE) {
                            initialDraggedBall = ball
                            dragOffsetX = x - ball.x
                            dragOffsetY = y - ball.y
                            listener?.onUserInteraction()
                            Log.d(TAG, "ACTION_DOWN: Target ball hit for potential drag.")
                            return true // Consume DOWN to track subsequent MOVE/UP events.
                        }
                    }
                }
                // If no ball was hit for a potential drag, allow pan/zoom or tap-empty-space.
                // We've already passed to GestureDetector, so it might consume.
                // We also pass to gestureHandler (pan/zoom) if in AIMING mode.
                val panZoomHandled = if (appState.currentMode == SelectionMode.AIMING) {
                    gestureHandler.onTouchEvent(event)
                } else {
                    false
                }
                return panZoomHandled || gestureDetectorConsumed || super.onTouchEvent(event)
            }

            MotionEvent.ACTION_MOVE -> {
                if (initialDraggedBall != null) { // Only proceed if a ball was initially touched
                    // The onScroll method of GestureDetector will set `isDraggingConfirmed` if movement exceeds slop.
                    if (isDraggingConfirmed) { // Only update position if drag is confirmed
                        val newCenterX = x - dragOffsetX
                        val newCenterY = y - dragOffsetY

                        val updatedBall = initialDraggedBall!!.copy(x = newCenterX, y = newCenterY)
                        if (initialDraggedBall?.id == appState.selectedCueBall?.id) {
                            appState.updateSelectedCueBall(updatedBall)
                        } else if (initialDraggedBall?.id == appState.selectedTargetBall?.id) {
                            appState.updateSelectedTargetBall(updatedBall)
                        }
                        invalidate()
                        return true // Consume event for dragging
                    }
                }
                // If not actively dragging a ball (either no ball was touched, or drag is not confirmed yet),
                // then pan/zoom might handle it.
                val panZoomHandled = if (appState.currentMode == SelectionMode.AIMING) {
                    gestureHandler.onTouchEvent(event)
                } else {
                    false
                }
                return panZoomHandled || gestureDetectorConsumed || super.onTouchEvent(event)
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                val wasConfirmedDrag = isDraggingConfirmed // Capture state before reset
                val ballToProcess = initialDraggedBall // Capture the ball reference

                // Reset all drag state regardless of outcome
                isDraggingConfirmed = false
                initialDraggedBall = null
                dragOffsetX = 0f
                dragOffsetY = 0f
                initialTouchDownX = 0f // Reset for next touch sequence
                initialTouchDownY = 0f // Reset for next touch sequence

                if (wasConfirmedDrag && ballToProcess != null) { // This was a confirmed drag
                    val finalX = x - dragOffsetX
                    val finalY = y - dragOffsetY
                    handleSnapOnRelease(finalX, finalY, ballToProcess) // ballToProcess is guaranteed non-null here
                    Log.d(TAG, "ACTION_UP/CANCEL: Confirmed drag ended for ${ballToProcess.id}.")
                    return true // Consume event
                } else if (ballToProcess != null) { // Was a potential drag (ball touched), but not confirmed (i.e., a tap on a ball)
                    Log.d(TAG, "ACTION_UP/CANCEL: Potential drag was a tap on ball (${ballToProcess.id}). Calling handleSingleTap.")
                    // Use the coordinates from the UP event for the tap detection
                    handleSingleTap(x, y)
                    return true // Consume event as it's handled as a tap
                }
                // If neither drag nor tap on ball handled, let gestureDetector's processing result or pan/zoom handle it
                val panZoomHandled = if (appState.currentMode == SelectionMode.AIMING) {
                    gestureHandler.onTouchEvent(event)
                } else {
                    false
                }
                return panZoomHandled || gestureDetectorConsumed || super.onTouchEvent(event)
            }
        }
        return gestureDetectorConsumed || super.onTouchEvent(event) // Fallback for other events
    }

    fun updateTrackedBalls(balls: List<Ball>, frameWidth: Int, frameHeight: Int) {
        Log.d(TAG, "updateTrackedBalls received: balls.size=${balls.size}, frame=${frameWidth}x${frameHeight}. Current view size: ${this.width}x${this.height}")
        if (!appState.isInitialized || this.width == 0 || this.height == 0 || frameWidth == 0 || frameHeight == 0) {
            Log.w(TAG, "updateTrackedBalls: view/frame dimensions not ready or zero, skipping. View: ${this.width}x${this.height}, Frame: ${frameWidth}x${frameHeight}")
            appState.trackedBalls = emptyList()
            invalidate()
            return
        }

        val scaleX = this.width.toFloat() / frameWidth.toFloat()
        val scaleY = this.height.toFloat() / frameHeight.toFloat()
        val scaleFactor = min(scaleX, scaleY)

        val scaledFrameWidth = frameWidth * scaleFactor
        val scaledFrameHeight = frameHeight * scaleFactor
        val offsetX = (this.width - scaledFrameWidth) / 2f
        val offsetY = (this.height - scaledFrameHeight) / 2f

        // Apply AppConfig.BALL_RADIUS_SCALE_FACTOR only at the detection level (BallDetector.kt).
        // Here, we just scale the detected pixels to screen pixels.
        val scaledBalls = balls.map { ball ->
            Ball(
                id = ball.id,
                x = ball.x * scaleFactor + offsetX,
                y = ball.y * scaleFactor + offsetY,
                radius = ball.radius * scaleFactor // Radius is already scaled by frame-to-view scale here
            )
        }
        appState.trackedBalls = scaledBalls
        Log.d(TAG, "updateTrackedBalls: Scaled ${scaledBalls.size} balls. First ball (scaled): ${scaledBalls.firstOrNull()}")


        appState.selectedCueBall?.let { selectedCue ->
            if (!selectedCue.id.startsWith("MANUAL_")) {
                scaledBalls.firstOrNull { it.id == selectedCue.id }?.let { latestCue ->
                    appState.updateSelectedCueBall(latestCue)
                    Log.d(TAG, "updateTrackedBalls: Selected detected cue ball ${latestCue.id} updated.")
                } ?: run {
                    Log.d(TAG, "updateTrackedBalls: Previously selected DETECTED cue ball disappeared from tracking, but remains selected.")
                }
            }
        }

        appState.selectedTargetBall?.let { selectedTarget ->
            if (!selectedTarget.id.startsWith("MANUAL_")) {
                scaledBalls.firstOrNull { it.id == selectedTarget.id }?.let { latestTarget ->
                    appState.updateSelectedTargetBall(latestTarget)
                    Log.d(TAG, "updateTrackedBalls: Selected detected target ball ${latestTarget.id} updated.")
                } ?: run {
                    Log.d(TAG, "updateTrackedBalls: Previously selected DETECTED target ball disappeared from tracking, but remains selected.")
                }
            }
        }

        invalidate()
    }

    fun updateCameraZoomCapabilities(minZoom: Float, maxZoom: Float) {
        appState.updateCameraZoomCapabilities(minZoom, maxZoom)
    }

    /**
     * Handles a single tap event on the overlay for ball selection or manual placement.
     */
    private fun handleSingleTap(tapX: Float, tapY: Float) {
        var closestDetectedBall: Ball? = null
        var minDistanceToDetected = Float.MAX_VALUE

        for (ball in appState.trackedBalls) {
            // Apply zoom factor to the ball's radius for hit testing.
            val effectiveRadius = ball.radius * appState.zoomFactor
            val dist = hypot(tapX - ball.x, tapY - ball.y)
            if (dist <= effectiveRadius + 20f) { // Use effectiveRadius for hit test
                if (dist < minDistanceToDetected) {
                    minDistanceToDetected = dist
                    closestDetectedBall = ball
                }
            }
        }

        val previousMode = appState.currentMode
        var modeChanged = false
        Log.d(TAG, "handleSingleTap: Tap at (${tapX}, ${tapY}), closestDetectedBall: ${closestDetectedBall?.id ?: "NONE"}. Current mode: ${appState.currentMode}")


        when (appState.currentMode) {
            SelectionMode.SELECTING_CUE_BALL -> {
                if (closestDetectedBall != null) {
                    appState.updateSelectedCueBall(closestDetectedBall)
                    appState.currentMode = SelectionMode.SELECTING_TARGET_BALL
                    listener?.onCueBallSelected(closestDetectedBall.id)
                    Log.d(TAG, "User selected detected cue ball: ${closestDetectedBall.id}. Mode changed to ${appState.currentMode}")
                    modeChanged = true
                } else {
                    appState.setManualCueBall(tapX, tapY)
                    appState.currentMode = SelectionMode.SELECTING_TARGET_BALL
                    listener?.onCueBallSelected(appState.selectedCueBall?.id)
                    Log.d(TAG, "User manually placed cue ball at (${tapX}, ${tapY}). Mode changed to ${appState.currentMode}")
                    modeChanged = true
                }
            }
            SelectionMode.SELECTING_TARGET_BALL -> {
                val selectedCueCenter = appState.selectedCueBall?.let { PointF(it.x, it.y) }
                // Use logicalBallRadius scaled by zoomFactor for hit test on selected cue.
                val selectedCueEffectiveRadius = appState.logicalBallRadius * appState.zoomFactor
                val distToSelectedCue = selectedCueCenter?.let { hypot(tapX - it.x, tapY - it.y) } ?: Float.MAX_VALUE

                if (distToSelectedCue <= selectedCueEffectiveRadius + 20f) { // Use effective radius
                    appState.clearSelectedCueBall()
                    appState.clearSelectedTargetBall()
                    appState.currentMode = SelectionMode.SELECTING_CUE_BALL
                    listener?.onCueBallSelected(null)
                    listener?.onTargetBallSelected(null)
                    Log.d(TAG, "User tapped current cue ball, resetting selection. Mode changed to ${appState.currentMode}")
                    modeChanged = true
                } else if (closestDetectedBall != null && closestDetectedBall.id != appState.selectedCueBall?.id) {
                    appState.updateSelectedTargetBall(closestDetectedBall)
                    appState.currentMode = SelectionMode.AIMING
                    listener?.onTargetBallSelected(closestDetectedBall.id)
                    Log.d(TAG, "User selected detected target ball: ${closestDetectedBall.id}. Mode changed to ${appState.currentMode}")
                    modeChanged = true
                } else {
                    appState.setManualTargetBall(tapX, tapY)
                    appState.currentMode = SelectionMode.AIMING
                    listener?.onTargetBallSelected(appState.selectedTargetBall?.id)
                    Log.d(TAG, "User manually placed target ball at (${tapX}, ${tapY}). Mode changed to ${appState.currentMode}")
                    modeChanged = true
                }
            }
            SelectionMode.AIMING -> {
                val selectedTargetBallCenter = appState.selectedTargetBall?.let { PointF(it.x, it.y) }
                val selectedTargetEffectiveRadius = appState.logicalBallRadius * appState.zoomFactor // Use effective radius
                val distToSelectedTarget = selectedTargetBallCenter?.let { hypot(tapX - it.x, tapY - it.y) } ?: Float.MAX_VALUE

                val selectedCueBallCenter = appState.selectedCueBall?.let { PointF(it.x, it.y) }
                val selectedCueEffectiveRadius = appState.logicalBallRadius * appState.zoomFactor // Use effective radius
                val distToSelectedCue = selectedCueBallCenter?.let { hypot(tapX - it.x, tapY - it.y) } ?: Float.MAX_VALUE


                if (distToSelectedCue <= selectedCueEffectiveRadius + 20f) { // Use effective radius
                    appState.clearSelectedCueBall()
                    appState.clearSelectedTargetBall()
                    appState.currentMode = SelectionMode.SELECTING_CUE_BALL
                    listener?.onCueBallSelected(null)
                    listener?.onTargetBallSelected(null)
                    Log.d(TAG, "User tapped selected cue ball during aiming, resetting selection. Mode changed to ${appState.currentMode}")
                    modeChanged = true
                } else if (distToSelectedTarget <= selectedTargetEffectiveRadius + 20f) { // Use effective radius
                    appState.clearSelectedTargetBall()
                    appState.currentMode = SelectionMode.SELECTING_TARGET_BALL
                    listener?.onTargetBallSelected(null)
                    Log.d(TAG, "User tapped selected target ball, deselecting. Mode changed to ${appState.currentMode}")
                    modeChanged = true
                } else if (closestDetectedBall != null && closestDetectedBall.id != appState.selectedCueBall?.id) {
                    appState.updateSelectedTargetBall(closestDetectedBall)
                    listener?.onTargetBallSelected(closestDetectedBall.id)
                    Log.d(TAG, "User selected new detected target ball: ${closestDetectedBall.id}.")
                } else {
                    appState.setManualTargetBall(tapX, tapY)
                    listener?.onTargetBallSelected(appState.selectedTargetBall?.id)
                    Log.d(TAG, "User manually re-placed target ball at (${tapX}, ${tapY}). Mode remains ${appState.currentMode}")
                }
            }
        }
        if (modeChanged) {
            listener?.onSelectionModeChanged(appState.currentMode)
        }
        listener?.onUserInteraction()
        invalidate()
    }

    /**
     * Handles snapping a dragged ball to a nearby detected ball on release.
     */
    private fun handleSnapOnRelease(finalX: Float, finalY: Float, draggedBall: Ball) {
        var snapped = false
        // Use the logicalBallRadius for snap tolerance calculation, scaled by zoomFactor for screen space.
        val snapTolerance = (appState.logicalBallRadius * appState.zoomFactor) * config.SNAP_RADIUS_FACTOR

        for (detectedBall in appState.trackedBalls) {
            // Avoid snapping a detected ball back to itself (it's already tracking)
            // A manually placed ball that snaps to a detected ball should get the detected ball's ID.
            // A previously detected ball should not snap to itself if it's still being tracked.
            if (draggedBall.id == detectedBall.id && !draggedBall.id.startsWith("MANUAL_")) {
                continue
            }

            val distance = hypot(finalX - detectedBall.x, finalY - detectedBall.y)
            if (distance <= snapTolerance) {
                // Snap to this detected ball
                val newBall = detectedBall.copy()
                if (draggedBall.id == appState.selectedCueBall?.id) {
                    appState.updateSelectedCueBall(newBall)
                    listener?.onCueBallSelected(newBall.id)
                    Log.d(TAG, "Cue ball snapped to detected ball: ${newBall.id}")
                } else if (draggedBall.id == appState.selectedTargetBall?.id) {
                    appState.updateSelectedTargetBall(newBall)
                    listener?.onTargetBallSelected(newBall.id)
                    Log.d(TAG, "Target ball snapped to detected ball: ${newBall.id}")
                }
                snapped = true
                break
            }
        }

        if (!snapped) {
            // If not snapped, just update its position to the final drag location as a manual ball
            // Ensure the radius remains the current logical radius (same as target/ghost balls)
            val finalBall = draggedBall.copy(x = finalX, y = finalY, radius = appState.logicalBallRadius) // Use logicalBallRadius
            if (draggedBall.id == appState.selectedCueBall?.id) {
                appState.updateSelectedCueBall(finalBall)
                listener?.onCueBallSelected(finalBall.id)
                Log.d(TAG, "Cue ball placed manually after drag, maintaining logical radius.")
            } else if (draggedBall.id == appState.selectedTargetBall?.id) {
                appState.updateSelectedTargetBall(finalBall)
                listener?.onTargetBallSelected(finalBall.id)
                Log.d(TAG, "Target ball placed manually after drag, maintaining logical radius.")
            }
        }
    }

    fun setZoomFactor(factor: Float) {
        setZoomFactorInternal(factor, false)
    }

    private fun setZoomFactorInternal(factor: Float, isUserInitiatedInView: Boolean) {
        if (!areComponentsInitialized || !appState.isInitialized) {
            Log.w(TAG, "setZoomFactorInternal returning early: components not initialized.")
            return
        }

        if (appState.updateZoomFactor(factor)) {
            cameraManagerRef?.setCameraZoomRatio(appState.zoomFactor)
            Log.d(TAG, "Camera zoom ratio set to: ${appState.zoomFactor}")

            if (isUserInitiatedInView) {
                listener?.onZoomChanged(appState.zoomFactor)
                listener?.onUserInteraction()
            } else {
                listener?.onZoomChanged(appState.zoomFactor)
            }
            invalidate()
        } else {
            Log.d(TAG, "setZoomFactorInternal: Zoom factor not significantly changed. Skipping update.")
        }
    }

    fun getZoomFactor(): Float = if (appState.isInitialized) appState.zoomFactor else config.DEFAULT_ZOOM_FACTOR
    fun getMinCameraZoomFactor(): Float = if (appState.isInitialized) appState.minCameraZoomRatio else 1.0f
    fun getMaxCameraZoomFactor(): Float = if (appState.isInitialized) appState.maxCameraZoomRatio else 1.0f

    fun setProtractorRotationAngle(angle: Float) {
        setProtractorRotationAngleInternal(angle, false)
    }

    private fun setProtractorRotationAngleInternal(angle: Float, isUserInitiatedInView: Boolean) {
        if (!areComponentsInitialized || !appState.isInitialized) {
            Log.w(TAG, "setProtractorRotationAngleInternal returning early: components not initialized.")
            return
        }

        if (appState.updateProtractorRotationAngle(angle)) {
            if (isUserInitiatedInView) {
                listener?.onRotationChanged(appState.protractorRotationAngle)
                listener?.onUserInteraction()
            } else {
                listener?.onRotationChanged(appState.protractorRotationAngle)
            }
            invalidate()
        } else {
            Log.d(TAG, "setProtractorRotationAngleInternal: Rotation angle not significantly changed. Skipping update.")
        }
    }

    fun getProtractorRotationAngle(): Float = if (appState.isInitialized) appState.protractorRotationAngle else config.DEFAULT_ROTATION_ANGLE

    fun setDevicePitchAngle(rawPitchAngle: Float) {
        if (!areComponentsInitialized || !appState.isInitialized) {
            Log.w(TAG, "setDevicePitchAngle returning early: components not initialized.")
            return
        }

        if (appState.updateDevicePitchAngle(rawPitchAngle)) {
            invalidate()
        }
    }

    fun getCurrentPitchAngle(): Float = if (appState.isInitialized) appState.currentPitchAngle else 0.0f
    fun getPlaneTargetCenter(): PointF = if (appState.isInitialized) PointF(appState.targetCircleCenter.x, appState.targetCircleCenter.y) else PointF()

    fun resetInteractionsToDefaults() {
        Log.d(TAG, "resetInteractionsToDefaults called.")
        if (!areComponentsInitialized && width > 0 && height > 0 && cameraManagerRef != null) {
            Log.d(TAG, "resetInteractionsToDefaults: Attempting late initialization.")
            initializeComponents()
        }

        if (!areComponentsInitialized || !appState.isInitialized) {
            Log.w(TAG, "resetInteractionsToDefaults: Cannot reset if components are not initialized or app state is not ready.")
            return
        }

        appState.resetInteractions()
        appPaints.resetDynamicPaintProperties()

        cameraManagerRef?.setCameraZoomRatio(config.DEFAULT_ZOOM_FACTOR)
        Log.d(TAG, "Camera zoom ratio reset to ${config.DEFAULT_ZOOM_FACTOR}.")

        listener?.onZoomChanged(appState.zoomFactor)
        listener?.onRotationChanged(appState.protractorRotationAngle)
        listener?.onCueBallSelected(appState.selectedCueBall?.id)
        listener?.onTargetBallSelected(appState.selectedTargetBall?.id)
        listener?.onSelectionModeChanged(appState.currentMode)
        listener?.onUserInteraction()
        invalidate()
        Log.d(TAG, "resetInteractionsToDefaults complete. App state mode: ${appState.currentMode}")
    }

    fun toggleHelperTextVisibility() {
        if (!areComponentsInitialized || !appState.isInitialized) {
            Log.w(TAG, "toggleHelperTextVisibility returning early: components not initialized.")
            return
        }

        appState.toggleHelperTextVisibility()
        listener?.onUserInteraction()
        invalidate()
        Log.d(TAG, "Helper text visibility toggled to: ${appState.areHelperTextsVisible}")
    }

    fun getAreHelperTextsVisible(): Boolean = if (appState.isInitialized) appState.areHelperTextsVisible else false // Default to false
    fun getSelectionMode(): SelectionMode = if (appState.isInitialized) appState.currentMode else SelectionMode.SELECTING_CUE_BALL
}