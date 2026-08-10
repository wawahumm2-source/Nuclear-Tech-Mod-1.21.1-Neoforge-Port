package com.hbm.weapon.data

import net.minecraft.resources.ResourceLocation
import kotlin.math.ceil

data class GunDefinition(
    val schema: Int,
    val id: ResourceLocation,
    val ammoFamily: ResourceLocation,
    val supportedAmmo: List<ResourceLocation>,
    val fireModes: List<FireMode>,
    val defaultFireMode: FireMode,
    val roundsPerMinute: Int,
    val burstSize: Int,
    val ads: AdsProfile,
    val movementWeight: Double,
    val spread: SpreadProfile,
    val recoil: RecoilProfile,
    val magazine: MagazineProfile,
    val reload: ReloadProfile,
    val baseDamage: Float,
    val headshotMultiplier: Float,
    val muzzleVelocity: Double,
    val maxRange: Double,
    val behavior: ResourceLocation,
    val sounds: Map<String, ResourceLocation>,
    val animations: Map<String, String>
) {
    val shotIntervalTicks: Double
        get() = 1200.0 / roundsPerMinute.toDouble()

    val minimumWholeTickInterval: Int
        get() = ceil(shotIntervalTicks).toInt().coerceAtLeast(1)

    data class AdsProfile(
        val fovMultiplier: Double,
        val movementMultiplier: Double,
        val sensitivityMultiplier: Double
    )

    data class SpreadProfile(
        val hipDegrees: Double,
        val adsDegrees: Double,
        val movementDegrees: Double
    )

    data class RecoilProfile(
        val pitch: Double,
        val yaw: Double,
        val recoveryPerTick: Double
    )

    data class MagazineProfile(
        val capacity: Int,
        val usesChamber: Boolean
    )

    data class ReloadProfile(
        val style: ReloadStyle,
        val startTicks: Int,
        val transferTicks: Int,
        val loopTicks: Int,
        val endTicks: Int,
        val emptyEndTicks: Int
    )
}
