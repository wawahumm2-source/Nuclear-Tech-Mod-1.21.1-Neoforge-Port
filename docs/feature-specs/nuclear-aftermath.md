# Nuclear Aftermath: Source Contract

## Scope

This feature ports the terrain and client-effect aftermath of Tier 1 HBM nuclear explosions. It applies to the Prototype Nuclear Charge and Little Boy test carrier. It does not claim full parity for every HBM bomb, flash shader, shrapnel projectile, crater biome, or source radiation bypass.

## Tier 1 Evidence

- `EntityNukeExplosionMK5`: Little Boy uses a terrain radius of `120`, doubles the ray strength to `240`, then spawns `EntityFalloutRain` at `radius * 2.5` (`300`).
- `EntityNukeExplosionMK5`: for ten ticks, bypass radiation uses `2,500,000 / (tick * 5 + 1)`, inverse-square distance loss, and summed intervening block blast resistance inside radius `240`.
- `EntityNukeExplosionMK5`: on every active explosion tick, `ExplosionNukeGeneric.dealDamage` applies the line-of-sight nuclear blast wave across radius `240`, with linear damage from a cap of `250`, five seconds of fire, and outward knockback. There is no separate source heat-wave class.
- `EntityFalloutRain`: processes the top three solid blocks in each column, deposits fallout, transforms terrain, and creates fire only from flammable surfaces inside `65%` of its fallout radius at a one-in-five roll.
- `FalloutConfigJSON`: stone, iron, sand, ground, and grass become `sellafield_slaked` in five-percent bands; the closest band is metadata `9` and the outer band is `0`.
- `FalloutConfigJSON`: coal can become Sellafield diamond/emerald ore, diamond becomes radioactive-gem ore, inner uranium becomes scorched uranium or Schrabidium ore, and bedrock becomes Sellafield bedrock.
- `FalloutConfigJSON`: logs become `waste_log`, planks become `waste_planks`, inner leaves/plants/vines are removed, outer leaves become `waste_leaves`, and outer sand has a five-percent trinitite conversion.
- `BlockSellafieldSlaked`: uses the four original Sellafield Slaked textures, a deterministic coordinate texture variant, and brightness `1 - stage / 15`.
- `ExplosionNukeAdvanced`: its vapor pass uses ellipsoid column bounds and `vaporDest` removes `BlockLiquid`; MK3 runs that pass at `2.5 *` its crater radius. MK5 Little Boy does not call it directly.
- `EntityNukeTorex` and `RenderTorex`: use the original `particle_base.png` cloudlet and `flare.png` flash assets, a 100-tick flash, a 150-tick shock front, 15 ticks of hurt-camera feedback after shock arrival, an uncapped rising core, torus growth, and a long-lived cloud simulation.
- Reloaded 1.12.2 `EntityNukeCloudSmall` and `RenderSmallNukeMK4`: use `mush.obj`, eleven diffuse fireball stages, eleven emissive lightmaps, radius-scaled width, continuous texture rise, and lifetime `max(300, 0.55 * (radius + 16)^2)` ticks.

## Modern Safety Rules

- Terrain and Fallout Rain jobs run only on the main server thread and persist their cursor state.
- A registered NeoForge ticket controller force-loads only the bounded active chunk frontier. It validates persisted owners on level load and releases every ticket after the associated local work completes.
- Source wildfire placement is configurable and defaults off. Enabling it uses the inner-65-percent, one-in-five source rule and the non-spreading short-lived HBM fire block.
- The Prototype Nuclear Charge retains its low-yield chunk-radiation pilot. Little Boy receives the ten-tick source radiation burst plus physical fallout terrain.
- Little Boy defaults to the original `452389`-point MK5 generalized spiral and source resistance curve. Immutable primitive snapshots and two bounded workers accelerate the math without accessing Minecraft state off-thread; the optional hybrid radial planner is reserved for weak hardware.
- The source top-three-solid scan is preserved directly: converted wood and vegetation do not consume depth, so the same column naturally continues through lower trunks until three true terrain solids are reached.
- Tier 1's placed `fallout` model uses `blocks/ash.png`; `items/fallout.png` is only the dropped item identity. The port keeps those models separate, while visible rain uses the source `textures/entity/fallout.png` as vertically scrolling local weather sheets.
- The active client cloud follows Reloaded 1.12.2's fixed-mesh presentation: the original `mush.obj` renders with the source eleven-stage diffuse and emissive texture sequence, radius-scaled cap width, and continuous rising texture coordinates. Early ground cloudlets retain Reloaded's placement, lifetime, palette, fade, and scale curves behind client-quality caps.
- Large yields use Reloaded 1.12.2's radius lifetime formula `max(300, 0.55 * (radius + 16)^2)` ticks; Little Boy radius `120` lasts `10172` ticks and fades during its final ten percent. This client-only persistence does not extend fallout processing, damage, fire, terrain work, or server jobs.
- Little Boy's default resistance exponent uses the source doubled strength (`240`), range (`120`), and depth multiplier `1`. The configurable quadratic downward bias now applies only to the optional hybrid mode.
- Blast terrain conversion is separated from Fallout Rain scheduling. Its deterministic chunk order is prepared without mutating resistance inputs, excavation finishes first, then source-ordered Sellafield/waste conversion applies. Fallout deposits and radioactive weather remain a later event.
- The conversion mapper includes Sellafield bedrock and the source diamond, emerald, scorched uranium, Schrabidium, and radioactive-gem ore outcomes. Converted rules consume depth only when Tier 1 marks them `sol(true)`; petrified wood and removed vegetation therefore do not prematurely stop the top-three-solid scan.
- Little Boy starts a separate connected-water resolution pass after crater excavation. It force-loads a bounded active frontier and surveys every water-tagged fluid state through the `300`-block Fallout Rain radius plus a two-block sentinel ring before changing any liquid. Fully enclosed components no larger than the configured volume cap evaporate as one classified target set. Boundary-connected or oversized components convert to HBM contaminated water inside the affected radius. Refill begins only when converted water exposes a horizontal face whose sampled air line reaches the crater interior, then uses that connected column's real surface height; mere horizontal-radius overlap or an isolated cave cannot flood a sealed crater. Waterlogged hosts participate in connectivity and are preserved but drained during mutation; lava is not changed by this water-specific pass. Mutation and refill cursors persist; a pre-mutation survey safely restarts on reload.
- Grass and mycelium inside the source Sellafield bands follow Tier 1's narrower material rules. Surviving grass outside those bands becomes Waste Earth as an approved Reloaded/Waldemar-informed extension.
- Radon, Dense Radon, and Tomb Radon are independent source gas hazards. Little Boy does not spawn them.
- WMD `0.1.2` was inspected only as a visual benchmark. Its CurseForge project is All Rights Reserved, so this port does not copy its code, compute shaders, textures, sounds, or particle data. The active cloud uses the selected Reloaded 1.12.2 source assets and timing instead.
- Alex's Caves was inspected as a public timing reference for a cinematic flash and pressure impact. The HBM implementation remains independently written: it uses a complete immediate local white-out with a configurable forty-tick hold and sixty-tick blindness fade, then a short raw positional tremor only when the visible pressure front reaches the player. Tier 1 `flare.png` remains a world-space event asset and is never drawn across the screen. Minecraft's screen-effects setting and HBM's existing flash/shake controls remain authoritative client opt-outs.

## Acceptance Checks

- Little Boy creates a progressive, source-shaped Sellafield/waste aftermath without uncontrolled vanilla wildfire.
- The active MK5 blast wave repeats source-style line-of-sight damage and five-second ignition while its terrain job runs; its bypass-radiation pulse ends after ten ticks.
- Enclosed free-water bodies disappear only after whole-component classification. Persistent lakes and oceans survive and contaminate inside the affected radius, but refill occurs only through an exposed horizontal crater opening; no water is mutated outside the hard horizontal boundary.
- Prototype terrain removal remains immediate at its configured low yield.
- Sellafield Slaked uses original textures, coordinate variants, and stage tinting.
- Sellafield bedrock and all five source converted ore outcomes render distinctly and retain their fallout stage.
- Fallout deposits expose players and mobs through the existing radiation framework.
- Full, Reduced, and Minimal client quality modes preserve the source event timeline while bounding particle work.
- A visible client inside the flash range is fully white-out blinded immediately; the pressure front then produces positional tremor without hurt-camera rotation, while large terrain work publishes a restrained local progress line.
- Fallout Rain weather sheets begin after terrain excavation, remain inside the server radius, and stop when the server job stops reporting progress.
- Fallout Rain includes local rain audio and source rain sheets without the rejected stray cloud-billboard aerosol or any Radon creation.
- In ordinary stone, Little Boy follows the source depth produced by multiplier `1`; optional hybrid depth bias is not part of the parity default.
