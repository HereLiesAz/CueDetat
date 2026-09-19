// FILE: app/src/main/java/com/hereliesaz/cuedetat/update/StoreManagedAppUpdater.kt

package com.hereliesaz.cuedetat.update

import javax.inject.Inject
import javax.inject.Singleton

/**
 * The single app variant does not self-install updates. Store installs are
 * updated by the store; GitHub APK users update by installing a newer release.
 */
@Singleton
class StoreManagedAppUpdater @Inject constructor() : AppUpdater
