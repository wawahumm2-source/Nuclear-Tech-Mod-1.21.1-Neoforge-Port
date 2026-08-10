# Source-Density Nuclear Planner

## Scope

The source-density generalized-spiral planner is the default large-MK5 terrain planner for the four-core, 8 GB target. Little Boy uses the Tier 1 default of `452389` rays, strength `240`, range `120`, and no downward resistance bias. A lower-density hybrid radial planner remains available only as an optional weak-hardware mode.

`nukeBoyUseHybridPlanner=false` is the parity default. Low-yield `ExplosionNT` remains a distinct immediate path.

## Thread Boundary

- The server thread owns chunk tickets, chunk reads, snapshot capture, block entities, terrain mutation, heightmaps, lighting, packets, persistence, entities, radiation, and fallout.
- A bounded two-thread executor receives only immutable primitive snapshot arrays and immutable profile values.
- Workers produce source-ordered ray batches as per-chunk target bitsets. They never retain or access a `Level`, `Chunk`, `BlockState`, entity, registry, or renderer.
- A saved or reloaded in-flight job preserves merged target masks and its contiguous ray cursor, then safely recaptures the immutable snapshot. Unmerged worker batches are retried.

## Source Math

- Snapshot capture stores air, fluid, and quantized effective MK5 resistance inside the configured crater sphere.
- The planner traces Tier 1's generalized-spiral sequence through that snapshot with its masquerade-resistance and distance-exponent formulas.
- Each successful ray removes the same non-air path represented by Tier 1's per-chunk tip reconstruction. Fluids do not attenuate the ray but remain removal targets.
- The optional hybrid mode uses a deterministic cube-map radial field and does not claim block-for-block parity.

## Performance Contract

- Defaults target two workers, `8192` rays per source batch, bounded queued work, and section-batched main-thread snapshots.
- Multiple explosions are always accepted. A globally bounded, round-robin scheduler interleaves snapshot capture, source batches, terrain mutation, water, and Fallout Rain without discarding a detonation. Both wall-time shares and new chunk-ticket shares rotate across active lanes, preventing a continuous stream of new planners from freezing older aftermath work.
- Ten seconds is a target for pregenerated terrain, not a guarantee when force-loading must generate chunks or transmit large terrain changes.

## Acceptance Checks

- Identical snapshot, profile, and seed produce identical target masks.
- Worker output does not depend on scheduling order.
- Source MK5, optional hybrid MK5, and `ExplosionNT` remain selectable and functional.
- Save/reload during snapshot or worker calculation restarts safely without losing the explosion.
- No worker thread accesses Minecraft world objects.
- Snapshot memory and executor queues remain bounded for the target hardware.
