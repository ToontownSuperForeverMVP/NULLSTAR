# API and reference research

Inspected on October 9, 2026. Local dependencies and generated 26.3 source/bytecode were the final authority for implementation.

- [Fabric 26.3 example branch](https://github.com/FabricMC/fabric-example-mod/tree/26.3): fetched its build/properties files through GitHub's raw endpoint and compared them with the supplied template. The template already targets 26.3; its resolved Loom snapshot was 1.18.3 and is now pinned. Its API version 0.162.0+26.3 is preserved.
- [September 15 Fabric migration article](https://www.fabricmc.net/2026/09/15/263.html): reviewed API removals, reloadable registries and SDL migration. Input uses vanilla `InputConstants` and current `KeyMappingHelper`, with no GLFW dependency. Current permissions, creative tabs, GUI extraction and camera APIs were verified locally rather than inferred from old tutorials.
- [Fabric networking documentation](https://docs.fabricmc.net/develop/networking): reviewed typed payload registration and server validation. The web documentation labels itself 26.2; all production signatures were checked against the installed 26.3 API JARs.
- [Fabric testing documentation](https://docs.fabricmc.net/develop/automatic-testing): reviewed Loom's separate GameTest source set and client context. Actual 26.3 module sources/Javadocs were inspected, and dedicated plus real-client tests were run. Pure algorithms use JUnit without requiring Minecraft bootstrap; wire tests exercise the actual current codecs.

The user's Prism instance stores saves/options under `instances/26.3/minecraft`, but the unobfuscated client is in `PrismLauncher/libraries/com/mojang/minecraft/26.3/minecraft-26.3-client.jar`. Loader metadata confirms 0.19.5. Loom generates split sources in the project's `.gradle/loom-cache/minecraftMaven`. Java 25.0.4 was found in the local Gradle JDK cache. No Prism save was opened or modified during tests.

## Public inspiration only

- [The Shooting Star demo](https://www.curseforge.com/minecraft/mc-mods/the-shooting-star-demo) documents remote targeting, distant weapon reveals and large destructive strikes. General inspiration: make the charge/impact progression readable; NULLSTAR uses its own small orbital reveal, identities and capped craters.
- [Orbital Railgun Reforged](https://www.curseforge.com/minecraft/mc-mods/orbital-railgun-reforged) documents aimed activation, configuration and claim support. General inspiration: pair spectacular effects with explicit authorization/protection extension points. NULLSTAR does not claim an implemented FTBChunks/OPAC bridge.
- [Celestial Dragon Strike](https://www.curseforge.com/minecraft/mc-mods/celestial-dragon-strike-v1-0-0) describes staged visual spectacle and lasting terrain deformation. General inspiration: a distinct beginning, attack and aftermath. Its dragon/gate/sky sequences are not reproduced.
- [Orbital Bowcanon](https://www.curseforge.com/minecraft/mc-mods/orbital-bowcanon) describes descending projectiles and impact effects. General inspiration: readable trajectories and staggered impacts, implemented here as original crystalline barrage timing.
- [Zeus Cannon](https://www.curseforge.com/minecraft/mc-mods/zeus-cannon) could not be retrieved through the available web fetch/search. No undocumented behavior or technical architecture was assumed from its name.

Only public feature descriptions were consulted. No reference-mod binaries, source, textures or sound files were downloaded or incorporated.
