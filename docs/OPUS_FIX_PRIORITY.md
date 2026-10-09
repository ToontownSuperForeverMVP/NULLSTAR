# Part 2 fix priority — ordered guide for Claude Opus 5.5

Companion to `docs/DEEPSEEK_QA_REPORT.md`. Findings and severities are defined there; this file is the
**execution order**.

**Readiness: CONDITIONAL PASS.** There are **no release blockers** and **no CRITICAL defects**. You may
start Part 2 cinematic work immediately. Do stage P0 first — it is a two-line change that removes a
landmine sitting directly under the code you are about to extend.

Guiding constraint from `AGENTS.md`, unchanged by this audit: server authority, the global candidate
budget, chunk-loading safety, `ProtectionHooks`, and frame-local camera restoration are contracts.
Everything in P0/P1 is a *repair* — do not redesign around it.

---

## P0 — Do this before writing any Part 2 feature (≈ 20 minutes)

### P0-1 · Remove the dead duration-scaling statement — **and fix the scaling it was accidentally guarding** (QA-01 + QA-13, MEDIUM)
**File:** `src/main/java/com/nullstar/Nullstar.java`, line 40
```java
var weapons=new WeaponCatalog(directory.resolve("weapons"));
weapons.all().forEach(weapon->weapon.scaled(next.cinematicDurationMultiplier)); // ← delete this line
config=next;catalog=weapons;
```
`WeaponCatalog.all()` returns the immutable map values and `WeaponDefinition.scaled` returns a **new
record**, so the statement has no effect on the catalog. Scaling is already applied correctly, once, at
`WeaponEngine.java:67`. **Do not** "fix" it by assigning the results back into the catalog, or every phase
length and cue tick will be scaled twice (`multiplier²`) across all three weapons.

**Read QA-13 before deleting it, because the line is not purely inert.** It is currently the only place a
scaled definition is ever validated at load time. `WeaponDefinition` accepts a stage of up to **2400
ticks**, and `ServerConfig` accepts a duration multiplier of up to **4**; their product (9600) then fails
the same constructor's own `ticks > 2400` check, so `scaled()` throws for two individually legal values
(reproduced: `def(2400).scaled(4.0)` → `IllegalArgumentException: Invalid or repeated phase`). Today that
surfaces at boot as the misleading `Invalid NULLSTAR configuration; fix config/nullstar before startup`.
If you delete line 40 first, the same input instead throws from `WeaponEngine.java:67` inside a packet
handler on the server thread, where nothing catches it.

Do P0-1 in this order:
1. Make scaling non-throwing — clamp the scaled stage length to the validator's bound inside
   `WeaponDefinition.scaled()` (or build through a private non-revalidating constructor), so a legal input
   can never produce an exception.
2. Validate the scaled result **once at load** and store it (`WeaponCatalog`), so the multiplier and the
   per-stage bound are checked together rather than independently.
3. *Then* delete `Nullstar.java:40` — or, better, replace it with the stored scaled definitions from
   step 2 so the load-time check is real rather than incidental.
4. Correct the sentence in `docs/ARCHITECTURE.md` under "Authoritative execution" that implies
   catalog-level scaling, so the doc matches the code.

**Verify:** `./gradlew test` still passes; `def(2400).scaled(4.0)` no longer throws; and a
`cinematicDurationMultiplier` of `2` still yields ~600 ticks for Helios (proving the original line was
behaviourally inert for the shipped definitions).

### P0-2 · Record the decision about preview side effects (QA-02, MEDIUM)
**File:** `src/main/java/com/nullstar/server/WeaponEngine.java`, lines 103 and 112

Preview strikes are non-destructive (confirmed: `0 changed blocks` in the automated run), but they still
emit sounds to all nearby players and broadcast visual cues, and they consume one of
`maximumActiveStrikes`. Decide and document one of:
- **(a) keep as designed** — then say so explicitly in `docs/ARCHITECTURE.md` and `README.md` ("preview is
  operator-optional by design; it produces sound and visual cues but no damage or terrain change"), or
- **(b) tighten it** — gate the `SoundScheduler.phase(...)` call at `:103` and the `VisualPayload`
  broadcast at `:112` behind `!strike.preview`, and/or exclude preview strikes from the
  `active.size()>=config.maximumActiveStrikes` test at `:74`.

Prefer (b) if the mod will ever run on a public server; prefer (a) if preview is meant to be a free
spectator feature. Either way, **write it down** — today the behaviour is neither documented nor tested.

---

## P1 — Engineering repairs before final release (no Part 2 dependency)

### P1-1 · Attribute or scope living-entity damage (QA-03, MEDIUM)
**File:** `src/main/java/com/nullstar/server/EntityEffects.java`, line 21

```java
entity.hurtServer(level, level.damageSources().explosion(null,null), (float)(damage*falloff));
```
The source has no attacker entity, so the damage is not a player attack: vanilla's `pvp` gamerule does
not gate it, no death message credits the operator, and it reads oddly in server logs. Choose:
- use an owned source (`damageSources().explosion(ownerEntityOrNull, null)`) when the owner is online so
  attribution and PvP gating behave like a normal explosion, **or**
- keep the unattributed source and document explicitly in `README.md`/`ARCHITECTURE.md` that NULLSTAR blast
  damage is exempt from the `pvp` gamerule, and add a test asserting the chosen behaviour.

An undecided policy here is worse than either option, because the current behaviour is silent.

### P1-2 · Make the server tick fail safe, not fail hard (QA-04, MEDIUM)
**Files:** `src/main/java/com/nullstar/server/WeaponEngine.java` (`request`, `tick`),
`src/main/java/com/nullstar/server/DestructionManager.java:38`, `src/main/java/com/nullstar/core/Timeline.java:25`

Two invariants currently `throw` on the server thread with no guard:
`DestructionManager.enqueue` raises `IllegalStateException("Unreserved destruction work")` at
`DestructionManager.java:38`, and `Timeline.advance` raises `IllegalArgumentException("Timeline cannot rewind")`
at `Timeline.java:25`. Analysis says neither is
reachable under the current accounting — but "unreachable" is exactly the assumption that breaks when
Part 2 adds a weapon, a cue type or a second queue.

Wrap the per-strike body of `WeaponEngine.tick` in a guard that logs the strike id plus exception, cancels
that strike (`finish(server, strike, true)` + `destruction.cancel(strike.id)`) and continues with the
remaining strikes, and do the same for the packet handler. One bad strike must never stop the tick loop.

### P1-3 · Close the two highest-value test gaps (QA-05, MEDIUM)

> Add one assertion to this work: `WeaponDefinition.scaled(4.0)` on a definition whose longest stage is
> 2400 ticks must not throw (QA-13). One line in `EngineTest` would have caught it.

**New files:** `src/test/java/com/nullstar/client/CinematicCameraTest.java`,
`src/test/java/com/nullstar/server/DestructionManagerTest.java`

You are about to rewrite the camera and the effects. Cover, at unit level:
- `CinematicCamera.blend` / `ease`: yaw wraps across ±180° by the short path; `t=0` reproduces the source
  pose; `releaseStart` ramps smoothly and hands back to vanilla within 12 ticks; `skip()` suppresses only
  the owner's strike; a dead player or an open screen returns `null` without leaving blend state behind.
- `DestructionManager.reserve/enqueue/releaseReservation`: reservations for N impacts admit exactly N jobs;
  releasing a reservation without enqueuing frees the slot; `jobs.size() + Σreservations` never exceeds
  `limit`; `progress().active()` reaches 0 after the last job.

These are pure-logic targets — no Minecraft bootstrap needed, same as the existing `EngineTest`.

### P1-4 · Harden `broadcast()` against future payload types (QA-11, LOW)
**File:** `WeaponEngine.java:145-152` — when the payload is neither `StatePayload` nor `VisualPayload`,
the method currently has no owner and sends to **every** player in every dimension. Make the default
explicitly deny, or overload `broadcast` per payload type so a new payload cannot silently become global.

### P1-5 · Build reproducibility guard (Domain 2, LOW)
Add a Java 25 toolchain/version guard in `build.gradle` so a machine whose default JDK is 21 (including
this one) fails with a clear message instead of a Loom error. Cheap, and it is the single most likely
first failure for a fresh clone.

---

## P2 — Low-severity polish (fold into Part 2 work, do not schedule separately)

| ID | Item | File |
| --- | --- | --- |
| QA-06 | Controller in the offhand is accepted by the server but shows no client aiming preview — make the client check main **or** offhand, matching `WeaponEngine.java:36` | `NullstarClient.java:78`, `EffectRenderer.java:74`, `NullstarHud.java:13` |
| QA-07 | `reset()` zeroes the client tick baseline, so the HUD can read "Ready" while the server still holds a cooldown — keep the last status without resetting `statusReceived`, or display the server value verbatim | `NullstarClient.java:88` |
| QA-08 | An override naming an unknown sound id silently produces no sound; log once at load instead | `SoundScheduler.java:18` |
| QA-09 | `data/nullstar/weapons/*.json` is loaded from the **classpath**, so a data pack can never override it. Either move the bundled defaults somewhere honest (e.g. `assets/`) or document that only `config/nullstar/weapons/<id>.json` overrides work | `WeaponCatalog.java:22-31` |
| QA-10 | `/nullstar cancel` with nothing to cancel returns 0 and prints nothing | `NullstarCommands.java` |
| QA-12 | `docs/evidence/artifact-validation.json` has no generator or command transcript — add the script and the command that produced it when you regenerate release evidence | `docs/evidence/` |

---

## P3 — Part 2 artistic work (separate from the repairs above)

Nothing in P0–P2 blocks this. Do not let the repairs gate the visuals.

1. **Helios art first** — authored orbital collector mesh, compressed gold source, deliberate alignment
   motion, sharp descending column. Keep the target readable throughout. The camera already frames the
   charge source and follows the descending beam (`CinematicCamera` `orbital`/`beam` presets).
2. **Native 26.3 GPU pipelines** via `PresentationLayers.Layer` and the real RenderPearl/extraction APIs.
   Do not add immediate-mode GL, do not bypass the extraction pipeline.
3. **Camera choreography through `CameraPreset`**, never weapon-specific mixin branches. Preserve
   frame-local restoration, held-item suppression and every accessibility control. If a richer sequence
   needs data the client does not have, add it as a **new channel version** (`state_v2`) — never extend
   `state_v1` in place.
4. **Authored audio** replacing the vanilla sound IDs in the weapon JSON, coordinated with the existing
   server phase cues. Keep the deliberate bypass of the vanilla player-distance filter for orbital
   sources.
5. **Singularity and Glassfall art** — keep the staggered Glassfall server timestamps and the five
   reserved impact slots; keep Singularity's entity-only gravity semantics.
6. **Concurrent-client and dense-world validation** — the one acceptance area with no evidence at all.

---

## P4 — Release checklist (owner: whoever ships)

```bash
export JAVA_HOME=<JDK 25>          # mandatory on this machine
./gradlew clean build              # release check + JUnit + dedicated GameTests
./gradlew runClientGameTest        # presentation/input/network
./gradlew genSources               # separate invocation only
```

- Confirm `build/libs/nullstar-1.0.0.jar` and that `nullstar-test` is absent from the artifact.
- **Baseline already established at `0e07887`:** all three commands were executed by this audit and passed
  (`clean build` exit 0 with JUnit 19/19 and GameTests 5/5; `runClientGameTest` exit 0). The rebuilt JAR
  hashes to the same SHA-256 recorded in `docs/evidence/artifact-validation.json`, and the packaged JAR
  contains `com.nullstar.Nullstar` with **0** test classes. Re-run the same three commands after your
  edits — a green compile does not cover a behaviour change.
- Re-run `./gradlew clean build` **after** every P0/P1 edit; a green compile does not cover a behaviour
  change.
- Update `docs/ACCEPTANCE.md` honestly: mark the multi-client, density and third-party rows per what was
  actually done, not per what compiled.

---

## Do-not-touch list

Preserve these while working; they are verified contracts, not incidental code:

- The global candidate-budget semantics in `DestructionJob.step` — **every** cursor index consumes budget,
  including air, unloaded, protected and skipped cells.
- `DestructionManager`'s reserve-before-launch invariant (Glassfall reserves all five impacts up front).
- The `hasChunk` guard before every terrain read and write; never force-load.
- `ProtectionHooks` fail-closed behaviour (a throwing hook denies).
- Never accept client-supplied coordinates, radius, depth, dimension or seed as trusted input.
- The camera Mixin's injection point and its frame-local, self-restoring write. Note it is anchored on a
  bytecode instruction in `Camera.update` and is the one version-brittle seam in the mod — if a 26.3.x
  patch changes that method, expect a load-time failure (`defaultRequire: 1`), and fix the anchor.
