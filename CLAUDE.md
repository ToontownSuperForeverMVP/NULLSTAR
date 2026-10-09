# NULLSTAR: Cataclysm Engine

This repository contains the Fabric 26.3 NULLSTAR mod: the Part 1 engineering foundation plus the Part 2 presentation release. Start with `AGENTS.md`, then `docs/PART2_RELEASE_REPORT.md`, `docs/PROJECT_STATE.md` and `docs/ARCHITECTURE.md`; `docs/HANDOFF_TO_OPUS.md` is historical.

Part 2 added the custom RenderPearl effects engine, authored weapon scenes, directed camera shots, original synthesized audio, HUD and settings while preserving server authority, safe destruction budgets and the three playable weapon loops. Read `docs/PART2_RELEASE_REPORT.md` for the current state; vanilla particles remain only as the LOW/fallback layer.

Build with Java 25: `./gradlew clean build`. Launch development client: `./gradlew runClient`. Automated graphical test: `./gradlew runClientGameTest`. Generate API sources separately: `./gradlew genSources`.

Never import client classes into common code. Do not replace 26.3 APIs with old Yarn/GLFW tutorials. Extend `PresentationLayers`, `CameraPreset`, immutable weapon JSON, and `ProtectionHooks` instead of duplicating the engine.
