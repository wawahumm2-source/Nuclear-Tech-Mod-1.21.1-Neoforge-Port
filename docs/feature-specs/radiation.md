# Radiation Framework

Status: active framework pilot
Classification: framework

## Tier 1 Evidence

- `RadiationConfig`: contamination and chunk radiation are enabled by default; PRISM is opt-in.
- `HazardTypeRadiation`: carried radioactive stacks apply their RAD/s value divided over twenty ticks and multiply by stack size.
- `ContaminationUtil` and `HazmatRegistry`: intake uses `10^-resistance`; armor and Rad-X add resistance.
- `HbmLivingProps` and `EntityEffectHandler`: radiation is capped at `2500 RAD`, sickness bands begin at `200`, `400`, `600`, and `800`, and fatal exposure begins at `1000`.
- `ItemGeigerCounter`: passive five-tick Geiger feedback uses the current environmental radiation rate.
- `ItemSyringe` and `HbmPotion`: RadAway removes `140 RAD` over `14` ticks, strong removes `350`, and flush removes `1000`.
- `ChunkRadiationHandlerSimple`: the default field retains `60%`, sends `7.5%` to cardinal neighbors, `2.5%` to diagonals, then decays established chunks every twenty ticks.

## Public Test Surface

- Uranium ore and ingot: `0.35 RAD/s` source material mapping.
- Uranium fuel pellet: `0.05 RAD/s` mapped from source uranium-fuel nugget behavior.
- Radioactive waste barrel: `5 RAD/s` mapped to long-lived nuclear waste for the alpha test surface.
- Geiger Counter: passive source-style clicking and a right-click reading.
- RadAway, RadAway Strong, and RadAway Flush: timed source dosage.
- Rad-X: source duration and resistance bonus.
- Vanilla iron and golden armor: source HazmatRegistry resistance values, pending the original HBM hazmat armor family.
- Loaded mobs: accumulate local block, chunk fallout, Nether, equipped-item, and direct explosion radiation; the Tier 1 vanilla immune set is data-driven through `hbm:radiation_immune`.

## Server Contract

- Player state is a persistent NeoForge attachment and survives respawn. Mob state is a separate persistent attachment.
- Contributors are inventory, block aura, fallout field, explosion pulse, dimension ambient rate, and scripted diagnostics.
- The server calculates all dose, protection, treatment, and effects. Clients receive diagnostics only.
- Local radioactive blocks use configurable quadratic distance attenuation from the block volume and reach zero at the configured aura radius. Chunk fallout remains a separate source-style per-chunk field.
- Chunk radiation is stored per dimension and uses the source simple-field cadence. PRISM remains pending.
- Source defaults are configurable through the common radiation configuration. Structural field settings require restart.

## Developer Diagnostics

- A separate developer tab supplies the held inspector, player reset, deterministic fallout injector, and direct explosion-radiation pulse.
- Holding the inspector shows dose, source contributions, the most recent direct explosion dose, resistance, and expected intake multiplier.
- These tools are not public HBM parity content and must remain outside the public tabs.

## Deferred Structural Shielding

- Concrete and other structural radiation shielding are a future radiation-framework pass, not active behavior in this alpha.
- That pass must source-audit whether shielding affects local block aura, chunk fallout, direct explosion exposure, or all three, then expose the resulting values through configuration.
- Tier 1 concrete erosion inside `ExplosionNT` is blast damage behavior. It must not be mistaken for radiation shielding or used as evidence for a shielding formula.

## Approved Divergences

- `RAD-001` and `RAD-002` in `docs/approved-divergences.md`.

## Acceptance Checks

- One RAD/s for twenty ticks adds one RAD before resistance.
- One resistance point leaves ten percent intake.
- Normal, strong, and flush RadAway remove `140`, `350`, and `1000 RAD` respectively.
- Player radiation survives death and respawn; developer reset clears only the player state.
- Geiger click rate increases with environmental dose and right-click reports dose/rate/resistance.
- Fallout injector persists through save/reload and spreads only on the configured server cadence.
- The developer explosion pulse remains a direct-contributor diagnostic. The Prototype Nuclear Charge now applies verified Low Yield chunk fallout instead of the former arbitrary direct dose.
- A non-immune mob beside a local emitter accumulates radiation, displays source-style sickness at thresholds, and receives Low Yield fallout exposure after a Prototype Nuclear Charge detonates.
- A zombie, skeleton, mooshroom, or ocelot remains radiation-immune unless a data pack changes `hbm:radiation_immune`.
- Dedicated server loads without client classes; holding the inspector shows the client overlay after joining.
