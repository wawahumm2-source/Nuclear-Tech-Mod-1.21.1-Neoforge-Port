package com.hbm.registry;

import com.hbm.HbmNuclearTech;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class HbmParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, HbmNuclearTech.MOD_ID);

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> NUCLEAR_CLOUD = register("nuclear_cloud");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> NUCLEAR_SHOCK = register("nuclear_shock");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> NUCLEAR_RING = register("nuclear_ring");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> NUCLEAR_FLASH = register("nuclear_flash");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> NUCLEAR_CAP = register("nuclear_cap");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> NUCLEAR_FALLOUT = register("nuclear_fallout");

    public static void register(IEventBus eventBus) {
        PARTICLE_TYPES.register(eventBus);
    }

    private static DeferredHolder<ParticleType<?>, SimpleParticleType> register(String name) {
        return PARTICLE_TYPES.register(name, () -> new SimpleParticleType(false));
    }

    private HbmParticles() {
    }
}
