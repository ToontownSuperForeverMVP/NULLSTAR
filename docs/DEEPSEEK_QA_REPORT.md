# NULLSTAR Part 1 — independent QA engineering review

**Auditor:** DeepSeek V4.1 Flash Fast (independent adversarial review)
**Revision audited:** `0e07887dcff77055eee4ed04d0c645c2b1236ad7` (`master`, clean tree)
**Author under review:** GPT-6.1 Sol High (Part 1)
**Next owner:** Claude Opus 5.5 Medium (Part 2)
**Execution evidence:** `docs/QA_EXECUTION_RESULTS.md`

> Scope note: every finding below is tagged **executed** or **inspection**. Nothing is called "runtime
> tested" unless it actually was. The audit completed its full command set against this revision:
> `./gradlew build` (exit 0), `./gradlew clean build` (**exit 0** — JUnit **19/19** re-run from source and
> **5/5** dedicated GameTests), and `./gradlew runClientGameTest` (**exit 0**, full graphical scenario).
> The release JAR rebuilt from a wiped output directory is **byte-identical** to the hash recorded in
> `docs/evidence/artifact-validation.json`. Command-by-command detail is in `docs/QA_EXECUTION_RESULTS.md`.
> The findings were not downgraded by that green build: they are defects a passing build cannot see.

---

## 0. Bottom line

Part 1 is a **credible, well-structured functional foundation**. The architectural claims that matter most —
server authority, no client-supplied destruction parameters, a global candidate budget that counts air and
skipped cells, never force-loading chunks, never removing containers/bedrock, and frame-local camera
override — **hold up under inspection**, and the key ones are backed by real automated evidence.

**No CRITICAL defect was found.** Six MEDIUM defects and a set of LOW issues are real and worth fixing
before or during Part 2; none of them blocks cinematic work. One of the six (QA-13) was added during the
final verification pass and is the only finding reproduced by direct execution.

**Readiness decision: CONDITIONAL PASS** — Opus can start Part 2 immediately on a branch, provided the
MEDIUM items are scheduled and the two highest-value test gaps are closed early.

---

## 1. Findings summary

| ID | Severity | Area | One-line | Status |
| --- | --- | --- | --- | --- |
| QA-01 | MEDIUM | Config/wiring | `Nullstar.reload()` discards `WeaponDefinition.scaled(...)`; duration-scaling wiring is a no-op with a double-scaling landmine | Confirmed (inspection) |
| QA-02 | MEDIUM | Server authority | Preview strikes bypass operator authorization yet still emit world sounds/particles and consume a global strike slot | Confirmed (inspection) |
| QA-03 | MEDIUM | Entity damage policy | Living-entity damage uses an unattributed explosion source → bypasses the vanilla `pvp` gamerule and yields no attribution | Confirmed (inspection) |
| QA-04 | MEDIUM | Robustness | Unguarded throw paths inside the server tick loop (`enqueue`, `Timeline.advance`) can take down the tick instead of failing safely | Confirmed as latent (inspection; reachability not demonstrated) |
| QA-05 | MEDIUM | Testing | The most intricate client logic (`CinematicCamera`, presets, HUD, effects) and the destruction reservation accounting have no direct unit tests | Confirmed (inspection of `src/test`) |
| QA-13 | MEDIUM | Config/validation | A *valid* stage length (≤2400 ticks) multiplied by a *valid* duration multiplier (≤4) exceeds the validator's own stage bound, so `scaled()` throws — today that is a boot failure, and after the P0-1 deletion it becomes an uncaught server-thread throw | **Confirmed (executed)** — reproduced on JDK 25 |
| QA-06 | LOW | Client/server parity | Controller-in-offhand is accepted by the server but produces no client aiming preview | Confirmed (inspection) |
| QA-07 | LOW | HUD correctness | `NullstarClient.reset()` zeroes the client tick baseline → HUD can read "Ready" while the server still enforces cooldown | Confirmed (inspection) |
| QA-08 | LOW | Diagnostics | Unknown sound id in an operator override silently produces no sound | Confirmed (inspection) |
| QA-09 | LOW | Configuration UX | Weapon overrides under `data/nullstar/weapons` load from the classpath, so a data pack can never override them; mis-named override files are silently ignored | Confirmed (inspection) |
| QA-10 | LOW | Commands | `/nullstar cancel` prints nothing when the count is 0 | Confirmed (inspection) |
| QA-11 | LOW | Networking robustness | `broadcast()` falls through to "send to every player" for any payload type other than `State`/`Visual` | Confirmed as latent (inspection) |
| QA-12 | LOW | Evidence provenance | `docs/evidence/artifact-validation.json` has no generator or command transcript in the repo (its *claims*, however, were all independently reproduced by this audit) | Confirmed (inspection); claims reproduced |

### 1b. Finding detail

Every code reference is `file:line` at revision `0e07887`. "Verified how" states whether the claim was
proved by running something or only by reading code.

| ID | File and line | Technical explanation | Evidence / reproduction | Impact | Suggested fix | Verified how | Owner |
| --- | --- | --- | --- | --- | --- | --- | --- |
| QA-01 | `Nullstar.java:40` | `WeaponCatalog.all()` (`WeaponCatalog.java:40`) returns `definitions.values()`; `WeaponDefinition.scaled` returns a **new immutable record** (`WeaponDefinition.java:47`). The statement therefore has no effect. Effective scaling is the single call at `WeaponEngine.java:67`. | Search for `.scaled(` returns exactly three sites: `Nullstar.java:40`, `WeaponEngine.java:67`, `EngineTest.java:19`. Only `:67` mutates anything observable. | Misleading architecture doc; a maintainer who makes `:40` effective would scale every phase and cue tick **twice** across all three weapons, with no test that would catch it. | Delete line 40. Never also move scaling into the catalog. | Inspection. Fix impact is testable (`cinematicDurationMultiplier=2` must stay ~600 ticks, not ~1200). | Sol follow-up (1-line) |
| QA-02 | `WeaponEngine.java:68`, `:74`, `:103`, `:112`; `NullstarCommands.java:25` | `if(!preview && !authorized(player))` lets a non-operator launch a `preview` strike. Terrain and damage are gated on `!strike.preview` (`:106`, `:111`, `:116-118`), but `:103` (`SoundScheduler.phase`) and `:112` (`VisualPayload` broadcast) are **not**, and `:74` counts the strike against `maximumActiveStrikes`. | Controller is a creative-inventory item via `Nullstar.java:48` (`TOOLS_AND_UTILITIES`), so any creative player has it. Trigger with a typed `RequestPayload(1,n,ACTIVATE,weapon,true)` or operator-free `/nullstar preview`. Author log confirms preview is inert for terrain (`34d42fb1 … 0 changed blocks`). | Availability/griefing: eat the 4 default strike slots; make `SoundScheduler` (which deliberately ignores the vanilla distance filter) blast every player within 256 blocks. No damage, no terrain change. | Decide and record: either document "preview is operator-optional by design", or gate `:103`/`:112` on `!strike.preview` and exclude preview from `:74`. | Inspection, plus the author's log for the `0 changed blocks` half. | Opus Part 2 (policy) |
| QA-03 | `EntityEffects.java:21` | `level.damageSources().explosion(null,null)` carries no attacker entity, so the damage is not a player attack. Exclusion is only creative/spectator (`:40`). | Read `:16-25`. No test exercises the damage source or the `pvp` gamerule. | Server `pvp=false` does not gate NULLSTAR blast damage; no death-message attribution; unexpected in server logs. | Use an owned source when the owner is online, or document the exemption explicitly and test it. | Inspection. | Opus Part 2 |
| QA-04 | `DestructionManager.java:38`; `Timeline.java:25`; `WeaponEngine.tick`/`request` | Two invariant throws sit on the server thread with no guard: `enqueue` raises `IllegalStateException("Unreserved destruction work")`, `advance` raises `IllegalArgumentException("Timeline cannot rewind")`. | Read `DestructionManager.java:37-39`, `Timeline.java:24-25`; traced the reservation accounting — every impact cue either enqueues or releases exactly once, so the throw is **not reachable today**. | Latent: adding a weapon, a cue type or a second queue in Part 2 can turn a bookkeeping bug into a dead tick loop instead of one cancelled strike. | Wrap the per-strike body of `tick` and the packet handler in a guard that logs, cancels that strike and continues. | Inspection; reachability explicitly **not** demonstrated. | Opus Part 2 |
| QA-05 | `src/test/java` (whole suite) | `CinematicCamera` (81 lines), `CameraPreset`, `EntityEffects`, `SoundScheduler`, `EffectRenderer`, `NullstarHud`, `PresentationLayers` and `DestructionManager.reserve/enqueue/releaseReservation` have no direct tests. The camera is asserted only as `CAMERA.applied()` plus a final pose. | Compared the test inventory (19 JUnit tests in 3 classes + 4 dedicated GameTests + 1 client GameTest) against the class inventory under `src/client/java` and `src/main/java/com/nullstar/server`. | Part 2's brief is camera choreography and effects — i.e. exactly the untested surface. A drifting, jittering or never-releasing camera would still pass today. | Add `CinematicCameraTest` (blend/yaw-wrap/ease/release/skip) and `DestructionManagerTest` (reservation invariant). | Inspection of the test inventory. | Opus Part 2 (early) |
| QA-13 | `core/WeaponDefinition.java:24` (bound `s.ticks > 2400`), `:47` (`scaled` re-runs the canonical constructor), `:57-59` (`scaleTick`); `config/ServerConfig.java` (`finite(cinematicDurationMultiplier, .25, 4, 1)`); `server/WeaponEngine.java:67`; `Nullstar.java:40` | A stage with `ticks = 2400` is legal; a multiplier of `4` is legal. Their **product** is 9600, which the same canonical constructor re-validates and rejects. `scaled()` therefore throws `IllegalArgumentException("Invalid or repeated phase")` for a fully validated pair of inputs. Threshold: any stage with `ticks > 600` at multiplier `4` (the bundled weapons max out at `170 × 4 = 680`, so they are safe). | **Reproduced by execution**: compiled `WeaponDefinition` + `Phase` standalone with JDK 25 and ran `def(2400).scaled(4.0)` → `IllegalArgumentException: Invalid or repeated phase`, while `def(600).scaled(4.0)` → `duration = 2480` (ok). | Two distinct failure modes, and the P0 fix changes which one you get. **(now)** `Nullstar.reload()` line 40 *does* execute `scaled()`, so an operator override with a long stage plus multiplier 4 is caught at load and the mod refuses to start with the misleading message `Invalid NULLSTAR configuration; fix config/nullstar before startup` — even though both values are individually valid. **(after deleting line 40 as P0-1 instructs)** the load-time check disappears and the first `activate()` at `WeaponEngine.java:67` throws on the server thread inside a packet hander, with no try/catch — a server-side crash instead of a boot refusal. | Make scaling non-throwing: clamp the scaled stage length inside `scaled()` (or build through a private non-revalidating constructor), then validate the scaled result **once** at load and store it. Do not simply delete `Nullstar.java:40`. | **Executed** (standalone JDK 25 run). Reachability from `activate()`/`reload()` proven by call-site reading. | Sol follow-up for the clamp; then P0-1 can be applied safely |
| QA-06 | `WeaponEngine.java:36` vs `NullstarClient.java:78`, `EffectRenderer.java:74`, `NullstarHud.java:13` | Server accepts the controller in main **or** offhand; all three client paths require the **main** hand. | Read the call sites. | With the controller in the offhand, targeting still works server-side but no aiming ring or coordinate readout appears. UI inconsistency only. | Make the client checks accept both hands. | Inspection. | Opus Part 2 (P2) |
| QA-07 | `NullstarClient.java:88` | `reset()` sets `tick=0` and `statusReceived=0` and reconstructs a zero-cooldown `StatusPayload`; it is called on every client tick without a level/player (`:59`). | Read `:58-63` and `:88`; `cooldown()` derives from `tick-statusReceived`. | HUD can show "Ready" while the server still enforces cooldown; the server rejects the strike and corrects it. Cosmetic. | Preserve the last server status across `reset()`, or render the server value verbatim. | Inspection. | Opus Part 2 (P2) |
| QA-08 | `SoundScheduler.java:18` | `BuiltInRegistries.SOUND_EVENT.getOptional(...).ifPresent(...)` — an unknown sound id is silently skipped, and `stage.sound().isBlank()` also returns early. | Read `:17-19`. | An operator override with a typo'd sound id produces no error and no sound; hard to diagnose. | Log once at config-load time for ids that do not resolve. | Inspection. | Opus Part 2 (P2) |
| QA-09 | `WeaponCatalog.java:22-31` | Overrides are read from `config/nullstar/weapons/<id>.json`; anything else in that folder is ignored. When absent, defaults are read from the **classpath** resource `/data/nullstar/weapons/<id>.json`, so a data pack shipping the same path has no effect. | Read `:20-33`; the resource path is a classpath lookup, not a registry/data-pack lookup. | The `data/` location implies a data-pack-overridable definition, which it is not; also a silent no-op for misnamed override files. | Move the bundled defaults to an honest location and/or document that only `config/nullstar/weapons` overrides work; log ignored files. | Inspection. | Opus Part 2 (P2) |
| QA-10 | `NullstarCommands.java:26` | `.executes(...)` returns `cancelOwner(...)`'s count directly; 0 produces no feedback. | Read `:26` against `WeaponEngine.cancelOwner`. | Minor UX: `/nullstar cancel` appears to do nothing when nothing was active. | Send a success/failure message. | Inspection. | Opus Part 2 (P2) |
| QA-11 | `WeaponEngine.java:145-153` | The owner lookup only resolves `StatePayload`/`VisualPayload`; for any other payload `strike == null`, the dimension/distance `continue` never fires, and the packet is sent to every player in every dimension. | Read `:145-153`; confirmed the only current callers pass `State`/`Visual` types. | Latent: a future payload broadcast through this helper would leak globally. | Make the default deny, or overload `broadcast` per payload type. | Inspection; not reachable with today's callers. | Opus Part 2 (P1/P2) |
| QA-12 | `docs/evidence/artifact-validation.json` | Asserts version, `=26.3`, entrypoint, 0 test classes, class major 69 and a SHA-256, with no generator script or command transcript in the repository. **Update:** this audit rebuilt the JAR via `clean build` and reproduced *every* field, including the byte-identical SHA-256 `31C45671…D7A29D2`, the class major `69`, the entrypoint and the `0` packaged test classes. | The file was read; then `sha256sum`, `unzip -l`, `od` and `unzip -p fabric.mod.json` were run against `build/libs/nullstar-1.0.0.jar`. See `docs/QA_EXECUTION_RESULTS.md` §Artifact verification. | Downgraded to a process nit: no claim is now unsupported, but the evidence file still has no generating script, so it cannot be regenerated by a future maintainer from the repo alone. | Add the inspection script and the exact command when regenerating release evidence. | **Executed** (hash and metadata reproduced) | Sol follow-up |

Deliberately **not** reported as defects (see §4): vanilla-only particle art, absent custom audio,
preview being non-destructive, cancellation not rolling back terrain, the cooperative 4 ms deadline,
`data/`-path resource placement, and job/cooldown non-persistence. These are documented boundaries,
not bugs.

---

## 2. Reviewer-domain detail

The audit was structured as twelve independently scoped review domains. Domains that found nothing
worth reporting are marked accordingly, with the positive checks that justify the pass.

### Domain 1 — Fabric 26.3 API and dependency compatibility
**Result: pass.** Pinned versions are consistent across `gradle.properties` (26.3 / 0.19.5 /
0.162.0+26.3 / Loom 1.18.3), the wrapper (`9.7.1` + `distributionSha256Sum`), `fabric.mod.json`
(`minecraft =26.3`, `java >=25`, `fabricloader >=0.19.5`, `fabric-api >=0.162.0+26.3`), and the runtime
mod list in the game-test log. Every Minecraft symbol used by `src/main` was located in the pristine 26.3
API dump (see `QA_EXECUTION_RESULTS.md`), including the 26.3-only `Permissions` /
`PermissionSet permissions()` model and `sendOverlayMessage`. No Yarn or GLFW usage exists anywhere.
The mixin config declares `compatibilityLevel: JAVA_25` and `defaultRequire: 1`, so a broken injection
point would fail loudly at load rather than silently — and the mixin did load in the author's client run.

### Domain 2 — Gradle, Java 25, packaging, reproducible builds
**Result: pass with one reproducibility note.** Split source sets map correctly (`mods { sourceSet
main; sourceSet client }`), the test mod is a separate `nullstar-test` source set that is not packaged,
`processResources` expands `${version}`, `options.release = 25`, `withSourcesJar()` is on, and the wrapper
is checksum-pinned. `./gradlew build` in this audit returned exit 0 with 5/5 GameTests passing.
**Note:** the default JDK on the audit machine is 21, so the build hard-fails without a `JAVA_HOME`
override — documented for this machine but worth making a Gradle precondition check (`JavaVersion` guard
or toolchain) so a fresh clone fails with a clear message rather than a Loom error.

### Domain 3 — Architecture, interfaces, unnecessary coupling
**Result: pass.** The layering is genuinely clean: `core/` is Minecraft-free except for the geometry/job
types, `network/` is record + `StreamCodec` only, `server/` owns all authority, `presentation/` is
read-only over `ClientStrikeState`, and the two intended extension seams (`PresentationLayers.register`,
`CinematicCamera.register` + `CameraPreset`) are real and used by the shipped presets. **QA-01** is the
one architectural wart: `Nullstar.java:40` implies the catalog holds scaled definitions when in fact
scaling happens per activation at `WeaponEngine.java:67`.

### Domain 4 — Server authority, packet validation, multiplayer security
**Result: pass on the core contract; two MEDIUM/LOW exposures.**

*Verified good.* `RequestPayload` (`network/RequestPayload.java`) carries **intent only** — protocol
version, monotonic sequence, action, weapon string, preview flag. There is no target, radius, depth,
dimension or damage field on the wire, so a client cannot supply destruction parameters. Everything
authoritative is recomputed server-side: `TargetingService.acquire/valid` (range, loaded chunk, world
border, build limits, alive/non-spectator), `ProtectionHooks.allows` (fail-closed: a throwing hook logs
and denies), `CraterGeometry` hard caps, and the seed from `player.level().getRandom().nextLong()`
(`WeaponEngine.java:83`). Every destructive path re-checks the operator gate at
`WeaponEngine.java:68`. `ActivationGate.request` enforces a positive, strictly increasing sequence for a
fresh `UUID` plus a 4-tick spacing gate, and `prune(tick)` runs every tick (`WeaponEngine.java:92`), so
neither the session map nor the cooldown map grows without bound. Payload compact constructors validate
on decode, `readUtf` bounds are enforced, and `SerializationTest.invalidRequestsAndStatesRejected` proves a
33-character weapon string is rejected with a `DecoderException` — a hostile packet can only drop its own
connection, not the server.

*Exposures.*

**QA-02 (MEDIUM) — preview authorization bypass with real world side effects.**
`WeaponEngine.java:68` reads `if(!preview && !authorized(player))`. A non-operator can therefore launch a
strike flagged `preview`. The destructive paths are genuinely inert (`:106` attraction, `:111` reservation
release, `:116-118` enqueue + entity damage are all gated on `!strike.preview`), and the client log
confirms a preview strike logged `0 changed blocks`. **But** two side effects are *not* gated:
`:103` calls `SoundScheduler.phase(...)` on every phase change, and `:112` broadcasts the `VisualPayload`.
`SoundScheduler` deliberately bypasses the vanilla player-distance filter and sends to every player within
256 blocks in the dimension. `WeaponEngine.java:74` also counts preview strikes against
`maximumActiveStrikes` (default 4), so four non-operators holding the controller can deny all capacity.
Reachability: the controller is a normal creative-inventory item registered into
`CreativeModeTabs.TOOLS_AND_UTILITIES`, so any player in creative mode obtains it without operator
rights; the typed packet path (`preview=true`) or the operator-free `/nullstar preview` command
(`NullstarCommands.java:25`) completes the trigger. Impact is availability and griefing only — no damage,
no terrain change. *Fix:* gate the sound/visual broadcast for preview strikes behind a config flag (or
suppress preview sound), and optionally exclude preview strikes from the `maximumActiveStrikes` count.

**QA-11 (LOW) — `broadcast()` default-allow.**
`WeaponEngine.java:145-152` resolves the owning strike only for `StatePayload`/`VisualPayload`; any other
payload type yields `strike == null`, the `continue` never fires, and the packet goes to **every** player in
every dimension. Today `broadcast` is only ever called with those two types (status packets go through
`ServerPlayNetworking.send` directly), so this is latent — but it is a trap for the next contributor.

### Domain 5 — Weapon state machines, timing, lifecycle
**Result: pass with one wiring defect (QA-01).**

`Timeline` is a genuinely pure monotonic machine: `advance(age)` rejects rewinds, computes `changed` from
a phase transition, and emits every cue with `lastAge < tick <= age`, so a delayed/lagging tick interval
still delivers each cue exactly once. `EngineTest.timelineDeliversExactlyOnceAndCannotRewind` and
`exactPhaseBoundariesAndCancellation` prove both properties on real definitions. Machine-generated
offsets match the specification exactly (Helios 20+60+40+20+30+10+100+20 = 300 with the impact cue at
170; Singularity 355 / cue 220; Glassfall 395 / cues 90-260, all `< duration`, so `scaleTick` can never
throw `"Cue outside timeline"` for bundled or validated data). Cooldown begins at authorization
(`:86`), one strike per owner is enforced at `:74`, and `finish(...)` always broadcasts a terminal
snapshot and cancels queued work before the strike is removed from `active`. Lifecycle cancellation is
correct on owner death, disconnect, dimension change, unloaded target and level loss (`:94-99`), and
`stop()` clears every map on server shutdown.

**QA-01 (MEDIUM) — duration-multiplier wiring.**
`Nullstar.java:40`:
```java
weapons.all().forEach(weapon->weapon.scaled(next.cinematicDurationMultiplier));
```
`WeaponCatalog.all()` returns `definitions.values()`, and `scaled` returns a **new immutable record** — so
this statement has no effect whatsoever. Scaling actually happens once, correctly, at
`WeaponEngine.java:67` (`Nullstar.catalog().get(weapon).scaled(config.cinematicDurationMultiplier)`).
Consequences: (a) `docs/ARCHITECTURE.md` describes catalog-level scaling that does not exist; (b) it is a
landmine — a maintainer who "fixes" this line by assigning results back into the catalog would apply the
multiplier **twice** (`multiplier²`), silently corrupting every phase length and cue tick for all three
weapons with no test coverage to catch it (the unit test only exercises `scaled` in isolation).
*Fix:* delete line 40, or move scaling into the `WeaponCatalog` constructor and delete the per-activation
call — never both.

### Domain 6 — Terrain destruction geometry, persistence, bounded processing
**Result: pass. This is the strongest subsystem in the project.**

*The global-budget rule genuinely holds.* `DestructionJob.step` (`core/DestructionJob.java:15-27`)
increments `used` for **every** cursor index, before and independent of the visitor's return value, and
`DestructionManager.tick` (`:45-75`) debits that from the single `left` counter for all dimensions. So air,
unloaded, protected and skipped cells all consume budget exactly as `AGENTS.md` demands — a claim most
implementations get wrong. `EngineTest.scanningBudgetIncludesEmptySpaceAndCancellationStopsAllWork` proves
it: a step of 17 returns 17 while the visitor is invoked at most 17 times.

*Constant memory.* The scan is cursor-based with deterministic index ordering; the queue holds exactly one
`Entry` per impact containing a `BlockPos`, never a position list. `EngineTest.queueCompletesWithoutDuplicateCoordinates`
proves the cursor visits each coordinate exactly once and that `progress()` reaches 1.0. Round-robin
scheduling (`removeFirst` / `addLast`) prevents one large crater from starving another.

*Safety of mutation.* Every candidate is filtered by world border, build limits, loaded chunk, air,
`getDestroySpeed < 0`, the `nullstar:protected_blocks` tag, `hasBlockEntity()`, the owner's
`mayInteract`, and every registered `ProtectionHooks` veto, in that order, before
`setBlock(..., UPDATE_CLIENTS | UPDATE_KNOWN_SHAPE)`. No force-loading (the `hasChunk` guard precedes every
read and write), no loot generation, no neighbour-shape cascade. `DestructionGameTest` exercises real
bedrock/chest/claim-veto preservation and `targetingDoesNotGenerateUnloadedChunks` asserts the chunk
count is unchanged after probing a distant position.

*Persistence.* Terrain goes through normal `setBlock`, so it saves with the chunk; the client log confirms a
crater survived save/reopen (`bbfd098c … 10230 changed blocks` then the reopened-world assertion).

*Residual.* The 4 ms deadline is evaluated only between 64-candidate slices, so one slice can overrun it;
the docs already state this is cooperative, not real-time. Entity enumeration in `EntityEffects` is
mutation-bounded but not query-bounded — also already documented.

### Domain 7 — Thread safety, races, memory leaks, resource cleanup
**Result: pass.** All mutable gameplay state is server-thread-only and confined to
`WeaponEngine`/`DestructionManager`/`ActiveStrike` (documented, and Fabric's `ServerPlayNetworking`
receivers plus item-use callbacks run on the server thread). `ProtectionHooks` uses
`CopyOnWriteArrayList`, so a hook registered during iteration is safe. Every collection is bounded and
released: `active`/`selections`/`targets` on disconnect and `stop()`; `destruction.reservations` and
`statistics` only via `reserve`/`cancel`/`clear` with `finish()` always calling `cancel`
(`WeaponEngine.java:138`); `ActivationGate.sessions` on disconnect, `cooldowns` pruned every tick; client
caches capped at 8 strikes and expired after 100 silent ticks. The only unclosed resource is the
`AutoCloseable` from `ProtectionHooks.register`, which is the documented integration contract.

### Domain 8 — Client rendering compatibility, effects registration, graphics architecture
**Result: pass.** `src/main` contains **zero** client, `blaze3d`, LWJGL or GLFW imports (verified by
search), so nothing client-only can load on a dedicated server. `fabric.mod.json` splits the entrypoints
and marks the mixin config `environment: client`, belt-and-braces with the mixin JSON's own `client` list.
`EffectRenderer` is a single tick-driven pass with one global particle budget divided between continuous
layers and event cues, honours `particleBudget == 0`, and honours `reducedFlashing` for both the screen
wash and the explosion flash. No per-frame custom packets exist. `PresentationLayers` is a real,
documented seam with a `reset()` contract. No resource-loading or GL lifecycle work happens in the wrong
phase — the client touches particles and HUD only from `ClientTickEvents`.

### Domain 9 — Cinematic camera state, interruption handling, restoration
**Result: pass — and the riskiest seam was verified against real 26.3 source.**

The injection point was checked against `.reference/Camera.java` and `.reference/GameRenderer.java`:

* `Camera.update(DeltaTracker)` contains **exactly one** `getViewRotationMatrix(Matrix4f)` invocation, and
  it sits *after* `alignWithEntity(...)` and `this.fov = calculateFov(...)` but *before*
  `prepareCullFrustum(this.getViewRotationMatrix(...), createProjectionMatrixForCulling(), this.position)`
  and `setupPerspective(0.05F, depthFar, fov, w, h)`. The override therefore reaches **both** the culling
  frustum and the perspective projection in the same frame — the stated design intent, and a genuinely
  hard thing to get right in 26.3's extraction pipeline.
* The `extractRenderState` RETURN injection mutates `gameRenderer.gameRenderState().optionsRenderState.cameraType`.
  `GameRenderer.extract()` calls `extractOptions()` **unconditionally every frame**, and that method ends
  with `optionsState.cameraType = options.getCameraType();` — *before* `extractCamera(...)` runs the mixin.
  The write is therefore frame-local and self-restoring; it cannot leak a permanent third-person view.
  The held-item suppression is consistent: `renderItemInHand` gates on
  `optionsRenderState.cameraType.isFirstPerson()`.

Restoration paths are complete: `sample()` returns `null` (pure vanilla camera) when the player is dead,
the level is gone, a screen is open, camera is toggled off, or reduced-motion is on; `reset()` clears the
blend state on level change and disconnect; and the release easing cedes to the vanilla pose over 12
ticks. `skip()` marks all current strikes and the server continues. The client GameTest asserts the camera
entity, `CameraType.FIRST_PERSON`, and a sub-0.1 position delta after completion, skip, cancellation and a
dimension change.

### Domain 10 — Weapon controls, configuration, commands, permissions
**Result: pass with LOW items.** `/nullstar give`, `fire` and `reload` require
`COMMANDS_GAMEMASTER`; `reload` additionally refuses while any strike is active and retains the prior
configuration on failure (`NullstarCommands.java`, matching `Nullstar.reload()`'s build-then-assign order).
`target`, `status`, `cancel` and `preview` are intentionally operator-free and non-destructive. Server
config clamps every value on load, and `EngineTest.configurationClampsInvalidAndNonFiniteValues` proves
NaN/∞/out-of-range input falls back safely — a malformed config can never widen a safety limit.
`QA-08`, `QA-09` and `QA-10` are the diagnostic/UX gaps listed above.

### Domain 11 — Automated testing, runtime verification, edge cases
**Result: good foundation, real gaps (QA-05).**

*What is genuinely covered:* seeded geometry and radial falloff, monotonic phases and one-time/scaled
cues, resumable/cancelled queue progress without duplicate coordinates, configuration clamps, duplicate
and replayed activation, finite/range targeting (including NaN and ∞), typed packet round trips below 512
bytes, malformed-packet rejection, and client snapshot age/dimension/terminal cleanup. Dedicated GameTests
cover the global candidate budget, real block mutation, bedrock/chest/claim-veto preservation,
cancellation, living-entity damage/knockback/gravity, and collision targeting without generating chunks.
The client GameTest drives the real controller, key mappings, all three weapon loops, preview, cancel,
skip, a dimension change and save/reopen.

**QA-05 (MEDIUM) — the tests do not reach the riskiest logic.**
`CinematicCamera` (81 lines: easing, yaw-wrap blending, release ramp, skip set, stale-snapshot
rejection), `CameraPreset` samplers, `EntityEffects` falloff, `SoundScheduler`, `EffectRenderer` budget
arithmetic, `NullstarHud` and `PresentationLayers` have **no unit tests**. The camera is asserted only as a
boolean (`CAMERA.applied()`) plus a final pose, so a camera that drifts, jitters, or never releases would
still pass. `DestructionManager.reserve/enqueue/releaseReservation` accounting — the invariant that keeps
Glassfall's five impacts reserved — is exercised only indirectly. `WeaponEngine.request`'s preview
authorization path (QA-02) has no test at all. Because Part 2's whole job is camera choreography and
effects, this is the gap most likely to bite.

### Domain 12 — Resource validity, assets, documentation accuracy, Part 2 readiness
**Result: pass, with documentation nits.**

Assets are valid 26.3 resources: `assets/nullstar/items/controller.json` uses the modern
`{"model":{"type":"minecraft:model",...}}` item definition, the model parents `minecraft:item/generated`,
`lang/en_us.json` covers the item and all four keybinds plus the key category, and the block tag lives at
the correct `data/nullstar/tags/block/protected_blocks.json` path. `fabric.mod.json` is well-formed with a
split-entrypoint, client-scoped mixin reference. Documentation is unusually accurate for a Part 1 handoff:
the claimed round-trip of `PROJECT_STATE.md`, `ARCHITECTURE.md`, `WEAPON_SPECIFICATIONS.md`,
`ACCEPTANCE.md` and `HANDOFF_TO_OPUS.md` matches the code in every spot this audit checked, including the
exact phase tables, the 42,025-candidate Helios volume, the 1,536/8,192 candidate figures, the 24/48
radius clamps, and the explicit list of what is *not* built. `QA-12` (evidence provenance) and the
`QA-01` architecture-description mismatch are the exceptions.

---

## 3. Verified-working subsystems

| Subsystem | Why it is trusted |
| --- | --- |
| Build from committed sources | `./gradlew clean build` exit 0, `BUILD SUCCESSFUL in 32s`, 14 tasks executed from a wiped output dir |
| JUnit suite | `clean build` re-ran `:test`: **19 tests, 0 failures, 0 errors, 0 skipped** (11 + 5 + 3), XML regenerated |
| Dedicated-server initialization | `runGameTest` booted a real 26.3 server with the mod: `5 tests are now running` → `All 5 required tests passed :)` |
| Real graphical client loop | `./gradlew runClientGameTest` exit 0, `BUILD SUCCESSFUL in 2m 21s`; real terrain deltas 10200 / 6602 / 5578, preview 0, cancellations 0, reopened-world 10196, 14 screenshots regenerated, no NULLSTAR exception |
| Release artifact | Rebuilt JAR is byte-identical to the recorded SHA-256; class major 69; 0 packaged test classes |
| Bundled vanilla sound ids | All 14 ids found once each in 26.3 `SoundEvents.class` — no typo in the shipped definitions |
| Server authority / anti-cheat contract | `RequestPayload` has no coordinate or blast fields; every parameter is recomputed server-side |
| Global destruction budget | `DestructionJob.step` counts skipped cells; proven by unit test |
| Bounded, constant-memory destruction | Cursor scan + one queue entry per impact; no position lists; round-robin |
| Chunk-loading safety | `hasChunk` guard before every terrain read/write; GameTest asserts no chunk is generated |
| Protection stack | Fail-closed hooks + tag + block entities + `mayInteract` + border/build limits; GameTest asserts bedrock/chest/veto survival |
| Terrain persistence | Real 30-deep crater reproduced after save/reopen |
| Preview is inert | `preview` gates terrain, damage and attraction; log shows `0 changed blocks` |
| Cancellation | Stops future work on command, dimension change and disconnect; log shows `cancelled: 0 changed blocks` |
| Timeline correctness | Monotonic, exactly-once cues across delayed ticks, cannot rewind; unit-tested |
| Wire format | `_v1` channels, bounded strings, validated records, round-trip + malformed-rejection tests |
| Camera seam | Injection point and frame-local restoration verified against actual 26.3 `Camera`/`GameRenderer` source |
| Environment separation | Zero client imports in `src/main`; split entrypoints; client-scoped mixin config |
| Accessibility plumbing | `reducedMotion`, `reducedFlashing` (default on), `particleBudget` incl. 0, rebindable keys |

## 4. Understood but *non-defect* boundaries

Recorded so the next owner does not treat them as regressions: vanilla-only particle art and vanilla
sound IDs (Part 2 scope); no custom audio assets; no authored meshes/shaders; Glassfall trajectories are
synchronised visual cues rather than entity physics; Singularity affects living entities only; destruction
suppresses neighbour-shape propagation and loot by design; cancellation is not rollback; jobs and
cooldowns do not persist across restart; the 4 ms deadline is cooperative; third-party claim bridges are
intentionally un-bundled; and entity-proximity query cost is not independently capped.

## 5. Architectural contracts that are *stable enough* for Part 2, with caveats

| Contract | Stability | Caveat for Part 2 |
| --- | --- | --- |
| `WeaponDefinition` record + JSON schema | Stable | Adding a weapon still requires a code change — `WeaponDefinition` hard-codes the three behaviour ids and `WeaponCatalog.IDS` hard-codes the three ids |
| `Phase` enum | Stable | Adding a phase changes `StatePayload` semantics only if you reuse an existing slot; new fields need a `state_v2` channel |
| `state_v1` / `visual_v1` | Stable | Any new field must be a new channel version; do not extend the record in place |
| `PresentationLayers.Layer` | Stable seam | `register` has no unregister — fine for static Part 2 layers |
| Bundled weapon JSON + `SoundEvents` ids | Stable | All 14 sound ids used by the shipped JSONs resolve in 26.3; an operator *override* with a typo'd id silently produces no sound (QA-08) |
| Build reproducibility | Stable on this machine/JDK | `clean build` reproduces the recorded JAR hash exactly; cross-machine reproducibility was not tested |
| `CameraPreset.Context/Frame` | Stable seam | Presets receive `ClientStrikeState.Strike`, so richer choreography that needs something not in `StatePayload` will first require a `state_v2` field |
| `ProtectionHooks.Protection` | Stable | Callers must register before admitting player strikes |
| `ProtectionHooks` call sites | Stable | `allows` is invoked per candidate block and per entity; a heavy adapter will throttle the destruction budget |
| `DestructionManager.reserve/enqueue/tick/progress/cancel` | Stable | The reservation invariant (reserve every impact before launch) is load-bearing and untested at unit level |
| Camera Mixin seam | Stable but fragile | It anchors on a *bytecode instruction* (`INVOKE getViewRotationMatrix`) inside `Camera.update`. Any 26.3.x patch that changes `update`'s body breaks it; `defaultRequire: 1` makes that a load-time failure, which is the safe direction, but Part 2 must know this is the one version-brittle anchor in the mod |
