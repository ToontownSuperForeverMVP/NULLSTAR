# Acceptance

## Part 2.5B colossal presentation acceptance

`PASS` = implemented and verified by the named automated check; `PASS (visual)` = implemented and inspected in
captured frames; `OPEN` = not verifiable in this environment. Colossal presentation frames come from real rendering
in a disposable client world, but a machine cannot judge artistry, so the artistic rows stay open on purpose.

| Requirement | Status | Evidence / boundary |
| --- | --- | --- |
| Project still builds; all prior validated gameplay intact | PASS | `clean build`: 61 JUnit, 10 dedicated GameTests; regression + dedicated client suites unchanged |
| Server authority, budgets, caps, protection, preview, cooldowns unchanged | PASS | server presentation change limited to one metadata resolution; dedicated fixtures and prior suites |
| Helios supports a 90+ second world-to-cosmos cinematic | PASS | APOCALYPSE timeline 2,000 ticks (100 s); the detached cosmic framing runs from tick 270 to the end of the stellar discharge at tick 1,656 (82.8 s of cosmic act, 96.3 s from first lock); 20 authored stages |
| Cosmic escalation reads as planetary → solar → galactic → observable-universe | PASS (machine + visual) | five normalized-scene captures; 2.4k–16.8k vertices; every beat carries structure and contrast |
| Mothership reveal is monumental, segmented, animated, with rings, harvest structures, pylons, aperture and nested focusing | PASS (visual) | `colossal_05_mothership_reveal`, `colossal_06_deployment`, `colossal_07_charging`, `colossal_08_stellar_lance`; 17.5k–18.6k vertices |
| Colossal layered, unstable, electrically violent stellar lance readable at distance | PASS (visual) | `colossal_08_stellar_lance`; three nested tube layers plus ejected edge streaks |
| Impact and aftermath read as planetary violation, not a dent | PASS (visual) | layered shock systems with re-emanating fronts, pressure wall, debris towers, ash columns, secondary rings, molten pools; 23.3k vertices at the pressure wall |
| The first attack targets 1,000×1,000 devastation | PASS, admission/budget | radius-500/depth-96 geometry; engine logs 13,248,010 and 6,770,404 candidate cells for the 575/650-block annular passes; full loaded-region excavation NOT performed |
| Singularity reads as regional spacetime failure with a sink basin | PASS (visual) | `singularity_*` capture set; 4.6k–7.2k vertices, sky-filling anomaly, tori, infalling slabs, release jets |
| Glassfall reads as a civilization-ending orbital barrage | PASS (visual) | `glassfall_*` capture set; monumental platform, 16 + 1 projectiles on authoritative cues, crystal-blast dome finale |
| Custom shaders materially improve the spectacle | PASS | four new original textures through the existing 18-program pipelines; opaque depth-writing backdrop; OIT-aware; optional registration with vanilla fallback |
| Audio scale increased; Part 2 mix preserved | PASS (format only) | 13 new events (41 total), mono 44.1 kHz, peaks −3.2..−1.2 dBFS; Part 2 assets byte-identical; colossal-only accent table |
| Camera sells enormity with new shots; accessibility preserved | PASS | 13 new bounded colossal shots plus `cosmic_montage`; reduced motion, reduced flashing, camera toggle, native skip and release unchanged and re-tested |
| Performance bounded at colossal scale | PASS (single machine) | 0 dropped primitives, collect ≤ 0.415 ms, frame ≤ 1.84 ms, particles ≤ 583 of 1,024, server tick ≤ 0.78 ms |
| Bounded budgets, fallbacks, cleanup, multiplayer authority and cancellation preserved | PASS | per-layer vertex budgets, optional pipelines, cosmetic purge on cancellation, dedicated admission/recovery/budget fixtures |
| New evidence for the colossal scale, not old Part 2 screenshots | PASS | `evidence/part25b/`: 103 frames, per-frame diagnostics and statistics; Part 2.5A evidence left untouched |
| Human artistic and audio approval | OPEN | requires the user to watch the sequence and listen to the mix |
| Full 1,000×1,000 excavation in a fully loaded region | UNVERIFIED | explicitly not represented by the bounded/captured runs |
| Real-time 100-second pacing and huge-region TPS | UNVERIFIED | test-thread engine advancement accelerates the timeline |
| Independent graphical clients, dense worlds, third-party mods | UNVERIFIED | one client and synthetic strikes only |
| The cosmic act is an astronomical simulation or a real sky/level change | NOT CLAIMED | authored illusion composed in view-local space in front of the gameplay camera |

## Part 2.5A engineering acceptance

The scale layer is implemented; automated engineering checks pass within the explicit boundaries below. The next stage is immense presentation artistry, not an unfinished terrain-cap patch. See `VALIDATION.md` and `PART25_ENGINEERING_REPORT.md`.

| Requirement | Status | Evidence / boundary |
| --- | --- | --- |
| Preserve 26.3/Java 25/Part 2 and STANDARD | PASS | clean build; original regression/dedicated/capture suites |
| All three weapons support larger configurable tiers | PASS | six validated profiles; generated definitions/math/cue tests |
| Helios nominal 1,000-diameter/depth-96 physical job | PASS, capability/admission | radius-500 geometry, real server admission/bounded scans; full region not excavated |
| Helios at least 90-second sequence | PASS | 2,000-tick default; tested integer-safe floor under duration multipliers |
| Cosmic scenes without world traversal | PASS, foundation | five real normalized-scene captures; camera remains at gameplay position |
| Gigantic structure/beam/anchor/checkpoint metadata | PASS | v2 round-trip; `StrikeView.scale()` / scene hook; final giant art belongs to 2.5B |
| Chunk-aware, constant-memory, globally bounded/fair destruction | PASS | cursor/column tests; simultaneous huge scans; real job budgets |
| Secondary passes/rings and broad synchronized barrage | PASS | wave dependency/cue tests; staged mutations; clipped public Glassfall real loop |
| Public/private caps including child centers, change and admission ceilings | PASS | geometry/footprint/config tests; confirmation/queue/change fixtures |
| Non-destructive full-size bounds/preview | PASS | nominal 1,000-diameter command; complete inert 2,000-tick preview |
| Cancellation, diagnostic causes and emergency/operator/owner abort | PASS | isolated cancellation, bounded history, terminal lifecycle; completed terrain stays changed |
| Graceful queued-job persistence/recovery | PASS | file/cursor tests; actual world close/reopen; authorization pause and capacity release |
| Compact versioned state, late reconstruction, authority/replay | PASS | state_v2/visual_v2 tests; one current snapshot; prior request_v1 authority fixtures |
| Skip restores camera and preserves committed damage | PASS | unit release/owner tests and real native K after commitment; controlled crater still completes |
| Tests/artifact/handoff | PASS | 56 JUnit, 10 dedicated tests, four client suites + final focused run; zero packaged test classes; current handoff |
| Complete physical excavation of a loaded 1,000×1,000 region | UNVERIFIED | explicitly not represented by smaller/synthetic runs |
| Huge-region completion timing/TPS, dense entities, independent graphical clients | UNVERIFIED | no invented benchmark or deployment claim |
| Power-loss transaction recovery | NOT CLAIMED | atomic checkpoint document is not synchronized atomically with chunk saves |
| Final cosmic/mothership visuals, long-form audio, artistic/pacing approval | PART 2.5B | the current normalized composition is engineering scaffolding |

Historical acceptance sections below retain their original scope and results.

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
