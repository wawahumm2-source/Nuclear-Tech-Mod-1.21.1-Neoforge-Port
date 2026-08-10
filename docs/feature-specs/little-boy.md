# Little Boy Large-Explosion Pilot

Status: source-shaped MK5 terrain and client presentation candidate; manual comparison review pending
Classification: source-backed large-explosion test wrapper

## Tier 1 Evidence

- `NukeBoy` is the original `nuke_boy` block and uses the Little Boy asset family.
- `BombConfig.boyRadius` defaults to `120`.
- The original block drives `EntityNukeExplosionMK5.statFac(world, BombConfig.boyRadius, ...)`.
- `EntityNukeExplosionMK5` doubles the terrain strength, uses `length = boyRadius`, calls nuclear direct damage at `length * 2`, and delegates terrain work to its batched MK5 ray engine.
- `ExplosionNukeRayBatched` derives `2.5 * pi * strength^2` generalized-spiral points: `452389` at Little Boy's doubled strength of `240`.
- `ExplosionNukeGeneric.dealDamage` uses maximum damage `250`, linear falloff, five seconds of fire, and normalized knockback.
- During its first ten ticks, `EntityNukeExplosionMK5` applies `2,500,000 / (tick * 5 + 1)` bypass radiation inside `length * 2`; distance is inverse-square and intervening block blast resistance attenuates the dose.
- Original, Reloaded, Waldemar, Well-Forged, Neo Edition, and Rebirth all retain `boyRadius = 120`. Modernized does not currently implement Little Boy. No checked reference provides a larger default yield.
- No checked reference connects Little Boy to the three Radon gas blocks. The mushroom cloud is visual, and `EntityFalloutRain` is the separate post-excavation radioactive-weather carrier.

## Current Pilot Contract

- `hbm:nuke_boy` is creative-only and uses the original Little Boy block texture.
- Empty-hand interaction and redstone are developer arming controls.
- The test profile keeps the source radius `120`, MK5 terrain strength `240` capped at `120` blocks of travel, direct-damage radius `240`, and maximum damage `250`.
- The first ten server ticks apply the source bypass-radiation pulse to players and mobs. The pulse is persisted with the explosion job so save/reload cannot restart or duplicate it.
- Default mode uses the source MK5 generalized-spiral sequence, `452389` points, resistance denominator at strength `240`, range `120`, and depth multiplier `1`. Pure worker batches read an immutable resistance snapshot; the optional hybrid radial field is retained only for weak hardware.
- Source-density mode uses the Tier 1 depth multiplier of `1` with no directional bias. The configurable `2.5` downward multiplier belongs only to the optional lower-density hybrid mode and does not claim crater parity.
- Blast-driven Sellafield/waste conversion and tree charring begin only after crater excavation completes. Fallout Rain begins after that conversion pass, then handles fallout deposits, radioactive weather, rain sound, and continuing environmental exposure.
- The dedicated Little Boy client event now uses Reloaded 1.12.2's `EntityNukeCloudSmall` and `RenderSmallNukeMK4` as the visual authority. It renders the original `mush.obj` `Stem` and `Ball` parts, the exact eleven `fireball` texture stages, the eleven matching emissive lightmaps, source radius scale `radius * 0.025`, first-100-tick cap widening, and the source UV-rise equation. The former Torex particle sculpture and 25-frame billboard bridge are not active in this path, preventing two mushroom clouds from occupying the same blast.
- Reloaded's first-200-tick expanding ground-cloudlet event is retained with the original `particle_base.png`, 50-tick fade/scale behavior, and `age * 2` radial envelope. Spawn totals are quality-bounded for modern clients, while the separate gray pressure wave, delayed impact audio, flash/blindness overlay, and camera response remain approved modern presentation wrappers.
- Total lifetime follows Reloaded 1.12.2's `max(300, 0.55 * (radius + 16)^2)` formula: radius `120` lasts `10172` ticks, approximately eight minutes and twenty-nine seconds. The mesh advances through Reloaded's radius-scaled texture ages and keeps its source UV motion through the final visible tick; a modern fade occupies the final ten percent. There is no five-minute minimum or independent lifetime setting.
- While the persisted Fallout Rain job is active, nearby clients see the source `textures/entity/fallout.png` as vertical scrolling weather sheets inside the job's source radius of `300` and hear local weather-rain audio. The rejected billboard aerosol is absent; Radon remains a separate block hazard.
- Source-density defaults capture up to `128` chunk sections per tick within a shared `20 ms` round-robin lane, calculate ordered `8192`-ray batches on two bounded workers, and remove up to `8192` blocks within an `18 ms` excavation lane and `35 ms` total scheduler cap. A four-lane ticket allocator keeps simultaneous planning, crater, water, and fallout work moving without exceeding the global force-load cap.

## Manual Verification

- Verify the original Reloaded mesh is upright at the blast origin, with its `Ball` visibly connected above its `Stem` and no second Torex cloud intersecting it.
- Verify Little Boy remains visible for approximately eight minutes and twenty-nine seconds at radius `120`, advances from yellow-white through the eleven source fireball stages, retains visible UV rise late in life, and fades smoothly during its final ten percent.
- Verify the cap widens during the first 100 ticks and the first-200-tick ground cloudlets expand across the terrain without becoming the gray pressure front.
- Verify the flash begins with a short hard-white impact, decays into a restrained afterimage, and keeps the source world flare visible for roughly 100 ticks.
- Verify shock arrival uses a centered damped impulse with no cumulative camera drift, and that the terrain-following pressure front is gray without a flat horizontal disc.
- Verify direct radiation affects players and mobs for exactly ten ticks and is attenuated by distance and intervening blast resistance.
- Verify rain, rain sound, and deposits do not begin until crater and blast-terrain work finish, and that no unrelated cloud billboards appear inside the rain.
- Verify the crater reaches the source `120`-block radial envelope and an approximately `50`-block center depth in ordinary stone without unacceptable tick spikes.
- Verify placed fallout uses Tier 1 `ash.png`, its dropped item uses `falloutitem`, and visible Fallout Rain has no atlas warnings.
- Verify the approved connected-water pass reaches the hard `300`-block horizontal radius plus its non-mutating sentinel ring. A fully enclosed body below the configured cap disappears only after classification; an ocean or boundary-connected lake remains and becomes contaminated inside the affected area. It refills the crater only when a horizontal fluid-to-air opening has a clear air line into the excavated cavity, using that connected column's surface height. Flowing water, aquatic plants, and waterlogged hosts must not split one body into false components; waterlogged hosts are preserved but drained, and lava remains untouched.

## Deferred

- Original five-component assembly, NukeBoy menu, full 3D renderer, and source crater biomes.
