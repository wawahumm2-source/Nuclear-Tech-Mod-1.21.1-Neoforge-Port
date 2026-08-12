# HBM 1.21.1 NeoForge Parity Audit

This file tracks active alpha content against the original HBM Nuclear Tech jar/source. Status values:

- `exact`: original asset or behavior is matched directly.
- `modern wrapper`: original identity is preserved, but the implementation uses a 1.21.1 NeoForge-safe wrapper.
- `temporary scaffold`: intentionally incomplete while the system is being rebuilt.
- `wrong/missing`: known defect that should not be treated as parity.
- `approved divergence`: intentional departure from Tier 1 behavior recorded in `docs/approved-divergences.md`.

## Active Alpha Audit

| Content | Names/IDs | Textures/Models | Sounds | GUI | Recipe/Behavior | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| Creative Tabs | modern wrapper | n/a | n/a | n/a | modern wrapper | Public alpha content remains in source-style NTM categories. A separate developer tab now contains only controlled radiation diagnostics. |
| Config framework | modern wrapper | n/a | n/a | n/a | modern wrapper | Radiation source defaults now include contamination/chunk toggles, source multipliers, Tier 1 thresholds, treatment timing, resistance, and simple-field spread values. Unported system families remain pending. |
| Hazard framework | modern wrapper | n/a | n/a | n/a | modern wrapper | Radiation now has active inventory, distance-attenuated local block aura, fallout, explosion, dimension, and scripted contributor contracts. Local aura falloff is the approved `RAD-003` modern wrapper; other source hazard families remain structural until their own audit passes. |
| Radiation saved data | modern wrapper | n/a | n/a | n/a | modern wrapper | Player radiation is a persistent NeoForge attachment; fallout is per-dimension saved data with a source-shaped simple-field solver. `ChunkRadiationHandlerNT`/PRISM remain pending. |
| Mob radiation | modern wrapper | n/a | n/a | n/a | modern wrapper | Loaded mobs now accumulate local, fallout, dimension, equipped-item, and explosion radiation through a persistent attachment. Source vanilla immune types are data-driven; Tier 1 creature transformations remain pending. |
| Networking framework | modern wrapper | n/a | n/a | n/a | modern wrapper | Radiation payloads update a client-only developer inspector overlay. Typed client effect dispatch now drives the nuclear visual wrapper; symptom particles and broader effect families remain pending. |
| Explosion framework | modern wrapper | n/a | modern wrapper | mapped | modern wrapper | Conventional HBM TNT deliberately delegates to the vanilla 1.21.1 blast API. Low Yield remains immediate. Little Boy now defaults to the Tier 1 `452389`-ray generalized spiral over immutable resistance snapshots and a bounded two-thread pool; the hybrid radial planner is optional weak-hardware mode. All chunk access and mutation remain server-thread work. |
| Machine framework | modern wrapper | n/a | n/a | n/a | modern wrapper | Burner Press now uses shared helpers for output merging and sided automation. Broader machine framework still needs energy, fluid, recipe, sync, and upgrade contracts. |
| Burner Press | modern wrapper | modern wrapper | exact | approved divergence | approved divergence | Uses original `gui_press.png`, `press_body.obj`, `press_head.obj`, `press_body.png`, `press_head.png`, `machine_press.png`, and `pressoperate.ogg`; NeoForge wrappers preserve source identity. Fuel, heat cadence, residual heat, and compact GUI are approved divergences in `docs/approved-divergences.md`. |
| Burner Press slots | modern wrapper | n/a | n/a | modern wrapper | modern wrapper | Player-facing GUI exposes fuel `0`, stamp `1`, input `2`, and output `3`. Internal/template backing slots `4..12` remain saved but hidden after manual review showed the visible row was wrong for the current Burner Press pass. |
| Burner Press fuel/heat | n/a | n/a | n/a | n/a | approved divergence | Fuel burns continuously at four units per tick. Heat rises once every four ticks, with a preheater adding four speed units on that interval. Residual heat may begin cycles at speed `16+` and retraction completes below that threshold. See `BP-001` to `BP-003`. |
| Burner Press Preheater | modern wrapper | modern wrapper | n/a | n/a | approved divergence | `press_preheater` is active as a placeable machine block using the original animated `press_preheater.png` texture. It adds four speed units per approved heat interval. Crafting remains pending because the source recipe needs copper plates, tungsten ingots, and a lava tank chain that is not active in this alpha yet. |
| Burner Press item icon | modern wrapper | modern wrapper | n/a | n/a | n/a | The inventory/crafting/JEI item model is separated from the placed OBJ renderer and now uses the original `hbm:block/machine_press` identity texture. Pending manual visual confirmation. |
| Burner Press stamps | modern wrapper | exact/temporary scaffold | n/a | n/a | modern wrapper | Flat, plate, wire, circuit, ammo, and printing stamp types are represented. Printing variants currently share original `stamp_book.png`. |
| Burner Press recipes | mapped | n/a | n/a | n/a | mapped | Temporary ore-to-ingot recipes were removed. Active `PressRecipes` parity slice now maps plate/wire recipes whose inputs and outputs exist in the alpha: iron plate, gold plate, steel plate, lead plate, and gold wire. Remaining original recipe families are pending missing items/material chains. |
| Burner Press JEI | modern wrapper | modern wrapper | n/a | modern wrapper | modern wrapper | Uses native JEI animated flame and arrow drawables over the original HBM machine panel. Fuel input cycles items accepted by the Burner Press fuel predicate. |
| Uranium Ore | modern wrapper | exact | n/a | n/a | temporary scaffold | Active block/item exists with original asset family; generation and processing chain still need full original progression audit. |
| Deepslate Uranium Ore | modern wrapper | modern wrapper | n/a | n/a | temporary scaffold | Modern block variant retained for 1.21 terrain expectations; not a direct 1.7.10 original block. |
| Lead Ore | modern wrapper | exact | n/a | n/a | temporary scaffold | Active block/item exists; full drop/processing audit pending. |
| Geiger Counter | modern wrapper | exact | exact | n/a | modern wrapper | Passive five-tick feedback and right-click diagnostics now use source-shaped environmental rate behavior. Exact source click weighting remains pending audio review. |
| RadAway | modern wrapper | exact | exact | n/a | modern wrapper | Normal, strong, and flush variants now use source timed totals of `140`, `350`, and `1000 RAD`; potion sickness and source syringe/container behavior remain pending. |
| Rad-X | modern wrapper | exact | n/a | n/a | modern wrapper | Active source-style three-minute `0.2` resistance treatment. Original pill-side effects remain pending. |
| Sellafite tiers | modern wrapper | exact generated remap | n/a | n/a | modern wrapper | One canonical `hbm:sellafield` block preserves the source metadata design with six levels, four deterministic coordinate variants, exact source color-endpoint remapping, level decay, and carried radiation values. Six state-bearing isometric item stacks and JEI subtype identity expose every tier without inventing registry IDs. |
| Sellafield Slaked | modern wrapper | exact | n/a | n/a | modern wrapper | Ten stage states use the four original textures and exact `1 - stage / 15` grayscale curve. Coordinate variants now survive placement and fallout conversion. |
| Sellafield converted ores | modern wrapper | exact/multipass wrapper | n/a | n/a | mapped | Bedrock plus diamond, emerald, scorched uranium, Schrabidium, and radioactive-gem outcomes are active. Original base-plus-overlay rendering is represented by modern layered block models; deterministic persisted rolls replace non-repeatable source RNG. |
| Radiation Developer Tools | approved divergence | reused original assets | n/a | developer overlay | approved divergence | Inspector, reset, fallout injector, and explosion pulse provide controlled framework verification. See `RAD-002`. |
| Radioactive Waste Barrel | mapped | exact | n/a | n/a | modern wrapper | Uses a mapped long-lived waste source profile for the framework showcase. Original barrel storage/interaction parity remains pending. |
| HBM TNT | approved divergence | exact | modern wrapper | n/a | modern wrapper | Public creative-only port of source `BlockTNT`, displayed as Dynamite under `EXP-002`: original top/side/bottom textures, 80-tick fuse, redstone/flint/fire/projectile priming, 10..29 tick chain fuse, and strength-10 vanilla blast. The source survival chain remains pending. |
| Prototype Nuclear Charge | temporary scaffold | exact | exact | mapped | mapped | Diagnostic wrapper for `ExplosionNukeSmall.PARAMS_LOW`: terrain strength 15, kill radius 45, max damage 250, resolution 64, no drops, exact 25-chunk Low Yield fallout, and an NTM Extended 3.0.3 Torex visual scaled from the test radius. Source shrapnel and matched low-yield visual review remain pending. |
| Little Boy | temporary scaffold | exact | modern wrapper | mapped | modern wrapper/approved divergence | Source `nuke_boy` identity is active as a creative large-explosion pilot. Its default terrain profile is radius `120`, strength `240`, depth multiplier `1`, and `452389` Tier 1 generalized-spiral rays. Immutable snapshots feed bounded source-ray batches to two primitive-only workers; jobs are accepted without truncation and scheduled round-robin. Crater masks persist by real chunk and mutate nearest-first only after force-loaded chunks report ready. Source-ordered top-three-solid Sellafield conversion follows excavation, including Waste Earth for surviving outer grass; Fallout Rain then starts separately. The approved water extension surveys connected bodies before mutation: enclosed components may evaporate, persistent water uses Waldemar's vanilla-water rendering basis with a darker biome tint, and crater refill requires a real horizontal air opening after excavation. The active cloud now uses NTM Extended 3.0.3's dynamic Torex lifecycle, exact cloudlet/flare assets, source motion, heat, humidity, flash, shock, and 54-second Little Boy lifetime. Matched live review is still required before its visuals can be marked exact. |

## Burner Press Defect List

| Defect | Status | Notes |
| --- | --- | --- |
| GUI height used `176x222` instead of original `176x202`. | accepted divergence | The approved compact presentation is `176x184`; see `BP-004`. |
| Inventory label overlapped player slots. | fixed, accepted | Compact-layout label and visible slots were manually accepted. |
| Temporary speed gauge overlay cut across the original dial art. | fixed, accepted | The current source-style needle is driven by synced speed and has been manually accepted. |
| Original burn and press overlays were missing. | fixed, pending screenshot | Burn and press overlays are drawn from `gui_press.png` in the original areas. |
| Burner Press item rendered as a tiny placeholder cube in crafting/JEI. | fixed, pending screenshot | Item model now uses original `machine_press.png`; placed-world OBJ renderer remains separate. |
| Temporary ore-to-ingot Burner Press recipes were not source-parity. | fixed | Temporary ore recipes were removed from active data. |
| Press movement was a fixed one-tick progress bar instead of the original accelerated press travel. | fixed, pending gameplay | The block entity now persists/syncs source-style `speed`, `delay`, and retracting state; press travel scales with speed up to the original 25 units per tick. |
| Internal/template slot row appeared as random empty GUI slots. | fixed, pending screenshot | Template slots are hidden from the menu and the baked row in `gui_press.png` is masked for the current GUI. |
| World model press head started in the down position. | fixed, pending gameplay | Renderer now applies an idle lift and moves the head downward only as press progress advances. |
| Heat/speed gauge did not visibly respond. | fixed, pending screenshot | GUI now draws a source-style speed needle driven by synced press speed, including low-speed movement. |
| Fuel state did not match the approved behavior. | fixed, accepted divergence | The flame now represents continuous active fuel burn; see `BP-001`. |
| Original `PressRecipes` catalog is incomplete. | open | First mapped plate/wire slice is active; flat, circuit, ammo, and printing families remain pending item/material ports. |
| Original preheater acceleration behavior is not implemented. | approved divergence | `press_preheater` adds `4` speed units every four ticks; see `BP-002`. |
| Burner Press Preheater crafting recipe is missing. | pending missing item | Source recipe needs copper plates, tungsten ingots, and a lava tank. Do not add an invented shortcut recipe; activate it when those material/fluid-tank items exist. |
| Original sound loop/timing is not exact. | open | Current implementation plays the original sound at operation completion. |

## Burner Press Recipe Families

| Family | Status | Active Entries |
| --- | --- | --- |
| Plate stamp | mapped | Iron plate, gold plate, steel plate, lead plate. |
| Wire stamp | mapped | Gold wire. |
| Flat stamp | pending missing item | Requires additional original materials and output items before activation. |
| Circuit stamp | pending missing item | Requires wafers/circuit materials before activation. |
| Ammo stamps | pending missing item | Requires original ammo casing/projectile chains before activation. |
| Printing stamps | pending missing item | Requires original printed page/book/note chains before activation. |

## Burner Press Manual Checks

- Place, remove, save, and reload the three-block-tall Burner Press.
- Insert solid fuel, the plate or wire stamp, and a valid mapped input; confirm output appears only with the correct stamp.
- Confirm a wrong stamp rejects the recipe.
- Confirm fuel burns continuously at four units per tick while the burner is active.
- Confirm the flame visibly shrinks during active burn.
- Confirm a Burner Press Preheater adjacent to the lower press block adds four speed units per heat interval.
- Confirm a preheater away from the lower press block does not accelerate heat/speed.
- Confirm heat/speed cools down after fuel runs out.
- Confirm output stacks when possible and blocks operation when full.
- Confirm stamp durability decreases on each completed operation.
- Confirm automation inserts only fuel, stamp, or input and extracts only output from the bottom.
- Compare the GUI slot positions against original `gui_press.png`.
- Observe the head moving down during pressing and retracting after completion.
- Confirm `block.pressoperate` plays at operation completion.

## Parity Rule

Any newly ported active block, item, machine, recipe, GUI, model, or sound must be added to this audit before it is treated as alpha-ready. Placeholder textures, generic cube models, simplified recipes, and replacement sounds are defects unless this file marks them as `temporary scaffold`.
