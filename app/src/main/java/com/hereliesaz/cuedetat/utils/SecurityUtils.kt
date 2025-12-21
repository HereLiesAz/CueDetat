package com.hereliesaz.cuedetat.utils

import java.util.regex.Pattern

object SecurityUtils {
    private val WEB_URL_PATTERN = Pattern.compile(
        "^(https://).*", Pattern.CASE_INSENSITIVE
    )

    fun isSafeUrl(url: String): Boolean {
        return WEB_URL_PATTERN.matcher(url).matches()
    }
}
