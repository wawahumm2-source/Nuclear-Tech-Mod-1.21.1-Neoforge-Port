package com.hbm.world.radiation;

import com.hbm.world.damage.HbmDamageTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

/** Server-only source-shaped radiation sickness and fatal thresholds for players and mobs. */
public final class RadiationEffects {
    public static void apply(LivingEntity entity, RadiationPlayerState state, int elapsedTicks) {
        RadiationEffectBand band = RadiationEffectBand.fromRadiation(state.getRadiation());
        if (band == RadiationEffectBand.NONE || isCreativePlayer(entity)) {
            return;
        }

        if (band == RadiationEffectBand.FATAL) {
            entity.hurt(HbmDamageTypes.radiation(entity.level()), 1000F);
            if (!(entity instanceof ServerPlayer)) {
                state.setRadiation(0D);
            }
            return;
        }

        switch (band) {
            case SICK -> applySickEffects(entity, elapsedTicks);
            case MODERATE -> applyModerateEffects(entity, elapsedTicks);
            case SEVERE -> applySevereEffects(entity, elapsedTicks, false);
            case CRITICAL -> applySevereEffects(entity, elapsedTicks, true);
            default -> {
            }
        }
    }

    private static void applySickEffects(LivingEntity entity, int elapsedTicks) {
        if (roll(entity, 300, elapsedTicks)) {
            entity.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0));
        }
        if (roll(entity, 300, elapsedTicks)) {
            entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0));
        }
    }

    private static void applyModerateEffects(LivingEntity entity, int elapsedTicks) {
        if (roll(entity, 300, elapsedTicks)) {
            entity.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 150, 0));
        }
        if (roll(entity, 500, elapsedTicks)) {
            entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 0));
        }
        if (roll(entity, 300, elapsedTicks)) {
            entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1));
        }
    }

    private static void applySevereEffects(LivingEntity entity, int elapsedTicks, boolean critical) {
        if (roll(entity, 300, elapsedTicks)) {
            entity.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 150, 0));
        }
        if (roll(entity, 300, elapsedTicks)) {
            entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 2));
        }
        if (roll(entity, 300, elapsedTicks)) {
            entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 2));
        }
        if (roll(entity, 500, elapsedTicks)) {
            entity.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 2));
        }
        if (critical && roll(entity, 700, elapsedTicks)) {
            entity.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 1));
        }
    }

    private static boolean roll(LivingEntity entity, int oneInChance, int elapsedTicks) {
        return entity.getRandom().nextDouble() < RadiationMath.chanceAcrossTicks(oneInChance, elapsedTicks);
    }

    private static boolean isCreativePlayer(LivingEntity entity) {
        return entity instanceof ServerPlayer player && player.getAbilities().instabuild;
    }

    private RadiationEffects() {
    }
}
