# Project state — Part 1

The functional engineering foundation is implemented. This is a playable development release with basic native visuals, not the final artistic release.

## Implemented

- Java 25 Fabric 26.3 project; pinned build/plugin versions, Gradle wrapper with distribution checksum, split client/common source sets, production and sources JARs.
- Main/client initialization; creative controller item/model/translation; native SDL-aware key mappings; targeting preview/designation, selection, phase/progress/cooldown HUD and operator commands.
- Validated immutable JSON weapon definitions, operator overrides, server and client configuration, duration scaling and reload guards.
- Server-owned targeting/authorization, monotonic replay/rate gate, cooldowns, per-owner activation exclusivity, global event limits and future job reservations.
- Deterministic phase timeline/cues, compact typed networking, bounded decoding, late-state resync, client cue deduplication and stale/dimension/disconnect cleanup.
- Resumable destruction with spherical/ellipsoidal/bowl geometry, seeded irregularity, falloff, ground probes, radius/depth/height/border caps, loaded-chunk-only reads, global candidate budget, round-robin scheduling, progress and cancellation.
- Unbreakable/tagged/container protection, vanilla interaction checks, public fail-closed claim veto hooks, bounded living-entity damage/knockback and attraction.
- Helios's full target/charge/alignment/fire/entry/impact/aftermath loop and persistent crater; gold energy source, descending beam, impact/shockwave/smoke and native sound scheduling.
- Singularity's anomaly, attraction, implosion and deep crater; Glassfall's five timed trajectories/impacts and final strike, using the shared engine.
- Reusable client camera presets, look-at/easing/interpolation/FOV/shake, frame-local view override, held-item suppression, skip/toggle, reduced-motion/reduced-flashing support and restoration.
- Non-destructive preview, testing commands, algorithm/wire/client-state tests and dedicated/client GameTest fixtures in a separate undistributed test mod.

## Evidence and boundaries

See `docs/ACCEPTANCE.md` and `docs/VALIDATION.md` for the final recorded checks. JUnit tests cover algorithms, configuration, timing, replay prevention, edge-case range validation, serialization and late/stale client state. Dedicated GameTests exercise real world mutations, protected blocks/containers, cancellation, loaded-block access and living-entity effects.

The real client suite uses a disposable world and verifies controller registration/designation/key activation, all three complete strike loops, terrain changes, camera skip/completion restoration, preview/cancellation, dimension changes, and crater persistence after save/reopen. Representative screenshots are under `docs/evidence`.

## Intentionally reserved for Part 2

Authored orbital machines/projectile meshes, custom GPU effects, atmosphere/lensing, detailed camera scripts, physical debris and custom sound assets are not implemented. Current source/beam/anomaly/projectiles are visible native particles and current sounds are Minecraft sounds. Final artistic quality and human audio approval remain open.

The implementation has public protection hooks, not bundled adapters for specific third-party claim mods. Weapon overrides are configuration files, not a resource-pack/data-pack reload registry; IDs and server behaviors are currently a fixed three-weapon catalog. There is no graphical settings screen; client settings use files and native Controls plus the camera-toggle key.

Jobs safely cancel on shutdown or unloading rather than persisting/resuming mid-job. Already changed terrain is saved normally and survives reopen. Cancellation does not undo terrain. Cooldowns persist only within the server session. Living-entity mutation counts are capped; vanilla's underlying entity query cost is not independently capped. Neighbor shape propagation is suppressed during crater mutation to keep tick work bounded.

Multiplayer with multiple independent clients, third-party renderers/claim mods, survival-owner death during a strike, and large real-world performance/audio quality have not been manually evaluated. Automated client-level changes and stale-state tests cover their implemented cleanup paths, but they are not substitutes for those separate acceptance scenarios.
