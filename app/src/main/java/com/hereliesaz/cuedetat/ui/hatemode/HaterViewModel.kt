package com.hereliesaz.cuedetat.ui.hatemode

import android.content.Context
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
import kotlin.random.Random

data class HaterState(
    val currentAnswer: String? = "Ask a question. Or don't. See if I care.",
    val isShaking: Boolean = false,
    val orientation: FullOrientation = FullOrientation(0f, 0f, 0f)
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

    init {
        viewModelScope.launch {
            shakeDetector.shakeFlow.collectLatest {
                triggerShakeAnimation()
            }
        }
        viewModelScope.launch {
            sensorRepository.fullOrientationFlow.collectLatest { orientation ->
                _uiState.value = _uiState.value.copy(orientation = orientation)
            }
        }
    }

    private fun triggerShakeAnimation() {
        animationJob?.cancel()
        animationJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isShaking = true, currentAnswer = null)
            delay(2500L) // Duration of the shake animation
            val newAnswer = answers[Random.nextInt(answers.size)]
            _uiState.value = _uiState.value.copy(isShaking = false, currentAnswer = newAnswer)
        }
    }
}