package com.hereliesaz.cuedetat.domain

import android.graphics.PointF

// Use the apply form rather than PointF(x, y) so callers are testable under
// android.testOptions.unitTests.isReturnDefaultValues = true — that flag makes
// the multi-arg PointF constructor return a default-zero instance in JVM tests,
// which silently breaks rail/pocket geometry that compares against real coords.
fun Vector2.toPointF(): PointF = PointF().apply { x = this@toPointF.x; y = this@toPointF.y }
fun PointF.toVector2(): Vector2 = Vector2(x, y)
