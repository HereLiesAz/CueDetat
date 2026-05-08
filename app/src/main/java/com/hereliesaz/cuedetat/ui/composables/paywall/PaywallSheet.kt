// FILE: app/src/main/java/com/hereliesaz/cuedetat/ui/composables/paywall/PaywallSheet.kt

package com.hereliesaz.cuedetat.ui.composables.paywall

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hereliesaz.cuedetat.billing.BasePlanId
import com.hereliesaz.cuedetat.billing.PaywallTrigger
import com.hereliesaz.cuedetat.billing.ProductDetailsState

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
