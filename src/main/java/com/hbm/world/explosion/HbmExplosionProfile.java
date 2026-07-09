package com.hbm.world.explosion;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

public record HbmExplosionProfile(float radius, double fallout, Level.ExplosionInteraction blockInteraction, ResourceLocation clientEffect) {
}
