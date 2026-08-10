package com.hbm.weapon;

import com.hbm.HbmNuclearTech;
import com.hbm.weapon.data.WeaponDefinitionReloadListener;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import com.hbm.weapon.ballistics.BallisticsService;
import com.hbm.world.damage.HbmGunDamageSource;
import net.neoforged.neoforge.common.damagesource.DamageContainer;

@EventBusSubscriber(modid = HbmNuclearTech.MOD_ID)
public final class WeaponCommonEvents {
    @SubscribeEvent
    public static void addReloadListeners(AddReloadListenerEvent event) {
        event.addListener(WeaponDefinitionReloadListener.INSTANCE);
    }

    @SubscribeEvent
    public static void playerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            HbmWeaponService.tick(player);
        }
    }

    @SubscribeEvent
    public static void levelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level) {
            BallisticsService.tick(level);
        }
    }

    @SubscribeEvent
    public static void changedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            HbmWeaponService.cancelSession(player);
        }
    }

    @SubscribeEvent
    public static void loggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            HbmWeaponService.cancelSession(player);
        }
    }

    @SubscribeEvent
    public static void died(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            HbmWeaponService.cancelSession(player);
        }
    }

    @SubscribeEvent
    public static void applyArmorPenetration(LivingIncomingDamageEvent event) {
        if (event.getSource() instanceof HbmGunDamageSource gunDamage) {
            // A shotgun's pellets and a fast automatic weapon may legitimately hit in the same
            // damage window. Firearm damage therefore owns its cadence instead of vanilla i-frames.
            event.setInvulnerabilityTicks(0);
            if (gunDamage.armorPenetration() > 0.0F) {
                event.addReductionModifier(DamageContainer.Reduction.ARMOR,
                        (container, reduction) -> gunDamage.modifyArmorReduction(reduction));
            }
        }
    }

    @SubscribeEvent
    public static void levelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            BallisticsService.clear(level);
        }
    }

    private WeaponCommonEvents() {
    }
}
