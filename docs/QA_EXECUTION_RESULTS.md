# Part 1 QA — execution results

Independent verification log for the NULLSTAR Cataclysm Engine Part 1 audit.
This file records **only** what was actually executed and observed, separated from what was inferred by
reading code, and from the previous author's committed evidence.

## Revision under audit

| Item | Value |
| --- | --- |
| Branch | `master` |
| `git rev-parse HEAD` | `0e07887dcff77055eee4ed04d0c645c2b1236ad7` |
| Subject | `docs: complete Part 1 handoff and runtime evidence` |
| Working tree at start | clean (`git status --short` produced no output) |
| Prior commits | `45d64a5 fix: verify cinematic framing and 26.3 lifecycle recovery`, `534fd2b feat: implement authoritative orbital weapons and bounded destruction engine`, `d38ec94 chore: preserve supplied Fabric 26.3 template` |
| Implementation commit quoted by the author | `45d64a5e87f9463e0e8b6fc4ea6cedc7f97a4ecb` |

**Commit identity — verified, not inferred.** `git rev-parse 45d64a5e87f9463e0e8b6fc4ea6cedc7f97a4ecb`
resolves exactly to that 40-character hash, so the hash quoted in `docs/HANDOFF_TO_OPUS.md` and
`docs/VALIDATION.md` is a real object in this repository.

**Source identity — verified.** `git diff --stat 45d64a5 0e07887` reports 30 files changed and **zero
Java, Gradle, mixin or Minecraft resource files**. The entire delta is documentation and evidence:

```
CLAUDE.md, README.md, docs/{ACCEPTANCE,ARCHITECTURE,HANDOFF_TO_OPUS,PROJECT_STATE,RESEARCH,
VALIDATION,WEAPON_SPECIFICATIONS}.md
docs/evidence/{0000..0013}.png (14), artifact-validation.json, clean-build.log, client-test-run.log,
docs/evidence/unit-tests/TEST-com.nullstar.{core.EngineTest,network.SerializationTest,client.ClientStateTest}.xml
```

Consequently the compiled code audited at `0e07887` **is** the code of the author's declared
implementation commit `45d64a5`. The earlier suspicion that `0e07887` might silently alter `src/` is
resolved: it does not.

## Environment actually used

| Item | Observed value |
| --- | --- |
| OS / shell | Windows (`win32`), commands run through bash |
| Default `java` on `PATH` | **Temurin 21.0.10+7** — *not* sufficient for this project |
| `JAVA_HOME` used for every build | `C:\Users\Shadow\.gradle\jdks\eclipse_adoptium-25-amd64-windows.2` (JDK 25) |
| Gradle | wrapper `9.7.1-bin`, `distributionSha256Sum` pinned in `gradle/wrapper/gradle-wrapper.properties` |
| Loom | `1.18.3` (pinned via `gradle.properties`) |
| Minecraft / Loader / API | `26.3` / `0.19.5` / `0.162.0+26.3` — confirmed at runtime from the mod list in both test logs |

**Environment finding (LOW, reproducibility):** the default JDK on this machine is Java 21, so
`./gradlew build` fails unless `JAVA_HOME` is overridden to the Java 25 path. `README.md` and
`docs/HANDOFF_TO_OPUS.md` document this for this machine, but `README.md` presents it as a
PowerShell-only step; the audit confirms the override is mandatory, not optional.

## Commands executed

All three commands below were run in this audit against the committed tree at `0e07887`.

```bash
export JAVA_HOME="/c/Users/Shadow/.gradle/jdks/eclipse_adoptium-25-amd64-windows.2"

# 1. Baseline build (warm tree)
./gradlew build --console=plain --no-daemon

# 2. Release check — fresh compile, JUnit and dedicated GameTests from a clean output dir
./gradlew clean build --console=plain --no-daemon

# 3. Real graphical client GameTest (presentation / input / network)
./gradlew runClientGameTest --console=plain --no-daemon
```

### Result 1 — `./gradlew build` (warm)

| Check | Result |
| --- | --- |
| Exit status | **0** |
| Gradle verdict | `BUILD SUCCESSFUL in 49s` |
| Dedicated GameTests | `All 5 required tests passed :)` |
| Mod bootstrap | `NULLSTAR ready: 3 weapons; 1536 candidate operations/tick, radius cap 24` |
| JUnit / JAR | `UP-TO-DATE` — reused previous outputs; this run alone does **not** re-prove them |

### Result 2 — `./gradlew clean build` (the release check mandated by `AGENTS.md`)

| Check | Result | Evidence |
| --- | --- | --- |
| Exit status | **0** | `EXIT=0` appended to the run log |
| Gradle verdict | **`BUILD SUCCESSFUL in 32s`** | `17 actionable tasks: 14 executed, 3 up-to-date` — a genuine fresh compile |
| Tasks that executed | `compileJava`, `compileClientJava`, `compileGametestJava`, `compileTestJava`, `jar`, `sourcesJar`, `test`, `runGameTest`, `check`, `build` | task graph in the log |
| **JUnit** | **19 tests, 0 failures, 0 errors, 0 skipped** — `build/test-results/test/*.xml` regenerated at 10:37 | per-suite: `EngineTest` `tests="11"`, `SerializationTest` `tests="5"`, `ClientStateTest` `tests="3"`; every `failures="0" errors="0" skipped="0"` |
| **Dedicated-server GameTests** | **`5 tests are now running` → `========= 5 GAME TESTS COMPLETE IN 1.735 s ==========` → `All 5 required tests passed :)`** | `runGameTest` task output |
| Mod bootstrap on a real 26.3 dedicated server | `NULLSTAR ready: 3 weapons; 1536 candidate operations/tick, radius cap 24` | same log |
| FAILED/`BUILD FAILED` markers | none | same log |

This closes the previous pass's two caveats: the JUnit suite and the dedicated GameTests were both
**re-executed from source**, not reused, and no `clean build` gap remains.

### Result 3 — `./gradlew runClientGameTest` (real graphical client + integrated server)

| Check | Result | Evidence |
| --- | --- | --- |
| Exit status | **0** | `EXIT=0` |
| Gradle verdict | **`BUILD SUCCESSFUL in 2m 21s`** | `12 actionable tasks: 3 executed, 9 up-to-date` |
| Scenario | full `StrikeClientGameTest` scenario passed end to end | no `AssertionError`, no `FAILED` |
| Real terrain mutation, all three weapons | `complete: 10200`, `complete: 6602`, `complete: 5578` changed blocks | server-thread NULLSTAR log lines |
| Preview is inert | `complete: 0 changed blocks` | same |
| Cancellation stops work | `cancelled: 0 changed blocks`; dimension-change cancellation `cancelled: 0 changed blocks` | same |
| Crater persistence across save/reopen | `complete: 10196 changed blocks` then the reopened-world block assertion | same |
| Camera/presentation restoration | asserted in-test after skip, completion, cancellation and a dimension change (`getCameraEntity()==player`, `CameraType.FIRST_PERSON`, `<0.1` block camera delta, `CAMERA.applied()==false`) | test source `src/gametest/java/com/nullstar/test/StrikeClientGameTest.java` |
| Screenshots | **14 PNGs regenerated** | `build/run/clientGameTest/build/evidence/0000…0013_*.png` |
| nullstar exceptions | **none** — every pre-existing error is environmental | `grep` of the client log for nullstar + exception/error/failed returns nothing; the only `RuntimeException` is the known Realms placeholder auth warning |

The screenshot directory resolves to `build/run/clientGameTest/build/evidence/` because the client test
process runs with that directory as its working directory (`TestScreenshotOptions…withDestinationDir(
Path.of("build/evidence"))`). The author's committed copies under `docs/evidence/` are therefore the same
artifacts, and the figures above match the author's committed log pattern
(`10211 / 6601 / 5555 / 0 / 0 / 0 / 10230` against this run's `10200 / 6602 / 5578 / 0 / 0 / 0 / 10196` —
different session seeds, same structure and same order of magnitude).

## Artifact verification (reproduced, not trusted)

`docs/evidence/artifact-validation.json` was previously flagged as having no reproducible provenance.
It is now **independently reproduced** by inspecting the JAR produced by the clean build:

| Claim in `artifact-validation.json` | Independent result | Match |
| --- | --- | --- |
| Version `1.0.0` | `fabric.mod.json` in the JAR: `"version": "1.0.0"` (`${version}` expanded) | ✅ |
| Entrypoint `com.nullstar.Nullstar` | `entrypoints.main = ["com.nullstar.Nullstar"]`, `entrypoints.client = ["com.nullstar.client.NullstarClient"]` | ✅ |
| Minecraft `=26.3` | `depends.minecraft = "=26.3"` | ✅ |
| Test classes `0` | `grep -c "com/nullstar/test/"` → `0` (56 `com/nullstar/**` classes packaged) | ✅ |
| Class major `69` (Java 25) | `od` of `com/nullstar/Nullstar.class`: magic `202 254 186 190`, major `0 69` | ✅ |
| SHA-256 `31C45671…D7A29D2` | `sha256sum build/libs/nullstar-1.0.0.jar` → `31C4567128ABE1C37460DEA923901A476DF9D1AEFD328C4E9133F8210D7A29D2` | ✅ **byte-identical** |

Supporting detail: the source artifact `build/libs/nullstar-1.0.0-sources.jar` contains 36 `.java` files;
the manifest carries `Fabric-Loom-Split-Environment: true`,
`Fabric-Loom-Client-Only-Entries: com/nullstar/client/**`, `Fabric-Mixin-Version: 0.17.4+mixin.0.8.7`,
`Fabric-Loom-Version: 1.18.3`, `Fabric-Minecraft-Version: 26.3`.

**Byte-for-byte reproducibility:** the JAR rebuilt by `clean build` in this session hashes identically to
the artifact the previous author recorded. JAR entries carry fixed `1980-02-01` timestamps, so the build
is reproducible on this machine and JDK. Cross-machine/cross-JDK reproducibility was not tested.

## Read-only static and bytecode verification performed

Against the pristine API/source dumps Loom generated into the local (git-ignored) `.reference/` directory
(`Camera.java`, `CameraRenderState.java`, `GameRenderer.java`, `camera-bytecode.txt`, `api*.txt`).

| Verification | Method | Result |
| --- | --- | --- |
| No client-only code on the dedicated server | `grep -rnE "import (net\.minecraft\.client\|com\.mojang\.blaze3d\|org\.lwjgl\|com\.nullstar\.client)" src/main/java` | **0 matches.** (The only client imports in the whole production-adjacent tree are in `src/gametest/.../StrikeClientGameTest.java`, which is not packaged — confirmed by the JAR containing 0 test classes.) |
| Camera mixin injection point exists and is **unique** | `grep -n "getViewRotationMatrix" .reference/camera-bytecode.txt` cross-referenced with method headers | Exactly **three** invocations exist in the whole class: line 263 inside `update()` (method spans 200–294), line 401 inside `extractRenderState()` (311–521), line 1382 inside `getViewRotationRotationProjectionMatrix()` (1361–1410). Within `update` the INVOKE is **unique** and sits after `alignWithEntity`/`calculateFov` and before `prepareCullFrustum`/`setupPerspective`, so both culling and projection pick up the override. `defaultRequire: 1` therefore resolves. |
| Camera override is frame-local | read `GameRenderer.extract()` | `extractOptions()` is called unconditionally at `extract()` line 441, **before** `extractCamera()` at line 444; `extractOptions()` line 760 does `optionsState.cameraType = options.getCameraType()`. The mixin's write at `extractRenderState` RETURN is re-derived every frame and cannot leak a persistent third-person view. |
| Mixin shadows resolve | read `Camera.java` member list plus bytecode headers | `position`, `xRot`, `yRot`, `fov`, `detached` fields exist; `setRotation(float,float)` (bytecode line 1213) and `setPosition(Vec3)` (line 1276) exist as protected methods; `extractRenderState(CameraRenderState,DeltaTracker)` (line 311) and `update(DeltaTracker)` (line 200) match the declared descriptors; `CameraRenderState.isFirstPerson`/`smartCull`/`pos` exist. |
| Duration-multiplier wiring | `grep -rn "\.scaled(" src/` | Exactly three sites: `Nullstar.java:40` (inert, QA-01), `WeaponEngine.java:67` (effective), `EngineTest.java:19` (test). No fourth consumer exists. |
| Vanilla sound ids in the bundled weapon JSONs are real 26.3 sound events | extracted `net/minecraft/sounds/SoundEvents.class` from `minecraft-common.jar` and searched the constant pool for each id | **14/14 found exactly once**: `beacon.activate`, `beacon.ambient`, `beacon.deactivate`, `amethyst_block.resonate`, `respawn_anchor.charge`, `respawn_anchor.deplete`, `portal.ambient`, `fire.ambient`, `glass.break`, `enderman.stare`, `firework_rocket.launch`, `generic.explode`, `lightning_bolt.thunder`, `warden.sonic_boom`. A typo would therefore have been caught; QA-08 remains a diagnostics gap for operator *overrides*, not a defect in the shipped defaults. |
| `src/main` API symbols | `rg` in `.reference/api*.txt` | `Permissions.COMMANDS_GAMEMASTER`, `sendOverlayMessage(Component)`, `hurtServer(ServerLevel,DamageSource,float)`, `ServerPlayer.connection`, `mayInteract(ServerLevel,BlockPos)`, `hasChunk(int,int)`, `setBlock(BlockPos,BlockState,int,int)`, `guiWidth()`/`fill()`/`text()` on the HUD extractor all present |

## Independent reproduction of a defect by execution (QA-13)

The one finding in `docs/DEEPSEEK_QA_REPORT.md` that is marked **executed** was reproduced outside the mod,
with no build-system changes, by compiling the two dependency-free classes on JDK 25 and driving them
directly:

```bash
JH=/c/Users/Shadow/.gradle/jdks/eclipse_adoptium-25-amd64-windows.2
# copy src/main/java/com/nullstar/core/{WeaponDefinition,Phase}.java into a scratch tree, plus a driver
"$JH/bin/javac" -d out src/com/nullstar/core/*.java src/Scratch.java
"$JH/bin/java" -cp out Scratch
```

Observed:

```
constructed stage=600 ok, duration=620
  scaled(4) ok -> duration=2480
constructed stage=2400 ok, duration=2420
  stage=2400 scaled(4) THREW: IllegalArgumentException: Invalid or repeated phase
```

`WeaponDefinition` accepts a 2400-tick stage and `ServerConfig` accepts a multiplier of 4, but the product
fails the same constructor's own `ticks > 2400` check inside `scaled()`. Reachability was then established
by reading the call sites: `Nullstar.java:40` (today — a boot-time refusal with a misleading message) and
`WeaponEngine.java:67` (after the P0-1 deletion — an uncaught throw on the server thread). No production
file was modified for this test; it ran entirely under a scratch directory.

## Author-supplied evidence — inspected and corroborated

These artifacts existed before this audit and were read and cross-checked; the two that matter most were
then independently reproduced above.

| Artifact | What it claims | Audit assessment |
| --- | --- | --- |
| `docs/evidence/clean-build.log` | `clean build`, 5/5 GameTests, exit 0 | Real Gradle transcript; **reproduced** by Result 2 above |
| `docs/evidence/client-test-run.log` | Full graphical client GameTest in a disposable world | Real Gradle client transcript; **reproduced** by Result 3 above |
| `docs/evidence/unit-tests/TEST-*.xml` | JUnit 11 + 5 + 3 = 19 tests, 0 failures/errors/skipped | Machine-generated JUnit XML; **reproduced** by Result 2 (same counts) |
| `docs/evidence/artifact-validation.json` | version 1.0.0, `=26.3`, entrypoint, 0 test classes, class major 69, SHA-256 `31C4567…` | **Every field reproduced** from a freshly built JAR, including the byte-identical hash |

## Limitations of this audit (must not be read as passes)

1. **No multi-client test.** Every multiplayer-authority conclusion is code inspection plus a
   single-client automated run. A second real client was never connected.
2. **No performance measurement.** Tick time, frame time and worst-case dense-world behaviour were not
   measured. The documented 4 ms destruction deadline is cooperative and remains unbenchmarked.
3. **No plain `runServer` boot.** Dedicated startup is covered by `runGameTest` booting a real dedicated
   server and by the integrated server in the client test; a standalone `runServer` launch was not issued.
4. **No third-party integration test** (claim mods, shader/renderer mods, resource packs).
5. **Cross-machine reproducibility not tested.** The byte-identical JAR hash holds on this machine and
   JDK 25 build; another JDK/platform was not tried.
6. **Artistic/audio judgement is not an engineering result.** The 14 screenshots were regenerated and are
   real automated captures, but final art, shader compatibility and human audio approval remain Part 2.
7. **Reachability of the QA-04 throw paths was not demonstrated.** They were proven unreachable by
   accounting analysis only; no attempt was made to synthesise a strike that breaks the invariant.
8. **`runClientGameTest` is not part of `build`.** It was run as a separate command, as `AGENTS.md`
   requires; a green `build` alone does not cover presentation, input or network.

## Verdict on the reported evidence

The author's acceptance claims are **honest and now independently reproduced**. Every claim that could be
checked against artifacts or a fresh run held: test counts, the GameTest pass line, real terrain deltas,
inert preview, working cancellation, save/reopen persistence, the absence of NULLSTAR exceptions,
client/common source-set separation, the dependency pins, and the release artifact's metadata and hash.
No claim was found to be fabricated or overstated. The remaining gaps in §Limitations are genuinely open
scenarios that the author also documents as open — not concealed failures.
