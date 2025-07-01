// hereliesaz/cuedetat/CueDetat-CueDetatLite/app/src/main/java/com/hereliesaz/cuedetatlite/view/renderer/TableRenderer.kt
package com.hereliesaz.cuedetatlite.view.renderer

import android.graphics.Canvas
import com.hereliesaz.cuedetatlite.view.PaintCache
import com.hereliesaz.cuedetatlite.view.state.OverlayState
import com.hereliesaz.cuedetatlite.view.model.TableModel
import com.hereliesaz.cuedetatlite.view.state.ScreenState
class TableRenderer(private val paintCache: PaintCache) {

    fun draw(canvas: Canvas, state: OverlayState) {
        state.screenState.tableModel?.let { tableModel ->
            canvas.save()
            canvas.concat(state.pitchMatrix)

            // Draw table surface
            canvas.drawRect(tableModel.surface, paintCache.whitePaint)

            // Draw pockets
            tableModel.pockets.forEach { pocket ->
                canvas.drawCircle(pocket.center.x, pocket.center.y, pocket.radius, paintCache.redPaint)
            }

            // Draw diamonds
            val diamondRadius = 5f // Or some other appropriate size
            tableModel.getDiamonds().forEach { diamond ->
                canvas.drawCircle(diamond.x, diamond.y, diamondRadius, paintCache.greenPaint) // Or a new diamond paint
            }

            canvas.restore()
        }
    }
}
