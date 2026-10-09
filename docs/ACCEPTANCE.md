# Acceptance

Part 2 release decision: **PASS with documented open items** (see the OPEN rows).

## Part 2 acceptance

`PASS` = implemented and verified by the named automated check; `PASS (visual)` = implemented and inspected in captured frames by the developer; `OPEN` = not verifiable in this environment. Evidence paths are under `docs/evidence/part2`.

| Criterion | Status | Evidence |
| --- | --- | --- |
| 26.3 / Java 25 / Loader / API unchanged; clean build | PASS | `clean-build.log`, `artifact-validation.json` |
| Server authority, wire schemas, destruction budgets, preview and PvP policy intact | PASS | unchanged server code; 7 dedicated GameTests; regression scenario |
| All three gameplay loops, persistent craters, save/reopen | PASS | `client-suite.log` |
| Camera restoration (skip, completion, cancel, dimension, disconnect) | PASS | regression + dedicated-server suites; camera JUnit (roll, decline, release) |
| Dedicated-server initialization and networked strike | PASS | dedicated-server client suite |
| Custom shaders load with fallbacks; OIT compatible | PASS | pipelines-ready log lines; OIT capture; optional registration + vanilla fallback code path |
| Helios authored collector with recognizable alignment | PASS (visual) | `frames/helios_03..06`, `sheet-helios.png` |
| Helios layered beam, entry, distinct shockwaves, aftermath | PASS (visual) | `frames/helios_07..16` |
| Singularity original anomaly with animated gravitational structures and implosion | PASS (visual) | `sheet-singularity.png` |
| Glassfall authored lattice; distinct projectile meshes on authoritative trajectories | PASS (visual + unit) | `sheet-glassfall.png`; `TrajectoryTest` |
| Distinct audiovisual identities per weapon | PASS (visual) | sheets; 28 weapon-specific sounds |
| Original textures, models (procedural meshes) and sounds; no third-party assets | PASS | `tools/assets`, `audio-validation.json` |
| Directed camera shots, roll, shake, occlusion lift, cave decline | PASS (visual) | sheets; `CameraScripts` |
| Holographic HUD, reticle, selector, banners; settings screen | PASS (visual) | `frames/ui_*.png` |
| Quality presets LOW/MEDIUM/ULTRA | PASS (visual) | `sheet-modes.png` |
| Reduced motion keeps player view while effects play | PASS (visual) | `sheet-modes.png` (reduced-motion row) |
| Reduced flashing default on; zero-particle mode; rebindable keys | PASS | `PresentationEngineTest`; native key mappings |
| Bounded resources under concurrent load | PASS | stress rows in `renderer-diagnostics.txt` (particle cap 1,024 held, vertex budgets dropped excess) |
| Performance measured, not estimated | PASS (single machine) | `VALIDATION.md` measurements |
| Final artistic and audio approval by a human | OPEN | requires listening/viewing by the user |
| Multiple independent graphical clients and late observers | OPEN | one client available; embedded two-owner and one-client dedicated tests only |
| Third-party render/shader/claim mod compatibility | OPEN | not installed |
| Survival-owner death in each phase; dense real-world timing | OPEN | death path covered by camera/strike cancellation code and earlier tests only |
| True screen-space lensing / heat refraction | Not implemented | geometric illusion documented in `PART2_RELEASE_REPORT.md` |

## Part 1.5B acceptance (historical)

Statuses refer to actual checks in this repository. `PASS` is automated unless explicitly marked visual inspection. Final run details and versions are in `VALIDATION.md`.

Part 1.5B engineering readiness: **CONDITIONAL PASS**. All confirmed critical/high defects are resolved (none were reported); targeted repairs pass 27 JUnit tests, seven dedicated GameTests and the expanded real client scenario. Detailed finding classifications and narrow runtime gaps are in `SOL_QA_REMEDIATION.md`.

| Part 1 criterion | Status | Evidence |
| --- | --- | --- |
| Fabric 26.3 / Java 25 clean build | PASS | `gradlew clean build` |
| Usable production JAR; test mod excluded | PASS | `build/libs/nullstar-1.0.0.jar`, archive inspection |
| Mod initializes on a dedicated server | PASS | dedicated `runGameTest` startup and real-world tests |
| Client initializes and enters a world | PASS | `runClientGameTest` |
| Controller registration and targeting | PASS | real item use; registered controller/model visible in client screenshots |
| Server-authoritative activation through keybind | PASS | native key mapping after controller designation |
| Helios full phase loop and completion | PASS | client/server phase assertions and completed crater |
| Visible charging source and descending beam | PASS, visual inspection | charge/entry screenshots, `EffectRenderer` and camera framing |
| Visible impact/aftermath and changed landscape | PASS, visual inspection | aftermath/crater screenshots; queue/world assertions |
| Persistent crater after save/reopen | PASS | reopened disposable world block assertion |
| Singularity and Glassfall playable end-to-end | PASS | charge/fire/aftermath/completion plus terrain assertions for both |
| Gravity, damage and knockback foundation | PASS | dedicated living-entity GameTest |
| Bounded processing and cancellation | PASS | algorithm tests; dedicated manager tests and real cancel |
| Protected/unbreakable blocks and containers | PASS | dedicated world fixture; protection veto check |
| Loaded-only target reads | PASS | dedicated loaded-block-view test; no new chunk loaded |
| Radius limits, seeded geometry and falloff | PASS | pure geometry/configuration tests; server caps |
| Timeline transitions and one-time cues | PASS | pure monotonic/boundary/scaling tests |
| Wire serialization and invalid data rejection | PASS | current typed codec round-trip/malformed request tests |
| Duplicate activation prevention | PASS | per-owner/existing-event checks and replay/cooldown unit tests |
| Late/stale/dimension client cache behavior | PASS | pure client-state tests; real dimension transition |
| Camera skip and completion restoration | PASS | player camera entity/type/position assertions; real skip |
| Preview leaves terrain unchanged | PASS | full preview loop and stone block assertion; zero changed-block log |
| Full engineering handoff and installation README | PASS | required docs and coherent commits |
| Legal long-stage duration scaling and single application | PASS | long-stage and operator-override JUnit regressions |
| Unauthorized preview/destructive admission rejected | PASS | dedicated embedded-player authority fixture |
| Owner attribution and PvP-safe damage/forces | PASS | dedicated PvP off/on and source-attribution assertions |
| Concurrent owners survive an isolated event failure | PASS, embedded server players | injected timeline fault cancels one; other Glassfall completes a crater; independent graphical clients remain open |
| Reservation saturation/release/overwrite/overflow | PASS | direct manager JUnit tests and completed-job world accounting |
| Offhand aiming/designation/key activation | PASS | expanded real graphical Helios scenario |
| Transient reset retains cooldown | PASS | nonzero cooldown checked across reset before real dimension travel |
| Camera blend/presets/release/death/unloaded frame | PASS | deterministic frame-input JUnit tests plus real restoration scenario |
| Disabled terrain destruction stays inert | PASS | dedicated complete Glassfall cycle with terrain disabled |
| Reproducible artifact evidence and JDK guard | PASS | executable archive inspector; actual Java 21 rejection |
