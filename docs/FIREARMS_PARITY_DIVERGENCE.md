# Firearms: Approved Parity Divergence

## Decision

The NeoForge firearm subsystem is an intentional behavior and architecture divergence from HBM 1.7.10. Future parity work must not replace it with the legacy gun handlers.

HBM identity remains authoritative for weapon names, visual design, ammunition themes, sounds, progression hooks, and special effects. Modern handling follows the accessible-tactical direction demonstrated by Superb Warfare commit `9b5284f4`: server-authoritative fire timing and ammunition, ADS, recoil, fire modes, staged reloads, physical trajectories, headshots, movement weight, and layered client effects.

Superb Warfare is a behavioral and GPL code reference only. Its models, textures, animations, and sounds are prohibited from `assets/hbm` unless separately authorized. Adapted code must identify the source and commit in `THIRD_PARTY_NOTICES.md`.

## Protected Framework Decisions

- Built-in guns and ammunition are schema-versioned datapack definitions under `data/hbm/guns` and `data/hbm/ammo`.
- Ordinary rounds use lightweight swept server trajectories; they are not one entity per bullet or pellet.
- Slow visible explosives use registered projectile entities and the existing HBM explosion service.
- Clients request inputs and commands but never submit damage, hit results, ammunition counts, fire timing, or projectile direction.
- Magazine ammunition is transferred at its insertion phase. Sequential reloads transfer one round per loop and may be interrupted only between rounds.
- Client prediction is cosmetic. The server owns ammunition, action state, damage, and correction.
- Exceptional weapons must use a reusable `GunBehavior` or ammunition effect rather than a weapon-specific parallel framework.
- Partial armor penetration modifies only the armor reduction calculation. It is not implemented as a second damage event and does not inherit Minecraft's full armor-bypass tag.
- Firearm damage owns its cadence and sets post-hit immunity to zero so shotgun pellets and validated automatic fire are not discarded by vanilla invulnerability frames.

## First Vertical Slice

| Weapon | HBM identity source | Modern handling baseline | Required ammunition |
| --- | --- | --- | --- |
| Target Pistol | Original HBM | MP-443-style semi-auto presentation and handling | .22 LR FMJ, .22 LR AP |
| StG 77 | Original HBM | AK-47-style semi/automatic | 5.56 FMJ, 5.56 AP |
| SPAS-12 | Original HBM fallback; the supplied Neo archive has no SPAS set | M870-style pump and interruptible shell loading | 12-gauge buckshot, 12-gauge slug |
| Congo Lake | Original HBM | M79-style arc with the HBM four-round sequential model | 40mm HE, 40mm HEAT |

The active models, textures, sounds, animation translations, hashes, and source paths are tracked in `docs/WEAPON_ASSET_PROVENANCE.md`.

## Dependency Boundary

The approved baseline is NeoForge `21.1.235`, KotlinForForge `5.10.0`, GeckoLib `4.7.5`, and Curios `9.2.0+1.21.1`. The project does not depend on the Superb Warfare jar, KSP module, SimpleBedrockModel, vehicles, perks, or unrelated compatibility mods.

## Temporary Pilot Progression

The four guns are creative/test-only until their assembly-machine progression is implemented. All eight ammunition types must remain survival-craftable for testing. The pilot steel-ingot and ammunition-stamp crafting recipes are explicitly temporary access paths; they must be replaced only when the corresponding HBM material and machine progression is active, never removed in a way that makes the ammunition chain unreachable.

## Release Gates

Roster expansion is blocked until the four-weapon slice passes:

- dependency build, data generation, GameTest, parity validation, weapon validation, and dedicated-server launch;
- malformed and semantically invalid definition reload rollback;
- malicious and stale packet rejection plus server fire-rate enforcement;
- magazine and sequential reload interruption without duplication or deletion;
- first-person, third-person, inventory, dropped-item, ADS, sprint, reload, empty-state, FOV, and shader-loader visual inspection;
- multiplayer latency checks and the stated StG 77/SPAS-12 tick-time profile.

Static validation or successful compilation does not satisfy visual, multiplayer, performance, or dedicated-server gates.
