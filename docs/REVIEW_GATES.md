# Review Gates

## Implementation Audit

- Feature packet exists and has Tier 1 evidence.
- Active assets, sounds, recipes, names, hitboxes, GUI coordinates, and behavior match the packet.
- Every divergence has an approved entry.
- No placeholder model, texture, sound, or substitute recipe is active.

## Adversarial Parity Audit

- Compare Tier 1 source/jar behavior against the port, not against prior port assumptions.
- Check client/server separation, save/load, automation, invalid inputs, full output, reload, chunk unload, and missing optional integration.
- Check that current audit text is still accurate.

## Visual/Gameplay Review

- Capture matched reference and port views.
- Compare GUI scale, coordinates, icon readability, model orientation, animation states, sound timing, and interaction/collision.
- Run the manual checks in the feature packet and record remaining defects or deferrals.
