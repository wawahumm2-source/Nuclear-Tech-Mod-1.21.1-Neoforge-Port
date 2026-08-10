package com.hbm.registry;

import com.hbm.HbmNuclearTech;
import com.hbm.entity.HbmPrimedTntEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.EntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class HbmEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, HbmNuclearTech.MOD_ID);
    public static final DeferredHolder<EntityType<?>, EntityType<HbmPrimedTntEntity>> HBM_TNT = ENTITY_TYPES.register("hbm_tnt",
            () -> EntityType.Builder.of(HbmPrimedTntEntity::new, MobCategory.MISC)
                    .sized(0.98F, 0.98F)
                    .clientTrackingRange(10)
                    .updateInterval(10)
                    .build("hbm_tnt"));

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }

    private HbmEntities() {
    }
}
