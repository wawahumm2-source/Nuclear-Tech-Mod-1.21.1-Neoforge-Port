# HBM 1.21.1 NeoForge Parity Audit

This file tracks active alpha content against the original HBM Nuclear Tech jar/source. Status values:

- `exact`: original asset or behavior is matched directly.
- `modern wrapper`: original identity is preserved, but the implementation uses a 1.21.1 NeoForge-safe wrapper.
- `temporary scaffold`: intentionally incomplete while the system is being rebuilt.
- `wrong/missing`: known defect that should not be treated as parity.

## Active Alpha Audit

| Content | Names/IDs | Textures/Models | Sounds | GUI | Recipe/Behavior | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| Creative Tabs | modern wrapper | n/a | n/a | n/a | modern wrapper | Active alpha content is now split into source-style NTM categories: blocks, consumables/gear, machine items/fuel, machines, bombs, resources/parts, and templates. |
| Config framework | modern wrapper | n/a | n/a | n/a | temporary scaffold | Config is split into source-style families with alpha defaults preserved. Original defaults still need full source audit before any value is marked exact. |
| Hazard framework | modern wrapper | n/a | n/a | n/a | temporary scaffold | Active alpha radioactive content is registered through an internal hazard registry. Source hazard families now exist structurally: radiation, digamma, fallout, contamination, heat, poison, blinding, asbestos, coal, hydroactive, and explosive. Runtime behavior beyond radiation remains pending source parity. |
| Radiation saved data | modern wrapper | n/a | n/a | n/a | modern wrapper | Player exposure and chunk fallout now use explicit saved-data APIs plus a source-shaped chunk radiation service for get/set/increment/decrement. Original 3D pocket/spread behavior from `ChunkRadiationHandlerNT` remains pending. |
| Networking framework | modern wrapper | n/a | n/a | n/a | temporary scaffold | Typed payloads are registered for radiation sync, machine GUI actions, and client effect events. Client HUD/effect handling is intentionally inert until a source-parity client pass. |
| Explosion framework | modern wrapper | n/a | temporary scaffold | n/a | temporary scaffold | Prototype Nuclear Charge now routes through a server-side explosion service. Original HBM blast classes, sound timing, crater math, and fallout behavior remain pending. |
| Machine framework | modern wrapper | n/a | n/a | n/a | modern wrapper | Burner Press now uses shared helpers for output merging and sided automation. Broader machine framework still needs energy, fluid, recipe, sync, and upgrade contracts. |
| Burner Press | modern wrapper | modern wrapper | exact | modern wrapper | modern wrapper | Uses original `gui_press.png`, `press_body.obj`, `press_head.obj`, `press_body.png`, `press_head.png`, `machine_press.png`, and `pressoperate.ogg`; NeoForge block entity/menu/renderer wrap the original identity. GUI now uses the source dimensions and coordinates, and ticking now follows the original speed/ramp/retract rhythm more closely, but remains pending screenshot and gameplay confirmation before it is marked `exact`. |
| Burner Press slots | exact | n/a | n/a | exact | modern wrapper | Restored original slot map: fuel `0`, stamp `1`, input `2`, output `3`, internal/template `4..12`. |
| Burner Press fuel | n/a | n/a | n/a | n/a | modern wrapper | Stores solid-fuel burn time and spends 200 per completed operation, matching original behavior through NeoForge fuel APIs. |
| Burner Press item icon | modern wrapper | modern wrapper | n/a | n/a | n/a | The inventory/crafting/JEI item model is separated from the placed OBJ renderer and now uses the original `hbm:block/machine_press` identity texture. Pending manual visual confirmation. |
| Burner Press stamps | modern wrapper | exact/temporary scaffold | n/a | n/a | modern wrapper | Flat, plate, wire, circuit, ammo, and printing stamp types are represented. Printing variants currently share original `stamp_book.png`. |
| Burner Press recipes | mapped | n/a | n/a | n/a | mapped | Temporary ore-to-ingot recipes were removed. Active `PressRecipes` parity slice now maps plate/wire recipes whose inputs and outputs exist in the alpha: iron plate, gold plate, steel plate, lead plate, and gold wire. Remaining original recipe families are pending missing items/material chains. |
| Uranium Ore | modern wrapper | exact | n/a | n/a | temporary scaffold | Active block/item exists with original asset family; generation and processing chain still need full original progression audit. |
| Deepslate Uranium Ore | modern wrapper | modern wrapper | n/a | n/a | temporary scaffold | Modern block variant retained for 1.21 terrain expectations; not a direct 1.7.10 original block. |
| Lead Ore | modern wrapper | exact | n/a | n/a | temporary scaffold | Active block/item exists; full drop/processing audit pending. |
| Geiger Counter | modern wrapper | exact | exact | n/a | modern wrapper | Active radiation feedback exists; needs full original timing/range comparison. |
| RadAway | modern wrapper | exact | exact | n/a | modern wrapper | Active exposure reduction exists; dosage and side-effect parity pending. |
| Radioactive Waste Barrel | modern wrapper | exact | n/a | n/a | modern wrapper | Active radiation source exists; storage/interaction parity pending. |
| Prototype Nuclear Charge | temporary scaffold | exact | temporary scaffold | n/a | temporary scaffold | Useful alpha explosive placeholder; blast, fallout, particles, and sounds are not full HBM parity yet. |

## Burner Press Defect List

| Defect | Status | Notes |
| --- | --- | --- |
| GUI height used `176x222` instead of original `176x202`. | fixed, pending screenshot | Screen now uses `176x202`. |
| Inventory label overlapped player slots. | fixed, pending screenshot | Label is now `x=8, y=108`; player inventory starts at `y=120`; hotbar starts at `y=178`. |
| Temporary speed gauge overlay cut across the original dial art. | fixed, pending screenshot | The overlay was removed. The original dial art remains untouched until source preheater/speed behavior is ported. |
| Original burn and press overlays were missing. | fixed, pending screenshot | Burn and press overlays are drawn from `gui_press.png` in the original areas. |
| Burner Press item rendered as a tiny placeholder cube in crafting/JEI. | fixed, pending screenshot | Item model now uses original `machine_press.png`; placed-world OBJ renderer remains separate. |
| Temporary ore-to-ingot Burner Press recipes were not source-parity. | fixed | Temporary ore recipes were removed from active data. |
| Press movement was a fixed one-tick progress bar instead of the original accelerated press travel. | fixed, pending gameplay | The block entity now persists/syncs source-style `speed`, `delay`, and retracting state; press travel scales with speed up to the original 25 units per tick. |
| Original `PressRecipes` catalog is incomplete. | open | First mapped plate/wire slice is active; flat, circuit, ammo, and printing families remain pending item/material ports. |
| Original preheater acceleration behavior is not implemented. | open | Base speed acceleration is restored, but the `press_preheater` block is not yet ported. No temporary GUI overlay should represent this. |
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
- Confirm each completed operation spends 200 stored burn time.
- Confirm output stacks when possible and blocks operation when full.
- Confirm stamp durability decreases on each completed operation.
- Confirm automation inserts only fuel, stamp, or input and extracts only output from the bottom.
- Compare the GUI slot positions against original `gui_press.png`.
- Observe the head moving down during pressing and retracting after completion.
- Confirm `block.pressoperate` plays at operation completion.

## Parity Rule

Any newly ported active block, item, machine, recipe, GUI, model, or sound must be added to this audit before it is treated as alpha-ready. Placeholder textures, generic cube models, simplified recipes, and replacement sounds are defects unless this file marks them as `temporary scaffold`.
