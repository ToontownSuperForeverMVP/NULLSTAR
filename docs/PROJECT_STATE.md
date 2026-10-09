# Project state — Part 2.5A

The colossal engineering expansion is implemented on `codex/part25-colossal`, preserving the Part 2 presentation and STANDARD gameplay. TITANIC/APOCALYPSE profiles support all three larger weapons. Apocalypse Helios has a nominal 1,000-diameter/depth-96 basin and a 100-second sequence with normalized planet/solar-system/galaxy/cosmic-boundary/mothership checkpoints. Chunk-aligned candidate queues, staged annular passes, planned-volume progress, strict change limits, public/private safeguards, explicit confirmation, diagnostics, v2 compact state and optional graceful restart recovery are in place.

Physical Apocalypse is opt-in (`apocalypseEnabled`); public caps stay enabled by default. No arbitrary chunks are loaded. The new normalized-scene imagery is a basic engineering foundation; immense final visuals/audio belong to Part 2.5B. Read `PART25_ENGINEERING_REPORT.md` and `HANDOFF_TO_OPUS_PART25.md` for the current architecture and source seams. Current validation/acceptance sections below distinguish controlled runtime evidence from the uncompleted full loaded 1,000×1,000 crater.

## Historical Part 2 baseline

NULLSTAR is a feature-complete three-weapon release candidate: the Part 1.5B server-authoritative engine, unchanged in its guarantees, now drives an original presentation layer (custom 26.3 render pipelines, authored procedural machinery, directed camera shots, synthesized audio, holographic HUD, settings screen, quality presets). Engineering decision for this stage: **PASS with documented open items** (human art/audio approval, multiple independent graphical clients and third-party mod compatibility). See `PART2_RELEASE_REPORT.md` for the release summary and `VALIDATION.md` for exact commands and results.

## Implemented in Part 2

- **Rendering engine:** optional RenderPearl pipelines with original GLSL (additive energy core/rim/flat, translucent smoke, lit hull with emissive seams, heated debris) plus OIT variants; vanilla render-type and native-particle fallbacks; bounded per-material vertex sinks submitted through Fabric's `COLLECT_SUBMITS`; static procedural meshes; strike-level culling; failure latch instead of crashes.
- **Helios:** descending asymmetric collector with choreographed segment/vane/focusing-ring/pod/petal/iris alignment, layered reactor, layered beam with atmospheric entry, fireball/pillar/shockwave/debris impact and a long aftermath.
- **Singularity:** depth-occluding event horizon, Doppler-shaded Keplerian disk, lensed far-side arcs, photon ring, unstable accretion rings, inward ribbons/motes/debris, contraction, silence and a violent implosion with jets.
- **Glassfall:** unfolding lattice with aiming rails, crystals launched on authoritative cues along curves that land exactly on server impact points, varied sub-impacts with dissolving crystal spikes, and a final energy dome.
- **Camera:** fifteen authored shot presets selected by weapon JSON, separate position/look smoothing, roll, rumble, impact shake and FOV punch, terrain clearance, occlusion lift, cave decline; Mixin seam extended only with roll.
- **Audio:** 28 original synthesized OGG sound events registered in common code; per-phase server sounds unchanged in delivery; client accent layers with distance-delayed aftershocks, resync-safe triggers and an instance cap.
- **Interface:** holographic controller HUD, animated reticle, selector, abort/complete banners, letterbox and weapon-tinted screen effects, ground targeting reticle, original controller texture and mod icon, settings screen (key N).
- **Quality/accessibility:** LOW/MEDIUM/ULTRA presets; particle intensity (including zero), glow, distortion, cinematic audio, letterbox; reduced motion and reduced flashing (default on) override quality.
- **Assets:** reproducible generators and validator under `tools/assets`; no third-party art or audio.
- **Tests:** 39 JUnit tests (12 new: sinks, canvas budgets, meshes, particles, phase helpers, config, trajectories, camera roll/decline/clearance), seven dedicated GameTests, and three client suites (regression, real dedicated-server connection, visual capture including modes and stress).

## Unchanged contracts

Server authority, `_v1` wire schemas, destruction budgets/caps, loaded-chunk-only access, protection hooks, preview policy, PvP/attribution rules, cooldown timing and the three validated timelines are untouched. Weapon JSON changed only in camera preset names and sound IDs (same schema and validation). Client effects never mutate blocks, entities or server state.

## Known limits

True screen-space gravitational lensing and heat distortion are geometric illusions, not post-process refraction (no supported dynamic-uniform post-chain seam without further mixins). Debris collides only with the client heightmap. Cinematic shots decline for targets under thick solid ceilings and lift above foliage rather than path-plan around it. Displayed reticle radius uses bundled definitions, so operator overrides may differ visually (the server crater remains authoritative). Multiple independent graphical clients, third-party render/shader/claim mods, survival-owner death in every phase and dense real-world timing were not tested. Final artistic and audio approval requires a human.
