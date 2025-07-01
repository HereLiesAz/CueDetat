// hereliesaz/cuedetat/CueDetat-CueDetatLite/app/src/main/java/com/hereliesaz/cuedetatlite/view/state/ScreenState.kt
package com.hereliesaz.cuedetatlite.view.state

import android.graphics.PointF
import com.hereliesaz.cuedetatlite.domain.WarningManager
import com.hereliesaz.cuedetatlite.view.model.ILogicalBall
import com.hereliesaz.cuedetatlite.view.model.ProtractorUnit
import com.hereliesaz.cuedetatlite.view.model.TableModel

data class ScreenState(
    val isProtractorMode: Boolean = false,
    val showActualCueBall: Boolean = true,
    val protractorUnit: ProtractorUnit = ProtractorUnit(),
    val actualCueBall: IlogicalBall? = null,
    val bankingPath: List<PointF> = emptyList(),
    val tableModel: TableModel? = null,
    val isBankingMode: Boolean = false,
    val warningText: WarningText? = null
)
