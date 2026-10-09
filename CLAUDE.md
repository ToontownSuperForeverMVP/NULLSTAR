# NULLSTAR: Cataclysm Engine

This repository contains the Fabric 26.3 NULLSTAR mod: the preserved Part 2 presentation release plus the Part 2.5A colossal engineering expansion. Start with `AGENTS.md`, `docs/PROJECT_STATE.md`, `docs/PART25_ENGINEERING_REPORT.md`, `docs/ARCHITECTURE.md` and `docs/HANDOFF_TO_OPUS_PART25.md`. The Part 2 release report and old handoff remain historical.

Part 2 added the custom RenderPearl effects engine, authored weapon scenes, directed camera shots, original synthesized audio, HUD and settings while preserving server authority, safe destruction budgets and the three playable weapon loops. Part 2.5A adds validated colossal profiles, long timelines, staged terrain jobs, restart recovery and normalized cosmic scene hooks. Vanilla particles remain the LOW/fallback layer.

Build with Java 25: `./gradlew clean build`. Launch development client: `./gradlew runClient`. Automated graphical test: `./gradlew runClientGameTest`. Generate API sources separately: `./gradlew genSources`.

Never import client classes into common code. Do not replace 26.3 APIs with old Yarn/GLFW tutorials. Extend `PresentationLayers`, `CameraPreset`, immutable weapon JSON, and `ProtectionHooks` instead of duplicating the engine.
