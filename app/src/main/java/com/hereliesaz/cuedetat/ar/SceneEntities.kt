package com.hereliesaz.cuedetat.ar

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.xr.scenecore.compose.Entity
import androidx.xr.scenecore.materials.rememberMaterial
import androidx.xr.scenecore.primitives.Cube
import androidx.xr.scenecore.primitives.Sphere
import com.google.ar.core.Pose

private const val BALL_DIAMETER = 0.057f
private const val TABLE_WIDTH = 1.27f
private const val TABLE_LENGTH = 2.54f
private const val TABLE_HEIGHT = 0.05f
private const val RAIL_HEIGHT = 0.07f
private const val RAIL_WIDTH = 0.1f

@Composable
fun BallEntity(pose: Pose, color: Color) {
    val material = rememberMaterial(color = color)
    Entity(pose = pose) {
        Sphere(radius = BALL_DIAMETER / 2f, material = material)
    }
}

@Composable
fun TableEntity(pose: Pose) {
    val greenFelt = rememberMaterial(color = Color(0.0f, 0.4f, 0.1f))
    val wood = rememberMaterial(color = Color(0.36f, 0.25f, 0.2f))

    Entity(pose = pose) {
        // Surface
        Cube(
            size = floatArrayOf(TABLE_WIDTH, TABLE_HEIGHT, TABLE_LENGTH),
            center = floatArrayOf(0f, -TABLE_HEIGHT / 2f, 0f),
            material = greenFelt
        )
        // Rails
        Cube(size = floatArrayOf(RAIL_WIDTH, RAIL_HEIGHT, TABLE_LENGTH), center = floatArrayOf(-TABLE_WIDTH/2f - RAIL_WIDTH/2f, 0f, 0f), material = wood)
        Cube(size = floatArrayOf(RAIL_WIDTH, RAIL_HEIGHT, TABLE_LENGTH), center = floatArrayOf(TABLE_WIDTH/2f + RAIL_WIDTH/2f, 0f, 0f), material = wood)
        Cube(size = floatArrayOf(TABLE_WIDTH + 2 * RAIL_WIDTH, RAIL_HEIGHT, RAIL_WIDTH), center = floatArrayOf(0f, 0f, -TABLE_LENGTH/2f - RAIL_WIDTH/2f), material = wood)
        Cube(size = floatArrayOf(TABLE_WIDTH + 2 * RAIL_WIDTH, RAIL_HEIGHT, RAIL_WIDTH), center = floatArrayOf(0f, 0f, TABLE_LENGTH/2f + RAIL_WIDTH/2f), material = wood)
    }
}
