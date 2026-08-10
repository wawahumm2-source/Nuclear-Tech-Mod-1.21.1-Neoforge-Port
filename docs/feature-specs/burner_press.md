# Burner Press

Status: accepted pilot pending workflow migration review
Classification: pilot machine

## Tier 1 Evidence
- Source behavior: `TileEntityMachinePress`, `ContainerMachinePress`, `GUIMachinePress`, and `PressRecipes`.
- Source assets: `gui_press.png`, `machine_press.png`, `press_body.obj`, `press_head.obj`, `press_body.png`, `press_head.png`, and `pressoperate.ogg`.
- Canonical active slots: fuel `0`, stamp `1`, input `2`, output `3`.

## Behavior Contract
- Server state: fuel, speed, press extension, delay, retraction, inventory, output merge, and sided automation.
- Active mapped recipes require the matching stamp type and input; output must stack safely.
- Continuous fuel, heat timing, residual heat, and compact GUI are governed by the approved divergences below.
- JEI uses native animated flame and arrow drawables over the original HBM machine panel; its fuel input cycles every item accepted by the machine fuel slot.

## Approved Divergences
- `BP-001` through `BP-004` in `docs/approved-divergences.md`.

## Validation
- Unit: `BurnerPressTimingTest` covers fuel drain, heat cadence, preheater rate, residual threshold, and low-heat retraction.
- Runtime: recipe matching, output capacity, stamp durability, persistence, sided automation, sound timing, world rendering, and GUI/JEI screenshots.
- Evidence: `build/visual-evidence/burner_press/`.

## Review Record
- Specification approval: retroactively approved through the Burner Press parity pass.
- Visual/gameplay approval: GUI and machine behavior accepted by Mr. Hummithy; JEI animation remains subject to the current runtime review.
