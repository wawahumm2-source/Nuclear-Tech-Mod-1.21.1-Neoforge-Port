package com.hbm.weapon.data

import net.minecraft.resources.ResourceLocation

data class AmmoDefinition(
    val schema: Int,
    val id: ResourceLocation,
    val family: ResourceLocation,
    val projectileMode: ProjectileMode,
    val damageMultiplier: Float,
    val armorPenetration: Float,
    val pelletCount: Int,
    val spreadMultiplier: Double,
    val gravity: Double,
    val drag: Double,
    val explosion: ExplosionProfile?,
    val impactEffect: ResourceLocation,
    val tracerColor: Int
) {
    data class ExplosionProfile(
        val power: Float,
        val blockDamage: Boolean,
        val shapedCharge: Boolean
    )
}
