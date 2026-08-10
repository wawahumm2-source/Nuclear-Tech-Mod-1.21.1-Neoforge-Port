package com.hbm.weapon.data

import net.minecraft.resources.ResourceLocation
import java.util.Collections
import java.util.concurrent.atomic.AtomicReference

object GunDefinitionRegistry {
    data class Snapshot(
        val guns: Map<ResourceLocation, GunDefinition>,
        val ammo: Map<ResourceLocation, AmmoDefinition>,
        val generation: Long
    )

    private val current = AtomicReference(Snapshot(emptyMap(), emptyMap(), 0L))

    @JvmStatic
    fun snapshot(): Snapshot = current.get()

    @JvmStatic
    fun gun(id: ResourceLocation): GunDefinition? = current.get().guns[id]

    @JvmStatic
    fun ammo(id: ResourceLocation): AmmoDefinition? = current.get().ammo[id]

    @JvmStatic
    fun requireGun(id: ResourceLocation): GunDefinition =
        gun(id) ?: throw IllegalStateException("No active gun definition for $id")

    @JvmStatic
    fun requireAmmo(id: ResourceLocation): AmmoDefinition =
        ammo(id) ?: throw IllegalStateException("No active ammo definition for $id")

    /**
     * Validates the complete candidate before the reference is replaced. Any exception leaves the
     * previous generation untouched, which makes failed datapack reloads non-destructive.
     */
    @JvmStatic
    fun install(guns: Map<ResourceLocation, GunDefinition>, ammo: Map<ResourceLocation, AmmoDefinition>): Snapshot {
        require(guns.isNotEmpty()) { "No gun definitions were found under data/*/guns" }
        require(ammo.isNotEmpty()) { "No ammo definitions were found under data/*/ammo" }

        guns.forEach { (id, gun) ->
            require(id == gun.id) { "Gun map key $id does not match definition id ${gun.id}" }
            require(gun.behavior.namespace == "hbm") { "Gun $id uses unsupported behavior namespace ${gun.behavior}" }
            require(gun.behavior.path in setOf("standard", "grenade_launcher")) {
                "Gun $id uses unsupported behavior key ${gun.behavior}"
            }
            gun.supportedAmmo.forEach { ammoId ->
                val ammoDefinition = ammo[ammoId]
                    ?: throw IllegalArgumentException("Gun $id references missing ammo $ammoId")
                require(ammoDefinition.family == gun.ammoFamily) {
                    "Gun $id expects family ${gun.ammoFamily}, but $ammoId belongs to ${ammoDefinition.family}"
                }
            }
        }
        ammo.forEach { (id, definition) -> require(id == definition.id) {
            "Ammo map key $id does not match definition id ${definition.id}"
        } }

        val previous = current.get()
        val next = Snapshot(
            Collections.unmodifiableMap(LinkedHashMap(guns)),
            Collections.unmodifiableMap(LinkedHashMap(ammo)),
            previous.generation + 1L
        )
        current.set(next)
        return next
    }
}
