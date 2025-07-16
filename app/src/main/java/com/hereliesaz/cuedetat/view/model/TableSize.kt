// FILE: app/src/main/java/com/hereliesaz/cuedetat/view/model/TableSize.kt

package com.hereliesaz.cuedetat.view.model

enum class TableSize(val feet: String, val longSideInches: Float, val shortSideInches: Float) {
    SIX_FOOT("6'", 72f, 36f),
    SEVEN_FOOT("7'", 84f, 42f),
    EIGHT_FOOT("8'", 92f, 46f),
    NINE_FOOT("9'", 100f, 50f),
    TEN_FOOT("10'", 112f, 56f);

    fun next(): TableSize {
        val nextOrdinal = (this.ordinal + 1) % entries.size
        return entries[nextOrdinal]
    }
}