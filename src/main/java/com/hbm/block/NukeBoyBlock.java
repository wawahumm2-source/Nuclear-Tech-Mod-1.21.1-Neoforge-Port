package com.hbm.block;

import com.hbm.world.explosion.HbmExplosionService;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * Developer-armed Little Boy wrapper. The original assembly container is a later bomb-family pass.
 */
public final class NukeBoyBlock extends PrototypeNukeBlock {
    public NukeBoyBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void detonate(ServerLevel level, BlockPos pos) {
        HbmExplosionService.detonateNukeBoy(level, pos, null);
    }

    @Override
    protected String detonationMessageKey() {
        return "message.hbm.nuke_boy.armed";
    }
}
