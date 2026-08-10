package com.hbm.weapon.behavior;

import com.hbm.weapon.data.AmmoDefinition;
import com.hbm.weapon.data.GunDefinition;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public interface GunBehavior {
    void fire(ServerPlayer player, GunDefinition gun, AmmoDefinition ammo,
              Vec3 origin, Vec3 direction, double spreadDegrees);
}
