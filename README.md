# NULLSTAR: Cataclysm Engine

An original Minecraft **26.3** Fabric mod with three server-authoritative orbital weapons, persistent terrain deformation and bounded destruction queues, presented with a custom RenderPearl effects engine: an asymmetric golden orbital collector and layered stellar beam (Helios), a lensed gravitational anomaly that implodes (Singularity) and a crystalline orbital lattice firing a synchronized barrage (Glassfall). Directed camera shots, original procedural textures, synthesized sound design, a holographic HUD, quality presets and accessibility options are included. See `docs/PART2_RELEASE_REPORT.md` for what is verified and what still needs human review.

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
- **C**: toggle cinematic camera effects.
- **N**: open NULLSTAR visual settings (quality, particles, camera, shake, reduced motion/flashing, distortion, glow, cinematic audio, letterbox).

Keybinds are editable in Minecraft's Controls settings.

The HUD displays the weapon icon and name, an animated targeting reticle with lock and range state, a segmented phase timeline, terrain progress, recharge and abort/complete banners. A ground reticle shows the selected weapon's effective radius. Designations expire after ten seconds. One strike per owner is allowed at a time. The camera does not teleport the player or take away movement controls.

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

`give`, `fire` and `reload` require game-master/operator permissions. Preview obeys the server's `operatorsOnly` activation setting (operators required by default). It emits sound/visual cues and uses a strike slot/cooldown, with no damage, attraction or block changes. Explicit coordinates still obey distance, loaded-ground, border and protection checks. Cancel stops future work; previously changed terrain remains. Reload requires all strikes to have finished or been cancelled.

## Configuration

Settings are created on first launch in `config/nullstar/server.json` and `config/nullstar/client.json`. Operator weapon overrides can be placed in `config/nullstar/weapons/helios.json`, `singularity.json` and `glassfall.json`, using the bundled files under `src/main/resources/data/nullstar/weapons` as examples.

Bundled definitions are classpath defaults, not data-pack overrides. Unknown override filenames/sound IDs warn in the log. Duration scaling occurs once at load; scaled stages cap at 2400 ticks and cues retain their proportional positions within those stages. Player damage/forces obey PvP and team protection, with blast damage attributed to the owner.

Server defaults: terrain destruction on, operators only, radius/depth caps 24, 1,536 candidate operations globally per tick, four concurrent strikes, 16 outstanding/reserved impact jobs, normal duration/cooldowns/sound volume, 64 living entities processed per interaction. Absolute caps are radius/depth 48 and 8,192 candidate operations/tick. Values are validated/clamped. Setting `terrainDestruction` false makes all strikes cinematic-only.

Client defaults: quality MEDIUM, particle intensity 100%, cinematic camera enabled, shake 0.35, reduced motion off, reduced flashing **on**, screen distortion on, glow 100%, cinematic audio 100%, letterbox on, native particle budget 192/tick (used by the LOW preset and the fallback). Reduced motion keeps the player's own view while all effects still play; reduced flashing replaces bright flashes with a soft tint; particle intensity 0 removes cosmetic particles and debris. Settings save from the N screen or the camera toggle; none of them reach the server or change destruction limits.

Destruction skips block entities, unbreakable/protected blocks, unloaded chunks, build limits and borders. Vanilla interaction protection is respected. Claim integrations must register `ProtectionHooks`; specific third-party claim bridges are not bundled.

## Build and verify

Use a Java 25 JDK, then:

```powershell
# Local JDK tested in this workspace; use your own Java 25 path elsewhere.
$env:JAVA_HOME = 'C:\Users\Shadow\.gradle\jdks\eclipse_adoptium-25-amd64-windows.2'
.\gradlew.bat clean build
.\gradlew.bat runClientGameTest
pwsh -File scripts/verify-artifact.ps1
.\gradlew.bat runClient
.\gradlew.bat runServer
```

On other systems use `./gradlew` and set `JAVA_HOME` appropriately. Build runs JUnit and dedicated server GameTests. Client GameTests create disposable worlds and run three suites: the regression scenario (all weapons, restored view, preview, cancellation, dimension change, save/reopen), a real dedicated-server connection (networked strike, disconnect cancellation, clean reconnect) and visual capture (authored story beats per weapon, accessibility/quality modes and a four-strike presentation stress test, written to `build/run/clientGameTest/build/evidence/part2`). Select suites with `-Ptests=regression,dedicated,capture` and capture scenes with `-Pcapture=helios,singularity,glassfall,modes,stress`.

Original assets are reproducible: `python -I tools/assets/generate_textures.py`, `python -I tools/assets/generate_audio.py` and `python -I tools/assets/validate_audio.py` (numpy, Pillow, soundfile). Loom's test configuration enables Minecraft's EULA for these automated test servers; standard development `runServer` uses its own `run/eula.txt`.

Generate sources separately with `./gradlew genSources`, then build in a second command. Running source generation and compilation in one invocation triggers a Loom/Gradle generated-JAR dependency validation error.

See [current project state](docs/PROJECT_STATE.md), [acceptance evidence](docs/ACCEPTANCE.md), [architecture](docs/ARCHITECTURE.md), and [the Part 2 handoff](docs/HANDOFF_TO_OPUS.md). Logs and evidence distinguish automated gameplay verification from artistic or human audio review.
