package com.hbm.weapon.behavior;

import com.hbm.weapon.ballistics.BallisticProjectileEntity;
import com.hbm.weapon.ballistics.BallisticsService;
import com.hbm.weapon.data.AmmoDefinition;
import com.hbm.weapon.data.GunDefinition;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public final class GrenadeLauncherBehavior implements GunBehavior {
    @Override
    public void fire(ServerPlayer player, GunDefinition gun, AmmoDefinition ammo,
                     Vec3 origin, Vec3 direction, double spreadDegrees) {
        Vec3 spreadDirection = BallisticsService.spreadDirection(
                direction,
                spreadDegrees * ammo.getSpreadMultiplier(),
                player.getRandom()
        );
        BallisticProjectileEntity.launch(player, gun, ammo, origin, spreadDirection);
    }
}
