package com.hereliesaz.cuedetat.ui.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hereliesaz.cuedetat.R
import com.hereliesaz.cuedetat.domain.ExperienceMode
import kotlinx.coroutines.delay

/**
 * The initial launch screen of the application.
 *
 * It displays the logo/tagline and then prompts the user to select an [ExperienceMode].
 *
 * @param onRoleSelected Callback triggered when the user selects a role (Expert, Beginner, etc.).
 */
@Composable
fun SplashScreen(onRoleSelected: (ExperienceMode) -> Unit) {
    // State to trigger the appearance of the question/buttons after delay.
    var showQuestion by remember { mutableStateOf(false) }

    // Start a timer on launch.
    LaunchedEffect(Unit) {
        delay(3000L) // Wait 3 seconds.
        showQuestion = true // Reveal UI.
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black) // Black background for cinematic feel.
    ) {
        // Logo and Tagline container, always centered in the screen.
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo_cue_detat),
                contentDescription = "Application Logo",
                modifier = Modifier.size(256.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Tagline fades out when the question appears to reduce clutter.
            AnimatedVisibility(
                visible = !showQuestion,
                exit = fadeOut(animationSpec = tween(durationMillis = 500))
            ) {
                Text(
                    text = stringResource(id = R.string.tagline),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Question and buttons container.
        // Slides in from the bottom while fading in.
        AnimatedVisibility(
            visible = showQuestion,
            modifier = Modifier.fillMaxSize(),
            enter = fadeIn(animationSpec = tween(durationMillis = 500, delayMillis = 200)) +
                    slideInVertically(
                        animationSpec = tween(durationMillis = 500, delayMillis = 200),
                        initialOffsetY = { it / 2 } // Start from halfway down.
                    )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 64.dp),
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "What's your relationship to pool?",
                    fontSize = 20.sp,
                    color = Color.White,
                    modifier = Modifier.padding(16.dp)
                )
                // Vertical list of choice buttons.
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(8.dp)
                ) {
                    QuestionButton("Expert") { onRoleSelected(ExperienceMode.EXPERT) }
                    QuestionButton("Beginner") { onRoleSelected(ExperienceMode.BEGINNER) }
                    QuestionButton("Hater") { onRoleSelected(ExperienceMode.HATER) }
                }
            }
        }
    }
}

/**
 * Helper composable for the role selection buttons.
 */
@Composable
private fun QuestionButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth(0.6f) // Consistent width (60% of screen).
            .clickable { onClick() }
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 24.sp
        )
    }
}
