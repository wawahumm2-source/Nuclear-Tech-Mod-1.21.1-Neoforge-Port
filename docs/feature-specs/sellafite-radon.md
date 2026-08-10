# Sellafite, Waste Earth, and Radon Source Contract

Status: implementation and automated validation complete; runtime visual review pending
Classification: radiation content family built on the active framework

## Tier 1 Evidence

- `waste_earth` uses `waste_grass_top.png`, `waste_grass_side.png`, and `waste_earth_bottom.png`, drops dirt, and supports only cave plants.
- `BlockSellafield` has six levels. Its level-scaled block emission is `0.5 * (level + 1)`, producing `0.5`, `1.0`, `1.5`, `2.0`, `2.5`, and `3.0`; its carried-item hazard records are independently `0.5`, `1.0`, `2.5`, `4.0`, `5.0`, and `10.0 RAD/s`.
- Sellafite remains one source-style metadata block represented by six modern states, not six invented IDs. Each level has four coordinate-selected textures generated with the source endpoint-remap algorithm.
- Sellafield Slaked and its five ore variants preserve metadata stages `9..0`, four coordinate textures, and the source grayscale curve. Sellafield bedrock is a distinct unbreakable converted block.
- Sellafite random ticks add that same level-scaled amount to chunk radiation. Levels above zero have a one-in-fifteen decay roll to the next lower level; level zero has a one-in-twenty-five roll to become `sellafield_slaked`.
- Walking on levels zero through four applies source radiation amplifier zero through four. Level five uses amplifier ten.
- Sellafite uses four coordinate-selected grayscale source textures remapped through the six exact source color pairs. The inventory item uses the source level texture for its stored level.
- `gas_radon`, `gas_radon_dense`, and `gas_radon_tomb` are invisible, replaceable, non-colliding gas blocks with source movement and lifetime rolls.
- Normal Radon applies `0.05` bypass radiation on contact and has a one-in-fifty disappearance roll.
- Dense Radon applies `0.5` radiation, has a one-in-twenty chance to convert grass below to Waste Earth, and a one-in-thirty chance to dissipate into fallout.
- Tomb Radon removes active RadAway/Rad-X treatment, applies `0.5` bypass radiation, damages vegetation, and has a one-in-six-hundred disappearance roll.

## Modern Wrapper Rules

- One `hbm:sellafield` block retains a `level=0..5` state. Its BlockItem stores the level through the standard `minecraft:block_state` component, exposes six JEI subtypes, and selects six dedicated isometric item-model wrappers through custom-model-data overrides.
- State-aware radiation lookup reads Sellafite's stored level for placed blocks and carried stacks.
- Source gas movement is scheduled on the server thread and never force-loads chunks.
- Fine-particle mask/filter and asbestos systems are not active yet. Until they are ported, Radon applies its source radiation channel and records the missing secondary hazards as pending, not as completed parity.
- Little Boy and Fallout Rain do not spawn Radon. The references do not support that linkage.

## Acceptance Checks

- Creative inventory shows six visually distinct Sellafite levels using source icons.
- Placing each tier preserves its level; pick-block and drops retain the same tier.
- Ambient block radiation reads `0.5` through `3.0` by level; carried items read the six verified `0.5`, `1.0`, `2.5`, `4.0`, `5.0`, and `10.0` hazard values.
- Random ticks add level-scaled chunk radiation and decay through all six levels into Sellafield Slaked.
- Waste Earth uses the correct three-face source model and drops dirt.
- All three Radon items use the original source textures; placed gas moves, affects players and mobs, and expires at its source roll.
- Dense Radon can create Waste Earth and fallout. Tomb Radon cancels active treatment.
