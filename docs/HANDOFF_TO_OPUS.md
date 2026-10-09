> Historical Part 1.5B handoff. Current state: see `PART2_RELEASE_REPORT.md` and `PROJECT_STATE.md`.

# Part 2 handoff to Claude Opus 5.5

NULLSTAR Part 1.5B is a remediated Java 25 / Fabric 26.3 foundation with three playable server-owned weapon loops and native visuals. Engineering decision: **CONDITIONAL PASS**. No confirmed critical/high defect remains; independent multi-client deployment, dense-world timing and third-party integrations remain unverified. Expand the art and presentation while retaining the validated engine contracts.

## Build and evidence

Validated production repair commit: `f2f8f563147db6425f7095e13b43290b3bb02838`. Final tested engineering revision: `554df5378f2fd094407be18f384e164838639b14` (diagnostic/network fixture expansion). Final handoff revision: Git tag `codex/part-1.5b-complete`; resolve its exact commit with `git rev-parse codex/part-1.5b-complete`. It includes the subsequent documentation/evidence commit. Recovery branch `codex/pre-remediation-20261009` preserves the original `0e07887` baseline; `7c7cf71` preserves the original four audit reports. `SOL_QA_REMEDIATION.md` supersedes their old fix priorities without rewriting the historical audit.

Tested versions: Minecraft **26.3**, Temurin **25.0.4+7**, Fabric Loader **0.19.5**, Fabric API **0.162.0+26.3**, Loom **1.18.3**, Gradle **9.7.1**, JUnit **5.11.4**. Dependency versions are pinned; wrapper distribution has SHA-256 verification. Kotlin was removed after replacing the unused template stubs with Java.

Production artifact: `build/libs/nullstar-1.0.0.jar`. Source artifact: `build/libs/nullstar-1.0.0-sources.jar`. Install only the production artifact plus Fabric API into a 26.3 Fabric instance; the separate `nullstar-test` mod is not packaged. The JAR metadata/entrypoints and absence of test classes were checked.

`docs/VALIDATION.md` records final commands, test counts and artifact hash. `docs/evidence` contains logs and representative client screenshots. These are actual automated graphical gameplay runs, not mockups or offscreen simulations. Human audio quality, final artistic polish, a multi-client production deployment and third-party compatibility are not approved by those tests.

Final release checks: 27 JUnit tests, seven dedicated GameTests and the expanded graphical scenario pass. All three weapons complete; preview and disabled terrain remain inert; save/reopen persists the crater. Embedded two-owner tests cover authorization, replay, cooldown and event fault isolation, and real-world tests cover PvP/owner attribution. Embedded players are not independent graphical clients.

## Remediation contracts and remaining observations

- `WeaponCatalog(Path,double)` now stores scaled, validated definitions; activation does not scale again. The original one-argument constructor remains multiplier 1. Scaled stage lengths cap at 2400 ticks; cues map proportionally within each actual destination stage. `all()` cannot remove catalog entries. Operator defaults/overrides remain classpath/config based, not data-pack reloadable.
- Preview now obeys `operatorsOnly` and intentionally retains shared sound/visual cues, bounded event slots and cooldowns. It never creates destructive work. Do not reopen a permission bypass for preview effects.
- Blast damage attributes the online owner with both explosion source fields. Other-player damage and forces require PvP plus vanilla team permission. Missing owners cannot affect players; self-forces remain allowed, self-damage follows vanilla attributed-explosion rules, and mob interactions remain allowed.
- Runtime event failures cancel that event and release its pending work while other strikes continue. Terrain step failure accounting still charges scanned candidates. Reservations reject repeated IDs and overflow-sized admissions.
- Offhand controller UI matches server acceptance. Transient level resets preserve the authoritative cooldown clock; actual disconnect clears it. Unsupported broadcasts fail closed, and join snapshots obey observer range.
- `CinematicCamera` retains its public Minecraft sampler and preset APIs, with an additional explicit frame-input sampler for deterministic tests. The Mixin seam and all v1 wire schemas are unchanged.

There are no reproduced unresolved engineering defects. A real client fixture also verifies missing/malformed sound diagnostics, zero-count cancellation feedback, rejected unsupported broadcasts and filtered distant non-owner join snapshots. These synthetic boundary tests do not replace the broader independent multi-client, survival-death, compatibility and performance acceptance below.

## Exact commands

```powershell
Set-Location 'C:\Users\Shadow\Desktop\NULLSTAR'
$env:JAVA_HOME = 'C:\Users\Shadow\.gradle\jdks\eclipse_adoptium-25-amd64-windows.2'
.\gradlew.bat clean build
.\gradlew.bat runClientGameTest
pwsh -File scripts/verify-artifact.ps1
git rev-parse codex/part-1.5b-complete
.\gradlew.bat runClient
.\gradlew.bat runServer
# Generate API sources in a separate invocation:
.\gradlew.bat genSources
```

Use `./gradlew` and another Java 25 JDK path on other machines. Automated tests use `build/run/gameTest` and `build/run/clientGameTest`; development runs use `run`. Do not direct test launchers at the user's Prism saves. `genSources` must be separate from build/compile because Loom rewrites a generated Minecraft JAR, which otherwise trips Gradle implicit-dependency validation.

The Prism client code is at `C:\Users\Shadow\AppData\Roaming\PrismLauncher\libraries\com\mojang\minecraft\26.3\minecraft-26.3-client.jar`. Generated split common/client sources are under `.gradle/loom-cache/minecraftMaven`. The instance's `minecraft` folder contains saves/options/mods, not the source JAR. No Prism data was modified by this phase.

## Source map and stable public contracts

| Area | Files/contracts |
| --- | --- |
| Boot/registration | `Nullstar`, `NullstarClient`, `WeaponControllerItem`, `NullstarCommands` |
| Definition/state | `core/WeaponDefinition`, `WeaponCatalog`, `Phase`, `Timeline`, `ActiveStrike` |
| Authoritative gameplay | `server/WeaponEngine.activate`, `TargetingService`, `LoadedBlockView`, `ActivationGate` |
| Terrain | `CraterGeometry`, `DestructionJob.step/Visitor`, `DestructionManager.reserve/enqueue/tick/progress/cancel`, `BlockTransformations` |
| Protection/entities | `ProtectionHooks.register/Protection`, `EntityEffects` |
| Network | `network/*Payload`, `NullstarNetworking`; `_v1` channels and validated field ownership in `ARCHITECTURE.md` |
| Presentation | `ClientStrikeState`, `EffectRenderer`, `PresentationLayers.Layer`, `NullstarHud`, `SoundScheduler` |
| Camera | `CameraPreset.Context/Frame`, `CinematicCamera.register/sample`, client-only `CameraMixin` |
| Configuration | `ServerConfig`, `ClientConfig`, `ConfigFiles`; `config/nullstar` paths |
| Tests | `src/test/java`, `src/gametest/java`; production artifact excludes test source set |

Keep server authority and candidate-budget semantics stable. Preserve packet v1 schemas or introduce new channel versions when adding fields. The camera preset and presentation layer interfaces are the intended expansion seams. Definition IDs and behavior dispatch currently allow exactly the three shipped weapons; extend the catalog/behavior allowlist intentionally when adding weapons.

## Destruction and lifecycle limits

Defaults: maximum radius/depth 24, 1,536 **candidates**, four active strikes and 16 pending/reserved jobs. Absolute clamps: radius/depth 48, 8,192 candidates/tick, eight active events and 32 jobs. One event per owner; admission reserves every scheduled impact before launch. A Glassfall barrage reserves five slots. Geometry/queue code never materializes giant position lists.

The manager processes round-robin slices of 64 under one global tick budget plus a cooperative four-millisecond deadline. The deadline is not a measured hard real-time guarantee. Unloaded cells are skipped and target unloading cancels remaining work. Containers, unbreakable/tagged blocks, vanilla interaction protection and registered claim vetoes are respected. No arbitrary chunk force-loading is permitted. Changed chunks are persistent; jobs/cooldowns are not serialized across restart. Cancellation is not terrain rollback.

Living-entity interaction count is bounded; dense vanilla query enumeration remains a performance consideration. Block updates suppress neighbor-shape propagation and loot to avoid uncontrolled cascades. Keep that property if adding fluids, physics or debris. Add independent bounded queues for any expensive new server effects. Unit reservation/camera tests and the dedicated injected-failure fixture protect these extension seams; retain them when extending event behavior.

## Most valuable Part 2 work

1. Replace the primitive orbital sources, anomaly and projectile trails with original authored meshes and render-state layers, retaining the tested timeline and target coordinates.
2. Build clear Helios source/beam/impact art first. The basic camera now frames the charging source and follows the descending beam; preserve visibility while expanding cinematic scale.
3. Add native 26.3 GPU pipelines and opt-in atmosphere/lensing, using the actual RenderPearl/extraction APIs. Do not resurrect GLFW/immediate OpenGL tutorials.
4. Author new sound assets and coordinate them with server cues. Native phase sounds already bypass vanilla player-distance packet filtering so orbital camera listeners receive them.
5. Refine camera scripts via `CameraPreset` rather than weapon-specific Mixin branches. Keep frame-local state restoration, held-item suppression and all accessibility controls.
6. Improve controller/HUD/settings art and optional config UX, while retaining native key rebinding and server-controlled activation.
7. Add exact-version claim bridges if the deployment needs them, and validate concurrent multi-client strikes and dense-world performance.

## Known limits and remaining visual verification

No final orbital machinery, shader effects, custom audio, physical debris, realistic fluid aftermath or optical lensing is present. Glassfall trajectories are synchronized visual cues, not physics projectile entities. Singularity affects living entities, not arbitrary block/item debris. These are intentional Part 2 content/art boundaries.

Manual final-art/audio review, multiple independent clients, third-party render/shader/claim mods, survival-owner death, and worst-case dense-world frame/tick performance remain unverified. Reduced-flashing is enabled by default; the optional brighter impact wash is implemented but not artistically approved. Native Windows performance-counter diagnostics and development Realms placeholder-auth warnings appear without failing tests. The passing dedicated log intentionally contains one injected `Timeline cannot rewind` exception, followed by isolated cancellation and a surviving completed strike; the graphical log contains no NULLSTAR exception.

Read `PROJECT_STATE.md`, `WEAPON_SPECIFICATIONS.md`, `ACCEPTANCE.md` and `RESEARCH.md` before expanding scope. Keep evidence accurate when replacing the current basic visuals.
