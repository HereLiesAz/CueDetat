// FILE: app/src/main/java/com/hereliesaz/cuedetat/ui/composables/CuedetatButton.kt

package com.hereliesaz.cuedetat.ui.composables

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A customized circular outlined button used consistently across the app.
 *
 * @param onClick Callback when the button is clicked.
 * @param text The text label inside the button.
 * @param modifier Modifier for external styling/positioning.
 * @param size The diameter of the button (default 72.dp).
 * @param color The primary color for the border and text.
 */
@Composable
fun CuedetatButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    size: Dp = 72.dp,
    color: Color = MaterialTheme.colorScheme.primary
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.size(size),
        shape = CircleShape,
        // Semi-transparent border.
        border = BorderStroke(3.dp, color.copy(alpha = 0.7f)),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.Transparent, // Transparent background.
            contentColor = color // Colored text.
        ),
        contentPadding = PaddingValues(4.dp)
    ) {
        // Button Label.
        Text(
            text = text,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 14.sp,
                lineHeight = 16.sp
            ),
        )
    }
}
