# Approved Parity Divergences

Every entry records a deliberate departure from Tier 1 behavior. Entries are not evidence of source parity.

| ID | Feature | Approved behavior | Reason | Acceptance check |
| --- | --- | --- | --- | --- |
| BP-001 | Burner Press fuel | Fuel drains continuously at four reserve units per server tick while the burner is active; recipe completion has no separate fuel charge. | Approved gameplay balance change. | Standard coal visibly drains over roughly 400 ticks. |
| BP-002 | Burner Press heat | Heat increases only once every four ticks. A preheater adds four speed units on that interval. | Approved slower warmup. | Normal and preheated gauges rise at the documented cadence. |
| BP-003 | Burner Press residual heat | A valid recipe may begin while speed is at least 16 after fuel is exhausted; retraction always completes below that threshold. | Approved hybrid behavior. | Residual operation stops naturally below the threshold without freezing the head. |
| BP-004 | Burner Press GUI | The player inventory/template area is compacted to the approved `176x184` presentation. | The original visible template row was rejected during review. | No random template slots or seam artifacts; all visible slots remain clickable. |
| RAD-001 | Radiation death persistence | Player radiation survives death and respawn, including radiation-caused death; non-player fatal radiation resets its transient state. | Approved diagnostic workflow decision. Tier 1 clears radiation at fatal exposure. | A developer pulse followed by death retains the recorded dose until RadAway or reset clears it. |
| RAD-002 | Radiation developer tools | Inspector, reset, fallout injector, and radiation pulse are separate developer-tab tools. | Framework visibility is required before all original content is ported. | Tools are absent from public HBM tabs and expose only controlled diagnostics. |
| RAD-003 | Local block radiation aura | Local radioactive blocks attenuate by configurable distance, reaching zero at the configured radius. Source-style chunk fallout remains uniform within each chunk. | Tier 1 evidence does not define one universal block-aura formula; distance attenuation is needed for readable local environmental risk in the modern framework. | The inspector's block rate decreases continuously while moving away from a single local emitter. |
| RAD-004 | Mob radiation evaluation cadence | Loaded mobs evaluate radiation every five ticks by default, preserving the same RAD/s dose and cumulative effect probabilities across the batch. | Tier 1 updates living entities per tick; batching prevents local aura scans from causing disproportionate mob-heavy server cost. | Changing `mobTickInterval` changes responsiveness only, not the dose accrued over a given duration. |
| EXP-001 | Nuclear terrain execution | World reads and mutation remain bounded main-thread jobs; immutable primitive ray mathematics runs on a bounded worker pool. Source-style targets are grouped by real chunk and processed nearest-first through persisted, non-ticking NeoForge force tickets. | Tier 1's central chunk loader does not guarantee full unrendered-radius coverage; the port must cover the requested blast area without unsafe off-thread world access. | A saved Little Boy job resumes its snapshot/ray/chunk cursors, affects every configured crater chunk, and releases every ticket on completion. |
| EXP-002 | Public HBM TNT name | The public alpha block is displayed as Dynamite. | Mr. Hummithy approved a clearer public-facing name for the current alpha. | The creative-tab name and tooltip display Dynamite. |
| EXP-003 | Nuclear terrain pacing | Profiles at or below the configurable terrain-strength limit of `15` complete in the detonation tick; larger profiles use the bounded terrain scheduler. | Mr. Hummithy approved immediate small explosions while preserving staged large explosions. | Low Yield terrain appears with the detonation; a larger profile remains queued and bounded. |
| EXP-004 | Little Boy arming | `hbm:nuke_boy` is a developer-armed creative test wrapper instead of requiring the original five-component assembly. | The source assembly items, menu, and automation are not active yet; the large-explosion scheduler needs a real source device now. | Redstone or empty-hand interaction starts the Little Boy test profile. |
# Explosion Polish Divergences

## EXP-005: Nuclear wildfire is disabled by default

Tier 1 Fallout Rain places vanilla fire on flammable surfaces within the inner 65 percent of its fallout area. The modern default disables that placement. If enabled by config, the one-in-five source roll creates a short-lived, non-spreading HBM fire block instead of spreading vanilla fire. This prevents uncontrolled world-scale wildfire and tick lag.

## EXP-006: Bounded full-area Fallout Rain loading

Tier 1 Fallout Rain's exact chunk-holding behavior is not a safe modern contract. The port force-loads the full configured Fallout Rain area through a bounded, non-ticking nearest-first work queue, then releases each chunk immediately after its columns are processed. A shared scheduler deadline limits the total server-thread cost.

## EXP-007: Superseded - source MK5 density restored

Tier 1 derives `2.5 * pi * strength^2` spiral points, which is `452389` for Little Boy's strength of `240`. The former `100000`-point default has been removed. `452389` is now the configurable default and worker batches provide performance without reducing ray density. This entry remains only to document the retired divergence.

## EXP-008: Tall-tree trunk completion

Tier 1 Fallout Rain stops after three solid blocks in a column, which can leave modern tall trees visibly half-charred. The port performs a bounded exposed-log scan inside the source wood-effect radius and converts remaining trunk logs to Waste Log. It does not transform non-log terrain through this extra pass.

## EXP-009: Nuclear calculation status line

The source exposes its large terrain calculation only through world changes. The port sends nearby clients a restrained local status line for calculation, excavation, and Fallout Rain progress so the staged job reads as intentional rather than stalled. Flash timing and shock-arrival shake remain source-informed.

## EXP-010: Visible Fallout Rain

Tier 1 `EntityFalloutRain` is an invisible terrain-processing carrier with a custom tall-quad rain renderer. The modern port renders bounded vertical weather sheets around clients inside the server-provided fallout radius while that job is active. It uses the original `textures/entity/fallout.png`, not the placed ash or `falloutitem` texture, and does not alter radiation, terrain conversion, radius, or server timing.

## EXP-011: Optional hybrid downward crater bias

The source-density default evaluates Little Boy against doubled strength `240`, retains its `120`-block travel limit, and uses source depth multiplier `1`. The configurable quadratic downward bias of `2.5` is now restricted to optional hybrid weak-hardware mode. It no longer changes the parity-default crater.

## EXP-012: Client quality caps for NTM Extended Torex cloudlets

NTM Extended 3.0.3 permits up to `20,000` standard Torex cloudlets and adds its early shock, ring, and condensation populations. `FULL` preserves that source ceiling and spawn equations. `REDUCED` and `MINIMAL` retain the source lifecycle, motion, heat, color, alpha, and scale equations while capping the active client population at `6,000` and `2,000`. These lower modes are performance wrappers and must not be described as exact particle-density parity.

## EXP-013: Time-bounded large-blast acceleration

Large nuclear jobs retain their source ray sequence and resistance math, but immutable planning uses two bounded workers with `8192`-ray batches and excavation permits `8192` blocks per tick by default. Main-thread snapshot, scheduler, and mutation deadlines remain authoritative. Actual block deletion still begins only after planning, because deleting terrain during ray collection would change the resistance seen by later rays.

## EXP-014: NTM Extended source-scaled mushroom-cloud lifetime

NTM Extended 3.0.3 scales `EntityNukeTorex` with `clamp(radius * 0.01, 0.25, 5)` and removes it after the float calculation `(int) (45 * 20 * scale)`. Little Boy radius `120` therefore uses the represented float scale near `1.2` and lasts `1,079` ticks, approximately 54 seconds. The parent alpha remains full for three quarters of that life and fades during the final quarter, exactly following the Extended source equations. This changes no server explosion, fallout, damage, or radiation behavior.

## EXP-015: Blast terrain conversion timing

Tier 1 performs Sellafield/waste conversion inside `EntityFalloutRain` after MK5 excavation. The port prepares conversion work beside ray planning but does not apply it until crater excavation completes. Fallout deposits, radioactive weather, and rain audio begin afterward as a separate event. This sequencing prevents terrain conversion from racing or visually preceding crater removal while retaining deterministic resistance inputs.

## EXP-016: Fallout rain audio

Tier 1 renders vertical `textures/entity/fallout.png` weather sheets but does not explicitly loop rain audio. The port adds local vanilla weather-rain sound while the persisted Fallout Rain job is active. An earlier dirty-gray billboard aerosol was removed after runtime review because it read as unrelated cloud particles inside the rain. The remaining effect is client-only and does not create Radon blocks or alter radiation values.

## EXP-017: Cinematic flash and pressure impact

Tier 1 combines a 100-tick world flare, a client flash timestamp, and a 15-tick hurt-camera response. The port keeps the original `particle/flare.png` in world space and uses the configured flash interval as a brief hard-white impact followed by a longer decaying afterimage. When the source-timed pressure front arrives, it applies a centered, damped positional and angular impulse instead of a one-direction random walk. This is an approved Alex's Caves-inspired presentation divergence; it is client-only, honors the vanilla screen-effects scale and HBM flash/shake settings, and does not alter explosion math, sound timing, radiation, or terrain work.

## EXP-018: Little Boy water vaporization

Tier 1's MK5 Little Boy carrier does not invoke `ExplosionNukeGeneric.vapor`; that path belongs to the MK3/advanced explosion family. Waldemar's 1.12.2 branch adds wet-biome flood/drain architecture, but it does not classify connected water bodies. At Mr. Hummithy's request, the port uses a modern bounded connected-body pass after crater excavation. It first surveys every water-tagged fluid state, including flowing water, aquatic plants, and waterlogged hosts, through the configured Fallout Rain radius plus a two-block sentinel ring without mutating the world. A body evaporates only when the survey proves the whole component is enclosed and no larger than the configurable volume cap. Boundary-connected or oversized bodies remain and convert to radioactive contaminated water inside the affected radius. Crater refill is permitted only when converted water has a horizontal air opening with a clear sampled air line into the excavated crater; nearby water, vertical overlap, open sky above a water surface, and isolated caves do not trigger a fill. The connected column's actual surface sets the fill height. This deliberately conservative line test may reject a winding tunnel, but avoids a multi-million-block air flood fill. Waterlogged host blocks are preserved but drained because vanilla waterlogging cannot store a custom fluid; lava is not changed by this water-specific pass. Survey/classification safely restart after a reload because they perform no mutation; compact mutation/refill cursors persist once classification completes. Waldemar 1.12.2's `RadWaterFluid` uses vanilla water textures rather than the saturated Reloaded `radwater` sheets. The port follows that basis and applies the local biome's water color at 88% brightness, so contaminated water remains subtly darker than adjacent ordinary water without double-tinting a blue source texture. It spreads and buckets like water and has configurable ambient, immersion, and carried-bucket radiation.

## EXP-019: Threaded immutable MK5 planning

Tier 1 traces every generalized-spiral point directly against mutable world chunks. The default modern path captures an immutable, quantized resistance snapshot on the server thread and computes the complete `452389`-point generalized spiral in ordered batches on a bounded two-thread pool. Workers never access Minecraft world objects, and all terrain mutation remains on the server thread. Snapshot quantization, batched persistence, and nonblocking chunk readiness are modern wrappers; the optional cube-map hybrid remains available only for weak hardware and does not claim block-for-block parity.

## EXP-020: Bounded crater neighbor updates

Large crater excavation still uses `Level.setBlock` on the server thread so block entities, heightmaps, lighting checks, dirty chunks, path caches, and client section updates remain valid. It suppresses per-block neighbor-shape cascades and drops while the crater mask is applied. This avoids millions of redundant recursive updates; fluid replacement state and special HBM TNT priming remain intact. The surviving crater boundary resumes ordinary Minecraft updates afterward.

## EXP-021: Outer dead-grass conversion

Tier 1 converts grass to Sellafield Slaked only through the inner 45 percent bands and otherwise leaves surviving grass unchanged. The port converts surviving grass outside those bands, but still inside the Fallout Rain radius, to Waste Earth. This Reloaded/Waldemar-informed extension provides the approved dead-ground aftermath without broadening Sellafield stages. Mycelium continues to use Tier 1's Waste Mycelium result.

## EXP-022: NTM Extended 3.0.3 nuclear-visual authority

Mr. Hummithy selected NTM Extended 1.12.2 version `3.0.3` as the nuclear-visual authority and rejected the Reloaded fixed mesh. Little Boy and the prototype carrier now use Extended's `EntityNukeTorex` lifecycle and `RenderTorex` presentation: toroidal cloudlet convection, rising stem emitter, growing cap roller, hot-to-cold palette, early shock cloudlets, source humidity-driven condensation rings, source flare, radial flash geometry, source lifetime, and delayed hurt-camera impact. The exact Extended `particle_base.png` and `flare.png` assets are active. NeoForge render types, deterministic visual seeds, client quality caps, and the configurable full-screen overexposure remain modern wrappers. Fallout Rain, terrain destruction, Sellafield conversion, water handling, radiation, and the server scheduler remain separate systems.
