package com.hereliesaz.cuedetat.ui.composables.support

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hereliesaz.cuedetat.support.SupportLink
import com.hereliesaz.cuedetat.support.SupportLinks

/**
 * The donation sheet. Replaces the paywall.
 *
 * Nothing here unlocks anything — every feature in the app is already available
 * to everyone. This exists purely so that someone who wants to pay for something
 * they like has somewhere to do it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportSheet(
    onDismiss: () -> Unit,
    onOpenUrl: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "It's all free",
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = "Every mode, every line, every insult. There is no paid tier " +
                    "and nothing to restore. If it has saved you an argument about " +
                    "whether that was a legal bank, the tip jar is below.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp),
            )

            SupportLinks.ALL.forEachIndexed { index, link ->
                if (index > 0) HorizontalDivider()
                SupportRow(link = link, onClick = { onOpenUrl(link.url) })
            }
        }
    }
}

@Composable
private fun SupportRow(link: SupportLink, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(text = link.label, style = MaterialTheme.typography.titleMedium)
        Text(
            text = link.blurb,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
