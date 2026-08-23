package com.hereliesaz.cuedetat.support

/**
 * Where to send someone who wants to chip in.
 *
 * ## Why there is no billing code here
 *
 * The app used to gate Expert mode behind a Play Billing purchase. That gate
 * cost more than it earned:
 *
 *  - entitlement was read from an unsigned, client-persisted DataStore blob with
 *    no server ever contradicting it, so anyone who could write the app's
 *    private storage had Expert for free anyway;
 *  - the one integrity signal that existed was documented in the source as
 *    never being consulted by any gating branch;
 *  - a billing and tester-license debug console shipped to every user in every
 *    build, with no `BuildConfig.DEBUG` check and no entitlement check;
 *  - and it forced the `play` and `foss` flavors apart across four duplicated
 *    interface implementations.
 *
 * Everything is free now. If the app is useful, there is a link. That is the
 * whole mechanism: no purchase flow, no entitlement cache, no restore, no
 * receipt validation, nothing to bypass, and nothing that can wrongly lock a
 * paying user out of a feature at the table.
 */
data class SupportLink(
    val id: String,
    val label: String,
    /** One line, in the app's voice. */
    val blurb: String,
    val url: String,
)

object SupportLinks {

    /**
     * Google Play forbids linking to external payment for *digital goods sold in
     * the app*. Nothing is sold here — every feature is free and unconditional —
     * so these are donations to the developer, not a purchase path. Keep it that
     * way: the moment a link unlocks something, it becomes billing again.
     */
    val ALL: List<SupportLink> = listOf(
        SupportLink(
            id = "kofi",
            label = "Buy me a coffee",
            blurb = "Or a beer. The geometry does not care which.",
            url = "https://ko-fi.com/hereliesaz",
        ),
        SupportLink(
            id = "github",
            label = "Sponsor on GitHub",
            blurb = "For the sort of person who reads a changelog voluntarily.",
            url = "https://github.com/sponsors/HereLiesAz",
        ),
        SupportLink(
            id = "instagram",
            label = "Look at the art instead",
            blurb = "The licence only ever asked for a shoutout. This counts.",
            url = "https://instagram.com/hereliesaz",
        ),
        SupportLink(
            id = "source",
            label = "Read the source",
            blurb = "Free as in speech, and now also as in every feature.",
            url = "https://github.com/HereLiesAz/CueDetat",
        ),
    )

    fun byId(id: String): SupportLink? = ALL.firstOrNull { it.id == id }
}
