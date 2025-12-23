package com.hereliesaz.cuedetat.view.state

sealed class SingleEvent {
    data class OpenUrl(val url: String) : SingleEvent()
    data class SendFeedbackEmail(val email: String, val subject: String) : SingleEvent()
    object InitiateHaterMode : SingleEvent()
}
