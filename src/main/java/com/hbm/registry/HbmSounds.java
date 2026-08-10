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

    public static void register(IEventBus eventBus) {
        SOUND_EVENTS.register(eventBus);
    }

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(HbmNuclearTech.MOD_ID, name)));
    }

    private HbmSounds() {
    }
}
