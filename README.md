# Hbm's Nuclear Tech - NeoForge 1.21.1 Port

This repository is a clean NeoForge 1.21.1 port foundation for Hbm's Nuclear Tech.

The original 1.7.10 project remains the canon reference for names, mechanics, systems, assets, balance, credits, and licensing:

- Original repository: https://github.com/HbmMods/Hbm-s-Nuclear-Tech-GIT
- NeoForge MDK baseline: https://github.com/NeoForgeMDKs/MDK-1.21.1-ModDevGradle

Parity direction is tracked in `docs/PARITY_STRATEGY.md`.

## Quick View

Double-click `Quick View Latest Build.bat` from the project folder to launch the latest workspace build in the Minecraft client. The launcher automatically uses the bundled Java 21 runtime under `.tooling`.

## Current Alpha Slice

- NeoForge `1.21.1` ModDevGradle project targeting Java 21.
- Preserved mod identity: `hbm`, package root `com.hbm`, display name `Hbm's Nuclear Tech`.
- Core registries for blocks, items, block entities, menus, sounds, entities, fluids, and creative tabs.
- First radiation system:
  - player exposure saved in world data
  - radioactive blocks/items
  - chunk fallout storage
  - custom radiation damage type
  - Geiger counter readout
  - RadAway exposure reduction
- First machine: Burner Press.
  - two-slot block entity
  - server-side ticking
  - menu and client screen
  - data-driven `hbm:burner_press` recipes
  - slot-restricted NeoForge item-handler capability for automation
- First explosive: Prototype Nuclear Charge.
  - right-click or redstone detonation
  - configurable blast radius
  - fallout applied to surrounding chunks
- Original HBM assets imported under `assets/hbm`, with maintained modern blockstates, models, language, recipes, loot tables, tags, and ore worldgen.
- Optional JEI dependency configured for local runtime testing.

## Requirements

- Java 21 JDK
- Internet access for Gradle dependency resolution

Build:

```powershell
.\gradlew.bat build
```

Run client:

```powershell
.\gradlew.bat runClient
```

Run data generation:

```powershell
.\gradlew.bat runData
```

## Porting Direction

The first milestone is a playable alpha, not full parity. The next major systems should be ported in this order:

1. Harden radiation, hazards, protective equipment, and damage types.
2. Expand materials, ores, recipes, loot, and JEI categories.
3. Port additional machine families with shared inventory/fluid/energy architecture.
4. Add fluids and tanks.
5. Port explosives, fallout effects, missiles, and client rendering in layers.
6. Profile dedicated-server ticking and packet volume.

Detailed milestone tracking lives in `docs/ALPHA_ROADMAP.md` and `docs/PORTING_STATUS.md`.

## Licensing

The original project licensing files are included:

- `LICENSE`
- `LICENSE.LESSER`

Retain upstream credits when porting original assets, mechanics, and code.
