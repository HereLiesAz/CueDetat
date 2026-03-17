// FILE: app/src/main/java/com/hereliesaz/cuedetat/ui/hatemode/HaterScreen.kt

package com.hereliesaz.cuedetat.ui.hatemode

import android.graphics.Bitmap
import android.graphics.Camera
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.domain.MainScreenEvent
import com.hereliesaz.cuedetat.ui.composables.AzNavRailMenu
import com.hereliesaz.cuedetat.ui.composables.TopControls
import kotlin.math.sqrt

/**
 * The main screen for "Hater Mode".
 *
 * Simulates a Magic-8-Ball experience where a 20-sided die floats in liquid.
 * The physics canvas is placed in a [background] layer; [TopControls] is placed
 * in an [onscreen] block per the AzNavRail architecture contract.
 *
 * @param haterViewModel The ViewModel managing the physics state.
 * @param uiState Global app state (for shared UI elements like the menu).
 * @param onEvent Event dispatcher.
 */
@Composable
fun HaterScreen(
    haterViewModel: HaterViewModel,
    uiState: CueDetatState,
    onEvent: (MainScreenEvent) -> Unit
) {
    val state by haterViewModel.haterState.collectAsStateWithLifecycle()

    val navController = rememberNavController()
    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStack?.destination?.route

    LaunchedEffect(Unit) {
        haterViewModel.onEvent(HaterEvent.EnterHaterMode)
    }

    AzNavRailMenu(
        uiState = uiState,
        onEvent = onEvent,
        navController = navController,
        currentDestination = currentRoute
    ) {
        // --- Background layer: physics / die canvas ---
        background(weight = 0) {
            val answerText = HaterResponses.allAnswers.getOrElse(state.answerIndex) { "" }
            val bitmap: Bitmap = remember(answerText) { createDieFaceBitmap(answerText) }

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDrag = { change, dragAmount ->
                                change.consume()
                                haterViewModel.onEvent(HaterEvent.Dragging(dragAmount))
                            },
                            onDragEnd = { haterViewModel.onEvent(HaterEvent.DragEnd) }
                        )
                    }
            ) {
                haterViewModel.setupBoundaries(size.width, size.height)
                val centerX = size.width / 2
                val centerY = size.height / 2

                drawRect(color = androidx.compose.ui.graphics.Color.Black)

                val targetSize = minOf(size.width, size.height) * 0.55f
                val hw        = bitmap.width  / 2f
                val hh        = bitmap.height / 2f
                val baseScale = (targetSize / maxOf(bitmap.width, bitmap.height)) * state.dieScale
                val cx        = centerX + state.diePosition.x
                val cy        = centerY + state.diePosition.y

                // Build a Camera perspective matrix for the current rock tilt.
                // Camera.rotateX(+deg) tilts the top away from viewer (top dips into liquid).
                // Camera.rotateY(+deg) tilts the right side away from viewer (right dips).
                val cam = Camera()
                cam.setLocation(0f, 0f, -6f) // closer camera = more pronounced perspective
                cam.rotateX(state.rockAngleX)
                cam.rotateY(state.rockAngleY)
                val perspMatrix = Matrix()
                cam.getMatrix(perspMatrix)
                // pivot the perspective around the bitmap centre
                perspMatrix.preTranslate(-hw, -hh)
                perspMatrix.postTranslate(hw, hh)

                // Directional shading: dipping edge darkens (into black liquid),
                // rising edge brightens (emerging from it).
                // rockAngleX > 0 → top dips → dark at -Y (top) in bitmap space
                // rockAngleY > 0 → right dips → dark at +X (right) in bitmap space
                val tiltMag = sqrt(
                    (state.rockAngleX * state.rockAngleX + state.rockAngleY * state.rockAngleY).toDouble()
                ).toFloat()
                val shadeStrength = (tiltMag / 12f).coerceIn(0f, 1f)
                val darkAlpha   = (shadeStrength * 100).toInt()   // max ~39% darken
                val brightAlpha = (shadeStrength * 50).toInt()    // max ~20% lighten
                // Gradient goes from bright (rising) side to dark (dipping) side
                val gx = (state.rockAngleY / 12f) * hw   // +x = right side dips
                val gy = -(state.rockAngleX / 12f) * hh  // positive X-tilt = top dips = -y direction

                drawIntoCanvas { canvas ->

                    // --- Adjacent faces: other sides of the d20 dimly visible through the liquid ---
                    // Most visible at mid-emergence (die half-submerged); fades when fully surfaced.
                    val t          = state.dieScale.coerceIn(0f, 1f)
                    val ghostAlpha = (t * (1f - t) * 4f * 55f).toInt().coerceAtMost(55)
                    if (ghostAlpha > 0) {
                        val ghostPaint = Paint().apply { alpha = ghostAlpha }
                        canvas.nativeCanvas.save()
                        canvas.nativeCanvas.translate(cx, cy)
                        canvas.nativeCanvas.rotate(state.dieAngle + 60f)
                        canvas.nativeCanvas.scale(baseScale * 1.25f, baseScale * 1.25f)
                        canvas.nativeCanvas.concat(perspMatrix)
                        canvas.nativeCanvas.drawBitmap(bitmap, -hw, -hh, ghostPaint)
                        canvas.nativeCanvas.restore()

                        canvas.nativeCanvas.save()
                        canvas.nativeCanvas.translate(cx, cy)
                        canvas.nativeCanvas.rotate(state.dieAngle - 60f)
                        canvas.nativeCanvas.scale(baseScale * 1.20f, baseScale * 1.20f)
                        canvas.nativeCanvas.concat(perspMatrix)
                        canvas.nativeCanvas.drawBitmap(bitmap, -hw, -hh, ghostPaint)
                        canvas.nativeCanvas.restore()
                    }

                    // --- Main face ---
                    val mainPaint = Paint().apply {
                        alpha = (state.dieScale.coerceIn(0f, 1f) * 255f).toInt()
                    }
                    canvas.nativeCanvas.save()
                    canvas.nativeCanvas.translate(cx, cy)
                    canvas.nativeCanvas.rotate(state.dieAngle)
                    canvas.nativeCanvas.scale(baseScale, baseScale)
                    canvas.nativeCanvas.concat(perspMatrix)
                    canvas.nativeCanvas.drawBitmap(bitmap, -hw, -hh, mainPaint)

                    // --- Directional liquid shading (drawn in bitmap space, after perspective) ---
                    // Dipping edge: dark gradient merging into the black liquid.
                    // Rising edge: slight brightening as the face lifts clear of the surface.
                    if (darkAlpha > 0) {
                        val shadePaint = Paint().apply {
                            shader = LinearGradient(
                                -gx, -gy,   // bright / rising side
                                gx, gy,     // dark / dipping side
                                intArrayOf(
                                    Color.argb(brightAlpha, 255, 255, 255),
                                    Color.argb(0, 0, 0, 0),
                                    Color.argb(darkAlpha, 0, 0, 0)
                                ),
                                floatArrayOf(0f, 0.5f, 1f),
                                Shader.TileMode.CLAMP
                            )
                        }
                        canvas.nativeCanvas.drawRect(-hw, -hh, hw, hh, shadePaint)
                    }

                    canvas.nativeCanvas.restore()
                }
            }
        }

        // --- Onscreen: top status bar ---
        onscreen(alignment = Alignment.TopEnd) {
            TopControls(
                experienceMode = uiState.experienceMode,
                isTableVisible = uiState.table.isVisible,
                tableSizeFeet = uiState.table.size.feet,
                isBeginnerViewLocked = uiState.isBeginnerViewLocked,
                targetBallDistance = uiState.targetBallDistance,
                distanceUnit = uiState.distanceUnit,
                onCycleTableSize = { onEvent(MainScreenEvent.CycleTableSize) }
            )
        }
    }
}

/**
 * Creates a 512×512 bitmap depicting a d20 triangular face with [text] centered inside.
 *
 * The triangle is equilateral (apex-up), deep navy fill, with a neon-blue inner glow
 * that feathers inward from the edge to transparent. Text is white bold, auto-sized and
 * placed in the triangle's wider lower zone so it never touches the edges.
 *
 * Text geometry constraint for an apex-up equilateral triangle with circumradius R:
 *   at height y from the top vertex, the triangle width = y × 2/√3
 *   so a text block of width W centred at (cx, textCY) is safe when its top line
 *   satisfies: W ≤ (textCY − halfH − apex_y) × 2/√3
 */
private fun createDieFaceBitmap(text: String): Bitmap {
    val size   = 512
    val bmp    = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bmp)

    val cx    = size / 2f
    val cy    = size / 2f
    val R     = 210f
    val sin60 = sqrt(3.0).toFloat() / 2f

    // Equilateral triangle: apex UP, centroid at (cx, cy)
    val path = Path().apply {
        moveTo(cx,             cy - R)           // top vertex
        lineTo(cx + R * sin60, cy + R * 0.5f)   // bottom-right
        lineTo(cx - R * sin60, cy + R * 0.5f)   // bottom-left
        close()
    }

    // --- Fill: deep navy ---
    canvas.drawPath(path, Paint().apply {
        color       = Color.argb(255, 6, 8, 52)
        style       = Paint.Style.FILL
        isAntiAlias = true
    })

    // --- Neon blue inner glow feathered inward from the triangle edge ---
    // BlurMaskFilter(INNER) on a filled path: alpha is highest at the perimeter
    // and decays toward the interior, creating the "inner-border glow" look.
    canvas.drawPath(path, Paint().apply {
        color      = Color.argb(255, 0, 180, 255)   // neon cyan-blue
        style      = Paint.Style.FILL
        maskFilter = android.graphics.BlurMaskFilter(22f, android.graphics.BlurMaskFilter.Blur.INNER)
        isAntiAlias = true
    })

    // --- Text: white bold, auto-sized to fit safely inside the triangle ---
    // textCY is shifted 12% of R below the geometric centroid — the triangle is wider
    // there, so more horizontal space is available for the top lines.
    val textCY   = cy + R * 0.12f   // ≈ 281 — sits in the wider lower zone
    // Max height budget: halfH ≈ 65 → yTop ≈ 216 → safe width = (216−46)×2/√3×0.88 ≈ 171
    val maxTextH = R * 0.62f        // ≈ 130px
    val maxTextW = 160              // well within safe width at yTop

    val textPaint = TextPaint().apply {
        color       = Color.WHITE
        isAntiAlias = true
        typeface    = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    var fontSize = 52f
    var layout: StaticLayout
    do {
        textPaint.textSize = fontSize
        layout = StaticLayout.Builder
            .obtain(text, 0, text.length, textPaint, maxTextW)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setLineSpacing(0f, 1.12f)
            .setIncludePad(false)
            .build()
        fontSize -= 2f
    } while (layout.height > maxTextH && fontSize > 10f)

    canvas.save()
    canvas.translate(cx - maxTextW / 2f, textCY - layout.height / 2f)
    layout.draw(canvas)
    canvas.restore()

    return bmp
}
