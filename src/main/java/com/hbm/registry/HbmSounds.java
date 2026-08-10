package com.hbm.registry;

import com.hbm.HbmNuclearTech;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class HbmSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(Registries.SOUND_EVENT, HbmNuclearTech.MOD_ID);

    public static final DeferredHolder<SoundEvent, SoundEvent> GEIGER_CLICK = SOUND_EVENTS.register("geiger_click",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(HbmNuclearTech.MOD_ID, "geiger_click")));

    public static final DeferredHolder<SoundEvent, SoundEvent> GEIGER_1 = register("item.geiger1");
    public static final DeferredHolder<SoundEvent, SoundEvent> GEIGER_2 = register("item.geiger2");
    public static final DeferredHolder<SoundEvent, SoundEvent> GEIGER_3 = register("item.geiger3");
    public static final DeferredHolder<SoundEvent, SoundEvent> GEIGER_4 = register("item.geiger4");
    public static final DeferredHolder<SoundEvent, SoundEvent> GEIGER_5 = register("item.geiger5");
    public static final DeferredHolder<SoundEvent, SoundEvent> GEIGER_6 = register("item.geiger6");

    public static final DeferredHolder<SoundEvent, SoundEvent> RADAWAY = SOUND_EVENTS.register("item.radaway",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(HbmNuclearTech.MOD_ID, "item.radaway")));

    public static final DeferredHolder<SoundEvent, SoundEvent> PRESS_OPERATE = SOUND_EVENTS.register("block.pressoperate",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(HbmNuclearTech.MOD_ID, "block.pressoperate")));
    public static final DeferredHolder<SoundEvent, SoundEvent> MUKE_EXPLOSION = register("weapon.mukeexplosion");
    public static final DeferredHolder<SoundEvent, SoundEvent> NUCLEAR_EXPLOSION = register("weapon.nuclearexplosion");

    public static final DeferredHolder<SoundEvent, SoundEvent> FIRE_PISTOL_LIGHT = register("weapon.fire.pistollight");
    public static final DeferredHolder<SoundEvent, SoundEvent> FIRE_ASSAULT = register("weapon.fire.assault");
    public static final DeferredHolder<SoundEvent, SoundEvent> FIRE_SHOTGUN = register("weapon.shotgunshoot");
    public static final DeferredHolder<SoundEvent, SoundEvent> FIRE_GRENADE = register("weapon.glshoot");
    public static final DeferredHolder<SoundEvent, SoundEvent> DRY_FIRE = register("weapon.reload.dryfireclick");
    public static final DeferredHolder<SoundEvent, SoundEvent> RELOAD_MAG_SMALL = register("weapon.reload.magsmallinsert");
    public static final DeferredHolder<SoundEvent, SoundEvent> RELOAD_MAG_SMALL_REMOVE = register("weapon.reload.magsmallremove");
    public static final DeferredHolder<SoundEvent, SoundEvent> RELOAD_MAG = register("weapon.reload.maginsert");
    public static final DeferredHolder<SoundEvent, SoundEvent> RELOAD_MAG_REMOVE = register("weapon.reload.magremove");
    public static final DeferredHolder<SoundEvent, SoundEvent> ACTION_PISTOL = register("weapon.reload.pistolcock");
    public static final DeferredHolder<SoundEvent, SoundEvent> RELOAD_SHOTGUN = register("weapon.reload.shotgunreload");
    public static final DeferredHolder<SoundEvent, SoundEvent> ACTION_SHOTGUN = register("weapon.reload.shotguncock");
    public static final DeferredHolder<SoundEvent, SoundEvent> RELOAD_GRENADE = register("weapon.glreload");

    private static DeferredHolder<SoundEvent, SoundEvent> register(String id) {
        return SOUND_EVENTS.register(id, () -> SoundEvent.createVariableRangeEvent(
                ResourceLocation.fromNamespaceAndPath(HbmNuclearTech.MOD_ID, id)));
    }

    public static void register(IEventBus eventBus) {
        SOUND_EVENTS.register(eventBus);
    }

    private HbmSounds() {
    }
}
