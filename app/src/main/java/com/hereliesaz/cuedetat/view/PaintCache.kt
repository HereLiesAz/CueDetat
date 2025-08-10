// FILE: app/src/main/java/com/hereliesaz/cuedetat/view/PaintCache.kt

package com.hereliesaz.cuedetat.view

import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Typeface
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.ui.theme.WarningRed
import com.hereliesaz.cuedetat.view.config.ball.ActualCueBall
import com.hereliesaz.cuedetat.view.config.ball.GhostCueBall
import com.hereliesaz.cuedetat.view.config.ball.TargetBall
import com.hereliesaz.cuedetat.view.config.line.AimingLine
import com.hereliesaz.cuedetat.view.config.line.BankLine1
import com.hereliesaz.cuedetat.view.config.line.BankLine2
import com.hereliesaz.cuedetat.view.config.line.BankLine3
import com.hereliesaz.cuedetat.view.config.line.BankLine4
import com.hereliesaz.cuedetat.view.config.line.ShotGuideLine
import com.hereliesaz.cuedetat.view.config.line.TangentLine
import com.hereliesaz.cuedetat.view.config.table.Holes
import com.hereliesaz.cuedetat.view.config.table.Rail

class PaintCache {

    // --- Primary Paint Objects ---
    val tableOutlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    val targetCirclePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    val cueCirclePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    val actualCueBallPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    val shotLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    val tangentLineSolidPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    val tangentLineDottedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; pathEffect = DashPathEffect(floatArrayOf(15f, 10f), 0f)
    }
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    val warningPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    val angleGuidePaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 3f }
    val pocketFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    val gridLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 5f; pathEffect =
        DashPathEffect(floatArrayOf(15f, 15f), 0f)
    }
    val pathObstructionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    val cvResultPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    val gradientMaskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
    }

    val glowPaints = mutableMapOf<String, Paint>()

    // --- Bank Line Paints ---
    val bankLine1Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    val bankLine2Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    val bankLine3Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    val bankLine4Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }

    fun setTypeface(typeface: Typeface?) {
        textPaint.typeface = typeface
    }

    fun updateColors(uiState: CueDetatState, isDark: Boolean) {
        // Load configs
        val railConfig = Rail()
        val targetBallConfig = TargetBall()
        val ghostCueBallConfig = GhostCueBall()
        val actualCueBallConfig = ActualCueBall()
        val shotGuideLineConfig = ShotGuideLine()
        AimingLine()
        val tangentLineConfig = TangentLine()
        val bankLine1Config = BankLine1()
        val bankLine2Config = BankLine2()
        val bankLine3Config = BankLine3()
        val bankLine4Config = BankLine4()
        val holesConfig = Holes()

        // Apply configs to paints
        tableOutlinePaint.color = railConfig.strokeColor.toArgb()
        tableOutlinePaint.strokeWidth = railConfig.strokeWidth

        targetCirclePaint.color = targetBallConfig.strokeColor.toArgb()
        targetCirclePaint.strokeWidth = targetBallConfig.strokeWidth

        cueCirclePaint.color = ghostCueBallConfig.strokeColor.toArgb()
        cueCirclePaint.strokeWidth = ghostCueBallConfig.strokeWidth

        actualCueBallPaint.color = actualCueBallConfig.strokeColor.toArgb()
        actualCueBallPaint.strokeWidth = actualCueBallConfig.strokeWidth

        shotLinePaint.color = shotGuideLineConfig.strokeColor.toArgb()
        shotLinePaint.strokeWidth = shotGuideLineConfig.strokeWidth

        tangentLineSolidPaint.color = tangentLineConfig.strokeColor.toArgb()
        tangentLineSolidPaint.strokeWidth = tangentLineConfig.strokeWidth

        tangentLineDottedPaint.color = tangentLineConfig.strokeColor.toArgb()
        tangentLineDottedPaint.strokeWidth = tangentLineConfig.strokeWidth
        tangentLineDottedPaint.alpha = (tangentLineConfig.opacity * 255).toInt()

        bankLine1Paint.color = bankLine1Config.strokeColor.toArgb()
        bankLine1Paint.strokeWidth = bankLine1Config.strokeWidth
        bankLine2Paint.color = bankLine2Config.strokeColor.toArgb()
        bankLine2Paint.strokeWidth = bankLine2Config.strokeWidth
        bankLine3Paint.color = bankLine3Config.strokeColor.toArgb()
        bankLine3Paint.strokeWidth = bankLine3Config.strokeWidth
        bankLine4Paint.color = bankLine4Config.strokeColor.toArgb()
        bankLine4Paint.strokeWidth = bankLine4Config.strokeWidth

        pocketFillPaint.color = holesConfig.fillColor.toArgb()

        // Universal paints
        warningPaint.color = WarningRed.toArgb()
        warningPaint.strokeWidth = 6f // Keep warning stroke consistent
        textPaint.color = Color.White.toArgb() // Debug text is always white
        fillPaint.color = Color.White.toArgb()
    }
}