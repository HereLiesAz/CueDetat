// FILE: app/src/play/java/com/hereliesaz/cuedetat/update/PlayAppUpdater.kt

package com.hereliesaz.cuedetat.update

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Play-flavor [AppUpdater]: a no-op. Google Play manages updates for store
 * installs, and a self-updating APK would violate Play policy. All defaults
 * apply (not supported, no update, no install).
 */
@Singleton
class PlayAppUpdater @Inject constructor() : AppUpdater
