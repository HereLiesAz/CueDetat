// FILE: app/src/main/java/com/hereliesaz/cuedetat/ui/composables/paywall/PaywallViewModel.kt

package com.hereliesaz.cuedetat.ui.composables.paywall

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hereliesaz.cuedetat.billing.BasePlanId
import com.hereliesaz.cuedetat.billing.EntitlementRepository
import com.hereliesaz.cuedetat.billing.PaywallTrigger
import com.hereliesaz.cuedetat.billing.ProductDetailsState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PaywallViewModel @Inject constructor(
    private val repository: EntitlementRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PaywallUiState())
    val uiState: StateFlow<PaywallUiState> = _uiState.asStateFlow()

    private val _purchaseFlowResults = MutableSharedFlow<PurchaseFlowEvent>(replay = 0)
    val purchaseFlowResults: SharedFlow<PurchaseFlowEvent> = _purchaseFlowResults.asSharedFlow()

    private var triggerSnapshot: PaywallTrigger? = null

    init {
        viewModelScope.launch {
            repository.productDetails().collect { state ->
                _uiState.value = _uiState.value.copy(productDetails = state)
            }
        }
        viewModelScope.launch {
            // StateFlow already deduplicates equal values, so no need for distinctUntilChanged.
            // Every paywall surface in this app is reached because the user
            // tried to use Expert. After a successful purchase we should drop
            // them into Expert automatically; making them tap Expert a second
            // time looks like the redemption silently failed.
            repository.entitlement.collect { entitlement ->
                if (entitlement.active && triggerSnapshot != null) {
                    _purchaseFlowResults.emit(PurchaseFlowEvent.PurchasedAutoEnterExpert)
                    triggerSnapshot = null
                }
            }
        }
    }

    fun setTrigger(trigger: PaywallTrigger) {
        triggerSnapshot = trigger
    }

    fun purchase(activity: Activity, basePlan: BasePlanId) {
        viewModelScope.launch {
            runCatching { repository.launchPurchase(activity, basePlan) }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = it.message ?: "Unable to start purchase"
                    )
                }
        }
    }

    fun restore() {
        viewModelScope.launch { runCatching { repository.restorePurchases() } }
    }

    sealed class PurchaseFlowEvent {
        object PurchasedAutoEnterExpert : PurchaseFlowEvent()
        object PurchasedNoAutoEnter : PurchaseFlowEvent()
    }
}

data class PaywallUiState(
    val productDetails: ProductDetailsState = ProductDetailsState.Loading,
    val errorMessage: String? = null
)
