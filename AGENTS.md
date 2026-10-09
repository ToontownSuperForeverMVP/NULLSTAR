# NULLSTAR engineering rules

- Read `docs/PROJECT_STATE.md` and `docs/HANDOFF_TO_OPUS.md` before changing systems.
- Minecraft **26.3 only**, Java 25, Fabric Loader 0.19.5, Fabric API 0.162.0+26.3, pinned Loom 1.18.3. Use unobfuscated names and inspect the generated sources. Never add Yarn mappings or GLFW calls.
- Common/server code belongs in `src/main/java`; Minecraft client imports belong only in `src/client/java`. One client-only camera Mixin is intentional; keep the projection/culling injection seam intact.
- Weapon JSON is validated operator data. Gameplay authorization, raycast, cooldown, destruction radius, entity interaction and job reservations remain server owned.
- Every destruction candidate consumes the **global** tick budget, including air and skipped cells. Do not force-load chunks, retain chunk references, remove containers/unbreakable blocks, or bypass `ProtectionHooks` and vanilla interaction protection.
- Keep payload suffixes and schemas versioned. Never accept client-supplied coordinates or destruction parameters as trusted input.
- Camera changes must be frame-local and restored on skip, cancellation, level changes, disconnect, death and stale state. Respect reduced-motion and reduced-flashing settings.
- Run `./gradlew build` (JUnit + dedicated GameTests) after gameplay changes. Run `./gradlew runClientGameTest` for presentation/input/network changes. `./gradlew clean build` is the release check. Run `genSources` separately from compile/build tasks because Loom rewrites the generated Minecraft JAR.
- Tests use disposable worlds under `build/run`; never point them at Prism's saves. Report automated gameplay observations separately from manual artistic/audio approval.
- Preserve working behavior, make coherent Git commits, and update the handoff/acceptance evidence honestly. The test mod must never be distributed as the production mod.
