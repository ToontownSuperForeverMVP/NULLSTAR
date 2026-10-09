# Validation

## Part 2.5B release validation

October 9, 2026. Branch `codex/part25-colossal`; Part 2.5B tested on the revision that follows the Part 2.5A
engineering commit `1c8e250` and its documentation commit `77d1da0` (resolve both with Git; the handoff tag
`codex/part-2.5b-complete` points at the Part 2.5B documentation/evidence commit). Recovery `codex/pre-part25-20261009`
retains Part 2 `c5ef353`; `codex/part-2.5a-complete` retains the 2.5A stage. Versions are unchanged: Minecraft 26.3,
Java/Temurin 25.0.4+7, Loader 0.19.5, Fabric API 0.162.0+26.3, Loom 1.18.3, Gradle 9.7.1 and JUnit 5.11.4. Every
world is disposable under `build/run`; no Prism saves, mods or configuration were touched.

Commands run from this checkout with `JAVA_HOME=C:\Users\Shadow\.gradle\jdks\eclipse_adoptium-25-amd64-windows.2`:

```powershell
.\gradlew.bat clean build
.\gradlew.bat runClientGameTest
pwsh -File scripts/verify-artifact.ps1 -Output docs/evidence/part25b/artifact-validation.json
python -I tools/assets/validate_audio.py docs/evidence/part25b/audio-validation.json
```

| Check | Result / scope | Evidence |
| --- | --- | --- |
| Final release check `clean build` | PASS in 23 s: **61 JUnit, zero failures/errors** and **all 10 required dedicated GameTests passed** | `evidence/part25b/clean-build.log`, `unit-tests/` |
| Full real-client suite | PASS in 6 m 11 s: regression, dedicated connection, capture (helios, singularity, glassfall, **colossal**, modes, stress) and the colossal lifecycle suite | `evidence/part25b/client-suite.log` |
| Colossal presentation measurement | PASS: 13 colossal beats captured with per-frame vertex/particle/collect/frame/server-tick diagnostics; **0 dropped primitives** in every beat | `evidence/part25b/renderer-diagnostics.txt`, `frame-metrics.json` |
| Frame review | PASS (machine): all 103 captured frames carry structure and contrast; lowest content fraction 25.6 %; no flat, empty or washed-out beat | `evidence/part25b/frame-metrics.json` |
| Real client lifecycle | PASS: five normalized cosmic-act captures, late one-snapshot mothership reconstruction, native K skip, skip after damage commitment, controlled diameter-64 real crater, inert nominal bounds, actual integrated-server close/reopen of an unfinished job | `evidence/part25b/client-suite.log` |
| Custom pipelines | PASS: 18 programs compiled, `custom=true` in every captured frame, no fallback and no renderer exception | `evidence/part25b/renderer-diagnostics.txt` |
| Audio assets | PASS: **41** valid mono 44.1 kHz Vorbis files, peaks −3.2..−1.2 dBFS, all referenced by `sounds.json` | `evidence/part25b/audio-validation.json` |
| Production artifact | PASS: major 69, exact 26.3, both production entrypoints, **0 test classes** | `evidence/part25b/artifact-validation.json` |

Final JUnit totals: EngineTest 13, ColossalEngineTest 13, SerializationTest 5, ColossalSerializationTest 1,
ClientStateTest 3, CinematicCameraTest 7, CosmicCameraTest 1, PresentationEngineTest 7, TrajectoryTest 2,
DestructionManagerTest 2, ColossalPersistenceTest 2 and the new ColossalPresentationTest 5. The new class pins the
colossal presentation contract: the magnitudes derived from the real authoritative profiles, the unchanged STANDARD
identity, the cosmic act holding through charge/alignment/discharge while every other weapon and phase stays in the
world, the colossal timeline selecting the cosmic act and the devastation sweeps, and every camera name a generated
profile can select being registered.

The three added dedicated fixtures from 2.5A still prove radius-500/depth-96 admission and single-use confirmation,
reservation saturation, a complete 2,000-tick inert preview, real staged jobs with bounded cursors, exact global
budgets and change caps, and no new corner chunk loading. The nominal APOCALYPSE footprint is logged by the
engine as 13,248,010 candidate cells in the 575-block annular pass and 6,770,404 in the 650-block pass. The one
`Timeline cannot rewind` stack trace remains the intentional injected isolation fixture. Windows OSHI/Perflib
warnings and development Realms placeholder-auth notices are environment noise.

Colossal presentation measurements (ULTRA, one synthetic APOCALYPSE Helios strike, single machine): 2,412–23,348
submitted vertices per beat, 0 dropped, peak 583 of the 1,024 cosmetic particles, extraction (collect) 0.198–0.415 ms,
measured frame work 0.84–1.84 ms and server tick 0.51–0.78 ms. The cosmic beats cost the same order of magnitude as
the Part 2 STANDARD presentation while presenting at roughly 4.4x machinery scale. The unfocused test window is
pinned to a 30 FPS background limit, so the recorded 29–30 FPS is not a frame-rate measurement; a focused run of the
same beats measured 111–117 FPS with essentially identical frame times. These are single-machine numbers on a flat
diorama with one client, not guarantees for dense worlds or weaker GPUs.

Graphical colossal evidence for this stage is the 13-beat colossal measurement pass (ascent, planet, solar system,
galaxy, observable-universe boundary, mothership reveal, deployment, charging, stellar lance, descent, impact,
pressure wall, aftermath), the five normalized cosmic-act captures from the lifecycle suite, and the three colossal
weapon sets. Because the capture client runs at render distance 16, a colossal crater is far wider than the loaded
region; the real terrain check therefore stays the controlled diameter-64 crater while the nominal 1,000-diameter
basin is verified as bounded admission, cursor and footprint accounting. The full nominal crater is
**supported, admitted and budget-tested, not completely excavated in a fully loaded 1,000×1,000 region**. Test-thread
engine advancement accelerates the 100-second timeline, so these checks prove transitions, rendering, networking,
lifecycle, bounded terrain and restoration, **not real-time pacing or huge-world performance**. Independent
multi-client deployment, dense-world timing, third-party integrations, power-loss recovery and human artistic/audio
approval remain unverified.

Production artifact: `build/libs/nullstar-1.0.0.jar`; exact current SHA-256 in `evidence/part25b/artifact-validation.json`.
No test mod is distributed.

## Part 2.5A release validation

October 9, 2026. Branch `codex/part25-colossal`; final tested engineering revision `1c8e250` (resolve with Git). Recovery `codex/pre-part25-20261009` retains Part 2 `c5ef353`. Existing release tags are unchanged. Versions remain Minecraft 26.3, Java/Temurin 25.0.4+7, Loader 0.19.5, Fabric API 0.162.0+26.3, Loom 1.18.3, Gradle 9.7.1 and JUnit 5.11.4. Every world is disposable under `build/run`; no Prism saves/mods/config were changed.

Commands run from this checkout with `JAVA_HOME=C:\Users\Shadow\.gradle\jdks\eclipse_adoptium-25-amd64-windows.2`:

```powershell
.\gradlew.bat build
.\gradlew.bat runClientGameTest '-Ptests=regression,dedicated,capture,colossal'
.\gradlew.bat clean build
.\gradlew.bat runClientGameTest -Ptests=colossal
pwsh -File scripts/verify-artifact.ps1 -Output docs/evidence/part25/artifact-validation.json
```

| Check | Result / scope | Evidence |
| --- | --- | --- |
| Part 2 baseline build | PASS, 7 dedicated tests; prior JUnit up-to-date | `evidence/part25/baseline-build.log` |
| Final clean release build | PASS, **56 JUnit**, **10 dedicated GameTests** including Fabric framework test | `evidence/part25/clean-build.log`, `unit-tests/` |
| All four client suites | PASS: regression, dedicated connection, Part 2 capture and colossal; run before final focused hardening | `evidence/part25/client-suite.log` |
| Final colossal client suite | PASS on final engineering revision after column-cache, footprint and camera-release/timeline-floor hardening | `evidence/part25/client-release.log` |
| Production artifact | PASS: major 69, exact 26.3, both production entrypoints/classes, **0 test classes** | `evidence/part25/artifact-validation.json` |

Final JUnit totals: EngineTest 13, ColossalEngineTest 13, SerializationTest 5, ColossalSerializationTest 1, ClientStateTest 3, CinematicCameraTest 7, CosmicCameraTest 1, PresentationEngineTest 7, TrajectoryTest 2, DestructionManagerTest 2, ColossalPersistenceTest 2. All assertions pass. New coverage includes exact world-chunk cursor replay, column math equivalence, radius-500 envelopes, public/private child-center bounds, the 90-second minimum after integer stage rounding, profile/config safety, complete one-time timelines, cue pairings, compact metadata, owner camera/skip/stale handling and persistence policy/structure rejection.

The three added dedicated fixtures prove: radius-500/depth-96 admission and explicit single-use confirmation/replay; reservation saturation; complete 2,000-tick inert preview; a real clipped public Glassfall loop; smaller real staged jobs and cursor recovery; unauthorized recovered-owner pause; completed recovery releasing capacity; exact change caps; fair simultaneous radius-500 scans under the global budget; cancellation isolation; and no new corner chunk loading. Existing protected/container/bedrock/PvP/replay/cooldown/timeline-fault fixtures still pass. The expected injected `Timeline cannot rewind` exception is intentional isolation evidence, not an unhandled defect. Windows OSHI/Perflib warnings remain environment noise.

Graphical colossal evidence includes five actual scene captures (`frames/0000..0004`), eye-local camera assertions, no self-avatar in virtual scenes, one-snapshot mothership reconstruction, native K skip/restoration, server-event survival after skip, a controlled diameter-64 real crater, skip after damage commitment, inert nominal 1,000-diameter bounds and actual integrated-server close/reopen of an unfinished job. The final run logs the resumed cursor; exact counts vary by ticks elapsed during shutdown/join. Test-thread engine advancement accelerates the long timeline, so these checks prove transitions/rendering/network/lifecycle and real bounded terrain, **not 100-second real-time pacing or huge-world performance**.

The full client run also completed all STANDARD weapons, offhand targeting/key activation, preview, cancellation, dimension return, save/reopen, real dedicated disconnect/reconnect and the existing modes/four-strike stress captures. Those captures are transient build outputs; historical curated Part 2 art remains in `evidence/part2`, and the new normalized scene captures are retained in `evidence/part25/frames`. Original assets were not changed, so no new generator/audio quality claim is made.

The full nominal 1,000-diameter crater is **supported/admitted/budget-tested, not completely excavated in a fully loaded real 1,000×1,000 region**. Unloaded cells are skipped permanently; no chunk forcing is hidden behind the fixture. Time-to-completion calculations in the engineering report are arithmetic lower bounds, not benchmarks. Independent multi-client/dense-world performance, third-party integrations, final cosmic/structure artwork/audio/pacing, every survival-death phase and power-loss transaction recovery remain unverified. Graceful restart recovery, not a cross-file crash journal, is tested.

Production artifact: `build/libs/nullstar-1.0.0.jar`; exact current SHA-256 in `evidence/part25/artifact-validation.json`. Final handoff tag: `codex/part-2.5a-complete`. Resolve the tag after the documentation/evidence commit; no test mod is distributed.

## Part 2 release validation

Validated October 9, 2026, on branch `part2/cinematic-release` (base: tag `codex/part-1.5b-complete` = `5ba60f1`). Disposable worlds under `build/run`; Prism data untouched. Versions unchanged: Minecraft 26.3, Temurin 25.0.4+7, Fabric Loader 0.19.5, Fabric API 0.162.0+26.3, Loom 1.18.3, Gradle 9.7.1, JUnit 5.11.4. Test machine: AMD EPYC 9354, NVIDIA RTX 2000 Ada (driver 595.79), Windows 11, graphics preset "fancy", render distance 16, 854x480 test window.

```powershell
$env:JAVA_HOME = 'C:\Users\Shadow\.gradle\jdks\eclipse_adoptium-25-amd64-windows.2'
.\gradlew.bat clean build                       # JUnit + dedicated GameTests
.\gradlew.bat runClientGameTest                 # regression + dedicated-server + visual capture suites
pwsh -File scripts/verify-artifact.ps1 -Output docs/evidence/part2/artifact-validation.json
python -I tools/assets/validate_audio.py docs/evidence/part2/audio-validation.json
```

| Check | Result | Evidence (`docs/evidence/part2`) |
| --- | --- | --- |
| Baseline before changes | PASS: 27 JUnit, 7 dedicated GameTests, graphical scenario | reproduced at the start of Part 2 |
| Release clean build | PASS: 39 JUnit (12 new), 0 failures/errors | `clean-build.log`, `unit-tests/` |
| Dedicated server GameTests | All 7 required tests passed | `clean-build.log` (one intentional injected timeline exception) |
| Client regression scenario | PASS: all three loops, crater assertions, skip/completion/cancel/dimension restoration, inert preview, cooldown across reset, save/reopen persistence | `client-suite.log` |
| Real dedicated-server connection | PASS: registry-synced original sounds resolve on the client, networked Helios drives camera and effects and creates a crater (10,209 blocks), owner disconnect mid-charge cancels Singularity with 0 changes, clean reconnect | `client-suite.log` |
| Visual capture | PASS: 70 frames across 5 fresh worlds (Helios, Singularity, Glassfall, modes, stress) plus 2 UI frames | `sheet-*.png`, `frames/`, `renderer-diagnostics.txt` |
| Custom pipelines | 18 programs compiled at runtime in every world; no fallback, no renderer exception | `client-suite.log` |
| Improved transparency (OIT) | Helios rendered with OIT enabled, no exception | `frames/modes_improved_transparency_01_beam.png` |
| Audio assets | 28 mono 44.1 kHz Vorbis files, peaks -3.2..-2.0 dBFS, all referenced by `sounds.json` | `audio-validation.json` |
| Artifact | PASS: exact 26.3, entrypoints, class major 69, 0 test entries, shaders/sounds/textures packaged | `artifact-validation.json` |

Production artifact `build/libs/nullstar-1.0.0.jar` (1,163,562 bytes), SHA-256:

```text
DC473F65721ECB9E9FC805A3DC6BF5A4D828F5F785FC5A12C05AA749A704A8AF
```

### Measurements

From `renderer-diagnostics.txt` (ULTRA unless noted). `collect` is NULLSTAR's per-frame extraction time (smoothed); `frame` is Minecraft's measured render work for the frame (`getFrameTimeNs`), which excludes waiting on the frame limiter. Reported FPS is unreliable here: the unfocused test window drops to a 30 FPS background limit, while focused runs showed 96–120 FPS during strikes.

| Scenario | Vertices | Cosmetic particles (cap) | Collect | Frame | Server tick (avg) |
| --- | --- | --- | --- | --- | --- |
| Single strike, all phases | 3k–22k, 0 dropped | ≤ 588 (1,024) | 0.08–0.45 ms | ≤ 2.6 ms | ≤ 2.3 ms during destruction |
| MEDIUM / LOW Helios impact | 18.4k / 12.3k | 376 / 94 | 0.25 / 0.17 ms | 1.9 / 1.6 ms | 0.5 ms |
| Four concurrent synthetic strikes | 40k–52k; 3.5k–6.3k dropped by budgets | 1,024 (cap held) | ≤ 0.85 ms | ≤ 3.4 ms | ≤ 1.1 ms |

These are single-machine measurements on a flat test diorama, not guarantees for dense worlds or weaker GPUs.

### Boundaries

Not verified: multiple independent graphical clients and late-joining observers on a production server (only one real client exists in this environment; the dedicated-server test covers one networked client, and the embedded two-owner server tests remain), third-party render/shader/claim mods, survival-owner death in every phase, worst-case dense-world timing, and human artistic/audio approval. Audio was checked by format/peak validation and spectrogram inspection only, not by listening.

## Part 1.5B validation (historical)

Validated October 9, 2026, in disposable Gradle worlds. Production repair: `f2f8f563147db6425f7095e13b43290b3bb02838`; final tested engineering revision: `554df5378f2fd094407be18f384e164838639b14`. Final handoff revision: tag `codex/part-1.5b-complete`. Prism mods, configuration and saves were untouched. Engineering decision: **CONDITIONAL PASS**; see `SOL_QA_REMEDIATION.md` for all finding classifications and remaining evidence boundaries.

### Environment and commands

Minecraft 26.3, Temurin 25.0.4+7, Fabric Loader 0.19.5, Fabric API 0.162.0+26.3, Loom 1.18.3, Gradle 9.7.1, JUnit 5.11.4. Versions were retained and API signatures checked in the installed generated sources.

```powershell
Set-Location 'C:\Users\Shadow\Desktop\NULLSTAR'
$env:JAVA_HOME = 'C:\Users\Shadow\.gradle\jdks\eclipse_adoptium-25-amd64-windows.2'
.\gradlew.bat clean build
.\gradlew.bat runClientGameTest
pwsh -File scripts/verify-artifact.ps1
git rev-parse codex/part-1.5b-complete
# Generate sources separately when needed:
.\gradlew.bat genSources
```

`clean build` passed in 24 seconds. The final graphical invocation passed in 2 minutes 12 seconds; an earlier expanded run also passed in 2 minutes 13 seconds. These are task durations, not performance benchmarks. The Java 21 guard was independently invoked and failed with the intended JDK 25 / JAVA_HOME message before Loom configuration.

### Results

| Check | Result | Evidence |
| --- | --- | --- |
| Initial baseline build | PASS, original five dedicated tests; JUnit up-to-date | `evidence/remediation/baseline-build.log` |
| Release clean build | PASS; 27 JUnit, zero failures/errors/skips | `evidence/remediation/clean-build.log`, `evidence/remediation/unit-tests/TEST-*.xml` |
| Dedicated server GameTests | All seven required tests passed | same release log; six NULLSTAR tests plus one Fabric framework test |
| Final graphical client scenario | PASS, real disposable integrated server/client | `evidence/remediation/client-final.log` |
| Java 21 negative startup | Expected failure with actionable guard | `evidence/remediation/jdk21-rejection.log` |
| Artifact inspection | PASS; production classes, exact 26.3, major 69, zero test entries | executable `scripts/verify-artifact.ps1`, `evidence/artifact-validation.json` |

JUnit counts: EngineTest 13, SerializationTest 5, ClientStateTest 3, CinematicCameraTest 4, DestructionManagerTest 2. Added coverage checks long-stage scaling, one-time catalog scaling, ignored override files, immutable catalog exposure, yaw wrapping, easing, release/skip/preset/dead/unloaded camera behavior and reservation admission/enqueue/release/cancel/overflow.

Dedicated tests exercise real protected/container/bedrock terrain mutation, candidate budgets and completed-job accounting, cancellation, loaded-only targeting, entity damage/gravity, PvP off/on and owner attribution. Embedded server players prove unauthorized preview/destructive requests consume no strike, replay and duplicate rejection, two-owner concurrency after an injected invariant failure, cooldown rejection, inert preview and a complete disabled-terrain Glassfall loop. Tests briefly change config in their synchronous local-engine fixture and restore it in `finally`; production authorization remains intact.

The final graphical scenario designates Helios with a controller in the offhand and an empty main hand, asserts aiming preview, and activates through the native keybind. All three weapons complete real terrain-changing loops. Logged changed-block totals for the final run: Helios 10,220; Singularity 6,606; Glassfall 5,562. Preview logs zero changes; both command/dimension cancellations log zero changes. A final Helios crater (10,213 changes) survives save/reopen. Camera skip, completion, cancellation and dimension restoration are asserted; a transient reset during charging retains a nonzero cooldown.

### Screenshots and artifact

Fourteen fresh captures from the final run are retained under `evidence/remediation/screenshots`. Helios descending beam (`0003`), Singularity firing (`0007`) and Glassfall aftermath (`0012`) were inspected: native gold beam, violet anomaly and cyan crater effects remain visible. These are basic Part 1 effects, without final artistic/audio approval. Older root evidence/screenshots and original Part 1 logs remain historical; their old artifact hash describes the pre-remediation build, not this release.

Production: `build/libs/nullstar-1.0.0.jar`; sources: `build/libs/nullstar-1.0.0-sources.jar`. Production SHA-256:

```text
9BCAB99E59C97062C466C0DED1C1547F1AE2999862D4815436FC2DEA40974B9D
```

The archive generator verifies metadata, entrypoint class presence, Java class major and absence of test entries, then writes JSON with its own reproducible command. The separate `nullstar-test` mod is not distributed.

### Evidence boundaries

The dedicated suite intentionally logs one injected timeline exception, isolates it and continues another owner's real strike. Its rapidly advanced local engine is synthetic fault/admission testing, not a tick-throughput benchmark. Native OSHI performance-counter diagnostics, initial disposable-server properties/EULA messages, development Realms placeholder-auth and render-buffer notices do not fail the suites. No NULLSTAR exception occurred in the graphical final run.

A real client fixture captures missing/malformed sound warnings and zero-count cancellation feedback, and asserts that an unsupported broadcast and a distant non-owner join snapshot do not reach its presentation state. These synthetic packet-boundary tests preserve production schemas. Multiple independent graphical clients, late observer production joins, survival-owner death, dense-world frame/tick timing, third-party claim/render/shader integration, cross-machine reproducibility and human audio/art approval remain open. The four-millisecond destruction deadline is cooperative, not a hard timing guarantee. Embedded two-owner coverage does not replace those scenarios.
