# Modern HBM Gun Framework: Milestone Status

Status date: 2026-08-10

This ledger separates implemented code from verified behavior. The first four-gun slice is structurally playable, but roster expansion remains blocked until the open manual and multiplayer gates are completed.

## Implemented Scope

- Four data-driven pilot guns: Target Pistol, StG 77, SPAS-12, and Congo Lake.
- Eight survival-craftable ammunition profiles: FMJ/AP pistol and rifle rounds, buckshot/slug shells, and HE/HEAT 40 mm grenades.
- Server-authoritative inputs, ammunition, fire timing, spread, damage, reload phases, fire modes, and stack identity.
- Versioned persistent gun state plus transient per-player weapon sessions.
- Lightweight swept trajectories for bullets and pellets; visible entities only for grenade-class projectiles.
- Headshots, partial armor penetration, fragile-block impacts, HBM explosion routing, movement weight, ADS, recoil, tracers, casings, smoke, flashes, screenshake, and HBM sound events.
- Atomic datapack definition reloads, reusable behavior registration, Curios ammo-provider integration, and a shared GeckoLib/OBJ renderer.
- Original-HBM-derived pilot geometry, textures, sounds, and animation translations with per-asset provenance.
- GPL-attributed Superb Warfare presentation architecture: synthetic camera/root hierarchy, eased ADS, continuous walk/sprint/draw motion, nonlinear recoil curves, player-skin arms attached to rotated model bones, mechanism motion, and model-local muzzle flare.
- The first-person renderer cancels GeckoLib's conventional half-block item-centering offset for already-centered HBM OBJ coordinates. Muzzle flashes now use explicit HBM muzzle coordinates and bounded sizes rather than misusing a rotation pivot as a position.
- Superb-style dynamic segmented crosshair, local hit/headshot feedback, and a bottom-right gun/ammunition panel rendered from HBM/vanilla primitives and item icons. Impact packets now carry the server-resolved shooter entity ID so feedback cannot be client-claimed.
- HBM-authored presentation rigs for all four pilots. The failed Target Pistol profile and its near-camera world-space flash were removed; this corrected replacement build still requires in-game visual acceptance.

## Automated Gate Evidence

| Gate | Result | Evidence |
| --- | --- | --- |
| Dependency compilation | pass | NeoForge `21.1.235`, KotlinForForge `5.10.0`, GeckoLib `4.7.5`, and Curios `9.2.0+1.21.1` compile together on Java 21. |
| Unit tests | pass | 15 transformed NeoForge/FML JUnit tests: codecs/validation, registry rollback, state migration, stack reminting, magazine bounds, per-shell interruption, recoil recovery, packet sequencing/rate limiting, and exact 650 RPM accumulation. |
| GameTests | pass | 7 live server tests: conventional damage, buckshot/slug projectile counts, Congo Lake gravity arc, HE/HEAT entity impacts, headshot multiplier, live armor penetration, and the simultaneous-user trajectory profile. |
| Ballistics tick budget | pass | The 16 StG 77 plus 8 SPAS-12 profile averaged `0.16317609756097562 ms` across 205 measured ballistics ticks in the corrected replacement build, below the `5 ms` gate. Profiling is dormant outside an explicitly reset test window. |
| Data generation | pass | `gradlew runData` completed successfully. |
| Packaged build | pass | `gradlew build` completed successfully. |
| Parity validator | pass | Active JSON, HBM model textures, and HBM sound references resolve. |
| Weapon validator | pass | 4 gun definitions, 8 ammo definitions, HBM sounds, animation-to-OBJ bone binding, item models, survival ammunition chain, and four legacy source sets validate. |
| Dedicated-server smoke | pass | Server loaded KotlinForForge, GeckoLib, Curios, and HBM and installed weapon definition generation 1 with 4 guns and 8 ammo profiles. |
| Client initialization smoke | pass | Client resource reload, OpenAL, texture atlases, and weapon definition generation completed with zero client error/fatal lines. This is not visual QA. |
| Package contamination audit | pass | The corrected `hbm-0.1.0-alpha.jar` contains 12,349 entries, SHA-256 `d7fb3d673fd104ccbc448b9ec1873605e2539f3feb61dc97bf007ed1091b15e5`, and no Superb Warfare, SimpleBedrockModel, or `META-INF/jarjar` paths. |

## Quick Load

Double-click `Quick Load Latest Build.bat` in the project root to launch the current workspace build. `Quick View Latest Build.bat` remains as a backwards-compatible alias. Both use `tools/quick-view-latest-build.ps1`, which discovers and verifies Java 21 from the project-local toolchain, `JAVA_HOME`, `PATH`, or common Windows JDK locations before invoking `gradlew runClient`.

Pass `-InfoOnly` from a terminal to verify the detected Java and latest jar without launching Minecraft. Pass `-RebuildFirst` to force a complete build before launch.

## Open Release Gates

- Inspect the corrected Target Pistol in first person and third person across hip-fire, ADS, firing, dry-fire, empty slide lock, sprint, and staged reload. Confirm that the gun is large/readable without clipping, both arms remain attached, the segmented crosshair and bottom-right HUD respond correctly, and the bounded model-local muzzle flash remains at the barrel. Then perform the same checks for StG 77, SPAS-12, and Congo Lake plus GUI, dropped-form, FOV-extreme, and shader-loader checks. Static bone validation cannot prove transform quality.
- Run two-player tests at simulated 100-200 ms latency for automatic fire, reload cancellation, switching, death, reconnect, and dimension transfer.
- Expand live GameTests to cover reload/inventory transactions with a fully negotiated test client, death/drop/save/reload, chunk unloading, and inventory-full unloading. The pure state suite covers between-shell interruption; Minecraft's mock server player cannot negotiate Curios or HBM payloads and is not a valid substitute for this gate.
- Exercise malicious held-stack and impossible-timing payloads against a real connected client. Pure tests already cover stale sequence rejection and the eight-packets-per-tick limit, but they do not replace protocol-level abuse testing.
- Replace the Original HBM SPAS-12 fallback if a complete, authorized Neo Edition SPAS asset set is located. The supplied Neo archive contains no such set.
- Replace temporary steel-ingot and ammunition-stamp access recipes when the final HBM material and assembly-machine progression is active.
- Add final assembly-machine recipes before moving the four guns out of creative/test-only availability.

## Expansion Rule

Do not expand the firearm roster while any visual, multiplayer, or performance gate above remains open. Additional weapon families must arrive in batches of four to six and express exceptions through reusable `GunBehavior` or ammunition effects.
