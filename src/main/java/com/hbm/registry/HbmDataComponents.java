package com.hbm.registry;

import com.hbm.HbmNuclearTech;
import com.hbm.weapon.state.GunState;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class HbmDataComponents {
    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, HbmNuclearTech.MOD_ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<GunState>> GUN_STATE =
            COMPONENTS.registerComponentType("gun_state", builder -> builder
                    .persistent(GunState.CODEC)
                    .networkSynchronized(GunState.STREAM_CODEC));

    public static void register(IEventBus eventBus) {
        COMPONENTS.register(eventBus);
    }

    private HbmDataComponents() {
    }
}
