# Explosion Math Foundation

Status: implementation complete; post-fix runtime verification pending
Classification: framework

## Tier 1 Evidence

- `BlockTNT` uses a public HBM block with a strength `10` vanilla-style explosion.
- `BlockTNTBase` primes on redstone, flint and steel, nearby fire, and burning arrows. Its normal fuse is `80` ticks; chain detonations use a randomized `10..29` tick fuse.
- `ExplosionNukeSmall.PARAMS_LOW` uses terrain strength `15`, kill radius `45`, radiation level `2`, resolution `64`, and the `mukeExplosion` sound.
- `ExplosionNT` traces boundary-cube rays in `0.3` block steps. Each ray begins at `strength * (0.7 + random * 0.6)`, loses `(resistance + 0.3) * 0.3` through a block, then loses `0.225` per step.
- `PARAMS_LOW` applies `FIRE`, `NOPARTICLE`, `NOSOUND`, `NODROP`, and `NOHURT` to terrain. `ExplosionNukeGeneric.dealDamage` separately applies line-of-sight nuclear damage, five seconds of fire, and normalized `0.2` knockback.
- `ExplosionNukeSmall` adds fallout to the 25 chunks whose Manhattan distance from the blast chunk is at most three: `50 / (distance + 1) * radiationLevel / 3`.

## Public Test Surface

- `hbm:tnt` is a source-backed public HBM explosive available in the NTM Bombs tab. It remains creative-only until the full original `stick_tnt`, detonating cord, and safety-fuse chain is active.
- `hbm:prototype_nuke` remains a diagnostic wrapper for the Low Yield profile, not a claim that a specific original bomb has been fully ported.

## Server Contract

- Conventional TNT delegates to the modern vanilla explosion API at source strength `10`.
- Low Yield terrain strength `15` executes in the detonation tick, matching the source's immediate behavior. Larger future profiles use planned, saved, bounded main-thread terrain work.
- Bounded jobs retain compact target sets grouped by real chunk in per-dimension saved-data state for one full server tick before destruction begins.
- Large jobs force-load their active crater, terrain, water, and Fallout Rain chunks through a bounded, non-ticking NeoForge ticket frontier. The work queue releases each ticket after its chunk completes.
- Nuclear entity damage and fallout occur when the charge detonates; terrain removal follows in bounded batches.
- Source values are captured from the common config at detonation, so reloads cannot alter a job already in progress.

## Visual Follow-Up Status

- Little Boy now uses Reloaded 1.12.2's original fixed mushroom mesh, diffuse and emissive fireball stages, bounded ground cloudlets, world flare, flash, pressure front, shock-arrival camera response, and post-crater Fallout Rain presentation. These remain under visual parity review rather than being treated as exact.
- The Low Yield shrapnel set and exact profile-specific `muke` particle behavior remain deferred.
- The source screwdriver/defuser tools are not active in the current alpha, so their TNT interactions remain pending tool-family work.

## Acceptance Checks

- HBM TNT has an 80-tick fuse, strength 10 blast, and a 10..29 tick chain fuse.
- The Low Yield pilot uses terrain strength 15, kill radius 45, max damage 250, resolution 64, and source fallout values.
- Nuclear terrain produces no drops and never causes off-thread world access. Little Boy uses persisted, bounded NeoForge force tickets to cover unrendered affected terrain; its fire is non-spreading under `EXP-005`.
- Fallout affects both players and mobs through the existing radiation framework.
