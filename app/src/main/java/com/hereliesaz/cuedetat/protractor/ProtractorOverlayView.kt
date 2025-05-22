package com.hereliesaz.cuedetat.protractor

import android.content.Context
import android.graphics.PointF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.compose.material3.ColorScheme
import com.hereliesaz.cuedetat.protractor.drawer.ProtractorDrawingCoordinator

class ProtractorOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // ... (interface, listener, other vars) ...
    interface ProtractorStateListener {
        fun onZoomChanged(newZoomFactor: Float)
        fun onRotationChanged(newRotationAngle: Float)
        fun onUserInteraction()
    }
    var listener: ProtractorStateListener? = null

    private val config = ProtractorConfig
    private val state = ProtractorState(config)
    private val paints = ProtractorPaints(context, config)
    private lateinit var gestureHandler: ProtractorGestureHandler
    private lateinit var drawingCoordinator: ProtractorDrawingCoordinator


    init {}

    private fun initializeComponents() {
        if (::gestureHandler.isInitialized && ::drawingCoordinator.isInitialized) return

        gestureHandler = ProtractorGestureHandler(
            context, state, config, listener,
            onZoomChangedByGesture = { newZoomFactor ->
                setZoomFactorInternal(newZoomFactor, true)
            },
            onRotationChangedByGesture = { newRotationAngle ->
                setProtractorRotationAngleInternal(newRotationAngle, true)
            }
        )

        drawingCoordinator = ProtractorDrawingCoordinator(state, paints, config,
            viewWidthProvider = { width },
            viewHeightProvider = { height }
        )
    }

    // ... (applyMaterialYouColors, onSizeChanged, onDraw, onTouchEvent, zoom, rotation, pitch methods) ...
    fun applyMaterialYouColors(colorScheme: ColorScheme) {
        if (width == 0 || height == 0 || !::gestureHandler.isInitialized) {
            if (width > 0 && height > 0) {
                initializeComponents()
            }
        }
        paints.applyMaterialYouColors(colorScheme)
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        initializeComponents()
        if (!state.isInitialized) { state.initialize(w,h) } else { state.initialize(w,h) }
        invalidate()
    }

    override fun onDraw(canvas: android.graphics.Canvas) {
        super.onDraw(canvas)
        if (!::drawingCoordinator.isInitialized) {
            if (width > 0 && height > 0) initializeComponents() else return
        }
        drawingCoordinator.onDraw(canvas)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!state.isInitialized || !::gestureHandler.isInitialized) return false
        return gestureHandler.onTouchEvent(event) || super.onTouchEvent(event)
    }

    fun setZoomFactor(factor: Float) { setZoomFactorInternal(factor, false) }
    private fun setZoomFactorInternal(factor: Float, isUserInitiatedInView: Boolean) {
        if (!state.isInitialized && width > 0 && height > 0) { initializeComponents(); state.initialize(width, height) }
        if (!state.isInitialized) return
        if (state.updateZoomFactor(factor)) {
            if (isUserInitiatedInView) { listener?.onZoomChanged(state.zoomFactor); listener?.onUserInteraction() }
            else { listener?.onZoomChanged(state.zoomFactor) }
            invalidate()
        }
    }
    fun getZoomFactor(): Float = if (state.isInitialized) state.zoomFactor else config.DEFAULT_ZOOM_FACTOR

    fun setProtractorRotationAngle(angle: Float) { setProtractorRotationAngleInternal(angle, false) }
    private fun setProtractorRotationAngleInternal(angle: Float, isUserInitiatedInView: Boolean) {
        if (!state.isInitialized && width > 0 && height > 0) { initializeComponents(); state.initialize(width, height) }
        if (!state.isInitialized) return
        if (state.updateProtractorRotationAngle(angle)) {
            if (isUserInitiatedInView) { listener?.onRotationChanged(state.protractorRotationAngle); listener?.onUserInteraction() }
            else { listener?.onRotationChanged(state.protractorRotationAngle) }
            invalidate()
        }
    }
    fun getProtractorRotationAngle(): Float = if (state.isInitialized) state.protractorRotationAngle else config.DEFAULT_ROTATION_ANGLE

    fun setPitchAngle(angle: Float) {
        if (!state.isInitialized && width > 0 && height > 0) { initializeComponents(); state.initialize(width, height) }
        if (!state.isInitialized) return
        if (state.updatePitchAngle(angle)) { invalidate() }
    }
    fun getPitchAngle(): Float = if (state.isInitialized) state.currentPitchAngle else 0.0f
    fun getTargetCircleCenter(): PointF = if (state.isInitialized) PointF(state.targetCircleCenter.x, state.targetCircleCenter.y) else PointF()


    fun resetToDefaults() {
        if (!state.isInitialized) {
            if (width > 0 && height > 0) { initializeComponents(); state.initialize(width, height) }
            else { return }
        }
        state.reset() // This sets areTextLabelsVisible to true
        paints.resetDynamicPaintProperties()
        listener?.onZoomChanged(state.zoomFactor)
        listener?.onRotationChanged(state.protractorRotationAngle)
        listener?.onUserInteraction()
        invalidate()
    }

    fun toggleHelpersVisibility() {
        if (!state.isInitialized) return
        state.toggleHelperTextVisibility()
        listener?.onUserInteraction()
        invalidate()
    }

    // New getter for MainActivity to sync state
    fun getAreTextLabelsVisible(): Boolean = if (state.isInitialized) state.areTextLabelsVisible else true


    companion object {
        const val MIN_ZOOM_FACTOR = ProtractorConfig.MIN_ZOOM_FACTOR
        const val MAX_ZOOM_FACTOR = ProtractorConfig.MAX_ZOOM_FACTOR
        const val DEFAULT_ZOOM_FACTOR = ProtractorConfig.DEFAULT_ZOOM_FACTOR
    }
}