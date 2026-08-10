package com.hbm.weapon.ammo;

import net.minecraft.resources.ResourceLocation;

import java.util.List;

public final class CompositeAmmoSource implements AmmoSource {
    private final List<AmmoSource> sources;

    public CompositeAmmoSource(AmmoSource... sources) {
        this.sources = List.of(sources);
    }

    @Override
    public int available(ResourceLocation ammoId) {
        long total = 0;
        for (AmmoSource source : sources) {
            total += source.available(ammoId);
            if (total >= Integer.MAX_VALUE) {
                return Integer.MAX_VALUE;
            }
        }
        return (int) total;
    }

    @Override
    public int extract(ResourceLocation ammoId, int requested) {
        int remaining = Math.max(0, requested);
        for (AmmoSource source : sources) {
            remaining -= source.extract(ammoId, remaining);
            if (remaining <= 0) {
                return requested;
            }
        }
        return requested - remaining;
    }
}
