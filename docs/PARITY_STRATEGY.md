# Parity Strategy

The port target is one-to-one parity with the original HBM Nuclear Tech Mod wherever Minecraft 1.21.1 and NeoForge allow it.

## Source Of Truth

- Canonical hierarchy: `docs/CANONICAL_REFERENCE_HIERARCHY.md`
- Original jar: `C:\Users\wawah\Downloads\HBM-NTM-[1.0.27_X5687].jar`
- Original source archive: `C:\Users\wawah\Downloads\Hbm-s-Nuclear-Tech-GIT-master.zip`
- Original zip copy: `C:\Users\wawah\Downloads\HBM-NTM-[1.0.27_X5687] - Copy.zip`
- Upstream repository: `HbmMods/Hbm-s-Nuclear-Tech-GIT`

## Rules

- Preserve original assets for every build unless a technical migration requires a modern wrapper.
- Preserve original item, block, machine, material, recipe, hazard, and weapon identity.
- Treat simplified alpha systems as scaffolding only.
- Replace scaffolding with parity systems incrementally, starting with creative tabs, core mod systems, shared machine/recipe frameworks, and then content families.
- Do not add broad item batches before the relevant framework exists.
- Keep NeoForge safety requirements: dedicated-server safe code, modern registries, data-driven resources, typed networking, and modern save formats.
- Use `tools/hbm-reference.ps1` for targeted tier lookups before changing parity-sensitive systems.

## Current Asset Status

- The original `assets/hbm` tree has been imported into the NeoForge project.
- The original `assets/minecraft` visual overrides bundled with HBM have also been imported.
- Active alpha models now reference original HBM textures where matching assets exist.
- Legacy `textures/items` and `textures/blocks` assets are retained, with modern `textures/item` and `textures/block` aliases generated for Minecraft 1.21.1 atlas loading.
- Original sound references are retained through `assets/hbm/sounds.json`, normalized to modern lowercase resource ids with explicit `hbm:` namespaces.
- Legacy language, manual, sound, structure, texture, OBJ, and model assets are retained for ongoing migration.

## Burner Press Parity Requirements

- The Burner Press must not remain a cube-model machine.
- Required source assets are present: `press_body.obj`, `press_head.obj`, `machine_press.png`, `gui_press.png`, and `pressoperate.ogg`.
- Current alpha status: custom three-block-tall hitbox, original body/head OBJ renderer, original press texture bindings, original `gui_press.png`, original slot coordinates, solid-fuel burn storage, stamp-gated recipes, press/retract head movement, and original `pressoperate.ogg` completion sound are in place.
- The original OBJ files remain retained; NeoForge wrapper copies add only material bindings needed for 1.21.1 model baking.
- Required parity work: full original `PressRecipes` catalog, exact preheater acceleration behavior, exact original sound loop/timing, and expanded stamp material families beyond the current active iron/ammo/printing set.
- The original speed/preheater dial art must not receive temporary overlay drawings. It should stay as source GUI art until the real source behavior is ported.

## Parity Order

1. Creative tabs and source content taxonomy.
2. Core mod systems: radiation, explosion math, config, saved data, networking, and client/server separation.
3. Shared machine and recipe frameworks: inventories, ticking, menus, screens, automation, JEI, and validation.
4. Asset identity: original textures, sounds, names, and visual references.
5. Content families: materials, blocks, machines, weapons, worldgen, recipes, and progression.
6. Stabilization: dedicated server behavior, profiling, log scans, and manual visual passes.
