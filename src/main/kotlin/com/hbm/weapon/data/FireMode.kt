package com.hbm.weapon.data

enum class FireMode {
    SEMI,
    BURST,
    AUTO;

    companion object {
        @JvmStatic
        fun parse(value: String): FireMode = when (value.lowercase()) {
            "semi" -> SEMI
            "burst" -> BURST
            "auto" -> AUTO
            else -> throw IllegalArgumentException("Unsupported fire mode '$value'")
        }
    }
}
