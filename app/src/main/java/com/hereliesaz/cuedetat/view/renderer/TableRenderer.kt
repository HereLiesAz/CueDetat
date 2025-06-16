// app/src/main/java/com/hereliesaz/cuedetat/view/renderer/TableRenderer.kt
package com.hereliesaz.cuedetat.view.renderer

import android.graphics.Canvas
import com.hereliesaz.cuedetat.view.PaintCache
import com.hereliesaz.cuedetat.view.model.PoolTable

class TableRenderer {

    fun draw(canvas: Canvas, table: PoolTable, paints: PaintCache) {
        canvas.save()

        // Translate to the table's center and apply its rotation
        canvas.translate(table.center.x, table.center.y)
        canvas.rotate(table.rotationDegrees)

        // Draw the playing surface
        canvas.drawRect(table.playingSurface, paints.tableRailPaint)

        // Draw the pockets
        table.pockets.forEach { pocketCenter ->
            canvas.drawCircle(
                pocketCenter.x,
                pocketCenter.y,
                table.pocketRadius,
                paints.tablePocketPaint
            )
        }

        canvas.restore()
    }
}
