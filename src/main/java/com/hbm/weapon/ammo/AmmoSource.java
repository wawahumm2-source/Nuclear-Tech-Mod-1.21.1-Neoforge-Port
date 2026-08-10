package com.hbm.weapon.ammo;

import net.minecraft.resources.ResourceLocation;

/** Server-side ammunition transaction boundary. */
public interface AmmoSource {
    int available(ResourceLocation ammoId);

    /** Removes up to {@code requested} rounds and returns the exact amount removed. */
    int extract(ResourceLocation ammoId, int requested);
}
