package com.hereliesaz.cuedetatlite.view.state

import android.graphics.PointF
import com.hereliesaz.cuedetatlite.domain.WarningText
import com.hereliesaz.cuedetatlite.view.model.ILogicalBall
import com.hereliesaz.cuedetatlite.view.model.ProtractorUnit
import com.hereliesaz.cuedetatlite.view.model.TableModel

data class ScreenState(
    val isProtractorMode: Boolean = true,
    val showActualCueBall: Boolean = true,
    val protractorUnit: ProtractorUnit = ProtractorUnit(),
    val actualCueBall: ILogicalBall? = null,
    val bankingPath: List<PointF> = emptyList(),
    val tableModel: TableModel? = null,
    val isBankingMode: Boolean = false,
    val warningText: WarningText? = null,
    val isImpossibleShot: Boolean = false
)