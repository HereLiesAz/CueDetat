package com.hereliesaz.cuedetat.domain

import android.graphics.PointF
import com.hereliesaz.cuedetat.view.model.Table
import com.hereliesaz.cuedetat.view.state.TableSize
import org.junit.Test
import kotlin.math.abs

private fun pf(x: Float, y: Float) = PointF().apply { this.x = x; this.y = y }

class MasseDebugTest {

    @Test
    fun `debug low elevation`() {
        val table = Table(size = TableSize.EIGHT_FT, isVisible = true)
        val contactOffset = pf(1f, 0f)
        println("contactOffset.x=${contactOffset.x} .y=${contactOffset.y}")
        
        val result = MassePhysicsSimulator.simulate(
            contactOffset = contactOffset,
            elevationDeg = 10f,
            shotAngle = 0f,
            table = table
        )
        println("Points size: ${result.points.size}")
        result.points.take(5).forEachIndexed { i, p -> 
            println("  [$i] x=${p.x} y=${p.y}")
        }
        val dropped = result.points.drop(3)
        println("After drop(3): ${dropped.size} points")
    }
}
