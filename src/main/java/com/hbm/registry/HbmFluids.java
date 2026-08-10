package com.hbm.registry;

import com.hbm.HbmNuclearTech;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class HbmFluids {
    public static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, HbmNuclearTech.MOD_ID);
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(BuiltInRegistries.FLUID, HbmNuclearTech.MOD_ID);

    public static final DeferredHolder<FluidType, FluidType> CONTAMINATED_WATER_TYPE = FLUID_TYPES.register(
            "contaminated_water",
            () -> new FluidType(FluidType.Properties.create()
                    .descriptionId("fluid_type.hbm.contaminated_water")
                    .canConvertToSource(true)
                    .canExtinguish(true)
                    .supportsBoating(true)
                    .density(1000)
                    .viscosity(1000)
                    .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                    .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY))
    );

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> CONTAMINATED_WATER = FLUIDS.register(
            "contaminated_water",
            () -> new BaseFlowingFluid.Source(contaminatedWaterProperties())
    );

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> FLOWING_CONTAMINATED_WATER = FLUIDS.register(
            "flowing_contaminated_water",
            () -> new BaseFlowingFluid.Flowing(contaminatedWaterProperties())
    );

    public static void register(IEventBus eventBus) {
        FLUID_TYPES.register(eventBus);
        FLUIDS.register(eventBus);
    }

    private static BaseFlowingFluid.Properties contaminatedWaterProperties() {
        return new BaseFlowingFluid.Properties(
                CONTAMINATED_WATER_TYPE,
                CONTAMINATED_WATER,
                FLOWING_CONTAMINATED_WATER
        )
                .bucket(HbmItems.CONTAMINATED_WATER_BUCKET)
                .block(HbmBlocks.CONTAMINATED_WATER)
                .slopeFindDistance(4)
                .levelDecreasePerBlock(1)
                .tickRate(5)
                .explosionResistance(100F);
    }

    private HbmFluids() {
    }
}
