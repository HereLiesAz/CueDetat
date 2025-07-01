// hereliesaz/cuedetat/CueDetat-CueDetatLite/app/src/main/java/com/hereliesaz/cuedetatlite/ui/MenuAction.kt
package com.hereliesaz.cuedetatlite.ui

sealed class MenuAction {
    object ToggleHelp : MenuAction()
    object StartTutorial : MenuAction()
    object Reset : MenuAction()
    object ToggleActualCueBall : MenuAction()
    object ToggleBankingMode : MenuAction()
    object ToggleForceTheme : MenuAction()
    object ToggleLuminanceDialog : MenuAction()
    object ViewArt : MenuAction()
    object ShowDonationOptions : MenuAction()
    object CheckForUpdate : MenuAction()
}
