# Nuclear Chunk Scheduler: Source Contract

Status: implementation in verification

## Tier 1 Evidence

- `ExplosionNukeRayBatched` assigns each generalized-spiral ray tip to every `ChunkCoordIntPair` crossed by that ray.
- The source orders those chunks by Manhattan distance from the blast chunk, then reconstructs and removes individual blocks inside one chunk at a time.
- `EntityNukeExplosionMK5` performs ray collection before chunk destruction and keeps its central explosion chunk alive through a chunk-loading entity.
- The source still mutates individual blocks. Chunks are its scheduling and work-ownership boundary, not a replacement terrain primitive.

## Modern Execution

- A nuclear job first force-loads its crater circle through a registered NeoForge `TicketController` using non-ticking tickets.
- The server thread captures an immutable primitive resistance volume. Bounded workers run the source generalized-spiral mathematics against that snapshot and return compact per-chunk bitsets; workers never read Minecraft world objects.
- MK5 snapshot capture reads force-loaded chunks only after `ServerLevel.hasChunk` reports them ready. A bounded active window is released behind the capture cursor instead of retaining the entire crater.
- Pure workers consume the immutable primitive resistance volume and calculate ordered generalized-spiral batches. Two batches may run concurrently per explosion; completed batches merge in ray order so saved progress always has a contiguous retry-safe cursor.
- After planning, chunks are processed nearest-first. A bounded prefetch window force-loads upcoming chunks; their bitsets are removed on the server thread and tickets are released immediately after each chunk finishes. A prefetch cursor advances only after NeoForge accepts its ticket, and an interrupted or legacy-gapped window rewinds to the current chunk instead of waiting permanently.
- Blast terrain conversion, connected-water resolution, and Fallout Rain use their own bounded chunk windows and release tickets as local work finishes. Water survey/classification changes no blocks and may safely restart after a reload; classified mutation and crater-refill cursors persist. This removes the former verification-sweep loop that could chase ocean source regeneration indefinitely.
- The scheduler has one configurable total per-tick wall-time cap. Active planning, crater mutation, water, and Fallout Rain lanes receive equal base shares with a rotating remainder, so fixed execution order cannot starve a later lane. Individual subsystem limits remain secondary caps.
- New chunk tickets share one hard per-level cap across planning, crater mutation, water, and Fallout Rain lanes. Active lanes receive equal base shares, the remainder rotates each tick, and water's configured cap applies only while another lane is competing. No accepted explosion can monopolize or be starved by ticket admission.
- Ticket owners are persisted with the jobs. NeoForge validates tickets on level load and removes tickets without a live HBM work owner.

## Intentional Modern Differences

- Tier 1 only guarantees the central explosion entity's chunk is forced. The port force-loads every required crater, conversion, liquid, and Fallout Rain chunk in bounded batches so a blast affects its full configured area. A ticket request never counts as readiness; no queue performs a synchronous `getChunk` until the chunk is present.
- Tier 1 retains per-chunk ray tips only in memory. The port persists compact per-chunk target bitsets so a save/reload cannot lose calculated destruction.
- No Minecraft world reads or writes occur off thread. The port uses the useful part of newer snapshot/worker designs while keeping snapshot capture, ordered merge, persistence, and all mutation under its own server-thread contract.

## Acceptance Checks

- An unrendered but affected crater chunk loads, receives the correct scheduled work, and releases its ticket afterward.
- A Little Boy job survives a save/reload without stranded tickets or skipped queued chunks, including recovery from an older saved window whose prefetch cursor advanced past an unforced chunk.
- The global scheduler cap keeps all active nuclear stages within the configured main-thread budget, and a continuous planning queue cannot starve crater, water, or Fallout Rain progress.
- Multiple simultaneous explosions continue making fair progress through planning, crater mutation, water, and Fallout Rain without discarding a job or exceeding the per-level ticket cap.
- Water work reaches the configured horizontal Fallout radius plus a non-mutating sentinel ring while skipping chunk sections that cannot contain free water. Only proven enclosed bodies evaporate; persistent bodies contaminate and refill the crater.
- Crater geometry remains controlled by ray count and resistance math; chunk scheduling changes ordering and coverage, not the governing source formula.
