package com.hbm.weapon;

import com.hbm.HbmNuclearTech;
import com.hbm.item.HbmGunItem;
import com.hbm.network.WeaponCommand;
import com.hbm.network.WeaponCommandPayload;
import com.hbm.network.WeaponEffectPayload;
import com.hbm.network.WeaponEffectType;
import com.hbm.network.WeaponInput;
import com.hbm.network.WeaponInputPayload;
import com.hbm.network.WeaponStatePayload;
import com.hbm.registry.HbmAttachments;
import com.hbm.registry.HbmDataComponents;
import com.hbm.weapon.ammo.AmmoSource;
import com.hbm.weapon.ammo.AmmoSources;
import com.hbm.weapon.behavior.GunBehaviorRegistry;
import com.hbm.weapon.data.AmmoDefinition;
import com.hbm.weapon.data.FireMode;
import com.hbm.weapon.data.GunDefinition;
import com.hbm.weapon.data.GunDefinitionRegistry;
import com.hbm.weapon.data.ReloadStyle;
import com.hbm.weapon.state.GunState;
import com.hbm.weapon.state.MagazineTransaction;
import com.hbm.weapon.state.ReloadPhase;
import com.hbm.weapon.state.WeaponSession;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import software.bernie.geckolib.GeckoLibConstants;
import software.bernie.geckolib.animatable.GeoItem;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Server authority for weapon input, timing, ammunition, and fire direction. */
public final class HbmWeaponService {
    private static final ResourceLocation WEAPON_WEIGHT_MODIFIER =
            ResourceLocation.fromNamespaceAndPath(HbmNuclearTech.MOD_ID, "weapon_weight");

    public static void acceptInput(ServerPlayer player, WeaponInputPayload payload) {
        WeaponSession session = player.getData(HbmAttachments.WEAPON_SESSION);
        if (!session.allowPacket(player.serverLevel().getGameTime())) {
            return;
        }
        if (payload.sequence() < 0 || !session.acknowledge(payload.sequence())) {
            return;
        }
        ensureUniqueGunStackIdentities(player);
        Optional<HeldGun> resolved = heldGun(player);
        if (resolved.isEmpty()) {
            session.reset();
            return;
        }

        HeldGun held = resolved.get();
        session.bind(held.state().stackIdentity());
        if (payload.input() == WeaponInput.ADS) {
            session.setAdsHeld(payload.pressed() && !player.isSprinting());
        } else if (payload.input() == WeaponInput.FIRE) {
            session.setTriggerHeld(payload.pressed());
            if (payload.pressed()) {
                player.setSprinting(false);
            }
        }
        syncState(player, held.stack(), session);
    }

    public static void acceptCommand(ServerPlayer player, WeaponCommandPayload payload) {
        WeaponSession session = player.getData(HbmAttachments.WEAPON_SESSION);
        if (!session.allowPacket(player.serverLevel().getGameTime())) {
            return;
        }
        if (payload.sequence() < 0 || !session.acknowledge(payload.sequence())) {
            return;
        }
        ensureUniqueGunStackIdentities(player);
        Optional<HeldGun> resolved = heldGun(player);
        if (resolved.isEmpty()) {
            session.reset();
            return;
        }

        HeldGun held = resolved.get();
        session.bind(held.state().stackIdentity());
        switch (payload.command()) {
            case RELOAD -> beginReload(player, held, session);
            case CYCLE_FIRE_MODE -> cycleFireMode(held, session);
            case CYCLE_AMMO -> cycleAmmo(held, session);
        }
        syncState(player, held.stack(), session);
    }

    public static void tick(ServerPlayer player) {
        ensureUniqueGunStackIdentities(player);
        WeaponSession session = player.getData(HbmAttachments.WEAPON_SESSION);
        Optional<HeldGun> resolved = heldGun(player);
        if (resolved.isEmpty() || !player.isAlive()) {
            clearMovementWeight(player);
            session.reset();
            return;
        }

        HeldGun held = resolved.get();
        if (session.bind(held.state().stackIdentity())) {
            syncState(player, held.stack(), session);
        }
        if (player.isSprinting()) {
            session.setAdsHeld(false);
        }
        applyMovementWeight(player, held.definition(), session.adsHeld());
        session.tickCooldown();
        coolWeapon(held);

        if (session.reloadPhase() != ReloadPhase.IDLE) {
            tickReload(player, held, session);
            return;
        }
        tickTrigger(player, held, session);
    }

    public static void cancelSession(ServerPlayer player) {
        clearMovementWeight(player);
        player.getData(HbmAttachments.WEAPON_SESSION).reset();
    }

    private static void tickTrigger(ServerPlayer player, HeldGun held, WeaponSession session) {
        if (session.cooldownTicks() > 0.0D) {
            return;
        }

        FireMode mode = held.state().fireMode();
        boolean shouldFire = switch (mode) {
            case SEMI -> session.consumeSemiQueued();
            case AUTO -> session.triggerHeld();
            case BURST -> {
                if (session.burstRemaining() == 0 && session.consumeSemiQueued()) {
                    session.beginBurst(held.definition().getBurstSize());
                }
                yield session.burstRemaining() > 0;
            }
        };
        if (!shouldFire) {
            return;
        }

        if (!fireOne(player, held, session)) {
            session.addCooldown(4.0D);
            if (mode == FireMode.BURST) {
                session.beginBurst(0);
            }
            return;
        }
        if (mode == FireMode.BURST) {
            session.consumeBurstRound();
        }
    }

    private static boolean fireOne(ServerPlayer player, HeldGun held, WeaponSession session) {
        GunState state = currentState(held.stack(), held.definition());
        if (state.ammoCount() <= 0 || (held.definition().getMagazine().getUsesChamber() && !state.chambered())) {
            emit(player, held.definition(), WeaponEffectType.DRY_FIRE,
                    held.definition().getSounds().get("dry_fire"), 0.0F, 0.0F, 0);
            playSound(player, held.definition().getSounds().get("dry_fire"), 0.6F, 1.0F);
            syncState(player, held.stack(), session);
            return false;
        }

        AmmoDefinition ammo = GunDefinitionRegistry.ammo(state.loadedAmmoId());
        if (ammo == null || !held.definition().getSupportedAmmo().contains(ammo.getId())) {
            HbmNuclearTech.LOGGER.error("Refusing to fire {} with invalid loaded ammo {}.",
                    held.definition().getId(), state.loadedAmmoId());
            return false;
        }

        int remaining = state.ammoCount() - 1;
        GunState firedState = state.withMagazine(
                state.loadedAmmoId(),
                remaining,
                held.definition().getMagazine().getUsesChamber() && remaining > 0
        ).withHeatAndDurability(state.heat() + 1.0F, state.durability() + 1);
        held.stack().set(HbmDataComponents.GUN_STATE.get(), firedState);

        Vec3 look = player.getLookAngle().normalize();
        Vec3 origin = player.getEyePosition().add(look.scale(0.45D));
        double movement = player.getDeltaMovement().horizontalDistance();
        double spread = session.adsHeld()
                ? held.definition().getSpread().getAdsDegrees()
                : held.definition().getSpread().getHipDegrees();
        spread += Math.min(1.0D, movement * 4.0D) * held.definition().getSpread().getMovementDegrees();
        GunBehaviorRegistry.require(held.definition().getBehavior())
                .fire(player, held.definition(), ammo, origin, look, spread);

        ResourceLocation fireSound = held.definition().getSounds().get("fire");
        playSound(player, fireSound, 1.0F, 0.96F + player.getRandom().nextFloat() * 0.08F);
        emit(player, held.definition(), WeaponEffectType.FIRE, fireSound,
                (float) held.definition().getRecoil().getYaw(),
                (float) held.definition().getRecoil().getPitch(),
                firedState.ammoCount());
        Vec3 up = new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 right = look.cross(up);
        if (right.lengthSqr() < 1.0E-6D) {
            right = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            right = right.normalize();
        }
        Vec3 eye = player.getEyePosition();
        Vec3 muzzle = eye.add(look.scale(0.72D)).add(right.scale(0.12D)).subtract(up.scale(0.12D));
        Vec3 casing = eye.add(look.scale(0.25D)).add(right.scale(0.24D)).subtract(up.scale(0.10D));
        emit(player, held.definition(), WeaponEffectType.MUZZLE_FLASH, ammo.getId(),
                0.0F, 0.0F, ammo.getTracerColor(), muzzle);
        emit(player, held.definition(), WeaponEffectType.SMOKE, ammo.getId(),
                0.0F, 0.0F, 0, muzzle);
        emit(player, held.definition(), WeaponEffectType.CASING, ammo.getId(),
                0.0F, 0.0F, 0, casing);
        session.addCooldown(held.definition().getShotIntervalTicks());
        syncState(player, held.stack(), session);
        return true;
    }

    private static void beginReload(ServerPlayer player, HeldGun held, WeaponSession session) {
        if (session.reloadPhase() != ReloadPhase.IDLE) {
            return;
        }
        GunState state = currentState(held.stack(), held.definition());
        if (state.ammoCount() >= held.definition().getMagazine().getCapacity()) {
            return;
        }
        if (state.ammoCount() > 0 && !state.loadedAmmoId().equals(state.selectedAmmoId())) {
            return;
        }
        AmmoSource source = AmmoSources.forPlayer(player);
        if (source.available(state.selectedAmmoId()) <= 0) {
            return;
        }
        session.setTriggerHeld(false);
        session.setReload(ReloadPhase.START, held.definition().getReload().getStartTicks());
        ResourceLocation sound = soundFor(held.definition(), "reload_remove", "reload");
        playSound(player, sound, 0.8F, 1.0F);
        emit(player, held.definition(), WeaponEffectType.RELOAD_START, sound,
                0.0F, 0.0F, state.ammoCount() == 0 ? 1 : 0);
    }

    private static void tickReload(ServerPlayer player, HeldGun held, WeaponSession session) {
        GunDefinition.ReloadProfile reload = held.definition().getReload();
        if (interruptPerRoundReload(session, reload)) {
            ResourceLocation actionSound = soundFor(held.definition(), "reload_action", "reload");
            playSound(player, actionSound, 0.75F, 1.0F);
            emit(player, held.definition(), WeaponEffectType.RELOAD_END,
                    actionSound, 0.0F, 0.0F, 0);
            syncState(player, held.stack(), session);
            return;
        }
        if (!session.tickAction()) {
            return;
        }

        switch (session.reloadPhase()) {
            case START -> {
                if (reload.getStyle() == ReloadStyle.MAGAZINE) {
                    session.setReload(ReloadPhase.TRANSFER, reload.getTransferTicks());
                    emit(player, held.definition(), WeaponEffectType.RELOAD_INSERT,
                            soundFor(held.definition(), "reload_insert", "reload"), 0.0F, 0.0F, 0);
                } else {
                    session.setReload(ReloadPhase.LOOP, reload.getLoopTicks());
                }
            }
            case TRANSFER -> {
                boolean needsAction = currentState(held.stack(), held.definition()).ammoCount() == 0;
                boolean inserted = transferMagazine(player, held);
                session.setReload(ReloadPhase.END,
                        needsAction ? reload.getEmptyEndTicks() : reload.getEndTicks());
                ResourceLocation insertSound = soundFor(held.definition(), "reload_insert", "reload");
                if (inserted) {
                    playSound(player, insertSound, 0.8F, 1.0F);
                }
                ResourceLocation endSound = needsAction
                        ? soundFor(held.definition(), "reload_action", "reload") : insertSound;
                if (inserted && needsAction) {
                    playSound(player, endSound, 0.75F, 1.0F);
                }
                emit(player, held.definition(), WeaponEffectType.RELOAD_END,
                        endSound, 0.0F, 0.0F, 0);
            }
            case LOOP -> {
                boolean inserted = transferOneRound(player, held);
                GunState updated = currentState(held.stack(), held.definition());
                if (inserted) {
                    ResourceLocation insertSound = soundFor(held.definition(), "reload_insert", "reload");
                    playSound(player, insertSound, 0.8F, 1.0F);
                    emit(player, held.definition(), WeaponEffectType.RELOAD_INSERT,
                            insertSound, 0.0F, 0.0F, updated.ammoCount());
                }
                boolean finished = !inserted
                        || updated.ammoCount() >= held.definition().getMagazine().getCapacity();
                session.setReload(finished ? ReloadPhase.END : ReloadPhase.LOOP,
                        finished ? reload.getEndTicks() : reload.getLoopTicks());
                if (finished) {
                    ResourceLocation actionSound = soundFor(held.definition(), "reload_action", "reload");
                    emit(player, held.definition(), WeaponEffectType.RELOAD_END,
                            actionSound, 0.0F, 0.0F, 0);
                }
            }
            case END -> {
                session.setReload(ReloadPhase.IDLE, 0);
            }
            case IDLE -> {
            }
        }
        syncState(player, held.stack(), session);
    }

    static boolean interruptPerRoundReload(WeaponSession session, GunDefinition.ReloadProfile reload) {
        if (session.reloadPhase() != ReloadPhase.LOOP
                || reload.getStyle() != ReloadStyle.PER_ROUND
                || !session.triggerHeld()) {
            return false;
        }
        session.setReload(ReloadPhase.END, reload.getEndTicks());
        return true;
    }

    private static boolean transferMagazine(ServerPlayer player, HeldGun held) {
        GunState state = currentState(held.stack(), held.definition());
        int needed = held.definition().getMagazine().getCapacity() - state.ammoCount();
        int extracted = AmmoSources.forPlayer(player).extract(state.selectedAmmoId(), needed);
        if (extracted <= 0) {
            return false;
        }
        MagazineTransaction.Result result = MagazineTransaction.load(
                state, held.definition(), state.selectedAmmoId(), extracted);
        held.stack().set(HbmDataComponents.GUN_STATE.get(), result.state());
        return result.acceptedRounds() > 0;
    }

    private static boolean transferOneRound(ServerPlayer player, HeldGun held) {
        GunState state = currentState(held.stack(), held.definition());
        if (state.ammoCount() >= held.definition().getMagazine().getCapacity()) {
            return false;
        }
        int extracted = AmmoSources.forPlayer(player).extract(state.selectedAmmoId(), 1);
        if (extracted != 1) {
            return false;
        }
        MagazineTransaction.Result result = MagazineTransaction.load(
                state, held.definition(), state.selectedAmmoId(), extracted);
        held.stack().set(HbmDataComponents.GUN_STATE.get(), result.state());
        return true;
    }

    private static void cycleFireMode(HeldGun held, WeaponSession session) {
        if (session.reloadPhase() != ReloadPhase.IDLE) {
            return;
        }
        GunState state = currentState(held.stack(), held.definition());
        List<FireMode> modes = held.definition().getFireModes();
        int next = (modes.indexOf(state.fireMode()) + 1) % modes.size();
        held.stack().set(HbmDataComponents.GUN_STATE.get(), state.withFireMode(modes.get(next)));
    }

    private static void cycleAmmo(HeldGun held, WeaponSession session) {
        if (session.reloadPhase() != ReloadPhase.IDLE) {
            return;
        }
        GunState state = currentState(held.stack(), held.definition());
        if (state.ammoCount() > 0) {
            return;
        }
        List<ResourceLocation> ammo = held.definition().getSupportedAmmo();
        int next = (ammo.indexOf(state.selectedAmmoId()) + 1) % ammo.size();
        held.stack().set(HbmDataComponents.GUN_STATE.get(), state.withSelectedAmmo(ammo.get(next)));
    }

    private static void coolWeapon(HeldGun held) {
        GunState state = currentState(held.stack(), held.definition());
        if (state.heat() <= 0.0F) {
            return;
        }
        held.stack().set(HbmDataComponents.GUN_STATE.get(),
                state.withHeatAndDurability(Math.max(0.0F, state.heat() - 0.04F), state.durability()));
    }

    private static Optional<HeldGun> heldGun(ServerPlayer player) {
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof HbmGunItem gunItem)) {
            return Optional.empty();
        }
        GunDefinition definition = GunDefinitionRegistry.gun(gunItem.definitionId());
        if (definition == null) {
            return Optional.empty();
        }
        GeoItem.getOrAssignId(stack, player.serverLevel());
        GunState state = currentState(stack, definition);
        return Optional.of(new HeldGun(stack, gunItem, definition, state));
    }

    /** ItemStack copies carry data components, so duplicate action identities are reminted server-side. */
    private static void ensureUniqueGunStackIdentities(ServerPlayer player) {
        Set<UUID> identities = new HashSet<>();
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!(stack.getItem() instanceof HbmGunItem gunItem)) {
                continue;
            }
            GunDefinition definition = GunDefinitionRegistry.gun(gunItem.definitionId());
            if (definition == null) {
                continue;
            }
            GunState state = currentState(stack, definition);
            if (!identities.add(state.stackIdentity())) {
                GunState reminted = state.withStackIdentity(UUID.randomUUID());
                stack.set(HbmDataComponents.GUN_STATE.get(), reminted);
                stack.remove(GeckoLibConstants.STACK_ANIMATABLE_ID_COMPONENT.get());
                GeoItem.getOrAssignId(stack, player.serverLevel());
                identities.add(reminted.stackIdentity());
            }
        }
    }

    private static GunState currentState(ItemStack stack, GunDefinition definition) {
        GunState state = stack.get(HbmDataComponents.GUN_STATE.get());
        if (state == null) {
            state = GunState.create(definition);
            stack.set(HbmDataComponents.GUN_STATE.get(), state);
            return state;
        }
        GunState migrated = state.migrated(definition);
        if (!migrated.equals(state)) {
            stack.set(HbmDataComponents.GUN_STATE.get(), migrated);
        }
        return migrated;
    }

    private static void playSound(ServerPlayer player, ResourceLocation soundId, float volume, float pitch) {
        if (soundId == null) {
            return;
        }
        SoundEvent sound = BuiltInRegistries.SOUND_EVENT.get(soundId);
        if (sound != null) {
            player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                    sound, SoundSource.PLAYERS, volume, pitch);
        }
    }

    private static void emit(ServerPlayer player, GunDefinition gun, WeaponEffectType effect,
                             ResourceLocation resource, float yaw, float pitch, int variant) {
        emit(player, gun, effect, resource, yaw, pitch, variant, player.getEyePosition());
    }

    private static void emit(ServerPlayer player, GunDefinition gun, WeaponEffectType effect,
                             ResourceLocation resource, float yaw, float pitch, int variant,
                             Vec3 position) {
        PacketDistributor.sendToPlayersNear(
                player.serverLevel(),
                null,
                position.x,
                position.y,
                position.z,
                160.0D,
                new WeaponEffectPayload(effect, gun.getId(), resource,
                        position.x, position.y, position.z,
                        yaw, pitch, player.getId(), variant)
        );
    }

    private static ResourceLocation soundFor(GunDefinition gun, String key, String fallbackKey) {
        ResourceLocation sound = gun.getSounds().get(key);
        return sound != null ? sound : gun.getSounds().get(fallbackKey);
    }

    private static void syncState(ServerPlayer player, ItemStack stack, WeaponSession session) {
        GunState state = stack.get(HbmDataComponents.GUN_STATE.get());
        if (state != null) {
            PacketDistributor.sendToPlayer(player, new WeaponStatePayload(
                    session.acknowledgedSequence(),
                    state,
                    session.adsHeld(),
                    (float) GunDefinitionRegistry.requireGun(((HbmGunItem) stack.getItem()).definitionId()).getAds().getFovMultiplier(),
                    (float) GunDefinitionRegistry.requireGun(((HbmGunItem) stack.getItem()).definitionId()).getAds().getSensitivityMultiplier(),
                    (float) GunDefinitionRegistry.requireGun(((HbmGunItem) stack.getItem()).definitionId()).getRecoil().getRecoveryPerTick(),
                    session.reloadPhase(),
                    session.actionTicks()
            ));
        }
    }

    private static void applyMovementWeight(ServerPlayer player, GunDefinition definition, boolean ads) {
        AttributeInstance movementSpeed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movementSpeed != null) {
            double baseMultiplier = 1.0D - definition.getMovementWeight() * 0.25D;
            double adsMultiplier = ads ? definition.getAds().getMovementMultiplier() : 1.0D;
            double totalMultiplier = Math.max(0.1D, baseMultiplier * adsMultiplier);
            movementSpeed.addOrUpdateTransientModifier(new AttributeModifier(
                    WEAPON_WEIGHT_MODIFIER,
                    totalMultiplier - 1.0D,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
            ));
        }
    }

    private static void clearMovementWeight(ServerPlayer player) {
        AttributeInstance movementSpeed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movementSpeed != null) {
            movementSpeed.removeModifier(WEAPON_WEIGHT_MODIFIER);
        }
    }

    private record HeldGun(ItemStack stack, HbmGunItem item, GunDefinition definition, GunState state) {
    }

    private HbmWeaponService() {
    }
}
