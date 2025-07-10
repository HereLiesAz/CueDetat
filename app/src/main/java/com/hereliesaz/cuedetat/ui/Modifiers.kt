package com.hereliesaz.cuedetat.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.layout

fun Modifier.vertical() = this
    .rotate(-90f)
    .layout { measurable, constraints ->
        val placeable = measurable.measure(
            constraints.copy(
                minWidth = constraints.minHeight,
                maxWidth = constraints.maxHeight,
                minHeight = constraints.minWidth,
                maxHeight = constraints.maxWidth,
            )
        )
        layout(placeable.height, placeable.width) {
            placeable.placeRelative(
                x = -(placeable.width / 2 - placeable.height / 2),
                y = -(placeable.height / 2 - placeable.width / 2)
            )
        }
    }