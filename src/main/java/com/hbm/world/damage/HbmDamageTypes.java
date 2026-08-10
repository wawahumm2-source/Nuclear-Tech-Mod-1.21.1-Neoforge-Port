package com.hbm.world.damage;

import com.hbm.HbmNuclearTech;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public final class HbmDamageTypes {
    public static final ResourceKey<DamageType> RADIATION = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(HbmNuclearTech.MOD_ID, "radiation")
    );
    public static final ResourceKey<DamageType> NUCLEAR_BLAST = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(HbmNuclearTech.MOD_ID, "nuclear_blast")
    );
    public static final ResourceKey<DamageType> GUNFIRE = key("gunfire");
    public static final ResourceKey<DamageType> HEADSHOT = key("headshot");
    public static final ResourceKey<DamageType> ARMOR_PIERCING = key("armor_piercing");
    public static final ResourceKey<DamageType> PROJECTILE_EXPLOSION = key("projectile_explosion");

    public static DamageSource radiation(Level level) {
        return new DamageSource(holder(level, RADIATION));
    }

    public static DamageSource gunfire(Level level, Entity shooter) {
        return new DamageSource(holder(level, GUNFIRE), shooter);
    }

    public static HbmGunDamageSource gunfire(Level level, Entity shooter, float armorPenetration) {
        return new HbmGunDamageSource(holder(level, GUNFIRE), shooter, armorPenetration);
    }

    public static DamageSource headshot(Level level, Entity shooter) {
        return new DamageSource(holder(level, HEADSHOT), shooter);
    }

    public static HbmGunDamageSource headshot(Level level, Entity shooter, float armorPenetration) {
        return new HbmGunDamageSource(holder(level, HEADSHOT), shooter, armorPenetration);
    }

    public static HbmGunDamageSource projectileGunfire(Level level, Entity projectile,
                                                       @Nullable Entity owner, float armorPenetration) {
        return new HbmGunDamageSource(holder(level, GUNFIRE), projectile, owner, armorPenetration);
    }

    public static DamageSource armorPiercing(Level level, Entity shooter) {
        return new DamageSource(holder(level, ARMOR_PIERCING), shooter);
    }

    public static DamageSource projectileExplosion(Level level, Entity projectile, @Nullable Entity owner) {
        return new DamageSource(holder(level, PROJECTILE_EXPLOSION), projectile, owner);
    }

    private static net.minecraft.core.Holder<DamageType> holder(Level level, ResourceKey<DamageType> key) {
        return level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(key);
    }

    private static ResourceKey<DamageType> key(String path) {
        return ResourceKey.create(Registries.DAMAGE_TYPE,
                ResourceLocation.fromNamespaceAndPath(HbmNuclearTech.MOD_ID, path));
    }

    public static DamageSource nuclearBlast(Level level) {
        return new DamageSource(level.registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(NUCLEAR_BLAST));
    }

    private HbmDamageTypes() {
    }
}
