# Alpha Roadmap

This roadmap keeps the first playable NeoForge alpha focused. The goal is not full 1.7.10 parity yet; the goal is a stable survival loop that proves the port architecture.

System implementation details are tracked in `docs/CORE_SYSTEM_UPDATE_PLANS.md`.

## Milestone 1: Buildable Foundation

- Confirm Java 21 is installed locally.
- Run `gradlew build`.
- Run `gradlew runClient`.
- Run `gradlew runServer`.
- Run `gradlew runData`.
- Package a jar that loads in a clean NeoForge 1.21.1 instance.

## Milestone 2: Creative Tab And Registry Framework

- Replace the single catch-all alpha tab with source-style NTM categories.
- Keep active content assigned to the correct category as it is ported.
- Establish registry naming rules for blocks, items, machines, templates, weapons, bombs, and parts.
- Add tab placement to the parity audit for new active content.

## Milestone 3: Radiation Core

- Add damage-type tags and armor interactions for `hbm:radiation`.
- Add synced client exposure data.
- Add Geiger counter sound feedback.
- Add configurable armor shielding rules.
- Add save/reload tests for player exposure and chunk fallout.

## Milestone 4: Explosion Framework

- Port reusable explosion math before adding more bombs.
- Separate server-side blast calculation from client-only particles, screenshake, and sounds.
- Add config guardrails for radius, fallout, block damage, and dedicated server performance.
- Keep Prototype Nuclear Charge as a test carrier until the framework is stable.

## Milestone 5: Machine Architecture

- Add shared machine helpers for inventory, progress data, ticking, and menu sync.
- Expand side-aware insertion/extraction rules as new machine families are added.
- Introduce fluid and energy interfaces after one machine family is stable.
- Keep client screens thin; server block entities own the truth.
- Finish Burner Press as the reference-quality machine before broad machine expansion.

## Milestone 6: Materials And Progression

- Port the first HBM material chain: uranium, lead, steel, graphite, fuel pellets.
- Replace placeholder textures with original-compatible or newly authored assets.
- Expand tags for storage blocks, dusts, ingots, ores, fuels, and shielding.
- Expand the data-driven Burner Press recipe set only after the required material items exist.

## Milestone 7: World And Hazards

- Runtime-test ore generation.
- Add radioactive structures and loot only after ore placement is stable.
- Expand fallout behavior from stored chunk values into visible gameplay effects.
- Add dedicated server smoke checks for chunk unload/reload.

## Milestone 8: Explosives And Weapons

- Replace the prototype charge with layered HBM explosive logic.
- Keep blast calculation server-side.
- Add particles, screenshake, and sound as client-only effects.
- Add guardrails for config limits and dedicated server performance.

## Release Bar

The first alpha is ready when a fresh world supports mining uranium, crafting the first materials, using the Burner Press, reading/removing radiation exposure, detonating a prototype charge, saving/reloading, and joining a dedicated server without client-only class loading.
