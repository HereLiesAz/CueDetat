// FILE: app/src/main/java/com/hereliesaz/cuedetat/ui/composables/billing/TesterOutcomeText.kt

package com.hereliesaz.cuedetat.ui.composables.billing

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.hereliesaz.cuedetat.billing.TesterLicenseResult

/**
 * Renders the user-facing message for a [TesterLicenseResult]. Used by
 * both the paywall sheet and the billing debug dialog so the two
 * surfaces never drift in what they tell the user about the same
 * outcome.
 */
@Composable
fun TesterOutcomeText(
    outcome: TesterLicenseResult,
    modifier: Modifier = Modifier,
) {
    val message = when (outcome) {
        TesterLicenseResult.Granted ->
            "Tester license granted. Expert is unlocked."
        TesterLicenseResult.NotOnAllowlist ->
            "That email is not on this build's tester allowlist."
        TesterLicenseResult.AllowlistEmpty ->
            "This build wasn't assembled with a tester allowlist (local / debug build)."
        TesterLicenseResult.InvalidEmail ->
            "Couldn't read an email from the account picker."
        TesterLicenseResult.NotApplicable ->
            "Tester license isn't available in this build (no OAuth Web Client ID configured)."
    }
    val color = if (outcome == TesterLicenseResult.Granted)
        MaterialTheme.colorScheme.primary
    else
        MaterialTheme.colorScheme.error
    Text(
        text = message,
        style = MaterialTheme.typography.bodySmall,
        color = color,
        modifier = modifier,
    )
}
