# NULLSTAR: Cataclysm Engine

An original Minecraft **26.3** Fabric mod with three server-authoritative orbital weapons, persistent terrain deformation, bounded destruction queues, native particle effects, cinematic camera foundations and operator controls. This is the functional Part 1 foundation; the final models, shaders and sound design belong to Part 2.

## Install

Use Minecraft 26.3, Java 25, Fabric Loader 0.19.5 or compatible, and Fabric API 0.162.0+26.3. Install NULLSTAR on both client and server. Kotlin is not required.

Copy **`build/libs/nullstar-1.0.0.jar`** into the instance's `mods` directory alongside the Fabric API JAR. Do not install the `-sources.jar` or the development test mod. For this machine the Prism destination is:

```text
C:\Users\Shadow\AppData\Roaming\PrismLauncher\instances\26.3\minecraft\mods
```

The release remains in this repository; the build/tests do not modify Prism's instance or its saves. Use a disposable creative world for destructive testing.

## Play

Enable cheats/operator permissions, then run `/nullstar give`. The controller is also in **Tools & Utilities** in creative inventory.

- **Sneak + use** or **V**: choose Helios, Singularity or Glassfall.
- **Use** or **G**: designate the solid surface under your aim, up to the weapon's range.
- **Use/G again**: authorize the confirmed strike. Move clear of the target in survival.
- **K**: skip your cinematic view while the strike continues.
- **C**: toggle cinematic camera effects. Keybinds are editable in Minecraft's Controls settings.

The HUD displays selection, targeting coordinates, phase/progress, terrain progress and cooldown. Designations expire after ten seconds. One strike per owner is allowed at a time. The camera does not teleport the player or take away movement controls.

## Commands

```text
/nullstar give
/nullstar target
/nullstar fire helios
/nullstar fire singularity <x> <y> <z>
/nullstar fire glassfall <x> <y> <z>
/nullstar preview helios [<x> <y> <z>]
/nullstar status
/nullstar cancel
/nullstar reload
```

`give`, `fire` and `reload` require game-master/operator permissions. Preview is non-destructive: no damage, attraction or block changes. Explicit coordinates still obey distance, loaded-ground, border and protection checks. Cancel stops future work; previously changed terrain remains. Reload requires all strikes to have finished or been cancelled.

## Configuration

Settings are created on first launch in `config/nullstar/server.json` and `config/nullstar/client.json`. Operator weapon overrides can be placed in `config/nullstar/weapons/helios.json`, `singularity.json` and `glassfall.json`, using the bundled files under `src/main/resources/data/nullstar/weapons` as examples.

Server defaults: terrain destruction on, operators only, radius/depth caps 24, 1,536 candidate operations globally per tick, four concurrent strikes, 16 outstanding/reserved impact jobs, normal duration/cooldowns/sound volume, 64 living entities processed per interaction. Absolute caps are radius/depth 48 and 8,192 candidate operations/tick. Values are validated/clamped. Setting `terrainDestruction` false makes all strikes cinematic-only.

Client defaults: cinematic camera enabled, reduced motion off, reduced flashing on, particle budget 192/tick, shake intensity 0.35. Reduced motion disables camera motion/shake; reduced flashing disables the optional impact screen wash/bright explosion accent. Particle budget can be zero. Camera toggle preference is saved; other client settings are read at startup.

Destruction skips block entities, unbreakable/protected blocks, unloaded chunks, build limits and borders. Vanilla interaction protection is respected. Claim integrations must register `ProtectionHooks`; specific third-party claim bridges are not bundled.

## Build and verify

Use a Java 25 JDK, then:

```powershell
# Local JDK tested in this workspace; use your own Java 25 path elsewhere.
$env:JAVA_HOME = 'C:\Users\Shadow\.gradle\jdks\eclipse_adoptium-25-amd64-windows.2'
.\gradlew.bat clean build
.\gradlew.bat runClientGameTest
.\gradlew.bat runClient
.\gradlew.bat runServer
```

On other systems use `./gradlew` and set `JAVA_HOME` appropriately. Build runs JUnit and dedicated server GameTests. Client GameTests create disposable worlds, activate the weapons, capture screenshots, verify restored view and save/reopen behavior. Loom's test configuration enables Minecraft's EULA for these automated test servers; standard development `runServer` uses its own `run/eula.txt`.

Generate sources separately with `./gradlew genSources`, then build in a second command. Running source generation and compilation in one invocation triggers a Loom/Gradle generated-JAR dependency validation error.

See [current project state](docs/PROJECT_STATE.md), [acceptance evidence](docs/ACCEPTANCE.md), [architecture](docs/ARCHITECTURE.md), and [the Part 2 handoff](docs/HANDOFF_TO_OPUS.md). Logs and evidence distinguish automated gameplay verification from artistic or human audio review.
