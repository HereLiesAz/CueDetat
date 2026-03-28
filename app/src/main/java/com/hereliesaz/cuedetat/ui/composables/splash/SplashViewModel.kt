// app/src/main/java/com/hereliesaz/cuedetat/ui/composables/splash/SplashViewModel.kt
package com.hereliesaz.cuedetat.ui.composables.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hereliesaz.cuedetat.data.UserPreferencesRepository
import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.domain.ExperienceMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val userPreferences: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SplashState())
    val uiState: StateFlow<SplashState> = _uiState.asStateFlow()

    fun onEvent(event: SplashEvent) {
        when (event) {
            is SplashEvent.SelectMode -> {
                viewModelScope.launch {
                    _uiState.update { it.copy(isSaving = true) }

                    val state = userPreferences.stateFlow.firstOrNull() ?: CueDetatState()
                    val modeEnum = try { ExperienceMode.valueOf(event.mode) } catch (e: Exception) { null }
                    userPreferences.saveState(state.copy(experienceMode = modeEnum))

                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            navigateToMain = true
                        )
                    }
                                        val experienceMode = try {
                        ExperienceMode.valueOf(event.mode)
                    } catch (_: Exception) {
                        ExperienceMode.EXPERT
                    }
                    userPreferences.saveState(CueDetatState(experienceMode = experienceMode))
                }
            }
            is SplashEvent.NavigationConsumed -> {
                _uiState.update { it.copy(navigateToMain = false) }
            }
        }
    }
}
