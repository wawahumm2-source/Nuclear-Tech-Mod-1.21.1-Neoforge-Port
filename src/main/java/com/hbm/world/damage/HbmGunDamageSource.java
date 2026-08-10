package com.hbm.world.damage;

import net.minecraft.core.Holder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

/** One firearm damage sequence carrying server-calculated fractional armor penetration. */
public final class HbmGunDamageSource extends DamageSource {
    private final float armorPenetration;

    public HbmGunDamageSource(Holder<DamageType> type, Entity shooter, float armorPenetration) {
        super(type, shooter);
        this.armorPenetration = clampPenetration(armorPenetration);
    }

    public HbmGunDamageSource(Holder<DamageType> type, Entity projectile,
                              @Nullable Entity owner, float armorPenetration) {
        super(type, projectile, owner);
        this.armorPenetration = clampPenetration(armorPenetration);
    }

    public float armorPenetration() {
        return armorPenetration;
    }

    public float modifyArmorReduction(float reduction) {
        return applyPenetration(reduction, armorPenetration);
    }

    /** Pure armor-reduction calculation shared by runtime handling and unit tests. */
    public static float applyPenetration(float reduction, float penetration) {
        return Math.max(0.0F, reduction) * (1.0F - clampPenetration(penetration));
    }

    private static float clampPenetration(float value) {
        if (!Float.isFinite(value)) {
            throw new IllegalArgumentException("Armor penetration must be finite");
        }
        return Math.max(0.0F, Math.min(1.0F, value));
    }
}
