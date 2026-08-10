package com.hbm.weapon.data

enum class ReloadStyle {
    MAGAZINE,
    PER_ROUND;

    companion object {
        @JvmStatic
        fun parse(value: String): ReloadStyle = when (value.lowercase()) {
            "magazine" -> MAGAZINE
            "per_round" -> PER_ROUND
            else -> throw IllegalArgumentException("Unsupported reload style '$value'")
        }
    }
}
