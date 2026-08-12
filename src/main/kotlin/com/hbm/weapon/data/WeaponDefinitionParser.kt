package com.hbm.weapon.data

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import net.minecraft.resources.ResourceLocation

object WeaponDefinitionParser {
    private const val CURRENT_SCHEMA = 1

    private val gunKeys = setOf(
        "schema", "ammo_family", "supported_ammo", "fire_modes", "default_fire_mode",
        "rpm", "burst_size", "ads", "movement_weight", "spread", "recoil", "magazine",
        "reload", "damage", "headshot_multiplier", "velocity", "range", "behavior",
        "sounds", "animations"
    )
    private val ammoKeys = setOf(
        "schema", "family", "projectile_mode", "damage_multiplier", "armor_penetration",
        "pellet_count", "spread_multiplier", "gravity", "drag", "explosion", "impact_effect", "tracer_color"
    )
    private val animationStates = setOf(
        "equip", "idle", "ads", "fire", "dry_fire", "reload_start", "reload_loop",
        "reload_end", "inspect", "sprint", "lower"
    )

    @JvmStatic
    fun parseGun(id: ResourceLocation, json: JsonObject): GunDefinition = scoped("gun $id") {
        requireOnly(json, gunKeys)
        requireSchema(json)

        val modes = stringList(json.requiredArray("fire_modes"), "fire_modes").map(FireMode::parse)
        val defaultMode = FireMode.parse(json.requiredString("default_fire_mode"))
        require(defaultMode in modes) { "default_fire_mode must appear in fire_modes" }

        val ads = json.requiredObject("ads")
        requireOnly(ads, setOf(
            "fov_multiplier", "movement_multiplier", "sensitivity_multiplier",
            "zero_pitch_degrees", "zero_distance"
        ))
        val spread = json.requiredObject("spread")
        requireOnly(spread, setOf("hip_degrees", "ads_degrees", "movement_degrees"))
        val recoil = json.requiredObject("recoil")
        requireOnly(recoil, setOf("pitch", "yaw", "recovery_per_tick"))
        val magazine = json.requiredObject("magazine")
        requireOnly(magazine, setOf("capacity", "uses_chamber"))
        val reload = json.requiredObject("reload")
        requireOnly(reload, setOf(
            "style", "start_ticks", "transfer_ticks", "loop_ticks", "end_ticks", "empty_end_ticks"
        ))

        val definition = GunDefinition(
            schema = CURRENT_SCHEMA,
            id = id,
            ammoFamily = location(json.requiredString("ammo_family"), "ammo_family"),
            supportedAmmo = stringList(json.requiredArray("supported_ammo"), "supported_ammo")
                .map { location(it, "supported_ammo") },
            fireModes = modes,
            defaultFireMode = defaultMode,
            roundsPerMinute = json.requiredInt("rpm"),
            burstSize = json.optionalInt("burst_size", 3),
            ads = GunDefinition.AdsProfile(
                ads.requiredDouble("fov_multiplier"),
                ads.requiredDouble("movement_multiplier"),
                ads.requiredDouble("sensitivity_multiplier"),
                ads.optionalDouble("zero_pitch_degrees", 0.0),
                ads.optionalDouble("zero_distance", 0.0)
            ),
            movementWeight = json.requiredDouble("movement_weight"),
            spread = GunDefinition.SpreadProfile(
                spread.requiredDouble("hip_degrees"),
                spread.requiredDouble("ads_degrees"),
                spread.requiredDouble("movement_degrees")
            ),
            recoil = GunDefinition.RecoilProfile(
                recoil.requiredDouble("pitch"),
                recoil.requiredDouble("yaw"),
                recoil.requiredDouble("recovery_per_tick")
            ),
            magazine = GunDefinition.MagazineProfile(
                magazine.requiredInt("capacity"),
                magazine.requiredBoolean("uses_chamber")
            ),
            reload = GunDefinition.ReloadProfile(
                ReloadStyle.parse(reload.requiredString("style")),
                reload.requiredInt("start_ticks"),
                reload.requiredInt("transfer_ticks"),
                reload.requiredInt("loop_ticks"),
                reload.requiredInt("end_ticks"),
                reload.optionalInt("empty_end_ticks", reload.requiredInt("end_ticks"))
            ),
            baseDamage = json.requiredFloat("damage"),
            headshotMultiplier = json.requiredFloat("headshot_multiplier"),
            muzzleVelocity = json.requiredDouble("velocity"),
            maxRange = json.requiredDouble("range"),
            behavior = location(json.requiredString("behavior"), "behavior"),
            sounds = locationMap(json.requiredObject("sounds"), "sounds"),
            animations = stringMap(json.requiredObject("animations"), "animations")
        )
        validate(definition)
        definition
    }

    @JvmStatic
    fun parseAmmo(id: ResourceLocation, json: JsonObject): AmmoDefinition = scoped("ammo $id") {
        requireOnly(json, ammoKeys)
        requireSchema(json)
        val explosion = json.get("explosion")?.takeUnless(JsonElement::isJsonNull)?.asJsonObject?.let {
            requireOnly(it, setOf("power", "block_damage", "shaped_charge"))
            AmmoDefinition.ExplosionProfile(
                it.requiredFloat("power"),
                it.requiredBoolean("block_damage"),
                it.requiredBoolean("shaped_charge")
            )
        }

        val definition = AmmoDefinition(
            schema = CURRENT_SCHEMA,
            id = id,
            family = location(json.requiredString("family"), "family"),
            projectileMode = ProjectileMode.parse(json.requiredString("projectile_mode")),
            damageMultiplier = json.requiredFloat("damage_multiplier"),
            armorPenetration = json.requiredFloat("armor_penetration"),
            pelletCount = json.requiredInt("pellet_count"),
            spreadMultiplier = json.requiredDouble("spread_multiplier"),
            gravity = json.requiredDouble("gravity"),
            drag = json.requiredDouble("drag"),
            explosion = explosion,
            impactEffect = location(json.requiredString("impact_effect"), "impact_effect"),
            tracerColor = parseColor(json.requiredString("tracer_color"))
        )
        validate(definition)
        definition
    }

    private fun validate(definition: GunDefinition) {
        require(definition.roundsPerMinute in 30..1800) { "rpm must be between 30 and 1800" }
        require(definition.burstSize in 2..10) { "burst_size must be between 2 and 10" }
        require(definition.magazine.capacity in 1..500) { "magazine capacity must be between 1 and 500" }
        require(definition.supportedAmmo.isNotEmpty()) { "supported_ammo must not be empty" }
        require(definition.supportedAmmo.distinct().size == definition.supportedAmmo.size) { "supported_ammo contains duplicates" }
        require(definition.baseDamage > 0.0F) { "damage must be positive" }
        require(definition.headshotMultiplier >= 1.0F) { "headshot_multiplier must be at least 1" }
        require(definition.muzzleVelocity in 0.05..20.0) { "velocity must be between 0.05 and 20 blocks/tick" }
        require(definition.maxRange in 1.0..1024.0) { "range must be between 1 and 1024 blocks" }
        require(definition.movementWeight in 0.0..1.0) { "movement_weight must be between 0 and 1" }
        require(definition.ads.fovMultiplier in 0.05..1.0) { "ads.fov_multiplier must be between 0.05 and 1" }
        require(definition.ads.movementMultiplier in 0.05..1.0) { "ads.movement_multiplier must be between 0.05 and 1" }
        require(definition.ads.sensitivityMultiplier in 0.05..1.0) { "ads.sensitivity_multiplier must be between 0.05 and 1" }
        require(definition.ads.zeroPitchDegrees in -10.0..10.0) { "ads.zero_pitch_degrees must be between -10 and 10" }
        require(definition.ads.zeroDistance in 0.0..definition.maxRange) {
            "ads.zero_distance must be zero or no greater than range"
        }
        require(definition.spread.hipDegrees >= 0.0 && definition.spread.adsDegrees >= 0.0) { "spread cannot be negative" }
        require(definition.recoil.pitch >= 0.0 && definition.recoil.yaw >= 0.0) { "recoil cannot be negative" }
        require(definition.reload.startTicks >= 0 && definition.reload.transferTicks >= 0 &&
            definition.reload.loopTicks >= 0 && definition.reload.endTicks >= 0 &&
            definition.reload.emptyEndTicks >= 0) { "reload timings cannot be negative" }
        require(definition.reload.style != ReloadStyle.PER_ROUND || definition.reload.loopTicks > 0) {
            "per_round reloads require a positive loop_ticks value"
        }
        require(definition.sounds.keys.containsAll(setOf("fire", "dry_fire", "reload"))) {
            "sounds must define fire, dry_fire, and reload"
        }
        val missingAnimations = animationStates - definition.animations.keys
        require(missingAnimations.isEmpty()) { "animations is missing required states: ${missingAnimations.sorted().joinToString()}" }
    }

    private fun validate(definition: AmmoDefinition) {
        require(definition.damageMultiplier > 0.0F) { "damage_multiplier must be positive" }
        require(definition.armorPenetration in 0.0F..1.0F) { "armor_penetration must be between 0 and 1" }
        require(definition.pelletCount in 1..64) { "pellet_count must be between 1 and 64" }
        require(definition.spreadMultiplier in 0.0..4.0) { "spread_multiplier must be between 0 and 4" }
        require(definition.gravity in 0.0..1.0) { "gravity must be between 0 and 1" }
        require(definition.drag in 0.0..0.5) { "drag must be between 0 and 0.5" }
        require(definition.projectileMode != ProjectileMode.ENTITY || definition.explosion != null) {
            "entity projectiles require an explosion profile"
        }
        definition.explosion?.let { require(it.power > 0.0F) { "explosion.power must be positive" } }
    }

    private fun requireSchema(json: JsonObject) {
        val schema = json.requiredInt("schema")
        require(schema == CURRENT_SCHEMA) { "unsupported schema $schema; expected $CURRENT_SCHEMA" }
    }

    private fun requireOnly(json: JsonObject, allowed: Set<String>) {
        val unknown = json.keySet() - allowed
        require(unknown.isEmpty()) { "unknown fields: ${unknown.sorted().joinToString()}" }
    }

    private fun locationMap(json: JsonObject, field: String): Map<String, ResourceLocation> =
        json.entrySet().associate { (key, value) -> key to location(value.asString, "$field.$key") }

    private fun stringMap(json: JsonObject, field: String): Map<String, String> =
        json.entrySet().associate { (key, value) ->
            require(value.isJsonPrimitive && value.asJsonPrimitive.isString) { "$field.$key must be a string" }
            key to value.asString
        }

    private fun stringList(json: JsonArray, field: String): List<String> = json.mapIndexed { index, value ->
        require(value.isJsonPrimitive && value.asJsonPrimitive.isString) { "$field[$index] must be a string" }
        value.asString
    }

    private fun location(value: String, field: String): ResourceLocation =
        ResourceLocation.tryParse(value) ?: throw IllegalArgumentException("$field contains invalid resource location '$value'")

    private fun parseColor(value: String): Int {
        val normalized = value.removePrefix("#")
        require(normalized.length == 6) { "tracer_color must be a six-digit RGB hex value" }
        return normalized.toIntOrNull(16) ?: throw IllegalArgumentException("tracer_color '$value' is not hexadecimal")
    }

    private inline fun <T> scoped(scope: String, block: () -> T): T = try {
        block()
    } catch (exception: IllegalArgumentException) {
        throw IllegalArgumentException("Invalid $scope: ${exception.message}", exception)
    } catch (exception: RuntimeException) {
        throw IllegalArgumentException("Invalid $scope: malformed or missing field (${exception.message})", exception)
    }

    private fun JsonObject.requiredObject(name: String): JsonObject =
        get(name)?.takeIf(JsonElement::isJsonObject)?.asJsonObject
            ?: throw IllegalArgumentException("$name must be an object")

    private fun JsonObject.requiredArray(name: String): JsonArray =
        get(name)?.takeIf(JsonElement::isJsonArray)?.asJsonArray
            ?: throw IllegalArgumentException("$name must be an array")

    private fun JsonObject.requiredString(name: String): String =
        get(name)?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }?.asString
            ?: throw IllegalArgumentException("$name must be a string")

    private fun JsonObject.requiredInt(name: String): Int {
        val primitive = get(name)?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isNumber }?.asJsonPrimitive
            ?: throw IllegalArgumentException("$name must be an integer")
        return try {
            primitive.asBigDecimal.intValueExact()
        } catch (_: ArithmeticException) {
            throw IllegalArgumentException("$name must be an integer")
        } catch (_: NumberFormatException) {
            throw IllegalArgumentException("$name must be an integer")
        }
    }

    private fun JsonObject.optionalInt(name: String, fallback: Int): Int =
        if (has(name)) requiredInt(name) else fallback

    private fun JsonObject.requiredFloat(name: String): Float {
        val value = get(name)?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isNumber }?.asFloat
            ?: throw IllegalArgumentException("$name must be a number")
        require(value.isFinite()) { "$name must be finite" }
        return value
    }

    private fun JsonObject.requiredDouble(name: String): Double {
        val value = get(name)?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isNumber }?.asDouble
            ?: throw IllegalArgumentException("$name must be a number")
        require(value.isFinite()) { "$name must be finite" }
        return value
    }

    private fun JsonObject.optionalDouble(name: String, fallback: Double): Double =
        if (has(name)) requiredDouble(name) else fallback

    private fun JsonObject.requiredBoolean(name: String): Boolean =
        get(name)?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isBoolean }?.asBoolean
            ?: throw IllegalArgumentException("$name must be a boolean")
}
