// app/src/main/java/com/hereliesaz/cuedetatlite/data

package com.hereliesaz.cuedetatlite.data

import com.hereliesaz.cuedetatlite.BuildConfig
import javax.inject.Inject

class UpdateChecker @Inject constructor(private val githubRepository: GithubRepository) {
    suspend fun isUpdateAvailable(): Boolean {
        return try {
            val latestVersionString = githubRepository.getLatestVersion()
            // Manual version comparison to avoid string manipulation issues during compilation.
            val latest = latestVersionString.filter { it.isDigit() || it == '.' }.split(".")
                .map { it.toInt() }
            val current = BuildConfig.VERSION_NAME.split(".").map { it.toInt() }

            for (i in 0 until maxOf(latest.size, current.size)) {
                val v1 = latest.getOrElse(i) { 0 }
                val v2 = current.getOrElse(i) { 0 }
                if (v1 > v2) return true
                if (v1 < v2) return false
            }
            false
        } catch (e: Exception) {
            false
        }
    }
}