# Core System Update Plans

Source plan derived from `docs/CORE_MOD_SYSTEMS_OFFICIAL_PARSE.md`.

The port should update systems in framework order. Each system below lists the purpose, update approach, dependencies, and acceptance checks. Content should only be added when its owning system is stable enough to support parity behavior.

## 1. Creative Tabs And Registry Taxonomy

Goal:
- Match the original NTM content organization and prevent future content from being dumped into temporary tabs.

Update Plan:
- Keep source-style tabs: blocks, consumables/gear, machine items/fuel, machines, missiles/satellites, bombs, resources/parts, templates, weapons/turrets.
- Add a registry placement rule: every new block/item declares its intended tab family during review.
- Add missing empty tabs for missiles/satellites and weapons/turrets before those content families are ported.
- Add a parity-audit column or note for tab placement.
- Add a small validation script/check that active registered items appear in at least one intended HBM tab.

Dependencies:
- Item and block registries.
- Modern language keys.

Acceptance Checks:
- Creative inventory shows source-style tab names.
- Active alpha content is not in a catch-all tab.
- New content cannot be considered alpha-ready without tab placement.

## 2. Config Framework

Goal:
- Replace scattered constants with a NeoForge config structure matching the original config families.

Update Plan:
- Split `HbmConfig` into sections or companion classes matching original families: general, radiation, bomb, machine, world, weapon, structure, fallout.
- Keep defaults close to original values where known.
- Make server-authoritative values explicit.
- Add reload-safe getters so systems do not cache stale config values.
- Document each config key with the original class it came from, such as `RadiationConfig`, `BombConfig`, or `WorldConfig`.

Dependencies:
- None; this should be one of the first framework updates.

Acceptance Checks:
- `gradlew runData` and dedicated server launch load configs cleanly.
- Radiation, explosion, and worldgen code read config values through one pattern.
- Config defaults are listed in docs for parity review.

## 3. Damage Sources, Player/Living State, And Hazard Registry

Goal:
- Rebuild the original hazard foundation before expanding radioactive, hot, asbestos, explosive, coal, hydroactive, or digamma content.

Update Plan:
- Create modern damage types for original `ModDamageSource` families that are active or soon needed.
- Build `HazardType` equivalents for radiation, explosive, hot, asbestos, coal, hydroactive, blinding, and digamma.
- Build a `HazardRegistry` that maps items, tags, or exact stacks to hazard data.
- Build inventory and dropped-item hazard ticking hooks.
- Move current item-specific radiation behavior into registry-driven hazard application.
- Add armor/protection hooks equivalent to original resistance behavior.

Dependencies:
- Config framework.
- Saved player/living state.
- Networking for client feedback.

Acceptance Checks:
- Geiger, RadAway, radioactive items, and waste barrel use the shared hazard/radiation system.
- Radiation/hazard values can be audited in one registry.
- Dropped radioactive items and carried radioactive items behave through the same framework.

## 4. Radiation, Chunk Fallout, Contamination, And Pollution

Goal:
- Replace the prototype radiation implementation with world/chunk saved systems inspired by `ChunkRadiationManager`, `ChunkRadiationHandler`, `ContaminationUtil`, and `PollutionHandler`.

Update Plan:
- Create server-side saved data for chunk radiation/fallout.
- Define APIs: get radiation, set radiation, increment, decrement, clear dimension, and tick decay/spread.
- Add player exposure accumulation from chunk radiation and item hazards.
- Add contamination effects for radiation first, then digamma/asbestos/lead/coal as later extensions.
- Add pollution saved data as a separate but similar world system.
- Add Geiger/dosimeter readouts against the unified state.
- Add cleanup items and block interactions after storage and ticking are stable.

Dependencies:
- Config framework.
- Hazard registry.
- Networking.
- Damage sources/player state.

Acceptance Checks:
- Chunk radiation survives save/reload and server restart.
- Player exposure survives death/respawn according to intended rules.
- Geiger readings match actual chunk/player values.
- Dedicated server does not load client-only radiation classes.

## 5. Networking And Typed Payloads

Goal:
- Establish modern NeoForge payload conventions before adding GUI buttons, machine sync, missiles, satellites, client effects, and recipe sync.

Update Plan:
- Create a central `HbmPayloads` registration class.
- Define channel/payload naming rules.
- Add payload families: player state sync, block entity state sync, GUI button actions, recipe/catalog sync, visual effects, sound triggers, explosion effects.
- Replace ad hoc sync with typed packets.
- Keep every payload side-specific and validation-heavy.
- Document payload ownership and server/client direction.

Dependencies:
- Config framework for packet-related toggles if needed.
- Player/living state.

Acceptance Checks:
- Client can join dedicated server without client-only class loading.
- GUI actions validate block position, player distance, and menu identity.
- Visual effect payloads never run server logic on the client.

## 6. Explosion And Bomb Framework

Goal:
- Build reusable explosion logic before adding more bombs.

Update Plan:
- Define an `HbmExplosion` service with server-side entry points.
- Model original responsibilities separately: radius/config, block allocation, block processing, block mutation, entity processing, player processing, fallout, EMP, vaporization, waste, drops, and SFX.
- Add `IBomb`-style interface for bomb blocks/items.
- Keep Prototype Nuclear Charge as a test carrier, not the final architecture.
- Add async/batched guardrails only after a simple server-safe version is verified.
- Add client payloads for particles, sound, screenshake, and knockback visuals.

Dependencies:
- Config framework.
- Damage sources.
- Networking.
- Sound/client-effect framework.
- Radiation/fallout saved data for nuclear effects.

Acceptance Checks:
- Explosion math is server-side.
- Config radius and lifespan limits work.
- Dedicated server can detonate test charge without client classes.
- Fallout/radiation effects are written through the shared world system.

## 7. Saved Data And Persistent World Systems

Goal:
- Provide a modern saved-data foundation for radiation, pollution, satellites, special pools, and later global systems.

Update Plan:
- Create reusable saved-data helpers for dimension/world state.
- Add versioned NBT/Codec serialization where practical.
- Create separate stores for radiation/fallout, pollution, satellites, and future global systems.
- Add lifecycle tests: world load, chunk load, chunk save, server stop, server restart.
- Keep data APIs independent from client rendering.

Dependencies:
- Config framework.

Acceptance Checks:
- Saved values persist after world reload and server restart.
- Missing/old data loads without crashing.
- No client-only imports in saved-data classes.

## 8. Machine And Inventory Framework

Goal:
- Make Burner Press the first reference machine, then extract reusable infrastructure for later machines.

Update Plan:
- Create shared machine helpers for inventory, sided automation, progress data, save/load, block updates, menu creation, and client sync.
- Add standard slot wrappers: fuel, input, output, upgrade, template/stamp, fluid container.
- Define a common button/action payload pattern for machines.
- Define a common screen-rendering rule: original GUI texture first, overlays only when source behavior is known.
- Add pollution-capable machine base after pollution framework exists.
- Refactor Burner Press onto shared helpers once helpers are stable.

Dependencies:
- Networking.
- Config framework.
- Recipe framework.
- Sound/client-effect framework.
- Fluid/energy frameworks for later machine families.

Acceptance Checks:
- Burner Press retains source slot layout and behavior.
- Wrong-side automation is rejected.
- Save/reload preserves inventory and progress.
- Shared helper use reduces duplication before the second machine family is ported.

## 9. Recipe And JEI Framework

Goal:
- Standardize machine recipes, validation, and JEI display before importing large recipe catalogs.

Update Plan:
- Create a shared recipe serializer pattern for HBM machine recipes.
- Add recipe validation checks for missing items, missing stamps/templates, invalid counts, and placeholder outputs.
- Add JEI category helpers for original GUI-style backgrounds.
- Add recipe-family audit statuses: exact, mapped, pending missing item, temporary scaffold.
- Add optional recipe sync payloads only if needed for dynamic/custom recipes.
- Continue `PressRecipes` only with active, valid item mappings.

Dependencies:
- Machine framework.
- Networking if dynamic recipe sync is required.
- Creative/resource taxonomy for item availability.

Acceptance Checks:
- `runData` catches invalid recipes.
- JEI shows active machine recipes.
- Missing original recipes are marked pending rather than replaced with invented substitutes.

## 10. Fluid And Pressure Framework

Goal:
- Recreate the original fluid identity, including pressure concepts, before chemical/refinery/reactor machine chains.

Update Plan:
- Define HBM fluid registry conventions.
- Model fluid amount and pressure as separate concepts where original behavior requires it.
- Add tank helpers for machines.
- Add sided fluid transfer helpers.
- Add fluid container item behavior only after the fluid model is stable.
- Map original `FluidType` names to modern fluids and tags.

Dependencies:
- Config framework.
- Machine framework.
- Networking for tank sync.

Acceptance Checks:
- Fluid tanks save/reload correctly.
- Fluid GUI gauges sync on client.
- Pressure-sensitive recipes/machines have a place to express pressure requirements.

## 11. Power And Energy Framework

Goal:
- Establish a NeoForge-compatible energy layer before power-consuming machines and fusion systems.

Update Plan:
- Define HBM energy units and conversion assumptions.
- Wrap NeoForge energy capability behind HBM helper interfaces.
- Add cable/network placeholders only after machine consumers exist.
- Add server-side energy storage and transfer helpers.
- Add config for conversion/compat behavior if needed.
- Avoid hardcoding energy behavior inside individual machines.

Dependencies:
- Config framework.
- Machine framework.
- Networking for GUI sync.

Acceptance Checks:
- Energy storage saves/reloads.
- Machine energy bars sync without custom one-off code.
- Future power network code can be added without changing every machine.

## 12. Sound, Particle, And Client Effect Framework

Goal:
- Rebuild source-style client effects without mixing client classes into server logic.

Update Plan:
- Create client-only effect dispatchers for particles, screen overlays, screenshake, and looping sounds.
- Create sound wrappers for machine loops, sirens, held items, explosions, and moving entities.
- Make server systems trigger typed effect payloads rather than directly touching client code.
- Add asset validation for active sound references.
- Add a manual visual/audio checklist for every active effect.

Dependencies:
- Networking.
- Sound registry.
- Explosion and machine frameworks.

Acceptance Checks:
- Dedicated server starts without client renderer/sound classes.
- Active sound events resolve to original assets.
- Machine loops and explosion effects can be stopped/updated cleanly.

## 13. Worldgen, Ores, Deposits, And Structures

Goal:
- Port configurable world generation as a system before relying on broad material progression.

Update Plan:
- Map original `WorldConfig` ore/deposit/structure toggles to modern config.
- Port ore/deposit placement in stages: ordinary ores, cave ores, bedrock ores, oil/depth deposits, geysers, meteorites, structures.
- Use modern configured/placed features and biome modifiers where possible.
- Add structure framework separately from ore framework.
- Add loot-table and block-state validation before structures are enabled.

Dependencies:
- Config framework.
- Saved-data framework for special world state if needed.
- Resource/data generation standards.

Acceptance Checks:
- New world generates expected active ores.
- Worldgen can be disabled via config.
- Dedicated server generation has no client dependencies.
- Structures remain off or pending until their NBT/procedural framework is stable.

## 14. Weapons, Ballistics, Missiles, And Satellites

Goal:
- Treat weapons and missiles as system families, not isolated items.

Update Plan:
- Do not add broad weapon items until configs, networking, entities, explosions, sounds, and client effects are ready.
- Port projectile/entity framework first.
- Port bullet/weapon config after base damage and effect systems exist.
- Port missile assembly only after multipart rendering, entity sync, and explosion framework exist.
- Port satellites only after saved-data and networking are stable.

Dependencies:
- Config framework.
- Damage sources.
- Networking.
- Explosion framework.
- Saved data.
- Sound/client-effect framework.

Acceptance Checks:
- Projectiles behave consistently on dedicated server.
- Missile/satellite state persists and syncs.
- Weapons use config-driven stats, not hardcoded prototype values.

## System Dependency Order

1. Creative tabs and registry taxonomy.
2. Config framework.
3. Damage sources and hazard registry.
4. Saved data.
5. Radiation, fallout, contamination, and pollution.
6. Networking and typed payloads.
7. Explosion and bomb framework.
8. Machine and inventory framework.
9. Recipe and JEI framework.
10. Fluid and pressure framework.
11. Power and energy framework.
12. Sound, particle, and client-effect framework.
13. Worldgen and structures.
14. Weapons, ballistics, missiles, and satellites.

## Near-Term Implementation Sequence

1. Finish config split and document original-default assumptions.
2. Build hazard registry and migrate active radioactive items/blocks onto it.
3. Add chunk radiation/fallout saved data and Geiger/RadAway integration.
4. Create typed payload conventions and move player radiation sync into them.
5. Design explosion service and migrate Prototype Nuclear Charge to it.
6. Extract Burner Press machine helpers after the above foundations are stable.
