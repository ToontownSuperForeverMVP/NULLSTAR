# Mod Menu support

NULLSTAR uses Mod Menu's public `ModMenuApi` / `ConfigScreenFactory` integration, pinned to the Minecraft 26.3 release **21.0.0**. Its `modmenu` entrypoint is `com.nullstar.client.integration.NullstarModMenu`, entirely in the client source set. Mod Menu creates the existing `NullstarSettingsScreen` with its own screen as the parent; Done and Escape save and return to that parent. It also works from the title screen before joining a world. The N key remains available in-game without Mod Menu.

Every client preference is exposed: graphics quality, custom particle intensity, native particle budget, cinematic camera, camera shake, reduced motion, reduced flashing, screen distortion, glow intensity, cinematic audio and letterbox. Reset defaults restores all eleven values, including reduced flashing enabled. Edits apply immediately and are validated and saved to `config/nullstar/client.json` when closing. Keybindings remain in Minecraft's Controls menu.

Server/operator settings and weapon JSON remain server controlled. No client setting is sent in a payload or changes authorization, destruction, damage, cooldowns or reservations. No shader, camera Mixin, server code or packet schema changed.

Mod Menu is suggested in production metadata, compile-only for client code, and included only on the development client's runtime classpath. It is not embedded in NULLSTAR or required on a server. `-PwithoutModMenu` removes it from development runs. The mod's name, summary and description use Mod Menu's documented translation keys.

## Verification commands

```powershell
$env:JAVA_HOME = 'C:\Users\Shadow\.gradle\jdks\eclipse_adoptium-25-amd64-windows.2'
.\gradlew.bat runClientGameTest '-Ptests=settings,regression,dedicated'
.\gradlew.bat runClientGameTest -Ptests=settings -PwithoutModMenu
.\gradlew.bat clean build
pwsh -File scripts/verify-artifact.ps1 -Output docs/evidence/modmenu/artifact-validation.json
```

The settings fixture resolves NULLSTAR through Mod Menu's registered factory when installed, checks the exposed controls and screen bounds, changes camera and native particle preferences, reads the saved JSON, verifies parent navigation, resets every preference and captures the real screen. The standalone path instantiates the same screen without loading any Mod Menu class. All worlds and configuration belong to disposable Gradle runs under `build/run`, never Prism.

## Observed client results — October 9, 2026

- Mod Menu 21.0.0 installed: registered factory, exposure of all eleven preferences, camera and native budget edits, JSON persistence, reset and parent navigation passed. The final settings-only run also explicitly verified Escape.
- Mod Menu absent: the same standalone settings, persistence, reset and Escape checks passed; client startup did not load the optional integration class.
- Existing regression and dedicated-server client suites passed with Mod Menu installed: three weapon loops, preview/cancellation, camera restoration, dimension transition, save/reopen, real server connection and disconnect/reconnect.
- [Mod Menu settings capture](evidence/modmenu/modmenu-settings.png) and [standalone settings capture](evidence/modmenu/standalone-settings.png) are real client frames. The former was inspected for layout and readable controls at the test client's GUI scale.

Logs are in `docs/evidence/modmenu`: `client-regression.log`, `settings-client.log` and `standalone-client.log`. The fixture exercises the registered factory directly rather than automating Mod Menu's mod-list search and Configure click; human usability/accessibility review and other third-party mod combinations remain separate from these automated observations. No new art/audio approval is claimed.

Final `clean build` passed 61 JUnit tests and 10 dedicated GameTests (`clean-build.log`). `artifact-validation.json` confirms the optional integration class/entrypoint, Java 25 bytecode, Minecraft 26.3 metadata and absence of bundled Mod Menu or test classes. Production JAR SHA-256: `AFD48DCEBEC6F80DD07531873BF0E3E069EDE4508A54250FC45186CDF72751EC`.

API reference: [TerraformersMC Mod Menu developer documentation](https://github.com/TerraformersMC/ModMenu/tree/26.3#developers). Downloads: [Modrinth](https://modrinth.com/mod/modmenu), [CurseForge](https://www.curseforge.com/minecraft/mc-mods/modmenu).
