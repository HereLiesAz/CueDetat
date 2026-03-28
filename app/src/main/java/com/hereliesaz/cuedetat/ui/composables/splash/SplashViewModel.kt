// app/src/main/java/com/hereliesaz/cuedetat/ui/composables/splash/SplashViewModel.kt
package com.hereliesaz.cuedetat.ui.composables.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hereliesaz.cuedetat.data.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

                    // Look at your UserPreferencesRepository class.
                    // Find the function that saves this. Put it here.
                    userPreferences.YOUR_ACTUAL_SAVE_METHOD_HERE(event.mode)

                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            navigateToMain = true
                        )
                    }
                }
            }
            is SplashEvent.NavigationConsumed -> {
                _uiState.update { it.copy(navigateToMain = false) }
            }
        }
    }
}