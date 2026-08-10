package com.hbm.weapon.data

enum class ProjectileMode {
    TRAJECTORY,
    ENTITY;

    companion object {
        @JvmStatic
        fun parse(value: String): ProjectileMode = when (value.lowercase()) {
            "trajectory" -> TRAJECTORY
            "entity" -> ENTITY
            else -> throw IllegalArgumentException("Unsupported projectile mode '$value'")
        }
    }
}
