package com.hbm.weapon.ammo;

import net.minecraft.server.level.ServerPlayer;

public final class AmmoSources {
    public static AmmoSource forPlayer(ServerPlayer player) {
        return new CompositeAmmoSource(
                new PlayerInventoryAmmoSource(player),
                new CuriosAmmoSource(player)
        );
    }

    private AmmoSources() {
    }
}
