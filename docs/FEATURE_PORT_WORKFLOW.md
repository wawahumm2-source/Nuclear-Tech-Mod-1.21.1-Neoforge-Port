# HBM Feature Port Workflow

This is the required workflow for every framework, machine, GUI, and content family.

## 1. Specification Approval

1. Create `docs/feature-specs/<feature>.md` from the template.
2. Use Tier 1 source and jar evidence for behavior, assets, sounds, GUI coordinates, recipes, hitboxes, and manual checks.
3. Classify the work as `framework`, `pilot machine`, or `content family`.
4. Record every non-parity choice in `docs/approved-divergences.md` with a reason and acceptance check.
5. Obtain the first approval before implementation.

## 2. Implementation Passes

Keep these passes independent: server behavior, resources/data, renderer, menu/screen, and JEI integration. Native NeoForge or JEI APIs are preferred over custom draw code. Source GUI dimensions and coordinate maps are the default; compacting, cropping, masking, or replacement artwork requires an approved divergence.

## 3. Review Build Gate

Before Quick View, complete two reviews:

1. Implementation audit against the approved feature packet.
2. Adversarial parity audit against Tier 1 evidence and regression risks.

Then run `tools/stability-gate.ps1`. It requires resource validation, build, data generation, a dedicated-server smoke test, and a fresh server-log scan. Missing assets, recipes, sounds, server/client separation errors, placeholders, and stale audit claims block the review build.

## 4. Visual and Gameplay Approval

Capture matched source/reference and port screenshots in `build/visual-evidence/<feature>/`. Match state, view, GUI scale, and inputs. List intentional differences in the feature packet. Quick View is launched only after the review-build gate, unless Mr. Hummithy explicitly asks for an earlier diagnostic launch.

The second approval is required before a feature is complete. Update `docs/PARITY_AUDIT.md` only with verified behavior and evidence.

## 5. Family Expansion

Finish one reference-quality pilot before porting a related content family. New content must use the approved framework, assets, recipes, creative tab, and validation matrix. Do not commit or push without explicit instruction from Mr. Hummithy.
