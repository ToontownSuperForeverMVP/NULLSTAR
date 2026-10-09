# QA handoff summary — NULLSTAR Part 1 → Opus Part 2

**Decision: CONDITIONAL PASS** — Opus may begin Part 2 cinematic work now. No CRITICAL defect and no
release blocker was found. Six MEDIUM items — four engineering repairs/policy decisions, one validation
defect and one test gap — should be staged first; most of them are small, local edits.

- Repository audited: `master` @ `0e07887dcff77055eee4ed04d0c645c2b1236ad7`, clean tree.
  The author's declared implementation commit `45d64a5e87f9463e0e8b6fc4ea6cedc7f97a4ecb` resolves exactly,
  and `git diff --stat 45d64a5 0e07887` contains **no Java, Gradle, mixin or Minecraft resource change** —
  only documentation and evidence. The audited tree's compiled code *is* the declared implementation
  commit. **Verified.**
- All three checks were run at this revision and passed: `./gradlew build` (exit 0), `./gradlew clean build`
  (exit 0, JUnit **19/19** re-run from source, dedicated GameTests **5/5**), and `./gradlew runClientGameTest`
  (exit 0, full graphical scenario). The rebuilt release JAR is **byte-identical** to the hash in
  `docs/evidence/artifact-validation.json`.
- Full report: `docs/DEEPSEEK_QA_REPORT.md` · Ordered fix guide: `docs/OPUS_FIX_PRIORITY.md` ·
  Command log: `docs/QA_EXECUTION_RESULTS.md`.

## Overall engineering readiness

Part 1 is a **credible functional foundation with unusually honest documentation**. The hard architectural
claims were checked against the actual 26.3 API and source, not against the author's prose, and they hold:
client-common separation is real (zero client imports in `src/main`), server authority is real
(`RequestPayload` carries intent only — no target, radius, depth, dimension or damage field exists on the
wire), the destruction budget genuinely counts air and skipped cells, chunks are never force-loaded, and
the camera override is frame-local and self-restoring. This audit ran the full command set at this
revision: `clean build` exit 0 with JUnit **19/19** re-executed from source and dedicated GameTests
**5/5**, and `runClientGameTest` exit 0 with real strikes changing 10,200 / 6,602 / 5,578 terrain blocks, a
preview at **0** changes, cancellations at **0** changes, and a crater surviving save/reopen.

The gaps are quality and coverage, not correctness: one misleading catalog-scaling statement that is also
the only load-time validation of scaling (and which throws for two individually legal values, QA-13), one
undecided policy (preview side effects), one unattributed damage source that sidesteps the `pvp` gamerule,
unguarded throw paths inside the server tick, and a test suite that never reaches the camera or the
destruction reservation accounting — the exact two areas Part 2 will rewrite. None of these is reachable
from the shipped configuration; they are exposed by operator data and by the P0 fix itself.

## Critical issues

**None.**

## High-severity issues

**None.** No runtime crash, no multiplayer breakage, no performance cliff, and no destructive-logic escape
was confirmed. The checks that would have found the usual suspects all came back clean:
client-class-on-dedicated-server (0 matches), client-supplied destruction parameters (impossible by schema),
duplicate strike execution (one strike per owner, monotonic replay gate), chunk generation from targeting
(asserted by GameTest), and un-restored camera state (asserted after skip, completion, cancellation and
dimension change).

## Medium-severity issues (schedule before or during Part 2)

| ID | Issue | File | Effort |
| --- | --- | --- | --- |
| QA-01 | `Nullstar.reload()` discards `weapon.scaled(...)` — inert; fixing it "properly" would double-scale every weapon | `Nullstar.java:40` | minutes |
| QA-02 | Preview strikes skip operator authorization yet still emit world sounds/particles and consume a global strike slot — behaviour is neither documented nor tested | `WeaponEngine.java:68,103,112` | small (decide + gate or document) |
| QA-03 | Living-entity damage uses an unattributed explosion source → bypasses the vanilla `pvp` gamerule, no attribution | `EntityEffects.java:21` | small (decide policy) |
| QA-04 | `DestructionManager.enqueue` and `Timeline.advance` throw unguarded inside the server tick; a bad strike could take down the tick loop | `DestructionManager.java:38`, `Timeline.java:25`, `WeaponEngine.tick` | small (guard + cancel one strike) |
| QA-05 | `CinematicCamera`, `CameraPreset`, `EntityEffects`, `SoundScheduler`, `EffectRenderer`, `NullstarHud`, `PresentationLayers` and `DestructionManager` reservation accounting have **no** direct unit tests; the camera is asserted only as a boolean | `src/test/java` | moderate |
| QA-13 | A legal stage length (≤2400) × a legal duration multiplier (≤4) exceeds the validator's own stage bound, so `scaled()` throws — a boot failure today, an uncaught server-thread throw once P0-1 deletes `Nullstar.java:40`. **Reproduced by execution.** | `WeaponDefinition.java:24,47`, `Nullstar.java:40`, `WeaponEngine.java:67` | small (clamp + validate once at load) |

## Confirmed working subsystems

Clean-build release check (`clean build`, exit 0; JUnit 19/19 re-run; 5/5 dedicated GameTests) · real
graphical client loop (all three weapons, preview, cancel, skip, dimension change, save/reopen) ·
byte-reproducible release artifact · dedicated-server init · server authority and packet validation ·
the **global destruction candidate budget including skipped cells** · bounded
constant-memory destruction queue with round-robin scheduling · chunk-loading safety (guarded reads and
writes; no generation) · the full protection stack (tag, block entities, `getDestroySpeed < 0`,
`mayInteract`, world border, build limits, fail-closed `ProtectionHooks`) · terrain persistence across
save/reopen · inert preview · cancellation on command, dimension change and disconnect · monotonic
exactly-once timeline with scaling · `_v1` wire format with bounded strings and malformed-input rejection ·
the **camera Mixin seam, verified against real 26.3 `Camera.update`/`GameRenderer.extract` ordering** ·
environment separation · accessibility plumbing (`reducedMotion`, `reducedFlashing`, `particleBudget`
including 0, rebindable keys).

## Unverified subsystems

- **Multi-client multiplayer.** Every multiplayer-authority conclusion is code inspection plus a
  single-client automated run.
- **Dense-world performance.** Tick time, frame time and worst-case candidate volumes were never measured.
  The 4 ms destruction deadline is cooperative and unbenchmarked.
- **Plain `runServer` boot.** Only `runGameTest` and the integrated client server were exercised.
- **Third-party integration.** No claim-mod bridge, shader/renderer mod or resource pack was tested.
- **Cross-machine reproducibility.** The JAR hash reproduced exactly on this machine/JDK 25; another
  JDK or platform was not tried.
- **QA-04 reachability.** The two unguarded throw paths were proven unreachable by accounting analysis
  only; no attempt was made to synthesise a strike that breaks the invariant.
- **Camera feel, audio quality, final art.** Not assessable automatically; explicitly part of Opus's brief.

## Potentially unstable architectural contracts

1. **The camera Mixin anchor** — the one genuinely version-brittle seam. It injects at a *bytecode
   instruction* (`INVOKE Camera.getViewRotationMatrix(Matrix4f)`) inside `Camera.update`. It is currently
   correct and correctly placed (after `alignWithEntity`/`calculateFov`, before
   `prepareCullFrustum`/`setupPerspective`, so both culling and projection pick up the override). Any
   26.3.x body change to `update` breaks it; `defaultRequire: 1` converts that into a loud load-time
   failure, which is the safe direction.
2. **`state_v1` / `visual_v1` schemas** — stable, but extending them in place would break older clients.
   Any new field for Part 2 camera choreography must be a new channel version.
3. **Weapon extensibility** — the three behaviour ids are hard-coded in both `WeaponDefinition`'s
   validation and `WeaponCatalog.IDS`; adding a fourth weapon is a code change, not data.
4. **The destruction reservation invariant** — load-bearing for Glassfall's five impacts and untested at
   unit level.
5. **`ProtectionHooks` adapter cost** — `allows` runs per candidate block and per entity, so a heavy
   claim-mod adapter will directly throttle the destruction throughput rather than merely slow one call.

## Safest continuation strategy

1. Land **P0-1** (delete `Nullstar.java:40`) and record the **P0-2** preview decision before touching the
   camera or the effects.
2. Branch for Part 2 and build the Helios presentation first against the existing, verified timeline and
   camera presets; do not modify server authority, the budget semantics, or the Mixin injection point.
3. Close **QA-05** early — write `CinematicCameraTest` and `DestructionManagerTest` — because Part 2 will
   rewrite exactly the code those tests protect.
4. Fold P2 items into the Part 2 work rather than scheduling them separately.
5. Before release: `./gradlew clean build`, then `./gradlew runClientGameTest`, then verify the artifact
   contains `com.nullstar.Nullstar` and no test mod — and update `ACCEPTANCE.md` honestly, including the
   still-open multi-client and performance rows.

Part 1 does **not** need to be rebuilt before Part 2 starts.
