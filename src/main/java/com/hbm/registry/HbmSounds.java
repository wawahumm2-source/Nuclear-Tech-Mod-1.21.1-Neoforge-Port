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

    public static final DeferredHolder<SoundEvent, SoundEvent> PRESS_OPERATE = SOUND_EVENTS.register("block.pressoperate",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(HbmNuclearTech.MOD_ID, "block.pressoperate")));

    public static void register(IEventBus eventBus) {
        SOUND_EVENTS.register(eventBus);
    }

    private HbmSounds() {
    }
}
