// app/src/main/java/com/hereliesaz/cuedetat/domain/StateReducer.kt
package com.hereliesaz.cuedetat.domain

import android.graphics.PointF
import android.util.Log
import com.hereliesaz.cuedetat.ui.MainScreenEvent
import com.hereliesaz.cuedetat.ui.ZoomMapping
import com.hereliesaz.cuedetat.view.model.ActualCueBall
import com.hereliesaz.cuedetat.view.model.Perspective
import com.hereliesaz.cuedetat.view.model.PoolTable
import com.hereliesaz.cuedetat.view.model.ProtractorUnit
import com.hereliesaz.cuedetat.view.model.TableSize
import com.hereliesaz.cuedetat.view.state.OverlayState
import javax.inject.Inject
import kotlin.math.min

class StateReducer @Inject constructor() {

    fun reduce(currentState: OverlayState, event: MainScreenEvent): OverlayState {
        return when (event) {
            is MainScreenEvent.SizeChanged -> {
                if (currentState.viewWidth == 0) createInitialState(event.width, event.height)
                else currentState
            }

            is MainScreenEvent.ZoomScaleChanged -> {
                val currentZoom = ZoomMapping.sliderToZoom(currentState.zoomSliderPosition)
                val newZoom = (currentZoom * event.scaleFactor)
                    .coerceIn(ZoomMapping.MIN_ZOOM, ZoomMapping.MAX_ZOOM)
                val newSliderPos = ZoomMapping.zoomToSlider(newZoom)
                val newRadius =
                    (min(currentState.viewWidth, currentState.viewHeight) * 0.30f / 2f) * newZoom

                Log.d(
                    "ZOOM_DEBUG",
                    "Reducer(Scale): currentZoom=${"%.3f".format(currentZoom)}, newZoom=${
                        "%.3f".format(newZoom)
                    }, newSliderPos=${"%.2f".format(newSliderPos)}"
                )

                currentState.copy(
                    protractorUnit = currentState.protractorUnit.copy(radius = newRadius),
                    actualCueBall = currentState.actualCueBall?.copy(radius = newRadius),
                    zoomSliderPosition = newSliderPos,
                    valuesChangedSinceReset = true
                )
            }

            is MainScreenEvent.ZoomSliderChanged -> {
                val newSliderPos = event.position.coerceIn(0f, 100f)
                val newZoom = ZoomMapping.sliderToZoom(newSliderPos)
                    .coerceIn(ZoomMapping.MIN_ZOOM, ZoomMapping.MAX_ZOOM)
                val newRadius =
                    (min(currentState.viewWidth, currentState.viewHeight) * 0.30f / 2f) * newZoom

                Log.d(
                    "ZOOM_DEBUG",
                    "Reducer(Slider): newZoom=${"%.3f".format(newZoom)}, newSliderPos=${
                        "%.2f".format(newSliderPos)
                    }"
                )

                currentState.copy(
                    protractorUnit = currentState.protractorUnit.copy(radius = newRadius),
                    actualCueBall = currentState.actualCueBall?.copy(radius = newRadius),
                    zoomSliderPosition = newSliderPos,
                    valuesChangedSinceReset = true
                )
            }
            is MainScreenEvent.RotationChanged -> {
                var normAng = event.newRotation % 360f
                if (normAng < 0) normAng += 360f
                currentState.copy(
                    protractorUnit = currentState.protractorUnit.copy(rotationDegrees = normAng),
                    valuesChangedSinceReset = true
                )
            }

            is MainScreenEvent.PitchAngleChanged -> currentState.copy(pitchAngle = event.pitch)

            is MainScreenEvent.UnitMoved -> {
                if (currentState.hasInverseMatrix) {
                    val logicalPos =
                        Perspective.screenToLogical(event.position, currentState.inversePitchMatrix)
                    currentState.copy(
                        protractorUnit = currentState.protractorUnit.copy(center = logicalPos),
                        valuesChangedSinceReset = true
                    )
                } else currentState
            }

            is MainScreenEvent.ActualCueBallMoved -> {
                currentState.copy(
                    actualCueBall = currentState.actualCueBall?.copy(center = event.position)
                        ?: ActualCueBall(
                            center = event.position,
                            radius = currentState.protractorUnit.radius
                        ),
                    valuesChangedSinceReset = true
                )
            }

            is MainScreenEvent.ToggleActualCueBall -> handleToggleActualCueBall(currentState)
            is MainScreenEvent.Reset -> {
                val newRadius = (min(
                    currentState.viewWidth,
                    currentState.viewHeight
                ) * 0.30f / 2f) * ZoomMapping.DEFAULT_ZOOM
                val updatedActualCueBall = currentState.actualCueBall?.copy(radius = newRadius)
                currentState.copy(
                    protractorUnit = ProtractorUnit(
                        center = PointF(currentState.viewWidth / 2f, currentState.viewHeight / 2f),
                        radius = newRadius,
                        rotationDegrees = 0f
                    ),
                    actualCueBall = updatedActualCueBall,
                    zoomSliderPosition = ZoomMapping.zoomToSlider(ZoomMapping.DEFAULT_ZOOM),
                    valuesChangedSinceReset = false,
                    pitchAngle = 0.0f,
                    tableRotation = 0f // Reset table rotation as well
                )
            }

            is MainScreenEvent.ToggleHelp -> currentState.copy(areHelpersVisible = !currentState.areHelpersVisible)

            // ADDED: Table logic
            is MainScreenEvent.InitialTableSizeLoaded -> {
                val size = try {
                    TableSize.valueOf(event.sizeName)
                } catch (e: Exception) {
                    TableSize.SEVEN_FOOT
                }
                currentState.copy(
                    poolTable = currentState.poolTable?.copy(size = size)
                )
            }

            is MainScreenEvent.ToggleTable -> {
                if (currentState.poolTable == null) {
                    val center = if (currentState.hasInverseMatrix) {
                        Perspective.screenToLogical(
                            PointF(
                                currentState.viewWidth / 2f,
                                currentState.viewHeight / 2f
                            ), currentState.inversePitchMatrix
                        )
                    } else {
                        PointF(currentState.viewWidth / 2f, currentState.viewHeight / 2f)
                    }
                    currentState.copy(poolTable = PoolTable(center = center))
                } else {
                    currentState.copy(poolTable = null)
                }
            }

            is MainScreenEvent.ChangeTableSize -> {
                currentState.copy(
                    poolTable = currentState.poolTable?.copy(
                        size = currentState.poolTable.size.getNext()
                    )
                )
            }

            is MainScreenEvent.RotateTable -> {
                currentState.copy(
                    tableRotation = event.rotation,
                    poolTable = currentState.poolTable?.copy(rotationDegrees = event.rotation)
                )
            }

            else -> currentState
        }
    }

    private fun handleToggleActualCueBall(currentState: OverlayState): OverlayState {
        return if (currentState.actualCueBall == null) {
            if (!currentState.hasInverseMatrix) return currentState

            val ghostCueBallPos = currentState.protractorUnit.protractorCueBallCenter
            val screenBottomCenter =
                floatArrayOf(currentState.viewWidth / 2f, currentState.viewHeight.toFloat())
            val logicalBottomCenterArray = FloatArray(2)
            currentState.inversePitchMatrix.mapPoints(logicalBottomCenterArray, screenBottomCenter)
            val logicalBottomPos = PointF(logicalBottomCenterArray[0], logicalBottomCenterArray[1])

            val newDefaultPos = PointF(
                (ghostCueBallPos.x + logicalBottomPos.x) / 2f,
                (ghostCueBallPos.y + logicalBottomPos.y) / 2f
            )
            currentState.copy(
                actualCueBall = ActualCueBall(
                    center = newDefaultPos,
                    radius = currentState.protractorUnit.radius
                )
            )
        } else {
            currentState.copy(actualCueBall = null)
        }
    }

    private fun createInitialState(viewWidth: Int, viewHeight: Int): OverlayState {
        val radius = (min(viewWidth, viewHeight) * 0.30f / 2f) * ZoomMapping.DEFAULT_ZOOM
        return OverlayState(
            viewWidth = viewWidth,
            viewHeight = viewHeight,
            protractorUnit = ProtractorUnit(
                center = PointF(viewWidth / 2f, viewHeight / 2f),
                radius = radius,
                rotationDegrees = 0f
            ),
            zoomSliderPosition = ZoomMapping.zoomToSlider(ZoomMapping.DEFAULT_ZOOM)
        )
    }
}
