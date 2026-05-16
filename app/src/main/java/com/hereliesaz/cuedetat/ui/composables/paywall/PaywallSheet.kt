// FILE: app/src/main/java/com/hereliesaz/cuedetat/ui/composables/paywall/PaywallSheet.kt

package com.hereliesaz.cuedetat.ui.composables.paywall

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hereliesaz.cuedetat.billing.BasePlanId
import com.hereliesaz.cuedetat.billing.PaywallTrigger
import com.hereliesaz.cuedetat.billing.ProductDetailsState
import com.hereliesaz.cuedetat.billing.TesterLicenseResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaywallSheet(
    trigger: PaywallTrigger,
    onDismiss: () -> Unit,
    onPurchasedAutoEnterExpert: () -> Unit,
    viewModel: PaywallViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(trigger) { viewModel.setTrigger(trigger) }

    LaunchedEffect(Unit) {
        viewModel.purchaseFlowResults.collect { event ->
            when (event) {
                is PaywallViewModel.PurchaseFlowEvent.PurchasedAutoEnterExpert -> {
                    onPurchasedAutoEnterExpert()
                    onDismiss()
                }
                is PaywallViewModel.PurchaseFlowEvent.PurchasedNoAutoEnter -> {
                    onDismiss()
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Unlock Expert Mode", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text(
                "Full AR table tracking, ball selection, glasses mode, and the ability to " +
                        "feel marginally less bad about yourself.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(24.dp))

            when (val pd = uiState.productDetails) {
                is ProductDetailsState.Loading -> CircularProgressIndicator()
                is ProductDetailsState.Loaded -> Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PlanCard(
                        title = "Yearly — Best Value",
                        formattedPrice = pd.yearlyFormattedPrice,
                        period = "/year",
                        ctaText = if (pd.trialDays > 0)
                            "Start ${pd.trialDays}-day free trial"
                        else "Subscribe",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            activity?.let { viewModel.purchase(it, BasePlanId.YEARLY) }
                        }
                    )
                    PlanCard(
                        title = "Monthly",
                        formattedPrice = pd.monthlyFormattedPrice,
                        period = "/month",
                        ctaText = if (pd.trialDays > 0)
                            "Start ${pd.trialDays}-day free trial"
                        else "Subscribe",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            activity?.let { viewModel.purchase(it, BasePlanId.MONTHLY) }
                        }
                    )
                }
                is ProductDetailsState.Error -> Text(
                    "Couldn't load plans: ${pd.message}",
                    color = MaterialTheme.colorScheme.error
                )
                is ProductDetailsState.NotApplicable -> Text("Expert Mode is unlocked.")
            }

            Spacer(Modifier.height(16.dp))
            Text(
                "Free trial, then the price shown. Cancel anytime in Google Play.",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(16.dp))
            TextButton(onClick = { viewModel.restore() }) { Text("Restore Purchases") }
            TextButton(onClick = onDismiss) { Text("Continue in Beginner Mode") }

            HorizontalDivider(Modifier.padding(vertical = 12.dp))

            TesterLicenseSection(viewModel = viewModel, uiState = uiState)

            Spacer(Modifier.height(16.dp))
            val githubFooter = buildAnnotatedString {
                append("Cue D'etat is always available in its entirety for free on ")
                withLink(
                    LinkAnnotation.Url(
                        url = "https://github.com/HereLiesAz/CueDetat",
                        styles = TextLinkStyles(style = SpanStyle(color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline))
                    )
                ) {
                    append("Github")
                }
                append(".")
            }
            Text(
                text = githubFooter,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

/**
 * Tester license + diagnostics block. Lets a user on the closed-testing
 * Google Group grant themselves Expert without buying — and gives them a
 * legible view of what Play Billing is actually returning when a real
 * purchase looks like it isn't being honoured.
 */
@Composable
private fun TesterLicenseSection(
    viewModel: PaywallViewModel,
    uiState: PaywallUiState,
) {
    val signInIntent = remember(viewModel) { viewModel.googleSignInIntent() }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        viewModel.applyTesterLicenseFromSignInResult(result.data)
    }

    var manualEmailExpanded by remember { mutableStateOf(false) }
    var manualEmail by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Tester / closed beta", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(4.dp))
        Text(
            "If your Google account is in the project's tester group, you can " +
                "unlock Expert directly — no purchase needed.",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(Modifier.height(8.dp))

        if (signInIntent != null) {
            Button(
                onClick = { launcher.launch(signInIntent) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Check via Google Sign-In") }
        }

        TextButton(onClick = { manualEmailExpanded = !manualEmailExpanded }) {
            Text(if (manualEmailExpanded) "Hide manual email" else "Use manual email instead")
        }

        if (manualEmailExpanded) {
            OutlinedTextField(
                value = manualEmail,
                onValueChange = { manualEmail = it },
                label = { Text("Tester email") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = { viewModel.applyTesterLicenseManually(manualEmail) },
                modifier = Modifier.fillMaxWidth(),
                enabled = manualEmail.contains('@'),
            ) { Text("Try tester license") }
        }

        val outcome = uiState.testerLicenseOutcome
        if (outcome != null) {
            Spacer(Modifier.height(8.dp))
            val message = when (outcome) {
                TesterLicenseResult.Granted ->
                    "Tester license granted. Expert is unlocked."
                TesterLicenseResult.NotOnAllowlist ->
                    "That email is not on this build's tester allowlist."
                TesterLicenseResult.AllowlistEmpty ->
                    "This build wasn't assembled with a tester allowlist (local / debug build)."
                TesterLicenseResult.InvalidEmail ->
                    "Couldn't read an email from the sign-in response."
                TesterLicenseResult.NotApplicable ->
                    "Tester license isn't available in this build flavor."
            }
            val isGood = outcome == TesterLicenseResult.Granted
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = if (isGood) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
        }

        Spacer(Modifier.height(8.dp))
        TextButton(onClick = { viewModel.toggleDiagnostics() }) {
            Text(if (uiState.showDiagnostics) "Hide billing diagnostics" else "Show billing diagnostics")
        }

        if (uiState.showDiagnostics) {
            val d = uiState.diagnostics
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 240.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    "Allowlist baked into APK: ${if (d.testerAllowlistConfigured) "yes" else "no"}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "Last refresh: ${d.lastRefreshOutcome}",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(Modifier.height(4.dp))
                Text("Recent purchase snapshots:", style = MaterialTheme.typography.bodySmall)
                if (d.recentPurchaseSnapshots.isEmpty()) {
                    Text("  (none recorded yet)", style = MaterialTheme.typography.bodySmall)
                } else {
                    d.recentPurchaseSnapshots.forEach {
                        Text(
                            "  $it",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlanCard(
    title: String,
    formattedPrice: String,
    period: String,
    ctaText: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Row {
                Text(formattedPrice, style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.width(2.dp))
                Text(period, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(12.dp))
            Button(onClick = onClick, modifier = Modifier.fillMaxWidth()) { Text(ctaText) }
        }
    }
}
