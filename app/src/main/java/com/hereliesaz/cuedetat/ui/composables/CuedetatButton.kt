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
        border = BorderStroke(2.dp, color.copy(alpha = 0.7f)),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.Transparent,
            contentColor = color
        ),
        contentPadding = PaddingValues(4.dp)
    ) {
        Text(
            text = text,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 12.sp,
                lineHeight = 14.sp
            ),
        )
    }
}