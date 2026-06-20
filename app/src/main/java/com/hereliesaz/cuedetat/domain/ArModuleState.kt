package com.hereliesaz.cuedetat.domain

/**
 * Lifecycle of the on-demand `:feature_expert_ar` dynamic feature module from the
 * UI's point of view. Distinct from [DepthCapability] (which describes the
 * device's ARCore support, known only *after* the module loads): this tracks the
 * download/install of the module itself so the UI can show progress and offer a
 * retry.
 *
 *  - [IDLE]    — not requested yet (free users, or expert users who haven't
 *                entered AR). The module is never fetched in this state.
 *  - [LOADING] — the split is downloading / installing, or the impl is being
 *                loaded. Show a "preparing" overlay.
 *  - [READY]   — the module is installed and its implementation is loaded.
 *  - [FAILED]  — the install or load failed; the UI offers a retry.
 */
enum class ArModuleState {
    IDLE,
    LOADING,
    READY,
    FAILED,
}
