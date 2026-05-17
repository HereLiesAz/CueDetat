// FILE: app/src/main/java/com/hereliesaz/cuedetat/ui/composables/dialogs/BillingDebugDialog.kt

package com.hereliesaz.cuedetat.ui.composables.dialogs

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hereliesaz.cuedetat.billing.EntitlementDiagnostics
import com.hereliesaz.cuedetat.billing.EntitlementRepository
import com.hereliesaz.cuedetat.billing.EntitlementSource
import com.hereliesaz.cuedetat.billing.TesterLicenseResult
import com.hereliesaz.cuedetat.billing.isPlausibleTesterEmail
import com.hereliesaz.cuedetat.ui.composables.billing.TesterOutcomeText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Dedicated billing debug screen. Surfaces everything an expert tester
 * needs to figure out why their purchase / tester license isn't unlocking
 * Expert. The data shown is read directly from EntitlementRepository so
 * it always reflects what the app actually sees, not a cached snapshot.
 */
@Composable
fun BillingDebugDialog(
    onDismiss: () -> Unit,
    viewModel: BillingDebugViewModel = hiltViewModel(),
) {
    val activity = LocalContext.current as? Activity
    val state by viewModel.uiState.collectAsState()
    var manualEmail by remember { mutableStateOf("") }

    // Pull a fresh diagnostics snapshot when the dialog opens.
    LaunchedEffect(Unit) { viewModel.refresh() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Billing & tester license debug") },
        text = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                EntitlementBlock(state)
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                TesterLicenseBlock(
                    state = state,
                    manualEmail = manualEmail,
                    onManualEmailChange = { manualEmail = it },
                    onPickAccount = { activity?.let { viewModel.runInteractivePicker(it) } },
                    onApplyManual = { viewModel.applyTesterLicenseManually(manualEmail) },
                    onClear = { viewModel.clearTesterLicense() },
                )
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                DiagnosticsBlock(state.diagnostics)
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                ActionsBlock(
                    onRefresh = { viewModel.refresh() },
                    onRestorePurchases = { viewModel.restorePurchases() },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
private fun EntitlementBlock(state: BillingDebugUiState) {
    Text("Current entitlement", style = MaterialTheme.typography.titleSmall)
    Spacer(Modifier.height(4.dp))
    KvLine("active", state.entitlementActive.toString())
    KvLine("source", state.entitlementSource.name)
    KvLine("productId", state.entitlementProductId ?: "n/a")
    KvLine("isDeviceGenuine", state.isDeviceGenuine.toString())
    KvLine("lastVerifiedMillis", state.lastVerifiedAtMillis?.toString() ?: "n/a")
}

@Composable
private fun TesterLicenseBlock(
    state: BillingDebugUiState,
    manualEmail: String,
    onManualEmailChange: (String) -> Unit,
    onPickAccount: () -> Unit,
    onApplyManual: () -> Unit,
    onClear: () -> Unit,
) {
    Text("Tester license", style = MaterialTheme.typography.titleSmall)
    Spacer(Modifier.height(4.dp))
    KvLine(
        "allowlist baked into APK",
        if (state.diagnostics.testerAllowlistConfigured) "yes" else "no"
    )
    KvLine(
        "Credential Manager configured",
        if (state.credentialManagerAvailable) "yes" else "no"
    )

    Spacer(Modifier.height(8.dp))
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Button(
            onClick = onPickAccount,
            modifier = Modifier.weight(1f),
            enabled = state.credentialManagerAvailable,
        ) { Text("Pick Google account") }
        OutlinedButton(
            onClick = onClear,
            modifier = Modifier.weight(1f),
            enabled = state.entitlementSource == EntitlementSource.TESTER_LICENSE,
        ) { Text("Revoke tester") }
    }

    Spacer(Modifier.height(8.dp))
    OutlinedTextField(
        value = manualEmail,
        onValueChange = onManualEmailChange,
        label = { Text("Manual tester email") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(4.dp))
    Button(
        onClick = onApplyManual,
        enabled = isPlausibleTesterEmail(manualEmail),
        modifier = Modifier.fillMaxWidth(),
    ) { Text("Try tester license") }

    state.lastTesterOutcome?.let { outcome ->
        Spacer(Modifier.height(8.dp))
        TesterOutcomeText(outcome)
    }
}

@Composable
private fun DiagnosticsBlock(diagnostics: EntitlementDiagnostics) {
    Text("Recent purchase snapshots", style = MaterialTheme.typography.titleSmall)
    Spacer(Modifier.height(4.dp))
    Text(
        "Last refresh: ${diagnostics.lastRefreshOutcome}",
        style = MaterialTheme.typography.bodySmall,
        fontFamily = FontFamily.Monospace
    )
    Spacer(Modifier.height(4.dp))
    if (diagnostics.recentPurchaseSnapshots.isEmpty()) {
        Text(
            "(none recorded yet — try \"Restore purchases\" below)",
            style = MaterialTheme.typography.bodySmall,
        )
    } else {
        diagnostics.recentPurchaseSnapshots.forEach { line ->
            Text(
                line,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun ActionsBlock(
    onRefresh: () -> Unit,
    onRestorePurchases: () -> Unit,
) {
    Text("Actions", style = MaterialTheme.typography.titleSmall)
    Spacer(Modifier.height(4.dp))
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedButton(onClick = onRefresh, modifier = Modifier.weight(1f)) {
            Text("Refresh state")
        }
        OutlinedButton(onClick = onRestorePurchases, modifier = Modifier.weight(1f)) {
            Text("Restore purchases")
        }
    }
}

@Composable
private fun KvLine(key: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(
            "$key:",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f),
        )
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.weight(2f),
        )
    }
}

data class BillingDebugUiState(
    val entitlementActive: Boolean = false,
    val entitlementSource: EntitlementSource = EntitlementSource.NONE,
    val entitlementProductId: String? = null,
    val isDeviceGenuine: Boolean = true,
    val lastVerifiedAtMillis: Long? = null,
    val credentialManagerAvailable: Boolean = false,
    val diagnostics: EntitlementDiagnostics = EntitlementDiagnostics(emptyList(), "not loaded", false),
    val lastTesterOutcome: TesterLicenseResult? = null,
)

@HiltViewModel
class BillingDebugViewModel @Inject constructor(
    private val repository: EntitlementRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BillingDebugUiState())
    val uiState: StateFlow<BillingDebugUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.entitlement.collect { e ->
                _uiState.value = _uiState.value.copy(
                    entitlementActive = e.active,
                    entitlementSource = e.source,
                    entitlementProductId = e.productId,
                    isDeviceGenuine = e.isDeviceGenuine,
                    lastVerifiedAtMillis = e.lastVerifiedAtMillis,
                )
            }
        }
        refresh()
    }

    fun refresh() {
        // Surface the synchronous bits immediately (allowlist + CM
        // configuration don't change at runtime), then re-read diagnostics
        // *after* the async refresh so the user sees post-refresh state.
        _uiState.value = _uiState.value.copy(
            credentialManagerAvailable = repository.isCredentialManagerAvailable,
            diagnostics = repository.diagnostics(),
        )
        viewModelScope.launch {
            runCatching { repository.refresh() }
            _uiState.value = _uiState.value.copy(diagnostics = repository.diagnostics())
        }
    }

    fun restorePurchases() {
        viewModelScope.launch {
            runCatching { repository.restorePurchases() }
            _uiState.value = _uiState.value.copy(diagnostics = repository.diagnostics())
        }
    }

    fun runInteractivePicker(activity: Activity) {
        viewModelScope.launch {
            val outcome = repository.resolveTesterLicenseViaCredentialManager(activity)
            _uiState.value = _uiState.value.copy(
                lastTesterOutcome = outcome,
                diagnostics = repository.diagnostics(),
            )
        }
    }

    fun applyTesterLicenseManually(email: String) {
        viewModelScope.launch {
            val outcome = repository.applyTesterLicense(email)
            _uiState.value = _uiState.value.copy(
                lastTesterOutcome = outcome,
                diagnostics = repository.diagnostics(),
            )
        }
    }

    fun clearTesterLicense() {
        viewModelScope.launch {
            runCatching { repository.clearTesterLicense() }
            _uiState.value = _uiState.value.copy(diagnostics = repository.diagnostics())
        }
    }
}
