package com.hereliesaz.poolprotractor

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.toArgb
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class ProtractorOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    interface ProtractorStateListener {
        fun onZoomChanged(newZoomFactor: Float)
        fun onRotationChanged(newRotationAngle: Float)
        fun onUserInteraction()
        fun onWarningStateChanged(isWarning: Boolean) // New callback for ineptitude
    }
    var listener: ProtractorStateListener? = null

    // ... (rest of the private variables are unchanged) ...
    private var M3_COLOR_PRIMARY: Int = Color.BLUE
    private var M3_COLOR_SECONDARY: Int = Color.RED
    private var M3_COLOR_TERTIARY: Int = Color.GREEN
    private var M3_COLOR_ON_SURFACE: Int = Color.WHITE
    private var M3_COLOR_OUTLINE: Int = Color.LTGRAY
    private var M3_COLOR_ERROR: Int = Color.RED
    private var M3_COLOR_PRIMARY_CONTAINER: Int = Color.CYAN
    private var M3_COLOR_SECONDARY_CONTAINER: Int = Color.MAGENTA
    private var M3_TEXT_SHADOW_COLOR: Int = Color.argb(180, 0, 0, 0)
    private var m3GlowColor: Int = Color.argb(100, 255, 255, 224)
    private val GLOW_RADIUS_FIXED = 8f
    private val targetCirclePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 5f }
    private val cueCirclePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 5f }
    private val centerMarkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val protractorLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeWidth = 3f }
    private val yellowTargetLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.YELLOW; strokeWidth = 5f }
    private val ghostCueOutlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 3f }
    private val targetGhostBallOutlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.YELLOW; style = Paint.Style.STROKE; strokeWidth = 3f }
    private val aimingAssistNearPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val aimingAssistFarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val aimingSightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeWidth = 2f; style = Paint.Style.STROKE }
    private val ghostBallTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        color = Color.WHITE
        setShadowLayer(2f, 1f, 1f, M3_TEXT_SHADOW_COLOR)
    }
    private var oNearDefaultStroke: Float = 4f
    private var oFarDefaultStroke: Float = 2f
    private var oYellowTargetLineStroke: Float = 5f
    private val boldStrokeIncrease = 4f
    private val oCueDeflectionStrokeWidth = 2f
    private val cueDeflectionDottedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeWidth = oCueDeflectionStrokeWidth; style = Paint.Style.STROKE; pathEffect = DashPathEffect(floatArrayOf(15f, 10f), 0f) }
    private val cueDeflectionHighlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeWidth = oCueDeflectionStrokeWidth + boldStrokeIncrease; style = Paint.Style.STROKE; pathEffect = null }
    private val targetCircleCenter = PointF()
    private val cueCircleCenter = PointF()
    private var baseCircleDiameter: Float = 0f
    private var currentLogicalRadius: Float = 1f
    private var zoomFactor = 0.4f
    private var protractorRotationAngle = 0.0f
    private var currentPitchAngle = 0.0f
    private var smoothedPitchAngle = 0.0f
    private val PITCH_SMOOTHING_FACTOR = 0.15f
    private var isInitialized = false
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private enum class InteractionMode { NONE, PINCH_ZOOMING, PAN_TO_ROTATE }
    private var currentInteractionMode = InteractionMode.NONE
    private val PROTRACTOR_ANGLES = floatArrayOf(0f, 14f, 30f, 36f, 43f, 48f)
    private val mGraphicsCamera = Camera()
    private val mPitchMatrix = Matrix()
    private val mInversePitchMatrix = Matrix()
    private var isPinching = false
    private var defaultZoomFactor = 0.4f
    private var defaultProtractorRotationAngle = 0.0f
    private val PAN_ROTATE_SENSITIVITY = 0.3f
    private var areTextLabelsVisible = true
    private val baseGhostBallTextSize = 30f
    private val minGhostBallTextSize = 15f
    private val maxGhostBallTextSize = 60f
    private lateinit var scaleGestureDetector: ScaleGestureDetector

    private var wasLastWarningState = false // Track the previous warning state

    init {
        scaleGestureDetector = ScaleGestureDetector(context, ScaleListener())
        storeOriginalPaintProperties()
    }
    
    // ... (applyMaterialYouColors and storeOriginalPaintProperties are unchanged) ...
    fun applyMaterialYouColors(colorScheme: ColorScheme) {
        M3_COLOR_PRIMARY = colorScheme.primary.toArgb()
        M3_COLOR_SECONDARY = colorScheme.secondary.toArgb()
        M3_COLOR_TERTIARY = colorScheme.tertiary.toArgb()
        M3_COLOR_ON_SURFACE = colorScheme.onSurface.toArgb()
        M3_COLOR_OUTLINE = colorScheme.outline.toArgb()
        M3_COLOR_ERROR = colorScheme.error.toArgb()
        M3_COLOR_PRIMARY_CONTAINER = colorScheme.primaryContainer.toArgb()
        M3_COLOR_SECONDARY_CONTAINER = colorScheme.secondaryContainer.toArgb()

        val primaryComposeColor = colorScheme.primary
        m3GlowColor = Color.argb(100, Color.red(primaryComposeColor.toArgb()), Color.green(primaryComposeColor.toArgb()), Color.blue(primaryComposeColor.toArgb()))
        val surfaceBrightness = (Color.red(colorScheme.surface.toArgb()) * 299 + Color.green(colorScheme.surface.toArgb()) * 587 + Color.blue(colorScheme.surface.toArgb()) * 114) / 1000
        M3_TEXT_SHADOW_COLOR = if (surfaceBrightness < 128) Color.argb(180,220,220,220) else Color.argb(180, 30,30,30)

        targetCirclePaint.color = M3_COLOR_SECONDARY
        cueCirclePaint.color = M3_COLOR_PRIMARY
        centerMarkPaint.color = M3_COLOR_ON_SURFACE
        val tertiaryBase = colorScheme.tertiary.toArgb()
        protractorLinePaint.color = Color.argb(170, Color.red(tertiaryBase), Color.green(tertiaryBase), Color.blue(tertiaryBase))
        ghostCueOutlinePaint.color = M3_COLOR_OUTLINE

        aimingSightPaint.color = M3_COLOR_ON_SURFACE
        ghostBallTextPaint.color = Color.WHITE
        ghostBallTextPaint.setShadowLayer(2f, 1f, 1f, M3_TEXT_SHADOW_COLOR)

        cueDeflectionDottedPaint.color = M3_COLOR_OUTLINE
        cueDeflectionHighlightPaint.color = M3_COLOR_PRIMARY
        cueDeflectionHighlightPaint.setShadowLayer(GLOW_RADIUS_FIXED, 0f, 0f, m3GlowColor)

        invalidate()
    }
    private fun storeOriginalPaintProperties() {
        oYellowTargetLineStroke = yellowTargetLinePaint.strokeWidth
    }


    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            currentInteractionMode = InteractionMode.PINCH_ZOOMING
            isPinching = true; listener?.onUserInteraction(); return true
        }
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            if (currentInteractionMode != InteractionMode.PINCH_ZOOMING) return false
            val oldZoom = zoomFactor
            val newZoomUncoerced = zoomFactor * detector.scaleFactor
            val newZoom = newZoomUncoerced.coerceIn(MIN_ZOOM_FACTOR, MAX_ZOOM_FACTOR)
            if (oldZoom == newZoom && newZoomUncoerced == newZoom) return true
            setZoomFactorInternal(newZoom)
            listener?.onZoomChanged(this@ProtractorOverlayView.zoomFactor)
            invalidate()
            return true
        }
        override fun onScaleEnd(detector: ScaleGestureDetector) {
            if (currentInteractionMode == InteractionMode.PINCH_ZOOMING) { currentInteractionMode = InteractionMode.NONE }
            isPinching = false
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        targetCircleCenter.set(w / 2f, h / 2f)
        if (!isInitialized) {
            baseCircleDiameter = min(w, h) * 0.30f // Initial diameter reference
            setProtractorRotationAngleInternal(defaultProtractorRotationAngle)
            smoothedPitchAngle = currentPitchAngle
            isInitialized = true
        }
        currentLogicalRadius = (baseCircleDiameter / 2f) * zoomFactor
        updateCueBallPosition(); invalidate()
    }

    private fun updateCueBallPosition() {
        if (!isInitialized || currentLogicalRadius <= 0) return
        val angleRad = Math.toRadians(protractorRotationAngle.toDouble())
        val distance = 2 * currentLogicalRadius
        cueCircleCenter.x = targetCircleCenter.x - (distance * sin(angleRad)).toFloat()
        cueCircleCenter.y = targetCircleCenter.y + (distance * cos(angleRad)).toFloat()
    }

    private fun mapPoint(logicalPoint: PointF, matrixToUse: Matrix): PointF {
        val pointArray = floatArrayOf(logicalPoint.x, logicalPoint.y); matrixToUse.mapPoints(pointArray)
        return PointF(pointArray[0], pointArray[1])
    }


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!isInitialized) return
        // ... (onDraw setup is mostly unchanged) ...
        currentLogicalRadius = (baseCircleDiameter / 2f) * zoomFactor
        if (currentLogicalRadius <= 0.01f) currentLogicalRadius = 0.01f
        updateCueBallPosition()

        mGraphicsCamera.save(); mGraphicsCamera.rotateX(this.currentPitchAngle); mGraphicsCamera.getMatrix(mPitchMatrix); mGraphicsCamera.restore()
        mPitchMatrix.preTranslate(-targetCircleCenter.x, -targetCircleCenter.y); mPitchMatrix.postTranslate(targetCircleCenter.x, targetCircleCenter.y)
        val hasInverse = mPitchMatrix.invert(mInversePitchMatrix)

        val pTGC_s_collision = mapPoint(targetCircleCenter, mPitchMatrix); val pCGC_s_collision = mapPoint(cueCircleCenter, mPitchMatrix)
        val tL_s_coll = mapPoint(PointF(targetCircleCenter.x - currentLogicalRadius, targetCircleCenter.y), mPitchMatrix); val tR_s_coll = mapPoint(PointF(targetCircleCenter.x + currentLogicalRadius, targetCircleCenter.y), mPitchMatrix)
        val tT_s_coll = mapPoint(PointF(targetCircleCenter.x, targetCircleCenter.y - currentLogicalRadius), mPitchMatrix); val tB_s_coll = mapPoint(PointF(targetCircleCenter.x, targetCircleCenter.y + currentLogicalRadius), mPitchMatrix)
        val gTSR_s_collision = max(distance(tL_s_coll.x,tL_s_coll.y,tR_s_coll.x,tR_s_coll.y), distance(tT_s_coll.x,tT_s_coll.y,tB_s_coll.x,tB_s_coll.y)) / 2f
        val cL_s_coll = mapPoint(PointF(cueCircleCenter.x - currentLogicalRadius, cueCircleCenter.y), mPitchMatrix); val cR_s_coll = mapPoint(PointF(cueCircleCenter.x + currentLogicalRadius, cueCircleCenter.y), mPitchMatrix)
        val cT_s_coll = mapPoint(PointF(cueCircleCenter.x, cueCircleCenter.y - currentLogicalRadius), mPitchMatrix); val cB_s_coll = mapPoint(PointF(cueCircleCenter.x, cueCircleCenter.y + currentLogicalRadius), mPitchMatrix)
        val gCSR_s_collision = max(distance(cL_s_coll.x,cL_s_coll.y,cR_s_coll.x,cR_s_coll.y), distance(cT_s_coll.x,cT_s_coll.y,cB_s_coll.x,cB_s_coll.y)) / 2f

        val logicalDistanceBetweenCenters = distance(cueCircleCenter.x, cueCircleCenter.y, targetCircleCenter.x, targetCircleCenter.y)
        val logicalSumOfRadii = currentLogicalRadius + currentLogicalRadius
        val isPhysicalOverlap = logicalDistanceBetweenCenters < logicalSumOfRadii - 0.1f

        var isCueOnFarSide = false
        // ... (isCueOnFarSide logic is unchanged) ...
        if (hasInverse) {
            val screenAimPointScreenCoords = floatArrayOf(width / 2f, height.toFloat()); val screenAimPointLogicalCoordsArray = FloatArray(2)
            mInversePitchMatrix.mapPoints(screenAimPointLogicalCoordsArray, screenAimPointScreenCoords)
            val screenAimLogicalX = screenAimPointLogicalCoordsArray[0]; val screenAimLogicalY = screenAimPointLogicalCoordsArray[1]
            val cueLogX = cueCircleCenter.x; val cueLogY = cueCircleCenter.y; val targetLogX = targetCircleCenter.x; val targetLogY = targetCircleCenter.y
            val aimDirLogX = cueLogX - screenAimLogicalX; val aimDirLogY = cueLogY - screenAimLogicalY
            val magAimDirSq = aimDirLogX * aimDirLogX + aimDirLogY * aimDirLogY
            if (magAimDirSq > 0.0001f) {
                val magAimDir = sqrt(magAimDirSq); val normAimDirLogX = aimDirLogX / magAimDir; val normAimDirLogY = aimDirLogY / magAimDir
                val vecScreenToTargetLogX = targetLogX - screenAimLogicalX; val vecScreenToTargetLogY = targetLogY - screenAimLogicalY
                val distCueProj = magAimDir; val distTargetProj = vecScreenToTargetLogX * normAimDirLogX + vecScreenToTargetLogY * normAimDirLogY
                isCueOnFarSide = distCueProj > distTargetProj && distTargetProj > 0
            }
        }

        val isDeflectionDominantAngle = (protractorRotationAngle > 90.5f && protractorRotationAngle < 269.5f)
        val useErrorColorForMainCue = isCueOnFarSide || isDeflectionDominantAngle
        val showWarningStyleForGhostAndYellowTargetLine = isPhysicalOverlap || isCueOnFarSide

        // *** NEW: Notify listener about the current warning state ***
        val isCurrentlyInWarningState = useErrorColorForMainCue || showWarningStyleForGhostAndYellowTargetLine
        if (isCurrentlyInWarningState != wasLastWarningState) {
            listener?.onWarningStateChanged(isCurrentlyInWarningState)
            wasLastWarningState = isCurrentlyInWarningState
        }
        // *** END NEW ***

        // ... (rest of the drawing logic is unchanged) ...
        yellowTargetLinePaint.apply {
            strokeWidth = oYellowTargetLineStroke
            clearShadowLayer()
            color = Color.YELLOW
        }
        if (showWarningStyleForGhostAndYellowTargetLine) {
            yellowTargetLinePaint.apply {
                strokeWidth = oYellowTargetLineStroke + boldStrokeIncrease
                setShadowLayer(GLOW_RADIUS_FIXED, 0f, 0f, m3GlowColor)
            }
        }

        aimingAssistNearPaint.strokeWidth = oNearDefaultStroke
        aimingAssistFarPaint.strokeWidth = oFarDefaultStroke

        if (isCueOnFarSide) {
            aimingAssistNearPaint.apply {
                color = M3_COLOR_ERROR
                clearShadowLayer()
            }
            aimingAssistFarPaint.apply {
                color = protractorLinePaint.color
                strokeWidth = protractorLinePaint.strokeWidth
                clearShadowLayer()
            }
        } else {
            aimingAssistNearPaint.apply {
                color = M3_COLOR_PRIMARY_CONTAINER
                setShadowLayer(GLOW_RADIUS_FIXED, 0f, 0f, m3GlowColor)
            }
            if (!isPhysicalOverlap) {
                aimingAssistFarPaint.apply {
                    color = aimingAssistNearPaint.color
                    strokeWidth = aimingAssistNearPaint.strokeWidth
                    setShadowLayer(GLOW_RADIUS_FIXED, 0f, 0f, m3GlowColor)
                }
            } else {
                aimingAssistFarPaint.apply {
                    color = M3_COLOR_SECONDARY_CONTAINER
                    setShadowLayer(GLOW_RADIUS_FIXED, 0f, 0f, m3GlowColor)
                }
            }
        }


        canvas.save(); canvas.concat(mPitchMatrix)
        if (hasInverse) {
            val spu = floatArrayOf(width/2f, height.toFloat()); val lps = FloatArray(2); mInversePitchMatrix.mapPoints(lps, spu)
            val sxL=lps[0]; val syL=lps[1]; val cxL=cueCircleCenter.x; val cyL=cueCircleCenter.y
            val dxL=cxL-sxL; val dyL=cyL-syL; val magL=sqrt(dxL*dxL + dyL*dyL)
            if(magL > 0.001f){
                val ndxL=dxL/magL; val ndyL=dyL/magL; val efL=max(width,height)*5f
                val exL=cxL+ndxL*efL; val eyL=cyL+ndyL*efL
                canvas.drawLine(sxL,syL,cxL,cyL,aimingAssistNearPaint)
                canvas.drawLine(cxL,cyL,exL,eyL,aimingAssistFarPaint)
            }
        }
        canvas.drawCircle(targetCircleCenter.x,targetCircleCenter.y,currentLogicalRadius,targetCirclePaint)
        canvas.drawCircle(targetCircleCenter.x,targetCircleCenter.y,currentLogicalRadius/5f,centerMarkPaint)

        cueCirclePaint.color = if (useErrorColorForMainCue) M3_COLOR_ERROR else M3_COLOR_PRIMARY
        canvas.drawCircle(cueCircleCenter.x,cueCircleCenter.y,currentLogicalRadius,cueCirclePaint)
        canvas.drawCircle(cueCircleCenter.x,cueCircleCenter.y,currentLogicalRadius/5f,centerMarkPaint)

        val dxTBP = targetCircleCenter.x - cueCircleCenter.x; val dyTBP = targetCircleCenter.y - cueCircleCenter.y
        val tBPMag = sqrt(dxTBP * dxTBP + dyTBP * dyTBP)
        if (tBPMag > 0.001f) {
            val nDxT = dxTBP / tBPMag; val nDyT = dyTBP / tBPMag; val dLL = max(width, height) * 1.5f
            val deflectionDir1X = -nDyT; val deflectionDir1Y = nDxT

            var paintForDir1 = cueDeflectionDottedPaint
            var paintForDir2 = cueDeflectionDottedPaint

            if (useErrorColorForMainCue) {
                // Both lines are dotted if main cue is in error state
            } else {
                val alphaDeg = protractorRotationAngle
                val epsilon = 0.5f
                if (alphaDeg > epsilon && alphaDeg < (180f - epsilon)) {
                    paintForDir2 = cueDeflectionHighlightPaint
                } else if (alphaDeg > (180f + epsilon) && alphaDeg < (360f - epsilon)) {
                    paintForDir1 = cueDeflectionHighlightPaint
                }
            }
            canvas.drawLine(cueCircleCenter.x, cueCircleCenter.y, cueCircleCenter.x + deflectionDir1X * dLL, cueCircleCenter.y + deflectionDir1Y * dLL, paintForDir1)
            canvas.drawLine(cueCircleCenter.x, cueCircleCenter.y, cueCircleCenter.x - deflectionDir1X * dLL, cueCircleCenter.y - deflectionDir1Y * dLL, paintForDir2)
        }
        canvas.save(); canvas.translate(targetCircleCenter.x,targetCircleCenter.y); canvas.rotate(protractorRotationAngle)
        val lineLength = max(width,height)*2f
        PROTRACTOR_ANGLES.forEach { angle ->
            val rad = Math.toRadians(angle.toDouble()); val endX1=(lineLength*sin(rad)).toFloat(); val endY1=(lineLength*cos(rad)).toFloat()
            if(angle == 0f){ canvas.drawLine(0f,0f,endX1,endY1,protractorLinePaint); canvas.drawLine(0f,0f,-endX1,-endY1,yellowTargetLinePaint) }
            else { canvas.drawLine(0f,0f,endX1,endY1,protractorLinePaint); canvas.drawLine(0f,0f,-endX1,-endY1,protractorLinePaint)
                val negRad=Math.toRadians(-angle.toDouble()); val negEndX1=(lineLength*sin(negRad)).toFloat(); val negEndY1=(lineLength*cos(negRad)).toFloat()
                canvas.drawLine(0f,0f,negEndX1,negEndY1,protractorLinePaint); canvas.drawLine(0f,0f,-negEndX1,-negEndY1,protractorLinePaint)
            }
        }
        canvas.restore(); canvas.restore()

        val targetGhostDrawnCenterY = pTGC_s_collision.y - gTSR_s_collision
        canvas.drawCircle(pTGC_s_collision.x, targetGhostDrawnCenterY, gTSR_s_collision, targetGhostBallOutlinePaint)

        val cueGhostDrawnCenterY = pCGC_s_collision.y - gCSR_s_collision
        val originalGhostCueColor = ghostCueOutlinePaint.color
        if (showWarningStyleForGhostAndYellowTargetLine) {
            ghostCueOutlinePaint.color = M3_COLOR_ERROR
        } else {
            ghostCueOutlinePaint.color = M3_COLOR_OUTLINE
        }
        canvas.drawCircle(pCGC_s_collision.x, cueGhostDrawnCenterY, gCSR_s_collision, ghostCueOutlinePaint)
        ghostCueOutlinePaint.color = originalGhostCueColor

        val sightArmLength = gCSR_s_collision * 0.6f
        canvas.drawLine(pCGC_s_collision.x - sightArmLength, cueGhostDrawnCenterY, pCGC_s_collision.x + sightArmLength, cueGhostDrawnCenterY, aimingSightPaint)
        canvas.drawLine(pCGC_s_collision.x, cueGhostDrawnCenterY - sightArmLength, pCGC_s_collision.x, cueGhostDrawnCenterY + sightArmLength, aimingSightPaint)
        canvas.drawCircle(pCGC_s_collision.x, cueGhostDrawnCenterY, sightArmLength * 0.15f, aimingSightPaint)

        if (areTextLabelsVisible) {
            val currentTextSize = (baseGhostBallTextSize * zoomFactor).coerceIn(minGhostBallTextSize, maxGhostBallTextSize)
            ghostBallTextPaint.textSize = currentTextSize; val tm = ghostBallTextPaint.fontMetrics
            val textPaddingBelowText = 5f * zoomFactor.coerceAtLeast(0.5f)

            val visualTopOfTargetGhostBall = targetGhostDrawnCenterY - gTSR_s_collision
            val targetTextBaselineY = visualTopOfTargetGhostBall - textPaddingBelowText - tm.descent
            canvas.drawText("Target Ball", pTGC_s_collision.x, targetTextBaselineY, ghostBallTextPaint)

            val visualTopOfCueGhostBall = cueGhostDrawnCenterY - gCSR_s_collision
            val cueTextBaselineY = visualTopOfCueGhostBall - textPaddingBelowText - tm.descent
            canvas.drawText("Cue Ball", pCGC_s_collision.x, cueTextBaselineY, ghostBallTextPaint)
        }
    }
    
    // ... (rest of the file is unchanged, including onTouchEvent, setters/getters, resetToDefaults, and companion object) ...
    fun toggleHelpersVisibility() { areTextLabelsVisible = !areTextLabelsVisible; invalidate() }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isInitialized) return false
        val scaleHandled = scaleGestureDetector.onTouchEvent(event)
        val touchX = event.x; val touchY = event.y

        if (isPinching || currentInteractionMode == InteractionMode.PINCH_ZOOMING) {
            if (scaleHandled || (event.actionMasked != MotionEvent.ACTION_UP && event.actionMasked != MotionEvent.ACTION_CANCEL)) {
                return true
            }
        }

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (!isPinching) {
                    currentInteractionMode = InteractionMode.PAN_TO_ROTATE
                    listener?.onUserInteraction()
                }
                lastTouchX = touchX; lastTouchY = touchY
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (currentInteractionMode == InteractionMode.PAN_TO_ROTATE) {
                    val dx = touchX - lastTouchX
                    val angleDelta = dx * PAN_ROTATE_SENSITIVITY
                    setProtractorRotationAngleInternal(protractorRotationAngle + angleDelta)
                    listener?.onRotationChanged(this.protractorRotationAngle)
                    invalidate()
                    lastTouchX = touchX; lastTouchY = touchY
                    return true
                } else if (currentInteractionMode == InteractionMode.PINCH_ZOOMING) {
                    return scaleHandled
                }
                return false
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                val wasInteracting = currentInteractionMode != InteractionMode.NONE
                if (currentInteractionMode == InteractionMode.PAN_TO_ROTATE) {
                    currentInteractionMode = InteractionMode.NONE
                }
                if (isPinching && !scaleGestureDetector.isInProgress && currentInteractionMode == InteractionMode.PINCH_ZOOMING) {
                    currentInteractionMode = InteractionMode.NONE
                }
                return wasInteracting || scaleHandled
            }
        }
        return scaleHandled || super.onTouchEvent(event)
    }

    private fun distance(x1: Float, y1: Float, x2: Float, y2: Float): Float { return sqrt((x1 - x2).pow(2) + (y1 - y2).pow(2)) }
    fun setZoomFactor(factor: Float) { if (isPinching) return; setZoomFactorInternal(factor); listener?.onUserInteraction(); invalidate() }
    private fun setZoomFactorInternal(factor: Float) { this.zoomFactor = factor.coerceIn(MIN_ZOOM_FACTOR, MAX_ZOOM_FACTOR); this.currentLogicalRadius = (baseCircleDiameter / 2f) * this.zoomFactor; updateCueBallPosition() }
    fun getZoomFactor(): Float = zoomFactor
    fun setProtractorRotationAngle(angle: Float) { setProtractorRotationAngleInternal(angle); listener?.onUserInteraction(); listener?.onRotationChanged(this.protractorRotationAngle); invalidate() }
    private fun setProtractorRotationAngleInternal(angle: Float) { var normAng = angle % 360f; if (normAng < 0) normAng += 360f; this.protractorRotationAngle = normAng; updateCueBallPosition() }
    fun getProtractorRotationAngle(): Float = protractorRotationAngle

    fun setPitchAngle(angle: Float) {
        val newPitch = angle.coerceIn(-85f, 90f)
        this.smoothedPitchAngle = (PITCH_SMOOTHING_FACTOR * newPitch) + ((1.0f - PITCH_SMOOTHING_FACTOR) * this.smoothedPitchAngle)

        if (abs(this.currentPitchAngle - this.smoothedPitchAngle) > 0.05f) {
            this.currentPitchAngle = this.smoothedPitchAngle
            invalidate()
        }
    }
    fun getPitchAngle(): Float = currentPitchAngle
    fun getTargetCircleCenter(): PointF = PointF(targetCircleCenter.x, targetCircleCenter.y)

    fun resetToDefaults() {
        if (!isInitialized) return
        setZoomFactorInternal(defaultZoomFactor)
        setProtractorRotationAngleInternal(defaultProtractorRotationAngle)
        areTextLabelsVisible = true
        yellowTargetLinePaint.apply {
            strokeWidth = oYellowTargetLineStroke
            color = Color.YELLOW
            clearShadowLayer()
        }
        listener?.onZoomChanged(this.zoomFactor)
        listener?.onRotationChanged(this.protractorRotationAngle)
        // Explicitly notify that the warning state is now false upon reset.
        if (wasLastWarningState) {
            listener?.onWarningStateChanged(false)
            wasLastWarningState = false
        }
        invalidate()
    }

    companion object {
        private const val TAG = "PoolProtractorApp"
        internal const val MIN_ZOOM_FACTOR = 0.1f
        internal const val MAX_ZOOM_FACTOR = 4.0f
    }
}
