# Machine Regression Template

Use this alongside a machine feature packet. Pure calculations belong in `src/test`; registry-backed interactions belong in NeoForge GameTests or the manual review record.

| Contract | Test type | Required evidence |
| --- | --- | --- |
| Fuel, heat, progress, and cooldown math | JUnit | Deterministic values and boundary cases. |
| Recipe input, output capacity, and invalid input | GameTest/manual | Correct result, blocked invalid result, and full-output behavior. |
| Stamp/tool durability and container items | GameTest/manual | One completed operation and break/replacement behavior. |
| Save/reload and chunk unload/reload | GameTest/manual | State and inventory preserved. |
| Sided automation | GameTest/manual | Allowed insertion/extraction by side. |
| Menu, GUI, item icon, renderer, and JEI | Matched visual evidence | Source/reference and port at the same state and scale. |
| Dedicated server | `tools/server-smoke.ps1` | Fresh ready-state log. |
