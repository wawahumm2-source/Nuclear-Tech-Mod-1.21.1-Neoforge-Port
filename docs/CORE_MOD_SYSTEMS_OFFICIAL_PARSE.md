# Official HBM Core Mod Systems Parse

Source parsed: `C:\Users\wawah\Downloads\HBM-NTM-[1.0.27_X5687].jar`

Implementation planning companion: `docs/CORE_SYSTEM_UPDATE_PLANS.md`

This list is based on two passes over the original build:

1. Package/class inventory pass: identified the major official code packages and class counts.
2. Core-system keyword/class pass: cross-checked packages and representative classes for systems such as radiation, hazards, explosions, config, networking, saved data, machines, recipes, fluids, worldgen, and sounds.

## Package Signals

The original jar contains 4,483 HBM classes. The strongest framework packages are:

| Package | Class Count | Meaning |
| --- | ---: | --- |
| `render` | 834 | Client renderers, model loaders, overlays, particles, visual systems. |
| `blocks` | 703 | Block definitions and block behavior. |
| `items` | 628 | Item definitions and item behavior. |
| `inventory` | 567 | Containers, recipes, slots, fluid stacks, ore dictionary helpers. |
| `tileentity` | 453 | Machine/block-entity logic and shared inventory/ticking bases. |
| `entity` | 249 | Projectiles, missiles, mobs, special effects, logic entities. |
| `world` | 241 | Worldgen, ores, structures, dungeons, deposits, dimensions. |
| `handler` | 218 | Radiation, pollution, NEI/recipe handlers, event/system handlers. |
| `explosion` | 63 | Nuclear, conventional, balefire, antimatter, and modular blast logic. |
| `packet` | 59 | Client/server sync, machine packets, effects, particles, recipes. |
| `config` | 23 | Bomb, radiation, machine, world, weapon, structure, and general config. |
| `hazard` | 22 | Item hazards, radiation hazards, hot/asbestos/coal/explosive/hydroactive behavior. |
| `saveddata` | 17 | World-level persistent systems such as satellites and special pools. |
| `creativetabs` | 9 | Source creative tab taxonomy. |
| `sound` | 10 | Looping/dynamic machine, siren, vehicle, and player sound wrappers. |

## Core Systems To Port Before Broad Content

### 1. Creative Tab And Content Taxonomy

Official anchors:
- `com.hbm.creativetabs.BlockTab`
- `com.hbm.creativetabs.ConsumableTab`
- `com.hbm.creativetabs.ControlTab`
- `com.hbm.creativetabs.MachineTab`
- `com.hbm.creativetabs.MissileTab`
- `com.hbm.creativetabs.NukeTab`
- `com.hbm.creativetabs.PartsTab`
- `com.hbm.creativetabs.TemplateTab`
- `com.hbm.creativetabs.WeaponTab`

Port requirement:
- Keep source-style categories active from the beginning.
- Every new item/block/machine must be assigned to its source family when registered.

### 2. Config Framework

Official anchors:
- `com.hbm.config.RunningConfig`
- `com.hbm.config.GeneralConfig`
- `com.hbm.config.RadiationConfig`
- `com.hbm.config.BombConfig`
- `com.hbm.config.MachineConfig`
- `com.hbm.config.WorldConfig`
- `com.hbm.config.WeaponConfig`
- `com.hbm.config.StructureConfig`
- `com.hbm.config.FalloutConfigJSON`

Port requirement:
- Create one NeoForge config layer before deep content expansion.
- Radiation, explosions, worldgen, machines, weapons, and structures must read from the same stable config pattern.

### 3. Radiation, Contamination, And Hazards

Official anchors:
- `com.hbm.handler.radiation.ChunkRadiationManager`
- `com.hbm.handler.radiation.ChunkRadiationHandler`
- `com.hbm.handler.radiation.ChunkRadiationHandlerNT`
- `com.hbm.handler.radiation.ChunkRadiationHandler3D`
- `com.hbm.hazard.HazardSystem`
- `com.hbm.hazard.HazardRegistry`
- `com.hbm.hazard.type.HazardTypeRadiation`
- `com.hbm.hazard.type.HazardTypeExplosive`
- `com.hbm.hazard.type.HazardTypeHot`
- `com.hbm.hazard.type.HazardTypeAsbestos`
- `com.hbm.hazard.type.HazardTypeCoal`
- `com.hbm.hazard.type.HazardTypeHydroactive`
- `com.hbm.util.ContaminationUtil`
- `com.hbm.extprop.HbmLivingProps`
- `com.hbm.extprop.HbmPlayerProps`
- `com.hbm.lib.ModDamageSource`

Official behavior signals:
- Chunk radiation has load/save/unload hooks and server tick updates.
- Item hazards are registry-driven and can apply to inventory, living entities, and dropped items.
- Contamination covers radiation, digamma, asbestos, Geiger/dosimeter diagnostics, and immunity/modifier rules.

Port requirement:
- Radiation must be a reusable saved-data/system layer, not individual item code.
- Hazard behavior should be data/registry driven before more radioactive content is added.

### 4. Pollution And Environmental Effects

Official anchors:
- `com.hbm.handler.pollution.PollutionHandler`
- `com.hbm.handler.pollution.PollutionHandler$PollutionData`
- `com.hbm.handler.pollution.PollutionHandler$PollutionPerWorld`
- `com.hbm.config.FalloutConfigJSON`
- `com.hbm.entity.effect.EntityFalloutRain`
- `com.hbm.blocks.generic.BlockFallout`

Port requirement:
- Pollution/fallout should be modeled as world/chunk systems.
- Visual effects and block effects should depend on the server-side environmental state.

### 5. Explosion And Bomb Framework

Official anchors:
- `com.hbm.explosion.ExplosionNT`
- `com.hbm.explosion.ExplosionNukeGeneric`
- `com.hbm.explosion.ExplosionNukeSmall`
- `com.hbm.explosion.ExplosionNukeAdvanced`
- `com.hbm.explosion.ExplosionNukeRayBatched`
- `com.hbm.explosion.ExplosionNukeRayParallelized`
- `com.hbm.explosion.ExplosionBalefire`
- `com.hbm.explosion.ExplosionThermo`
- `com.hbm.explosion.ExplosionSolinium`
- `com.hbm.explosion.ExplosionFleija`
- `com.hbm.explosion.vanillant.ExplosionVNT`
- `com.hbm.explosion.vanillant.interfaces.IBlockAllocator`
- `com.hbm.explosion.vanillant.interfaces.IBlockProcessor`
- `com.hbm.explosion.vanillant.interfaces.IEntityProcessor`
- `com.hbm.explosion.vanillant.interfaces.IPlayerProcessor`
- `com.hbm.explosion.vanillant.interfaces.IExplosionSFX`
- `com.hbm.interfaces.IBomb`
- `com.hbm.config.BombConfig`

Official behavior signals:
- Explosions are not one method. The original separates nuke logic, vanilla-style modular explosions, block allocation, block mutation, entity damage, player processing, drop chance, SFX, EMP, waste, vaporization, and config radius.

Port requirement:
- Build explosion framework before adding more bombs.
- Server blast math and client effects must stay separate.

### 6. Networking And Synchronization

Official anchors:
- `com.hbm.main.NetworkHandler`
- `com.hbm.packet.PacketDispatcher`
- `com.hbm.packet.PermaSyncHandler`
- `com.hbm.packet.toclient.BufPacket`
- `com.hbm.packet.toclient.ExtPropPacket`
- `com.hbm.packet.toclient.PermaSyncPacket`
- `com.hbm.packet.toclient.SerializableRecipePacket`
- `com.hbm.packet.toclient.TEFFPacket`
- `com.hbm.packet.toclient.TEMissileMultipartPacket`
- `com.hbm.packet.toclient.AuxParticlePacketNT`
- `com.hbm.packet.toclient.ExplosionKnockbackPacket`
- `com.hbm.packet.toserver.AuxButtonPacket`
- `com.hbm.packet.toserver.KeybindPacket`
- `com.hbm.packet.toserver.NBTControlPacket`

Port requirement:
- Establish typed NeoForge payloads before more machines, missiles, GUI buttons, recipes, effects, or player state are ported.

### 7. Saved Data And Persistent World Systems

Official anchors:
- `com.hbm.saveddata.SatelliteSavedData`
- `com.hbm.saveddata.AnnihilatorSavedData`
- `com.hbm.saveddata.TomSaveData`
- `com.hbm.saveddata.satellites.Satellite`
- `com.hbm.saveddata.satellites.SatelliteLaser`
- `com.hbm.saveddata.satellites.SatelliteMiner`
- `com.hbm.saveddata.satellites.SatelliteRadar`

Port requirement:
- Persistent systems should use modern level/server saved data before satellites, fallout, pollution, and advanced global state are expanded.

### 8. Machine And Inventory Framework

Official anchors:
- `com.hbm.tileentity.TileEntityMachineBase`
- `com.hbm.tileentity.TileEntityInventoryBase`
- `com.hbm.tileentity.TileEntityMachinePolluting`
- `com.hbm.tileentity.IConfigurableMachine`
- `com.hbm.tileentity.IBufPacketReceiver`
- `com.hbm.inventory.SlotUpgrade`
- `com.hbm.inventory.SlotDeprecated`
- `com.hbm.inventory.container.*`
- `com.hbm.inventory.gui.*`

Official behavior signals:
- Machines share sided inventories, NBT save/load, custom names, button packets, fluid gauges, redstone updates, container/screen pairs, and pollution variants.

Port requirement:
- Create shared machine base utilities before broad machine ports.
- Burner Press should become the first reference-quality machine framework.

### 9. Recipe And JEI/NEI Framework

Official anchors:
- `com.hbm.main.CraftingManager`
- `com.hbm.crafting.*`
- `com.hbm.inventory.recipes.*`
- `com.hbm.handler.nei.PressRecipeHandler`
- `com.hbm.handler.nei.AssemblyMachineRecipeHandler`
- `com.hbm.handler.nei.ShredderRecipeHandler`
- `com.hbm.handler.nei.ChemicalPlantRecipeHandler`
- `com.hbm.handler.nei.BlastFurnaceRecipes`
- `com.hbm.handler.nei.ParticleAcceleratorHandler`
- `com.hbm.packet.toclient.SerializableRecipePacket`

Port requirement:
- Machine recipes need shared serializers, validation, JEI display, and server/client sync before mass content import.

### 10. Fluid And Pressure Framework

Official anchors:
- `com.hbm.inventory.FluidStack`
- `com.hbm.inventory.fluid.FluidType`
- `com.hbm.inventory.fluid.Fluids`
- `com.hbm.tileentity.network.TileEntityFluidValve`
- `com.hbm.tileentity.network.TileEntityFluidCounterValve`
- `com.hbm.tileentity.network.TileEntityPipeBaseNT`
- `com.hbm.util.CompatFluidRegistry`

Official behavior signals:
- Original fluids include type, amount, and pressure concepts.
- Fluid networks are a machine framework dependency, not just bucket items.

Port requirement:
- Define modern fluid/pressure abstraction before chemical plants, refineries, reactors, turbines, and compressors are ported.

### 11. Power/Energy Framework

Official anchors:
- `com.hbm.uninos.networkproviders.PowerNetProvider`
- `com.hbm.tileentity.TileEntityProxyEnergy`
- `com.hbm.blocks.network.PowerCableBox`
- `com.hbm.tileentity.machine.fusion.IFusionPowerReceiver`
- `com.hbm.util.CompatEnergyControl`

Port requirement:
- Establish a NeoForge-compatible energy abstraction before power-consuming machines and fusion systems are expanded.

### 12. Worldgen, Ores, Deposits, And Structures

Official anchors:
- `com.hbm.world.gen.NTMWorldGenerator`
- `com.hbm.world.gen.MapGenNTMFeatures`
- `com.hbm.world.gen.ProceduralStructureStart`
- `com.hbm.world.gen.nbt.NBTStructure`
- `com.hbm.world.feature.OreLayer`
- `com.hbm.world.feature.OreLayer3D`
- `com.hbm.world.feature.OreCave`
- `com.hbm.world.feature.BedrockOre`
- `com.hbm.world.feature.DepthDeposit`
- `com.hbm.world.feature.OilSpot`
- `com.hbm.world.feature.Geyser`
- `com.hbm.world.feature.Meteorite`
- `com.hbm.world.dungeon.*`
- `com.hbm.config.WorldConfig`
- `com.hbm.config.StructureConfig`

Port requirement:
- Port configurable ore/deposit framework before adding full material progression.
- Structures should wait until NBT/procedural generation and loot pools are stable.

### 13. Sound And Client Effect Framework

Official anchors:
- `com.hbm.main.NTMSounds`
- `com.hbm.sound.AudioWrapper`
- `com.hbm.sound.AudioWrapperClient`
- `com.hbm.sound.AudioDynamic`
- `com.hbm.sound.SoundLoopMachine`
- `com.hbm.sound.SoundLoopSiren`
- `com.hbm.sound.MovingSoundPlayerLoop`
- `com.hbm.particle.*`
- `com.hbm.render.util.RenderScreenOverlay`
- `com.hbm.render.util.RenderMiscEffects`

Port requirement:
- Machine loops, sirens, explosions, held-item sounds, particles, and overlays need a shared client-only framework.
- Server logic should trigger events; client code should own rendering/sound playback.

### 14. Weapons, Ballistics, Missiles, And Satellites

Official anchors:
- `com.hbm.entity.missile.*`
- `com.hbm.render.util.MissilePart`
- `com.hbm.render.util.MissileMultipart`
- `com.hbm.render.tileentity.RenderMissileAssembly`
- `com.hbm.handler.guncfg.BulletConfigFactory`
- `com.hbm.handler.BulletConfiguration`
- `com.hbm.config.WeaponConfig`
- `com.hbm.saveddata.satellites.*`

Port requirement:
- Do not port weapons/missiles as isolated items. They depend on configs, networking, entities, client effects, saved satellite data, explosions, and sounds.

## Recommended Port Order From Official Structure

1. Creative tabs and registry taxonomy.
2. Config framework.
3. Damage sources, player/living state, and hazard registry.
4. Radiation/chunk fallout/pollution saved systems.
5. Networking and typed payloads.
6. Explosion/bomb framework.
7. Shared machine/inventory/menu/recipe framework.
8. Fluid and pressure framework.
9. Power/energy framework.
10. Sound/particle/client-effect framework.
11. Worldgen/deposit/structure framework.
12. Content families: materials, ores, machines, bombs, weapons, missiles, satellites.

## Immediate Action List

1. Build a modern `HbmConfig` split matching original config families.
2. Replace current radiation prototype with a registry-driven hazard/radiation framework.
3. Add chunk/world saved data for fallout/radiation/pollution.
4. Define typed payload conventions for player state, block entity sync, GUI buttons, and visual effects.
5. Design the explosion framework before porting additional bombs.
6. Finish Burner Press only as part of the shared machine/recipe framework.
7. Keep additional item imports pending until their owning system exists.
