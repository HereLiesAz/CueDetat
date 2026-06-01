// FILE: app/src/main/java/com/hereliesaz/cuedetat/ui/composables/paywall/PaywallViewModel.kt

package com.hereliesaz.cuedetat.ui.composables.paywall

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hereliesaz.cuedetat.billing.EntitlementDiagnostics
import com.hereliesaz.cuedetat.billing.EntitlementRepository
import com.hereliesaz.cuedetat.billing.PaywallTrigger
import com.hereliesaz.cuedetat.billing.ProductDetailsState
import com.hereliesaz.cuedetat.billing.TesterLicenseResult
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

    /** True once we've kicked off the silent Credential Manager resolve for this paywall session. */
    private var hasAttemptedSilentResolve = false

    val isCredentialManagerAvailable: Boolean
        get() = repository.isCredentialManagerAvailable

    init {
        _uiState.value = _uiState.value.copy(trialAvailable = repository.isExpertTrialAvailable)
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
        refreshDiagnostics()
    }

    fun setTrigger(trigger: PaywallTrigger) {
        triggerSnapshot = trigger
    }

    fun purchase(activity: Activity) {
        viewModelScope.launch {
            runCatching { repository.launchPurchase(activity) }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = it.message ?: "Unable to start purchase"
                    )
                }
        }
    }

    fun restore() {
        viewModelScope.launch {
            runCatching { repository.restorePurchases() }
            refreshDiagnostics()
        }
    }

    /**
     * Start the one-time, one-hour Expert preview. On success the entitlement
     * flow flips active and the existing collector above auto-enters Expert and
     * dismisses the sheet — same path as a completed purchase.
     */
    fun startTrial() {
        viewModelScope.launch {
            runCatching { repository.startExpertTrial() }
            _uiState.value = _uiState.value.copy(trialAvailable = repository.isExpertTrialAvailable)
        }
    }

    /**
     * Try a silent (no-UI) Credential Manager resolve as soon as the paywall
     * sheet is composed with an Activity context. If the user has previously
     * authorized this app for a Google account that's on the allowlist, this
     * grants Expert without ever showing the picker.
     */
    fun attemptSilentResolveOnce(activity: Activity) {
        if (hasAttemptedSilentResolve) return
        hasAttemptedSilentResolve = true
        viewModelScope.launch {
            val outcome = repository.silentlyResolveTesterLicense(activity)
            // Only surface a UX message for actual grants; silent failures
            // are expected and the paywall just continues to show plans.
            if (outcome == TesterLicenseResult.Granted) {
                _uiState.value = _uiState.value.copy(testerLicenseOutcome = outcome)
            }
            refreshDiagnostics()
        }
    }

    /** Show the Credential Manager one-tap account picker. */
    fun runInteractivePicker(activity: Activity) {
        viewModelScope.launch {
            val outcome = repository.resolveTesterLicenseViaCredentialManager(activity)
            _uiState.value = _uiState.value.copy(testerLicenseOutcome = outcome)
            refreshDiagnostics()
        }
    }

    /** Manual email-entry fallback (e.g. user prefers not to use Credential Manager). */
    fun applyTesterLicenseManually(email: String) {
        viewModelScope.launch {
            val outcome = repository.applyTesterLicense(email)
            _uiState.value = _uiState.value.copy(testerLicenseOutcome = outcome)
            refreshDiagnostics()
        }
    }

    fun clearTesterLicenseOutcome() {
        _uiState.value = _uiState.value.copy(testerLicenseOutcome = null)
    }

    fun toggleDiagnostics() {
        _uiState.value = _uiState.value.copy(showDiagnostics = !_uiState.value.showDiagnostics)
        refreshDiagnostics()
    }

    private fun refreshDiagnostics() {
        _uiState.value = _uiState.value.copy(diagnostics = repository.diagnostics())
    }

    sealed class PurchaseFlowEvent {
        object PurchasedAutoEnterExpert : PurchaseFlowEvent()
        object PurchasedNoAutoEnter : PurchaseFlowEvent()
    }
}

data class PaywallUiState(
    val productDetails: ProductDetailsState = ProductDetailsState.Loading,
    val errorMessage: String? = null,
    val testerLicenseOutcome: TesterLicenseResult? = null,
    val diagnostics: EntitlementDiagnostics = EntitlementDiagnostics(emptyList(), "not loaded", false),
    val showDiagnostics: Boolean = false,
    val trialAvailable: Boolean = false,
)
