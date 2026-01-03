// FILE: app/src/main/java/com/hereliesaz/cuedetat/view/state/ScreenState.kt

package com.hereliesaz.cuedetat.view.state

sealed class ToastMessage {
    data class StringResource(val id: Int, val formatArgs: List<Any> = emptyList()) : ToastMessage()
    data class PlainText(val text: String) : ToastMessage()
}

sealed class SingleEvent {
    data object InitiateHaterMode : SingleEvent()
    data class OpenUrl(val url: String) : SingleEvent()
    data class SendFeedbackEmail(val email: String, val subject: String) : SingleEvent()
}