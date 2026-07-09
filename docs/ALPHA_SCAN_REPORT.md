# Alpha Scan Report

Date: 2026-07-08

## Runtime Verification

- `gradlew build` succeeds with Java 21.
- `gradlew runData` succeeds and loads the `hbm` mod through NeoForge registration.
- `gradlew runServer` reaches dedicated server startup: `Done (...)! For help, type "help"`.
- The built jar passes zip integrity validation.

## First Scan

- Fixed a startup blocker caused by copying vanilla Blast Furnace block properties into the Burner Press. The copied `lit` state did not exist on the custom block and broke NeoForge registration.
- Replaced copied vanilla block behavior with explicit custom properties for the Burner Press, Prototype Nuclear Charge, and Radioactive Waste Barrel.
- Corrected the Geiger click sound path to point at a sound file path rather than a vanilla sound event id.
- Added the missing Geiger click subtitle translation.

## Second Scan

- Removed the deprecated explicit event-bus option from the client menu screen subscriber.
- Confirmed NeoForge still subscribes the client screen registration class to the mod event bus.
- Tightened Burner Press input validation so automation and menus reject items without a matching `hbm:burner_press` recipe once a level is available.

## Remaining Notes

- The server smoke run uses a local dev server and times out intentionally after reaching startup; it is then stopped.
- Server log warnings about command ambiguity, NeoForge union asset URLs, and offline server mode are environmental or vanilla/dev warnings, not HBM mod failures.
- Client smoke scan no longer reports the HBM missing texture, invalid resource path, Geiger sound, or Burner Press recipe-book warnings observed during the first visual pass.
- A full manual client gameplay pass is still recommended for screen layout, model appearance, and interaction feel.
- After the client pass, the project direction changed to original-system parity. Original HBM assets are now imported and active alpha models should use original HBM textures where available.
- Burner Press parity pass: added a three-block-tall custom hitbox, NeoForge-baked original body/head OBJ renderer, original `gui_press.png`, original slot coordinates, solid-fuel burn storage, stamp-gated recipes, press/retract head movement, and the original `pressoperate.ogg` completion sound.
- The latest client smoke scan no longer reports Burner Press OBJ, missing texture, invalid resource path, sound, or recipe category warnings.
- Remaining Burner Press parity work: full original press recipe families, exact preheater acceleration behavior, expanded stamp material families, and exact original sound loop behavior.

## 2026-07-08 Burner Press Parity Build Check

- `tools/validate-parity.ps1` passes for JSON, HBM model texture references, and HBM sound references.
- `gradlew build` passes with Java 21 and produces `build/libs/hbm-0.1.0-alpha.jar`.
- `gradlew runData` passes.
- Manual client gameplay review is still needed for GUI feel, slot interaction, animation timing, and in-world behavior.

## 2026-07-08 Burner Press Repair Pass

- Corrected the Burner Press GUI to the original `176x202` layout with the source title, inventory label, player inventory, hotbar, machine slot, burn overlay, press overlay, and speed gauge positions.
- Split the inventory/crafting item model from the placed-world OBJ renderer. The item now uses the original `machine_press.png` identity texture instead of the tiny cube block model.
- Removed the non-parity ore-to-ingot Burner Press test recipes.
- Started the original `PressRecipes` port with active mapped recipes for iron plate, gold plate, steel plate, lead plate, and gold wire.
- Added JEI support for the active `hbm:burner_press` recipes so the mapped press catalog can be reviewed in-client.
- Updated the parity audit so Burner Press GUI/item status is not marked exact until visual confirmation is complete.
- Aligned the local JEI compile/runtime dependency path to the provided `jei-1.21.1-neoforge-19.32.0.359.jar`.
- Client launch with JEI reached an in-world integrated server; debug logs confirmed `hbm:jei_plugin` registered the `hbm:burner_press` recipe type.
- A follow-up `runData` pass succeeded while the client was still open. The log reported file lock warnings for `latest.log` and `debug.log` because the running client owned those files; this did not block data generation.

## 2026-07-08 Workflow Correction

- Removed the temporary Burner Press speed-gauge overlay after visual review showed it cutting across the original dial art.
- Left the original `gui_press.png` dial art untouched until the real source preheater/speed behavior is ported.
- Replaced the single alpha creative tab with source-style NTM creative categories for active content.
- Added `docs/FRAMEWORK_FIRST_WORKFLOW.md` and updated the roadmap/strategy to use framework-first ordering: creative tabs, core systems, machine/recipe frameworks, then content families.
