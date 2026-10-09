# Final Part 1 validation

Validated on October 9, 2026, in this workspace. Implementation commit: `45d64a5e87f9463e0e8b6fc4ea6cedc7f97a4ecb`. Tests ran in disposable Gradle worlds; the user's Prism instance, mods and saves were not modified.

## Environment and final commands

Minecraft 26.3, Temurin 25.0.4+7, Fabric Loader 0.19.5, Fabric API 0.162.0+26.3, Loom 1.18.3, Gradle 9.7.1 and JUnit 5.11.4.

```powershell
Set-Location 'C:\Users\Shadow\Desktop\NULLSTAR'
$env:JAVA_HOME = 'C:\Users\Shadow\.gradle\jdks\eclipse_adoptium-25-amd64-windows.2'
.\gradlew.bat clean build --console=plain
.\gradlew.bat runClientGameTest --console=plain
```

Both final invocations exited successfully. Clean build took 20 seconds; the graphical client test invocation took 2 minutes 9 seconds. These are task durations, not frame-time or server-performance benchmarks. `genSources` was run separately during API investigation and must remain a separate invocation.

## Results and reproducible evidence

| Check | Final result | Evidence |
| --- | --- | --- |
| Unit tests | 19 passed, zero skipped/failures/errors | [EngineTest: 11](evidence/unit-tests/TEST-com.nullstar.core.EngineTest.xml), [SerializationTest: 5](evidence/unit-tests/TEST-com.nullstar.network.SerializationTest.xml), [ClientStateTest: 3](evidence/unit-tests/TEST-com.nullstar.client.ClientStateTest.xml) |
| Dedicated server GameTests | All five required tests passed | [Clean build log](evidence/clean-build.log) |
| Real graphical client GameTest | Full scenario passed | [Client log](evidence/client-test-run.log), `src/gametest/java/com/nullstar/test/StrikeClientGameTest.java` |
| Production artifact | Correct metadata, Java 25, no test classes | [Artifact inspection](evidence/artifact-validation.json) |

The dedicated test count includes four NULLSTAR world tests and one Fabric framework test. NULLSTAR tests verify global candidate-budget enforcement, actual block mutation, protected bedrock/chest/claim-veto preservation, cancellation, living-entity damage/knockback/gravity, and collision targeting without generating unloaded chunks.

Pure tests cover seeded geometry and radial falloff, monotonic phases and one-time/scaled timeline cues, resumable/cancelled queue progress, configuration clamps and definition validation, duplicate/replayed activation and cooldown rejection, finite/range targeting, typed packet round trips and malformed input, and client snapshot age/dimension/terminal cleanup.

The graphical scenario enters a disposable world, grants the registered controller, uses the item to designate a real block surface, and activates via the native key mapping. It checks all three weapon loops, resulting terrain, preview with zero mutations, explicit cancellation, camera skip, camera entity/type/position restoration, dimension-transition cancellation and crater persistence after saving and reopening the world. It uses a native stone fixture and test cheats to exercise the default operator permission gate. Production authorization was not weakened for testing.

## Screenshots inspected

These images came from the final Minecraft client run. The charging source, descending gold beam, violet anomaly and cyan aftermath were visually inspected. Rendering is the intended basic Part 1 implementation using native particles; it is not final authored Part 2 art.

- [Helios designation](evidence/0000_helios-designation.png)
- [Helios charging source](evidence/0001_helios-charge.png)
- [Helios descending beam](evidence/0003_helios-descending-beam.png)
- [Helios completed crater](evidence/0005_helios-crater.png)
- [Singularity firing](evidence/0007_singularity-firing.png)
- [Singularity crater](evidence/0009_singularity-crater.png)
- [Glassfall aftermath](evidence/0012_glassfall-aftermath.png)
- [Glassfall crater](evidence/0013_glassfall-crater.png)

All fourteen captured images are retained in `docs/evidence`.

## Artifact

Production: `build/libs/nullstar-1.0.0.jar`; sources: `build/libs/nullstar-1.0.0-sources.jar`. Production SHA-256:

```text
31C4567128ABE1C37460DEA923901A476DF9D1AEFD328C4E9133F8210D7A29D2
```

Archive inspection confirmed version 1.0.0, exact Minecraft `=26.3` dependency, `com.nullstar.Nullstar` entrypoint, common/client classes, Java class major 69, and zero packaged test classes. The artifact hash applies to this final build; rebuilding may change archive bytes.

## Evidence boundaries

The passing logs contain native Windows OSHI/Perflib performance-counter diagnostics, development Realms placeholder-auth warnings and a native end-of-frame warning. No NULLSTAR exception remains in these passing runs.

Human sound-quality assessment, final art approval, multiple independent clients, third-party claim/render/shader integrations, survival-owner death and dense real-world performance were not tested. The four-millisecond destruction deadline is cooperative, not a benchmark or hard timing guarantee. Basic native cues and synchronized visual projectiles exist; custom audio, mesh/shader spectacle and physical debris remain Part 2. See [acceptance](ACCEPTANCE.md) and [project state](PROJECT_STATE.md) for precise status.
