package com.hbm.world.explosion;

import com.hbm.HbmNuclearTech;
import java.util.Set;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.common.world.chunk.RegisterTicketControllersEvent;
import net.neoforged.neoforge.common.world.chunk.TicketController;

/**
 * Owns the persistent, non-ticking chunk tickets used by resumable nuclear jobs.
 *
 * <p>Tickets are intentionally non-ticking: the explosion needs the chunk data and
 * server-thread mutation access, not an uncontrolled simulation radius.</p>
 */
public final class HbmNuclearChunkTickets {
    private static final TicketController CONTROLLER = new TicketController(
            ResourceLocation.fromNamespaceAndPath(HbmNuclearTech.MOD_ID, "nuclear_work"),
            (level, helper) -> {
                Set<UUID> activeOwners = HbmNuclearExplosionSavedData.get(level).activeTicketOwners();
                for (UUID owner : helper.getEntityTickets().keySet()) {
                    if (!activeOwners.contains(owner)) {
                        helper.removeAllTickets(owner);
                    }
                }
            }
    );

    private HbmNuclearChunkTickets() {
    }

    public static void register(RegisterTicketControllersEvent event) {
        event.register(CONTROLLER);
    }

    static void force(ServerLevel level, UUID owner, long chunkKey) {
        CONTROLLER.forceChunk(level, owner, ChunkPos.getX(chunkKey), ChunkPos.getZ(chunkKey), true, false);
    }

    static void release(ServerLevel level, UUID owner, long chunkKey) {
        CONTROLLER.forceChunk(level, owner, ChunkPos.getX(chunkKey), ChunkPos.getZ(chunkKey), false, false);
    }
}
