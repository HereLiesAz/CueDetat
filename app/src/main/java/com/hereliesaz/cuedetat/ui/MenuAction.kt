package com.hereliesaz.cuedetat.ui

/**
 * A sealed class to represent all possible user actions originating from UI menus or buttons.
 * This provides a clean, type-safe way to pass events from the UI to the ViewModel.
 */
sealed class MenuAction {
    object Reset : MenuAction()
    object ToggleHelp : MenuAction()
    object ToggleJumpingBall : MenuAction()
    object CheckForUpdate : MenuAction()
    object ToggleTheme : MenuAction()
}
