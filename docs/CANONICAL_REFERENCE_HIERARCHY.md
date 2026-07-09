# HBM Port Canonical Reference Hierarchy

This file is the single authority for how references are used in the 1.21.1 NeoForge port.

## Reference Tiers

| Tier | Source | Role | Answers |
| --- | --- | --- | --- |
| 1A | Original HBM 1.7.10 source | Canonical gameplay authority | What should this do? |
| 1B | Original HBM 1.7.10 jar | Packaged asset/runtime authority | What shipped in the original build? |
| 2 | HBM Reloaded 1.12.2 | Primary migration reference | How was this faithfully migrated before? |
| 3 | HBM Community/Rebirth/Waldemar variants | Secondary parity and bug-fix reference | Does another migration clarify this? |
| 4 | HBM Neo Edition 1.21.1 | NeoForge implementation reference | How can this be implemented in NeoForge 1.21.1? |
| 5 | HBM Well-Forged | Engineering and optimization reference | Can this be cleaner or faster without changing behavior? |
| 6 | HBM Modernized | Architecture reference | What modular structure may help? |
| 7 | Current port | Implementation target | What currently exists here? |

## Rules

- Original 1.7.10 source and jar decide gameplay, recipes, balance, GUI layout, assets, sounds, hazards, explosions, and progression.
- Newer projects do not override original gameplay because they are newer or cleaner.
- Use Reloaded, Waldemar, Rebirth, and similar forks to understand migration choices and possible bug fixes.
- Use Neo Edition for NeoForge mechanics such as registration, block entities, menus, rendering, networking, and side separation.
- Use Well-Forged and Modernized only for invisible engineering improvements or architecture ideas.
- The current port is never an authority. If it disagrees with Tier 1, the current port has a parity defect.

## Workflow

For a system-level decision, compare relevant references before implementation:

1. Identify what the original source and jar do.
2. Check migration references for prior porting decisions.
3. Check Neo Edition only for 1.21.1 NeoForge mechanics.
4. Borrow engineering or architecture only when gameplay remains unchanged.
5. Update `docs/PARITY_AUDIT.md` with the resulting status and evidence.

For a small content item, use `tools/hbm-reference.ps1 compare <name>` first, then inspect only the tiers that contain relevant files.
