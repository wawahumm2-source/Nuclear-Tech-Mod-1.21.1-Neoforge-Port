package com.hbm.weapon.ballistics;

import com.hbm.HbmNuclearTech;
import com.hbm.config.HbmConfig;
import com.hbm.registry.HbmItems;
import com.hbm.weapon.data.AmmoDefinition;
import com.hbm.weapon.data.GunDefinition;
import com.hbm.world.damage.HbmDamageTypes;
import com.hbm.world.explosion.HbmExplosionService;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class BallisticProjectileEntity extends ThrowableProjectile implements ItemSupplier {
    private static final ResourceLocation DEFAULT_AMMO = ResourceLocation.fromNamespaceAndPath(HbmNuclearTech.MOD_ID, "g40_he");
    private static final ResourceLocation DEFAULT_EFFECT = ResourceLocation.fromNamespaceAndPath(HbmNuclearTech.MOD_ID, "grenade_he");
    private static final EntityDataAccessor<String> AMMO_ID = SynchedEntityData.defineId(
            BallisticProjectileEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Float> GRAVITY = SynchedEntityData.defineId(
            BallisticProjectileEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DRAG = SynchedEntityData.defineId(
            BallisticProjectileEntity.class, EntityDataSerializers.FLOAT);

    private float explosionPower = 3.0F;
    private boolean profileAllowsBlockDamage = true;
    private boolean shapedCharge;
    private float directDamage = 7.0F;
    private float armorPenetration = 0.1F;
    private ResourceLocation impactEffect = DEFAULT_EFFECT;
    private double remainingRange = 128.0D;

    public BallisticProjectileEntity(EntityType<? extends BallisticProjectileEntity> type, Level level) {
        super(type, level);
    }

    public static BallisticProjectileEntity launch(LivingEntity shooter, GunDefinition gun,
                                                   AmmoDefinition ammo, Vec3 origin, Vec3 direction) {
        if (!(shooter.level() instanceof ServerLevel serverLevel)) {
            throw new IllegalArgumentException("Ballistic projectiles must originate on a server level");
        }
        BallisticProjectileEntity projectile = new BallisticProjectileEntity(
                com.hbm.registry.HbmEntities.BALLISTIC_PROJECTILE.get(), serverLevel);
        projectile.setOwner(shooter);
        projectile.setPos(origin);
        projectile.setDeltaMovement(direction.normalize().scale(gun.getMuzzleVelocity()));
        projectile.entityData.set(AMMO_ID, ammo.getId().toString());
        projectile.entityData.set(GRAVITY, (float) ammo.getGravity());
        projectile.entityData.set(DRAG, (float) ammo.getDrag());
        projectile.directDamage = gun.getBaseDamage() * ammo.getDamageMultiplier();
        projectile.armorPenetration = ammo.getArmorPenetration();
        projectile.impactEffect = ammo.getImpactEffect();
        projectile.remainingRange = gun.getMaxRange();
        AmmoDefinition.ExplosionProfile explosion = ammo.getExplosion();
        if (explosion != null) {
            projectile.explosionPower = explosion.getPower();
            projectile.profileAllowsBlockDamage = explosion.getBlockDamage();
            projectile.shapedCharge = explosion.getShapedCharge();
        }
        serverLevel.addFreshEntity(projectile);
        return projectile;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(AMMO_ID, DEFAULT_AMMO.toString());
        builder.define(GRAVITY, 0.045F);
        builder.define(DRAG, 0.01F);
    }

    @Override
    protected double getDefaultGravity() {
        return entityData.get(GRAVITY);
    }

    @Override
    public void tick() {
        Vec3 velocityBeforeDrag = getDeltaMovement();
        Vec3 projectedPosition = position().add(velocityBeforeDrag);
        if (level() instanceof ServerLevel serverLevel
                && !serverLevel.hasChunkAt(BlockPos.containing(projectedPosition))) {
            discard();
            return;
        }

        Vec3 previousPosition = position();
        boolean inWater = isInWater();
        super.tick();
        if (isRemoved()) {
            return;
        }

        remainingRange -= previousPosition.distanceTo(position());
        if (remainingRange <= 0.0D) {
            discard();
            return;
        }

        if (!inWater) {
            double vanillaRetention = 0.99D;
            double profileRetention = 1.0D - entityData.get(DRAG);
            setDeltaMovement(getDeltaMovement().add(
                    velocityBeforeDrag.scale(profileRetention - vanillaRetention)));
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (level().isClientSide) {
            return;
        }
        Entity target = result.getEntity();
        Entity owner = getOwner();
        float damage = directDamage * (shapedCharge ? 2.25F : 1.0F);
        target.hurt(HbmDamageTypes.projectileGunfire(level(), this, owner, armorPenetration), damage);
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!(level() instanceof ServerLevel serverLevel) || isRemoved()) {
            return;
        }
        HbmExplosionService.detonateProjectile(
                serverLevel,
                result.getLocation(),
                this,
                getOwner(),
                explosionPower,
                profileAllowsBlockDamage && HbmConfig.GRENADE_BLOCK_DAMAGE.get(),
                impactEffect
        );
        discard();
    }

    @Override
    public ItemStack getItem() {
        ResourceLocation ammoId = ResourceLocation.tryParse(entityData.get(AMMO_ID));
        ResourceLocation heatId = ResourceLocation.fromNamespaceAndPath(HbmNuclearTech.MOD_ID, "g40_heat");
        return heatId.equals(ammoId)
                ? HbmItems.G40_HEAT.get().getDefaultInstance()
                : HbmItems.G40_HE.get().getDefaultInstance();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("AmmoId", entityData.get(AMMO_ID));
        tag.putFloat("Gravity", entityData.get(GRAVITY));
        tag.putFloat("Drag", entityData.get(DRAG));
        tag.putFloat("ExplosionPower", explosionPower);
        tag.putBoolean("ProfileBlockDamage", profileAllowsBlockDamage);
        tag.putBoolean("ShapedCharge", shapedCharge);
        tag.putFloat("DirectDamage", directDamage);
        tag.putFloat("ArmorPenetration", armorPenetration);
        tag.putString("ImpactEffect", impactEffect.toString());
        tag.putDouble("RemainingRange", remainingRange);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(AMMO_ID, tag.getString("AmmoId"));
        entityData.set(GRAVITY, tag.getFloat("Gravity"));
        entityData.set(DRAG, tag.contains("Drag") ? tag.getFloat("Drag") : 0.01F);
        explosionPower = tag.getFloat("ExplosionPower");
        profileAllowsBlockDamage = tag.getBoolean("ProfileBlockDamage");
        shapedCharge = tag.getBoolean("ShapedCharge");
        directDamage = tag.getFloat("DirectDamage");
        armorPenetration = tag.getFloat("ArmorPenetration");
        ResourceLocation parsedEffect = ResourceLocation.tryParse(tag.getString("ImpactEffect"));
        impactEffect = parsedEffect == null ? DEFAULT_EFFECT : parsedEffect;
        remainingRange = tag.contains("RemainingRange") ? tag.getDouble("RemainingRange") : 128.0D;
    }
}
