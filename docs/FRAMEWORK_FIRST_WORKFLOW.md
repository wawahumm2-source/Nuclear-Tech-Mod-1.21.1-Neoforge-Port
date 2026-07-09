# Framework-First Port Workflow

The port should move from stable frameworks to content, not from item volume to systems. Every parity pass should preserve original HBM identity while making the NeoForge foundation stronger.

Reference priority is defined in `docs/CANONICAL_REFERENCE_HIERARCHY.md`. Use `tools/hbm-reference.ps1 compare <name>` before system work so parity decisions are based on targeted evidence from the correct tier.

## Working Order

1. Creative tabs and content taxonomy
   - Establish source-style NTM creative categories before adding large item batches.
   - New active content must be assigned to the correct source category when it is registered.
   - Do not use a single catch-all tab as the long-term testing surface.

2. Core mod systems
   - Radiation exposure, fallout storage, shielding, damage sources, Geiger feedback, and cleanup items.
   - Explosion math, blast classes, fallout effects, structure damage, particles, and sound event routing.
   - Shared config, saved-data, networking, client/server separation, and performance guardrails.

3. Shared machine and recipe frameworks
   - Block entity ticking, inventories, sided automation, menu sync, screens, recipe serializers, JEI categories, sounds, and animation hooks.
   - Burner Press remains the pilot machine until its framework is source-parity enough to reuse.

4. Resource and data generation standards
   - Models, blockstates, loot tables, tags, language keys, recipes, and validation scripts must exist before broad content expansion.
   - Placeholder assets or recipes are defects unless explicitly marked temporary in the parity audit.

5. Content families
   - Add items, blocks, materials, machines, weapons, and worldgen in original progression groups.
   - Each family needs texture, name, tab placement, recipe behavior, drops, sounds, and parity-audit status before it is treated as alpha-ready.

## Immediate Priority

The next passes should be:

1. Run the stability gate before and after each framework pass.
2. Keep creative tabs source-style and avoid broad new content while core systems are temporary.
3. Audit and stabilize radiation as a reusable hazard-backed system.
4. Audit and stabilize explosion math before adding more bombs.
5. Finish Burner Press as the first reusable machine framework.
6. Expand items only inside those established systems.

## Stability Gate

Use `tools/stability-gate.ps1` as the repeatable check before calling a framework pass stable. It runs parity resource validation, build, data generation, and a runtime log scan for missing assets, broken recipes, and client-only server class loading patterns.

Manual client and dedicated-server launches are still required for visual parity and gameplay feel; the gate is a baseline, not a substitute for review.

## Current Framework Baseline

- Config is grouped into source-style system families while preserving current alpha defaults.
- Active alpha radiation sources are registered through the internal hazard registry, with the legacy emitter interface retained as a compatibility fallback.
- Radiation saved data has explicit get, set, add, remove, clear, and fallout-decay operations, with `ChunkRadiationService` providing the source-shaped chunk API for get, set, increment, and decrement.
- The hazard framework now includes the original major hazard families structurally, though only radiation has meaningful active gameplay behavior in the alpha.
- HBM typed payloads are registered for radiation sync, machine GUI actions, and client effect events; HUD/effect handling remains intentionally inert until the source-parity client pass.
- Prototype Nuclear Charge detonates through the server-side HBM explosion service.
- Burner Press output merging, sided automation, source slot layout, fuel burn storage, speed ramp, press delay, and retracting state now sit on reusable machine helpers.

## Rule

If a new item requires a system that is still temporary, port the system first. If the system cannot be ported yet, mark the item or recipe as pending instead of inventing substitute behavior.
