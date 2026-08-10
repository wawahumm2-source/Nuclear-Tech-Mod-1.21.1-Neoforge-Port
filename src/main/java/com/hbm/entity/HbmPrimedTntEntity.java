package com.hbm.entity;

import com.hbm.registry.HbmBlocks;
import com.hbm.registry.HbmEntities;
import com.hbm.world.explosion.HbmExplosionService;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.level.Level;

/**
 * Reuses vanilla primed-TNT movement and rendering while routing detonation through the HBM strength-10 profile.
 */
public final class HbmPrimedTntEntity extends PrimedTnt {
    @Nullable
    private LivingEntity hbmOwner;

    public HbmPrimedTntEntity(EntityType<? extends HbmPrimedTntEntity> entityType, Level level) {
        super(entityType, level);
    }

    public static HbmPrimedTntEntity create(ServerLevel level, double x, double y, double z, @Nullable LivingEntity owner, int fuse) {
        HbmPrimedTntEntity entity = new HbmPrimedTntEntity(HbmEntities.HBM_TNT.get(), level);
        entity.setPos(x, y, z);
        float angle = level.getRandom().nextFloat() * ((float) Math.PI * 2F);
        entity.setDeltaMovement(-Mth.sin(angle) * 0.02F, 0.2D, -Mth.cos(angle) * 0.02F);
        entity.setFuse(fuse);
        entity.hbmOwner = owner;
        entity.setBlockState(HbmBlocks.TNT.get().defaultBlockState());
        return entity;
    }

    @Nullable
    @Override
    public LivingEntity getOwner() {
        return this.hbmOwner != null ? this.hbmOwner : super.getOwner();
    }

    @Override
    protected void explode() {
        if (this.level() instanceof ServerLevel level) {
            HbmExplosionService.detonateHbmTnt(level, this.getX(), this.getY(), this.getZ(), getOwner());
        }
    }
}
