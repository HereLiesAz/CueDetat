package com.hereliesaz.cuedetat.domain

/**
 * Derivative-free local search (compass / pattern search) for a handful of parameters.
 *
 * Tries each parameter a step up and down, keeps any move that lowers the cost, and halves the
 * steps when no move helps. Suits the table snap: a few parameters (offset, rotation, zoom), a
 * cost that is cheap but not smooth (pixel overlap), and a good starting point (where the user
 * put the table). Pure Kotlin; runs in plain JUnit.
 */
object PoseSearch {

    data class Result(val params: DoubleArray, val cost: Double, val evaluations: Int)

    /**
     * @param start starting parameters
     * @param steps initial step per parameter
     * @param minSteps stop when every step has shrunk below these
     * @param maxEvaluations hard budget on cost calls
     */
    fun minimize(
        start: DoubleArray,
        steps: DoubleArray,
        minSteps: DoubleArray,
        maxEvaluations: Int,
        cost: (DoubleArray) -> Double,
    ): Result {
        require(start.size == steps.size && steps.size == minSteps.size)
        val x = start.copyOf()
        val step = steps.copyOf()
        var best = cost(x)
        var evals = 1
        while (evals < maxEvaluations && step.indices.any { step[it] >= minSteps[it] }) {
            var improved = false
            for (i in x.indices) {
                if (step[i] < minSteps[i]) continue
                for (dir in doubleArrayOf(1.0, -1.0)) {
                    if (evals >= maxEvaluations) break
                    val trial = x.copyOf().also { it[i] += dir * step[i] }
                    val c = cost(trial); evals++
                    if (c < best) {
                        best = c; x[i] = trial[i]; improved = true
                        break
                    }
                }
            }
            if (!improved) for (i in step.indices) step[i] /= 2.0
        }
        return Result(x, best, evals)
    }
}
