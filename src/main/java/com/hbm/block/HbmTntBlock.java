package com.hbm.block;

import com.hbm.config.HbmConfig;
import com.hbm.entity.HbmPrimedTntEntity;
import com.hbm.world.explosion.HbmExplosionMath;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;

/** Source-style HBM TNT block. Its actual blast remains deliberately vanilla-compatible. */
public final class HbmTntBlock extends Block {
    public HbmTntBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        if (!oldState.is(state.getBlock()) && level instanceof ServerLevel serverLevel && shouldPrime(level, pos)) {
            primeAndRemove(serverLevel, pos, null, HbmConfig.BOMBS.hbmTntFuseTicks.get());
        }
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos fromPos, boolean isMoving) {
        if (level instanceof ServerLevel serverLevel && shouldPrime(level, pos)) {
            primeAndRemove(serverLevel, pos, null, HbmConfig.BOMBS.hbmTntFuseTicks.get());
        }
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!stack.is(Items.FLINT_AND_STEEL)) {
            return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
        }

        if (level instanceof ServerLevel serverLevel) {
            primeAndRemove(serverLevel, pos, player, HbmConfig.BOMBS.hbmTntFuseTicks.get());
            stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onProjectileHit(Level level, BlockState state, BlockHitResult hitResult, Projectile projectile) {
        if (level instanceof ServerLevel serverLevel && projectile.isOnFire() && projectile.mayInteract(level, hitResult.getBlockPos())) {
            Entity owner = projectile.getOwner();
            primeAndRemove(serverLevel, hitResult.getBlockPos(), owner instanceof LivingEntity living ? living : null, HbmConfig.BOMBS.hbmTntFuseTicks.get());
        }
    }

    @Override
    public void wasExploded(Level level, BlockPos pos, Explosion explosion) {
        if (level instanceof ServerLevel serverLevel) {
            Entity source = explosion.getIndirectSourceEntity();
            spawnPrimed(serverLevel, pos, source instanceof LivingEntity living ? living : null, chainFuse(serverLevel));
        }
    }

    public static void primeFromExplosion(ServerLevel level, BlockPos pos, @Nullable LivingEntity owner) {
        spawnPrimed(level, pos, owner, chainFuse(level));
    }

    @Override
    public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return 100;
    }

    @Override
    public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return 15;
    }

    private static boolean shouldPrime(Level level, BlockPos pos) {
        if (level.hasNeighborSignal(pos)) {
            return true;
        }
        for (Direction direction : Direction.values()) {
            if (level.getBlockState(pos.relative(direction)).is(Blocks.FIRE)) {
                return true;
            }
        }
        return false;
    }

    private static int chainFuse(ServerLevel level) {
        int base = HbmConfig.BOMBS.hbmTntChainFuseTicks.get();
        return HbmExplosionMath.chainFuse(base, level.getRandom().nextInt(base));
    }

    private static void primeAndRemove(ServerLevel level, BlockPos pos, @Nullable LivingEntity owner, int fuse) {
        if (!level.getBlockState(pos).is(com.hbm.registry.HbmBlocks.TNT.get())) {
            return;
        }
        level.removeBlock(pos, false);
        spawnPrimed(level, pos, owner, fuse);
    }

    private static void spawnPrimed(ServerLevel level, BlockPos pos, @Nullable LivingEntity owner, int fuse) {
        HbmPrimedTntEntity entity = HbmPrimedTntEntity.create(level, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, owner, fuse);
        level.addFreshEntity(entity);
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 1F, 1F);
    }
}
