# Third-Party Notices

This project preserves attribution and license boundaries for the upstream works used as implementation and content references. A reference entry does not grant rights beyond the terms published by its owner.

## HBM's Nuclear Tech Mod (Original, Minecraft 1.7.10)

- Project: HBM's Nuclear Tech Mod
- Author and copyright holder: HbmMods (The Bobcat) and the project's contributors
- Source: https://github.com/HbmMods/Hbm-s-Nuclear-Tech-GIT
- Role in this project: canonical source for HBM identity, gameplay, code, and assets already ported with attribution
- License: the upstream README declares the software licensed under the GNU Lesser General Public License version 3. The upstream repository distributes both `LICENSE` (GNU GPL version 3) and `LICENSE.LESSER` (GNU LGPL version 3); copies of both texts are retained at this repository's root.

File-local copyright or license notices from the original project remain controlling and must be preserved when material is ported.

## HBM's Nuclear Tech Mod: NEO EDITION (Minecraft 1.21.1)

- Project: HBM's Nuclear Tech Mod: NEO EDITION
- Maintainer: ohiomannnn and the project's contributors; original HBM credit belongs to HbmMods (The Bobcat)
- Source: https://github.com/ohiomannnn/HBMsNTM-NEO-EDITION
- Role in this project: NeoForge 1.21.1 implementation reference and source of specifically documented HBM assets when selected by the asset-provenance manifest
- License: the upstream README declares the software licensed under the GNU Lesser General Public License version 3. The upstream repository distributes both `LICENSE` (GNU GPL version 3) and `LICENSE.LESSER` (GNU LGPL version 3).

File-local copyright or license notices from NEO EDITION remain controlling and must be preserved when material is ported.

## Superb Warfare 0.8.9 (Minecraft 1.21.1)

- Project: Superb Warfare
- Authors: Atsuishio, Roki27, Light_Quanta, and the project's contributors
- Exact reference: https://github.com/Mercurows/SuperbWarfare/tree/9b5284f42ef79532e6fb7f03ab07425c693b0b43
- Commit: `9b5284f42ef79532e6fb7f03ab07425c693b0b43`
- Role in this project: behavior, balance, and architecture reference for the modern gun framework
- Code license: GNU General Public License version 3, as declared by the upstream README at the referenced commit
- Asset restriction: the upstream README states that models, textures, and other resources belong to the Superb Warfare team unless specially marked and may not be used without the team's authorization.

No Superb Warfare model, texture, animation, sound, or other asset may be imported into this repository without explicit authorization from its rights holder and a corresponding update to this notice. This restriction applies even when an asset is present in a public source tree or compiled JAR.

No Superb Warfare code or asset is included by the dependency baseline. If GPL-3.0-covered code is adapted later, the implementing change must identify the source file and exact commit, preserve required notices, and remain compliant with GPL-3.0.

## Framework Dependencies

The build resolves Kotlin for Forge `5.10.0`, GeckoLib `4.7.5` for NeoForge 1.21.1, and Curios `9.2.0+1.21.1` from their publishers' Maven repositories. They remain separate runtime dependencies and are not copied from Superb Warfare or bundled into this project's source assets.
