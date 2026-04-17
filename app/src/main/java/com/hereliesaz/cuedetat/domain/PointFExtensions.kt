package com.hereliesaz.cuedetat.domain

import android.graphics.PointF

fun Vector2.toPointF(): PointF = PointF(x, y)
fun PointF.toVector2(): Vector2 = Vector2(x, y)
