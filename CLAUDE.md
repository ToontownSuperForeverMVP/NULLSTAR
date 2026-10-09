# NULLSTAR: Cataclysm Engine

This repository contains Part 1's functional Fabric 26.3 foundation. Start with `AGENTS.md`, then `docs/HANDOFF_TO_OPUS.md`, `docs/PROJECT_STATE.md`, and `docs/ARCHITECTURE.md`.

Part 2 should improve visual direction, world rendering, camera scripts, authored sounds and additional content while preserving server authority, safe destruction budgets and the three playable weapon loops. The current visuals deliberately use vanilla particles and sounds; they are an implementation foundation, not final cinematic art.

Build with Java 25: `./gradlew clean build`. Launch development client: `./gradlew runClient`. Automated graphical test: `./gradlew runClientGameTest`. Generate API sources separately: `./gradlew genSources`.

Never import client classes into common code. Do not replace 26.3 APIs with old Yarn/GLFW tutorials. Extend `PresentationLayers`, `CameraPreset`, immutable weapon JSON, and `ProtectionHooks` instead of duplicating the engine.
