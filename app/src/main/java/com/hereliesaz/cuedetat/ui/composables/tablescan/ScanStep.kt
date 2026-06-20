package com.hereliesaz.cuedetat.ui.composables.tablescan

/**
 * Phases of the table-scan flow.
 *
 * Lives in the base (not the on-demand `:feature_expert_ar` module) because
 * [com.hereliesaz.cuedetat.data.TableScanRepository] persists/restores it, and
 * the base must not depend on the AR module. The module's TableScanViewModel
 * imports this type.
 */
enum class ScanStep {
    FELT_CAPTURE,
    CORNER_QUAD,
    POCKET_GUIDE,
    AUTO_READY
}
