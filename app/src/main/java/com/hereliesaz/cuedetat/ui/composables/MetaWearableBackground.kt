package com.hereliesaz.cuedetat.ui.composables

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.hereliesaz.cuedetat.data.MetaWearableRepository

@Composable
fun MetaWearableBackground(
    modifier: Modifier = Modifier,
    metaWearableRepository: MetaWearableRepository
) {
    val videoFrame by metaWearableRepository.videoFrame.collectAsState()

    Box(modifier = modifier.fillMaxSize()) {
        videoFrame?.let { bitmap ->
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Meta Glasses Stream",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    }
}
