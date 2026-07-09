# Porting Status

## Implemented

- Official NeoForge 1.21.1 ModDevGradle foundation.
- `hbm` metadata and package structure.
- Registry shell for primary NeoForge object types.
- Radioactive block/item abstraction.
- Player radiation exposure saved data.
- Chunk fallout saved data.
- Geiger counter and RadAway items.
- Burner Press block entity, menu, and client screen.
- Burner Press item-handler capability with slot-restricted automation.
- Burner Press parity pilot with original GUI texture, original slot coordinates, stamp-gated recipes, stored solid-fuel burn time, and original press sound asset.
- Prototype Nuclear Charge with fallout.
- Maintained JSON resources for the first alpha content slice.
- Original HBM `assets/hbm` tree imported for all builds.
- Reference index workflow with canonical hierarchy documentation.

## Verification

- JSON/resources validated with `tools/validate-parity.ps1`.
- `gradlew build` passes with Java 21.
- `gradlew runData` passes.
- Reference paths can be checked with `tools/hbm-reference.ps1 tiers`.
- Latest packaged jar: `build/libs/hbm-0.1.0-alpha.jar`.

## Known Gaps

- Original textures, sounds, legacy language files, manuals, structures, and model assets are now present. Active alpha models use original HBM textures where direct matches exist.
- Radiation now uses a custom `hbm:radiation` damage type; damage tags and deeper protection interactions are still pending.
- No packet-based client radiation HUD yet; the Geiger counter is the first readout path.
- JEI integration is dependency-ready but no custom recipe category has been implemented.
- Burner Press processing now uses the custom `hbm:burner_press` recipe type with explicit stamp requirements.
- Ore worldgen JSON is present; runtime in-world placement validation is still pending a manual client pass.
- Fluids, missiles, advanced explosives, energy networks, and legacy machine families are still staged work.
