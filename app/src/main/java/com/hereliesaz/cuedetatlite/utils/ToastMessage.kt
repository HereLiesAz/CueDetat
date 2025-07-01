package com.hereliesaz.cuedetatlite.utils

sealed class ToastMessage {
    data class StringResource(val id: Int, val formatArgs: List<Any> = emptyList()) : ToastMessage()
    data class Text(val text: String) : ToastMessage()
}
