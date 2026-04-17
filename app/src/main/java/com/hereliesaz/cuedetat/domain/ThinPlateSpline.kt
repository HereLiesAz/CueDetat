// app/src/main/java/com/hereliesaz/cuedetat/domain/ThinPlateSpline.kt
package com.hereliesaz.cuedetat.domain

import android.graphics.PointF
import kotlin.math.ln

/**
 * Thin-Plate Spline solver and evaluator.
 *
 * For N control points, solves two independent (N+3)×(N+3) linear systems
 * (one per output axis x, y) using Gaussian elimination with partial pivoting.
 *
 * Weights are cached at runtime in a WeakHashMap keyed by TpsWarpData instance.
 * Both forward (src→dst) and inverse (dst→src) directions are available
 * from a single stored TpsWarpData.
 */
object ThinPlateSpline {

    internal data class SolvedTps(
        val srcPoints: List<PointF>,
        val weightsX: DoubleArray,
        val weightsY: DoubleArray
    )

    private val forwardCache = java.util.WeakHashMap<TpsWarpData, SolvedTps>()
    private val inverseCache = java.util.WeakHashMap<TpsWarpData, SolvedTps>()
    private val lock = Any()

    /**
     * Maps src→dst (forward direction).
     * CV use: homography-estimated logical → true logical.
     * Image-space drag use: ideal image position → warped image position.
     */
    fun applyWarp(tps: TpsWarpData, point: PointF): PointF {
        val solved = synchronized(lock) {
            forwardCache.getOrPut(tps) { solve(tps.srcPoints, tps.dstPoints) }
        }
        return evaluate(solved, point)
    }

    /**
     * Maps dst→src (inverse direction, solved by swapping src/dst).
     * Rendering use: true logical point → homography-estimated logical (for drawing inside pitchMatrix).
     */
    fun applyInverseWarp(tps: TpsWarpData, point: PointF): PointF {
        val solved = synchronized(lock) {
            inverseCache.getOrPut(tps) { solve(tps.dstPoints, tps.srcPoints) }
        }
        return evaluate(solved, point)
    }

    internal fun solve(srcPoints: List<PointF>, dstPoints: List<PointF>): SolvedTps {
        val n = srcPoints.size
        val m = n + 3
        val A = Array(m) { DoubleArray(m) }
        val bX = DoubleArray(m)
        val bY = DoubleArray(m)

        // Kernel block: U(r²) where U(r²) = r² * ln(r²)
        for (i in 0 until n) {
            for (j in 0 until n) {
                val dx = (srcPoints[i].x - srcPoints[j].x).toDouble()
                val dy = (srcPoints[i].y - srcPoints[j].y).toDouble()
                A[i][j] = tpsKernel(dx * dx + dy * dy)
            }
        }

        // Affine block P and P^T
        for (i in 0 until n) {
            A[i][n] = 1.0; A[i][n + 1] = srcPoints[i].x.toDouble(); A[i][n + 2] = srcPoints[i].y.toDouble()
            A[n][i] = 1.0; A[n + 1][i] = srcPoints[i].x.toDouble(); A[n + 2][i] = srcPoints[i].y.toDouble()
            bX[i] = dstPoints[i].x.toDouble()
            bY[i] = dstPoints[i].y.toDouble()
        }
        // Bottom-right 3×3 is zero (already initialized)

        val (wX, wY) = solveLinearSystem(A, bX, bY)
        return SolvedTps(srcPoints, wX, wY)
    }

    private fun evaluate(solved: SolvedTps, point: PointF): PointF {
        val n = solved.srcPoints.size
        // Affine terms: weights[n], weights[n+1], weights[n+2]
        var x = solved.weightsX[n] + solved.weightsX[n + 1] * point.x + solved.weightsX[n + 2] * point.y
        var y = solved.weightsY[n] + solved.weightsY[n + 1] * point.x + solved.weightsY[n + 2] * point.y
        for (i in 0 until n) {
            val dx = (point.x - solved.srcPoints[i].x).toDouble()
            val dy = (point.y - solved.srcPoints[i].y).toDouble()
            val u = tpsKernel(dx * dx + dy * dy)
            x += solved.weightsX[i] * u
            y += solved.weightsY[i] * u
        }
        return PointF(x.toFloat(), y.toFloat())
    }

    private fun tpsKernel(r2: Double): Double =
        if (r2 < 1e-10) 0.0 else r2 * ln(r2)

    /**
     * Solves [A | bX | bY] simultaneously via Gaussian elimination with partial pivoting.
     * Returns (weightsX, weightsY) each of length n+3.
     */
    internal fun solveLinearSystem(
        A: Array<DoubleArray>,
        bX: DoubleArray,
        bY: DoubleArray
    ): Pair<DoubleArray, DoubleArray> {
        val n = A.size
        // Augmented matrix [A | bX | bY]
        val aug = Array(n) { i ->
            DoubleArray(n + 2).also { row ->
                A[i].copyInto(row)
                row[n] = bX[i]
                row[n + 1] = bY[i]
            }
        }

        // Forward elimination
        for (col in 0 until n) {
            var maxRow = col
            for (row in col + 1 until n) {
                if (Math.abs(aug[row][col]) > Math.abs(aug[maxRow][col])) maxRow = row
            }
            val tmp = aug[col]; aug[col] = aug[maxRow]; aug[maxRow] = tmp

            val pivot = aug[col][col]
            if (Math.abs(pivot) < 1e-12) continue

            for (row in col + 1 until n) {
                val factor = aug[row][col] / pivot
                for (k in col until n + 2) aug[row][k] -= factor * aug[col][k]
            }
        }

        // Back substitution
        val resultX = DoubleArray(n)
        val resultY = DoubleArray(n)
        for (i in n - 1 downTo 0) {
            var sumX = aug[i][n]
            var sumY = aug[i][n + 1]
            for (j in i + 1 until n) {
                sumX -= aug[i][j] * resultX[j]
                sumY -= aug[i][j] * resultY[j]
            }
            resultX[i] = sumX / aug[i][i]
            resultY[i] = sumY / aug[i][i]
        }

        return Pair(resultX, resultY)
    }
}
