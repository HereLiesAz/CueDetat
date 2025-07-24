package com.hereliesaz.cuedetat.ui.hatemode

import android.content.Context
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hereliesaz.cuedetat.R
import com.hereliesaz.cuedetat.data.FullOrientation
import com.hereliesaz.cuedetat.data.SensorRepository
import com.hereliesaz.cuedetat.data.ShakeDetector
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HaterState(
    val currentAnswer: String? = null,
    val isTriangleVisible: Boolean = false,
    val recentlyUsedAnswers: List<String> = emptyList(),
    val isShaking: Boolean = false,
    val orientation: FullOrientation = FullOrientation(0f, 0f, 0f),
    val trianglePosition: Offset? = null,
    val triggerNewAnswer: Int = 0 // A counter to trigger recomposition
)

@HiltViewModel
class HaterViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val shakeDetector: ShakeDetector,
    private val sensorRepository: SensorRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HaterState())
    val uiState = _uiState.asStateFlow()

    private val answers: Array<String> =
        context.resources.getStringArray(R.array.hater_mode_responses)
    private var animationJob: Job? = null
    private val initialAnswerKey = "HaterIcon01"

    init {
        // Initial emergence sequence
        viewModelScope.launch {
            delay(500L) // Wait a moment before the first appearance
            val initialRecentList = listOf(initialAnswerKey)
            _uiState.value = _uiState.value.copy(
                currentAnswer = initialAnswerKey,
                isTriangleVisible = true,
                recentlyUsedAnswers = initialRecentList
            )
        }

        viewModelScope.launch {
            shakeDetector.shakeFlow.collectLatest {
                // Only trigger if an animation isn't already running
                if (animationJob?.isActive != true) {
                    triggerSubmergeAndReemerge()
                }
            }
        }
        viewModelScope.launch {
            sensorRepository.fullOrientationFlow.collectLatest { orientation ->
                _uiState.value = _uiState.value.copy(orientation = orientation)
            }
        }
    }

    fun onTriangleTapped() {
        if (animationJob?.isActive != true) {
            triggerSubmergeAndReemerge()
        }
    }

    fun onTriangleDragged(newPosition: Offset) {
        _uiState.value = _uiState.value.copy(trianglePosition = newPosition)
    }

    fun setInitialTrianglePosition(position: Offset) {
        if (_uiState.value.trianglePosition == null) {
            _uiState.value = _uiState.value.copy(trianglePosition = position)
        }
    }

    private fun getNewAnswer(): String {
        val currentState = _uiState.value
        val availableAnswers = answers.filter { it !in currentState.recentlyUsedAnswers }

        return if (availableAnswers.isNotEmpty()) {
            availableAnswers.random()
        } else {
            // Fallback if all answers have been used recently (e.g., small answer pool)
            // This will clear the history and start fresh, preventing repeats for as long as possible.
            _uiState.value =
                currentState.copy(recentlyUsedAnswers = listOfNotNull(currentState.currentAnswer))
            answers.random()
        }
    }

    private fun triggerSubmergeAndReemerge() {
        animationJob?.cancel()
        animationJob = viewModelScope.launch {
            val currentState = _uiState.value
            // 1. Submerge
            if (currentState.isTriangleVisible) {
                _uiState.value = currentState.copy(isTriangleVisible = false)
                delay(1200L) // Time for submersion animation
            }

            // 2. Change answer and position while submerged
            val newAnswer = getNewAnswer()
            val updatedRecent =
                (listOf(newAnswer) + currentState.recentlyUsedAnswers).distinct().take(10)

            _uiState.value = _uiState.value.copy(
                currentAnswer = newAnswer,
                recentlyUsedAnswers = updatedRecent,
                trianglePosition = null, // Nullify to get a new random position on re-emergence
                triggerNewAnswer = currentState.triggerNewAnswer + 1
            )
            delay(500L) // Pause while submerged

            // 3. Re-emerge
            _uiState.value = _uiState.value.copy(isTriangleVisible = true)
        }
    }
}