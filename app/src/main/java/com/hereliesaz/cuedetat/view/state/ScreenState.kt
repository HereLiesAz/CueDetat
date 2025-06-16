package com.hereliesaz.cuedetat.view.state

sealed class ToastMessage {
    data class StringResource(val id: Int, val formatArgs: List<Any> = emptyList()) : ToastMessage()
    data class PlainText(val text: String) : ToastMessage()
}

sealed class SingleEvent {
    data class OpenUrl(val url: String) : SingleEvent()
    object ShowDonationDialog : SingleEvent()
}
