package com.hbm.registry;

import com.hbm.HbmNuclearTech;
import com.hbm.blockentity.BurnerPressBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class HbmBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, HbmNuclearTech.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BurnerPressBlockEntity>> BURNER_PRESS =
            BLOCK_ENTITY_TYPES.register("burner_press", () -> BlockEntityType.Builder.of(
                    BurnerPressBlockEntity::new,
                    HbmBlocks.BURNER_PRESS.get()
            ).build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITY_TYPES.register(eventBus);
    }

    private HbmBlockEntities() {
    }
}
