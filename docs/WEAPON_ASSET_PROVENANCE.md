# Pilot Weapon Asset Provenance

Audit date: 2026-08-10

This inventory covers the first gun-framework milestone only: Target Pistol (`gun_star_f`), StG 77, SPAS-12, and Congo Lake. It records the checked-in source assets, source-informed GeckoLib animation translations, the runtime OBJ-to-Gecko geometry bridge, and the GPL-attributed Superb Warfare presentation-code adaptation. Static binding checks do not establish in-game visual parity.

## Source anchors and licensing

| Reference | Local evidence | SHA-256 | Use |
| --- | --- | --- | --- |
| Original HBM source archive | `C:/Users/wawah/Downloads/Hbm-s-Nuclear-Tech-GIT-master.zip` | `fde64f38964b3b6d097e72f51a38329f57b8a8922be412012164f82834a73c2a` | Canonical pilot models, textures, legacy bus animations, and sounds. |
| Original HBM packaged jar | `C:/Users/wawah/Downloads/HBM-NTM-[1.0.27_X5687].jar` | `121482b2d199c7e3b1cd7b148293077265df0a90efbf92f6db48968935ab20c3` | Confirms the assets shipped in build `1.0.27_X5687`. |
| HBM Neo Edition source archive | `C:/Users/wawah/Downloads/HBM/HBMsNTM-NEO-EDITION-master.zip` | `a7f0fa5d8a986f485c97e527f34215b0714481591f34a29a5667f52c754679a0` | Intended SPAS-12 source, but the audited archive contains no SPAS-12 model, texture, or animation. |
| Current sound catalog | `src/main/resources/assets/hbm/sounds.json` | `a75ad576332cac8ce75f244bc9e4124bf2d7727087f45e556ec6dfd225ad66f6` | Lowercase 1.21.1 resource-event mapping for the original HBM audio. |

Original HBM and the audited Neo Edition archive both include `LICENSE` and `LICENSE.LESSER`; their README license statements identify GNU LGPL version 3. No per-file author or alternate asset license is embedded in the pilot OBJ, PNG, JSON, or OGG files. Preserve the upstream HBM credits and LGPL notices with distributions. This inventory is provenance evidence, not a legal re-licensing of individual contributions.

Superb Warfare commit `9b5284f42ef79532e6fb7f03ab07425c693b0b43` is the behavior and renderer-architecture source. GPL-3.0 code adaptations are enumerated in `THIRD_PARTY_NOTICES.md` and in file-local comments. No Superb Warfare model, texture, animation, sound, rig coordinate, or other visual asset is selected or inventoried here.

## Source decision and conversion gate

| Weapon | Required source decision | Checked-in reality | Conversion status |
| --- | --- | --- | --- |
| Target Pistol | Original HBM | Original HBM `star_f` OBJ/PNG are present. The pistol animation is code-driven upstream; no legacy `star_f` animation JSON exists. | GeckoLib animation translation and exact OBJ/UV runtime bridge are present. The modern renderer synthesizes camera/root, hand, and muzzle-flare bones and renders the local player's skin/sleeves without adding third-party art. The accepted hip-fire mesh/hands and physical-sight ADS position are separate baked endpoints. Static binding passes; fire/reload/sprint transition QA remains open. |
| StG 77 | Original HBM | Original HBM OBJ/PNG/bus-animation JSON are present. | GeckoLib animation translation, exact OBJ/UV runtime bridge, and HBM-authored Superb-style presentation rig are present. Static bone binding passes; replacement visual QA remains open. |
| SPAS-12 | Neo Edition intended | The available Neo Edition archive has no SPAS-12 assets. The checked-in OBJ/PNG/bus-animation JSON are Original HBM. Do not label them Neo-derived. | Intended Neo source remains blocked. The Original-HBM fallback animation, exact OBJ/UV runtime bridge, and HBM-authored presentation rig are present; replacement visual QA remains open. |
| Congo Lake | Original HBM | Original HBM OBJ/PNG/bus-animation JSON are present. | GeckoLib animation translation, exact OBJ/UV runtime bridge, and HBM-authored Superb-style presentation rig are present. Static bone binding passes; replacement visual QA remains open. |

All four checked-in OBJ files are text-identical to the Original source/jar after normalizing CRLF to LF. Their raw working-tree hashes differ from the archives only because Git checked them out with CRLF line endings. The PNG and legacy animation JSON hashes match the Original archive bytes exactly.

## Geometry, texture, animation, and icon inventory

| Weapon | Asset | Current local SHA-256 | Legacy contents |
| --- | --- | --- | --- |
| Target Pistol | `src/main/resources/assets/hbm/models/weapons/star_f.obj` | `e3f9c719c3493cd335b9121705db52a2e272cb06794bc114faeba8e8a836ff3b` | OBJ parts: `Bullet`, `Gun`, `Hammer`, `Mag`, `Slide`. Normalized source hash: `7cfa1c24eed126ecb7706e8efa3b0c1ac16d91abf1d86d97f1033f18f57099b8`. |
| Target Pistol | `src/main/resources/assets/hbm/textures/models/weapons/star_f.png` | `cea88f4f9f0a0c48d456096a40ae9cb91013f9fdacfc113dbcee476fcaac58f8` | Original texture atlas. |
| Target Pistol | legacy animation JSON | missing | Upstream `XFactory22lr.LAMBDA_STAR_F_ANIMS` creates bus animations in code rather than loading a JSON file. |
| Target Pistol | `src/main/resources/assets/hbm/animations/weapon/star_f.animation.json` | `a820e4efc211306e9a30c2edecf687f24420a4c29c0ca8f3955c1585f218d6ad` | GeckoLib `1.8.0`; 11 framework clips plus coordinated `reload_normal` and `reload_empty` presentation clips. The independently authored MP-443-style motion leaves equip, idle, ADS, sprint, lowering, and recoil to one frame-interpolated procedural state while the clip owns only mechanism/reload motion. It animates `Slide`, `Hammer`, and `Mag` without redistributing Superb Warfare artwork. |
| Target Pistol | `src/main/resources/assets/hbm/models/item/gun_star_f.json` | `ce5c6e3d7cfb444b1d880a608d10c9ced431784abc25cd62d315cd5a66df5d4c` | Modern `builtin/entity` model; renderer-controlled first-person presentation prevents JSON/profile double transforms. Enlarged GUI and the reference MP-443 third-person translation/scale remain declarative and isolated from first-person calibration. |
| StG 77 | `src/main/resources/assets/hbm/models/weapons/stg77.obj` | `eb10c829a122eeffbd2a021a828497f29dac738cb252083b8169fdc26605df49` | OBJ parts: `Barrel`, `Breech`, `Bullets`, `Gun`, `Handle`, `Lever`, `Magazine`, `Safety`. Normalized source hash: `6dcc51092557f102d2e38eabcfaf00155096b1c1807f3d5b5892ad4e665c58d4`. |
| StG 77 | `src/main/resources/assets/hbm/textures/models/weapons/stg77.png` | `e43831d00ac1b82b4af712778774077ac025e3ae78cc6389ca75d032864e32b8` | Original texture atlas. |
| StG 77 | `src/main/resources/assets/hbm/models/weapons/animations/stg77.json` | `1b9cb1f0df6c38f337f3556e82936fa91a7256b96d125253971e244b61b17608` | Legacy clips: `Fire`, `FireDry`, `Inspect`, `Reload`; legacy bus format, not GeckoLib. |
| StG 77 | `src/main/resources/assets/hbm/animations/weapon/stg77.animation.json` | `f77d317c7c7ba4725d6fa5a10c5d47ad157463c1d7fd61a8e5d03352309dd420` | GeckoLib `1.8.0`; 11 required clips. Fire/reload motion covers `Breech`, `Handle`, `Safety`, and `Magazine`, mapped from the audited bus clips. |
| StG 77 | `src/main/resources/assets/hbm/models/item/gun_stg77.json` | `fa6e69fa8ac47a35040fb392ef70b6cf9f51bc6260d9828b8b88d27117c7da8e` | Modern `builtin/entity` model with renderer-controlled first-person presentation and declarative third-person, GUI, ground, and fixed transforms. |
| SPAS-12 | `src/main/resources/assets/hbm/models/weapons/spas-12.obj` | `33e54f96c19a09d7637e629b75d3baecbf3665f05eab4801c4141bedebf0b5da` | OBJ parts: `MainBody`, `PumpGrip`, `Shell`, `ShellFore`. Normalized source hash: `5e25cf9c59e0c689dc64d1479f889723b469e050786e9313d9c597e0ead109a7`. |
| SPAS-12 | `src/main/resources/assets/hbm/textures/models/weapons/spas-12.png` | `4b66138c53a10c76e5de273b83b5f6b96eaebe49066cd3f623d644446916f7ca` | Original texture atlas; no Neo Edition candidate was found. |
| SPAS-12 | `src/main/resources/assets/hbm/models/weapons/animations/spas12.json` | `5d6691f9219f7d94f322724ae6f85065e150f1d1b7c27cc56e1dd3698dede8c4` | Legacy clips: `Fire`, `FireAlt`, `FireDry`, `Inspect`, `Jammed`, `Reload`, `ReloadEmptyStart`, `ReloadEnd`, `ReloadStart`; legacy bus format, not GeckoLib. |
| SPAS-12 | `src/main/resources/assets/hbm/animations/weapon/spas12.animation.json` | `0fd2b496e8cfcf468504fa53b6031a7adf9231bc5f29e6cd0126867714a5c60f` | GeckoLib `1.8.0`; 11 required clips. Fire/reload motion covers `PumpGrip`, `Shell`, and `ShellFore`, mapped only from Original HBM. This is not a Neo-derived asset. |
| SPAS-12 | `src/main/resources/assets/hbm/models/item/gun_spas12.json` | `fa6e69fa8ac47a35040fb392ef70b6cf9f51bc6260d9828b8b88d27117c7da8e` | Modern `builtin/entity` model with renderer-controlled first-person presentation and declarative third-person, GUI, ground, and fixed transforms. |
| Congo Lake | `src/main/resources/assets/hbm/models/weapons/congolake.obj` | `be90c4ca53f62bc264a29780590d5dc071abfb9df68e8a33095b2a6597c26420` | OBJ parts: `GuardInner`, `GuardOuter`, `Gun`, `Loop`, `Pump`, `Shell`, `ShellFore`, `Sight`. Normalized source hash: `9bf96cfc1e903a2a5d7100f10f1ec999f0b7dac21f91f0f117ed9baddefaac4a`. |
| Congo Lake | `src/main/resources/assets/hbm/textures/models/weapons/congolake.png` | `6e1de2f21f87d3bae45cc5342e3b3739a43115b83facf146e869cbb36773fd0e` | Original texture atlas. |
| Congo Lake | `src/main/resources/assets/hbm/models/weapons/animations/congolake.json` | `eff3c714f71aade099ae505db3a5a2630975ef28a871f8d773cdb55553454bcb` | Legacy clips: `Equip`, `Fire`, `FireEmpty`, `Inspect`, `Jammed`, `Reload`, `ReloadEmpty`, `ReloadEnd`, `ReloadStart`; legacy bus format, not GeckoLib. |
| Congo Lake | `src/main/resources/assets/hbm/animations/weapon/congolake.animation.json` | `8de73ce7794f1daf7592ca343bc845be84d46163c3c52ad63620058bb58d266c` | GeckoLib `1.8.0`; 11 required clips. Fire/reload motion covers `Pump`, `Shell`, `ShellFore`, `GuardInner`, and `GuardOuter`, mapped from the audited bus clips. |
| Congo Lake | `src/main/resources/assets/hbm/models/item/gun_congolake.json` | `fa6e69fa8ac47a35040fb392ef70b6cf9f51bc6260d9828b8b88d27117c7da8e` | Modern `builtin/entity` model with renderer-controlled first-person presentation and declarative third-person, GUI, ground, and fixed transforms. |

No lossy `*.geo.json` duplicate exists. `ObjBakedGeoModelLoader` (`4b025960efc7899819a205e2eb830470de3f3a824b7f16e3d2d72dabfda2463d`) reads the original OBJ vertices, UVs, normals, and faces into a cached GeckoLib `BakedGeoModel`; each OBJ object/group becomes an animation bone. It surrounds that mechanism with synthetic `camera` and `root` bones and adds empty `Righthand`, `Lefthand`, and `flare` bones from `SuperbGunRig` (`bcfc27091c3c91bc9bc93dcc828b0716b1382eb7663feedca7fb51dda5971f4c`). These bones contain no copied geometry or texture. The Target Pistol additionally uses a `Gun -> model_space -> Gun_mesh` split, keeping both player-hand bones outside the mesh space. `HbmPlayerArmRenderer` (`c085e6231f42c2cbc5ea2952da2ec1b0822daf0c8641ce5f5426090140586f7a`) uses the GPL-attributed generic bone-bound player-arm setup; all weapon-specific pivots and rotations remain independently authored HBM values. `TargetPistolCalibrationState` (`609efb632ebdcf3a59bb6d7515c49681278ba8c4e9b8cfd35da18a98b884a106`) preserves separate hip and ADS endpoints, isolates GUI/third-person transforms, and applies absolute hand rotations so GeckoLib deltas cannot accumulate into an ADS arm spin. `TargetPistolCalibrationMarkerRenderer` (`2ef54c57820de4b24132c43941966b5a2be10cd9373d43ba51bcac6eb52ba2fc`) displays only HBM-authored grip/muzzle references and synthetic hand anchors. `HbmGunGeoRenderer` (`5c0d706bf1a1ee7197c568222498bfae2f32ca215a0f1d85859d2145f91a68a5`) dispatches player-skin arms and first-person-only model-local muzzle effects while suppressing unsupported left/off-hand display contexts. `HbmMuzzleFlashRenderer` (`b3cf82dea62bf3e9dc0750503a0fa7e0d6f45a38f3223d0bcdcb18581e8883af`) translates HBM's muzzle coordinate into the same `(-x, y, z) / 16` model space and uses bounded effect sizes. `HbmGunArmPose` (`7aa17533b8cca51344474e0e5ba665a75d07f16bb4bcf990e790486225d40741`) adapts Superb Warfare's normal `BOW_AND_ARROW` and grounded-sprint `CROSSBOW_CHARGE` selection without importing visual assets or weapon coordinates. The Target Pistol item model (`ce5c6e3d7cfb444b1d880a608d10c9ced431784abc25cd62d315cd5a66df5d4c`) uses the reference MP-443 third-person translation and scale but only the original HBM mesh and texture. `SuperbGunPresentationState` (`ea1b6661fd4f4c062dcbf9d834de92817756396b7d3e0414c5cfffd0c5e03939`) implements continuous motion, ADS easing, nonlinear recoil, calibrated movement cadence, complete ADS sway suppression, a reload envelope, monotonic sprint/lowering, stack-identity resets, and frame interpolation. `ClientWeaponController` (`13212315a49db9706797edc26296552668cae7b2f5431b1c7d23be00a269b09c`) controls input prediction, disables native view bob during ADS while restoring the user's setting afterward, centers the hip reticle on the server camera ray, suppresses off-hand and third-person effects, and renders the source-guided HUD. `WeaponAim` (`29f75c5c20c256b2ab623cc591a82a9e0cf497afd7cc4b1f486c4ed46d43fd71`) applies the data-defined zero only to ADS trajectories and matching HUD projection. `HitFeedbackAnimation` (`d183f721bf884911eed1e469d0d991f9e8c0846e67f5cebf179bc27fac32872f`) contains four code-rendered body/headshot/kill/headshot-kill states and no third-party artwork. `BallisticsService` (`e23736b3777a646219a6b864ed4fddbd178def87eb22cdb035caf3f212cd4df7`) separates world impacts from shooter-only confirmed damage and emits the combined headshot-kill state. `HbmGunItem` (`6f3d201b7864b2ef6df0666a33e1da1bc3cd90a8dc6465c74419beb2325d27fe`) suppresses vanilla re-equip animation for component-only changes while retaining real switching. All four animation documents provide the 11 required states with nonempty mechanism motion. The validator confirms every animated bone exists in its source OBJ or explicit synthetic contract. The accepted Target Pistol hip-fire and ADS values are baked; combined action-state visual parity still requires in-game inspection.

## Sound inventory

These are the Original HBM sound events used by the legacy pilot configurations and orchestras. Shared casing families are included because they are part of the intended ejection effects. Built-in modern definitions may use a smaller subset, but every referenced event must still resolve through `sounds.json` to an existing OGG.

All 38 OGG files listed below byte-match the Original `1.0.27_X5687` jar. Only resource filenames/event IDs were normalized to lowercase for modern resource-location rules.

### Target Pistol

| Event | Resolved OGG SHA-256 |
| --- | --- |
| `hbm:weapon.fire.pistollight` | `weapon/fire/pistollight.ogg` — `c46030ec8d43bf1c2f2d1c5f220d06b85f82d7114140054662b16e7272291bca` |
| `hbm:weapon.reload.dryfireclick` | `weapon/reload/dryfireclick.ogg` — `4fd3d98a7255948e92c6de880ffd2a474ec0c7ed7c308f9302d9a02efd4d3698` |
| `hbm:weapon.reload.pistolcock` | `weapon/reload/pistolcock.ogg` — `831b888cd796f551275495683958c7a857fd515ea6e7bb0764542f61ad224d59` |
| `hbm:weapon.reload.magremove` | `weapon/reload/magremove.ogg` — `d1f70a7767a1f898a24c163866214903c3dacb921f98f4f05be2b9e3b9f10580` |
| `hbm:weapon.reload.maginsert` | `weapon/reload/maginsert.ogg` — `e49f8a0fc3e8e2982f0932635c131908def693ee940861ac9c61583addb7f600` |
| `hbm:weapon.reload.revolverclose` | `weapon/reload/revolverclose.ogg` — `ded7781e2fc1ef3e8e0113a3cca4501eb6a6c8d3dafbf8099e9dfc736a3b6f85` |
| `hbm:weapon.casing.small` | `small1.ogg` — `2d06ef677a64ed3bfc9ac3b9225d2c5c5435b47c90185d59d2f6a143ba513bf6`; `small2.ogg` — `46bc922293ea0e56036ff04605034acf8056801d75ec32f9de621799e6cd37f9`; `small3.ogg` — `0715a552325b908ef7364b9efa45dd9fc02782f2e85ed71c6687db46e8de0868` |

### StG 77

| Event | Resolved OGG SHA-256 |
| --- | --- |
| `hbm:weapon.fire.assault` | `weapon/fire/assault.ogg` — `2e47b7ae31e9878c6b65d491bd60d64c1d70a7d9d7423fc9ca7b18bdc0288819` |
| `hbm:weapon.reload.dryfireclick` | shared Target Pistol file above |
| `hbm:weapon.reload.pistolcock` | shared Target Pistol file above |
| `hbm:weapon.reload.magremove` / `hbm:weapon.reload.maginsert` | shared Target Pistol files above |
| `hbm:weapon.reload.magsmallremove` | `weapon/reload/magsmallremove.ogg` — `bbeee07cd174a7fd054e66f1927df04ae83aecfaae86f784f989261f04f00f8f` |
| `hbm:weapon.reload.magsmallinsert` | `weapon/reload/magsmallinsert.ogg` — `1cdc2f5101e6216960841a504eeae9412750fa315fd2a429819525d8144db7a7` |
| `hbm:weapon.reload.impact` | `impact1.ogg` — `500bf43058857dfa81f4611d6fe551309a1fc30ea898b6342af89652e4f62fc2`; `impact2.ogg` — `09a43291307325599e4cc907bc3cf2d9dac842504d24e931789a9baffd12cf02`; `impact3.ogg` — `e41d0493552d317ad35f0e76e5ee6c1dfe3311323de05d9444b0df8333241a9e` |
| `hbm:weapon.casing.medium` | `medium1.ogg` — `9ac1f5998424ca8748de1ae781da6b6e190b8c7aa3e42ad0a99f6bb5f7690a1b`; `medium2.ogg` — `973c969908cf07c08a67af78ef3c67e0a2c6988547488e3a83a08dd829ba3621`; `medium3.ogg` — `b4c6869625e8191f6c3ef3d58c6ad3fc5d430bed76d98e025f0edea49fad691a` |

### SPAS-12

| Event | Resolved OGG SHA-256 |
| --- | --- |
| `hbm:weapon.shotgunshoot` | `weapon/shotgunshoot.ogg` — `76e0fefc71f10d6431d0112335fddb6df3c0abb18f20ebee7c6a5ef1f1f13887` |
| `hbm:weapon.reload.shotguncock` | `weapon/reload/shotguncock.ogg` — `edd1724b8113db85961b114a5bf4a2929711bb4a1f33c311c5b5a06e4263e099` |
| `hbm:weapon.reload.revolvercock` | `weapon/reload/revolvercock.ogg` — `3f273512745d4bb2f84e2d68864484838cbae008c3d63c57bb2fec1f47061102` |
| `hbm:weapon.reload.shotgunreload` | `shotgunreload1.ogg` — `588408822878e9e45424a0e977bab5a11a77fde2e3a5305546301cf125326d56`; `shotgunreload2.ogg` — `e80de931ae5675c7c99f236e61fa1852575270270c7c9cf4adfa611f7402e877`; `shotgunreload3.ogg` — `08459cf0816f96c016076f72afa89c9fe68ee0dcf77348197a4163e6b850a19a` |
| `hbm:weapon.reload.shotguncockopen` | `weapon/reload/shotguncockopen.ogg` — `7ed95fe641594f36beeef57ed266d7e9b5e39e02ab36ae335dd685e914a6f4ca` |
| `hbm:weapon.reload.shotguncockclose` | `weapon/reload/shotguncockclose.ogg` — `373112bdbb6e54a774bf7a544f36fc49d496323ca9d9bd6bf79ccecd0193d8a7` |
| `hbm:weapon.foley.gunwhack` | `gunwhack.ogg` — `6c5d00a5f699314e4948f4d27a2f4468e277d6af1a7201f38e314c94ab5fa732`; `gunwhack2.ogg` — `d307e8cc68b99836afddefb5ae6f1f5ad020a02aa523ef6428be17ca76afc7ff` |
| `hbm:weapon.casing.shell` | `shell1.ogg` — `6a99cca9d8613f363603dd4c85375b4a9e2002a1f663231eafcece575ee12e97`; `shell2.ogg` — `f18cbde636692955ce655049f290026b0bd793230f94784c814304ac9862e177`; `shell3.ogg` — `9a09408b8ce966b1572cbf23b928b535efffed3e8bfcdc0a36cfc9bf33fcaf16` |

SPAS-12 also shares the dry-fire and revolver-close files listed for the Target Pistol.

### Congo Lake

| Event | Resolved OGG SHA-256 |
| --- | --- |
| `hbm:weapon.glshoot` | `weapon/glshoot.ogg` — `303d27d78b44fe4d32cdd908954d3e8e549910958ddebe203de1e03e1a38db9b` |
| `hbm:weapon.glreload` | `weapon/glreload.ogg` — `0a04e05da6123f9b17a233d60511eda342d92f10141859ef360f7f0ae2b34554` |
| `hbm:weapon.glopen` | `weapon/glopen.ogg` — `7e9ea7155bc1e6a8740fe730991d47be44d83af179db6fad3e689421d1352d2c` |
| `hbm:weapon.glclose` | `weapon/glclose.ogg` — `1408eb88a178dddfd30634d04fae3c959d30b74d99cc85ac2c2245daaf1d6633` |
| `hbm:weapon.casing.large` | `large1.ogg` — `e4b6bfe16160c33da621ed6de2f9f4f07fa059dc4c8866a802a39f524dbdd8a0`; `large2.ogg` — `6d4ea988bab47e9176c285cd2afc99b1e357eff879611e445ff70140d7c84ddb`; `large3.ogg` — `024f44a6241d187b7a2a32ed2e7f024725d3949900fa93de247b61667b40accb` |

## Missing-resource conclusions

- The selected Neo Edition SPAS-12 source is unavailable in the audited archive. A different Neo revision or a specifically supplied asset set is required before that source decision can be fulfilled.
- All four pilots have an exact runtime OBJ-to-Gecko geometry rig, a Superb-style camera/root/hand/flare hierarchy, and statically valid animation-bone binding. The previous Target Pistol profile failed visual inspection and was removed. The replacement's hip-fire mesh/hands and centered physical-sight ADS endpoint are approved and baked; combined firing, reload, sprint, and lowering transitions remain visually unverified.
- Target Pistol still has no legacy animation JSON; the active GeckoLib document is explicitly translated from its upstream code-driven animation logic.
- The four modern item-model JSON files deliberately use the animated 3D renderer rather than separate flat icons.
- HBM provides the close gunshot samples above, but no dedicated near/far layers for these four weapons. Distance treatment must be produced through the modern sound implementation without importing Superb Warfare audio.
- Geometry/UV, moving-part, first-person, third-person, and shader compatibility remain visual acceptance tests. File presence and hashes cannot prove rendering correctness.

Run `powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\tools\validate-weapons.ps1` to validate the built-in definitions, HBM sound resolution, GeckoLib clips and mechanism motion, animation-to-OBJ bone binding, item models and textures, runtime geometry-bridge binding, and the complete pilot ammunition recipe chain. It cannot replace in-game geometry and visual acceptance tests.
