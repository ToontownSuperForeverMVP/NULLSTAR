# Project state — Part 2.5B

## Mod Menu integration

Optional Mod Menu 21.0.0 support exposes all eleven client preferences through the existing settings screen, including a new native particle budget control and Reset defaults. The N shortcut remains available without Mod Menu. Integration, installation and validation details are in [MOD_MENU_SUPPORT.md](MOD_MENU_SUPPORT.md). Server authority and operator configuration are unchanged.

The colossal presentation pass is implemented on `codex/part25-colossal`, preserving the Part 2.5A engineering layer and the Part 2 presentation. All three weapons now present at genuinely colossal scale, and colossal Helios plays a full world-to-cosmos cinematic: atmospheric ascent, planet, solar system, galaxy, observable-universe boundary, an enormous star-harvesting mothership that deploys, contains a star and fires a colossal stellar lance back down at the world, followed by a planetary-scale devastation sequence and a long crater aftermath. TITANIC and APOCALYPSE events scale the machinery, the emitter altitude, the lance, the pressure waves and the aftermath, while STANDARD resolves to exactly the Part 2 magnitudes.

Server authority, `_v2` wire schemas, destruction budgets, loaded-only access, protection hooks, preview policy, cooldowns and the three validated timelines are unchanged. The only server-side change is one metadata resolution: `CinematicScene.forPhase(phase, weapon, tier)`, so a colossal Helios holds the detached cosmic act through its charge, alignment and discharge. Audio adds nine cosmic events and four colossal releases. Evidence and exact boundaries are in `VALIDATION.md`, `ACCEPTANCE.md` and `PART25_RELEASE_REPORT.md`; `ARCHITECTURE.md` describes the presentation layer.

## Implemented in Part 2.5B

- **Cosmic act (`CosmicMontageScene`):** fully authored normalized scenes — ascent, planetary body (atmosphere, terminator, cloud bands, night-side glints, debris belt, moon), solar system (star, corona, orbital paths, planets with atmospheres), galaxy (spiral arms, dust lanes, core bulge, polar jets), observable universe (filament web, redshifted boundary shells, emptiness beyond) and a star-harvesting mothership (spine, counter-rotating collar rings, armoured segments, unfolding harvest vanes and shells, targeting pylons, iris aperture, contained star, colossal lance and its contact bloom on the world below). Composition recedes in depth and shrinks per stage so the act keeps travelling outward.
- **Scale mapping (`PresentationScale`):** one logarithmic mapping from the authoritative colossal descriptors to machinery size (1/~2.9/~4.4), emitter altitude (72/~216/~331 blocks), cosmetic beam radius (1.7/~7.2/27 blocks), shock reach, secondary reach and structure count. STANDARD is identity.
- **Helios world presentation:** monumental collector with reactor keel and crown shroud, a layered stellar lance scaled from the beam descriptor, and a planetary-scale impact/aftermath built from the shock reach (layered shock systems with follow-up fronts, pressure wall, debris towers, ash columns, secondary rings, molten pools, ash domes, residual heat).
- **Singularity:** sky-filling anomaly, enormous outer torus and ring systems, infalling streams and torn-up slabs, prolonged attraction, compression, a held-breath pause, violent release and a gravitational sink basin.
- **Glassfall:** monumental bombardment platform (spine, counter-rotating ring stacks, cradles, capacitors, booms, struts), colossal crystalline shells with internal cores, refractive rinds, bow shocks and long trails that still land exactly on the authoritative cues, plus regional shock systems, a crystal-blast finale with a pressure dome and a long aftermath.
- **Materials:** ten dedicated cosmic material slots with disjoint budgets and an order that puts opaque bodies before blended shells, backed by four new original procedural textures. No new GLSL program (the compiled count stays at 18) and no new render-type class of failure: the existing optional/OIT-aware pipelines carry the new textures.
- **Camera:** thirteen new colossal shots plus bounded orbital distances, and the cosmic act reaches through charge/alignment/discharge.
- **Audio:** thirteen new original synthesized events (cosmic act, mothership deployment, stellar charge/fire/invocation, vacuum, release, lattice charge, dome) with a colossal-only client accent table. The Part 2 asset set is byte-identical.
- **Docs:** this file, `VALIDATION.md`, `ACCEPTANCE.md`, `ARCHITECTURE.md`, `WEAPON_SPECIFICATIONS.md`, `PART25_RELEASE_REPORT.md`.

## Unchanged contracts

Server authority, `_v1` request/status and `_v2` state/visual schemas, destruction budgets and caps, loaded-chunk-only access, protection hooks, preview policy, PvP/attribution, cooldown timing and the three validated STANDARD timelines. Client effects never mutate blocks, entities or server state, and no client-supplied value becomes gameplay input. Accessibility (reduced motion, reduced flashing, particle intensity, cinematic camera toggle, skip) is unchanged.

## Known limits

The cosmic act is an authored illusion composed in view-local space in front of the gameplay camera, not an astronomical simulation and not a sky replacement: the world is hidden by an opaque depth-writing backdrop quad, and geometry nearer than that quad (for example terrain directly below the eye) can still appear. The mothership, planet and galaxy are stylized geometry, not a physically simulated environment. Because a colossal crater is far wider than the client's render and simulation distance, colossal camera shots are deliberately bounded and frame the crater with nearby terrain, machine and dust as the scale reference rather than showing the whole 1,000-diameter basin from outside. Complete excavation of an entire loaded 1,000×1,000 region, huge-region completion timing, independent multi-client deployment, third-party mod compatibility and human artistic/audio approval remain unverified.

## Historical Part 2.5A baseline

The colossal engineering expansion implemented scale classes, six validated operator profiles, a 20-stage Helios sequence, chunk-aligned constant-memory terrain traversal, staged wave dependencies, planned-volume progress, change limits, bounded diagnostics, optional graceful restart recovery, v2 spectacle snapshots and normalized scene hooks. See `PART25_ENGINEERING_REPORT.md` and `HANDOFF_TO_OPUS_PART25.md`.

## Historical Part 2 baseline

NULLSTAR is a feature-complete three-weapon release candidate: the Part 1.5B server-authoritative engine, unchanged in its guarantees, now drives an original presentation layer (custom 26.3 render pipelines, authored procedural machinery, directed camera shots, synthesized audio, holographic HUD, settings screen, quality presets). Engineering decision for that stage: **PASS with documented open items** (human art/audio approval, multiple independent graphical clients and third-party mod compatibility). See `PART2_RELEASE_REPORT.md`.

- **Rendering engine:** optional RenderPearl pipelines with original GLSL (additive energy core/rim/flat, translucent smoke, lit hull with emissive seams, heated debris) plus OIT variants; vanilla render-type and native-particle fallbacks; bounded per-material vertex sinks submitted through Fabric's `COLLECT_SUBMITS`; static procedural meshes; strike-level culling; failure latch instead of crashes.
- **Camera:** authored shot presets selected by weapon JSON, separate position/look smoothing, roll, rumble, impact shake and FOV punch, terrain clearance, occlusion lift, cave decline; one client-only Mixin seam.
- **Audio:** originally 28 synthesized OGG sound events registered in common code; client accent layers with distance-delayed aftershocks and an instance cap.
- **Interface:** holographic controller HUD, animated reticle, selector, abort/complete banners, letterbox, weapon-tinted screen effects, ground targeting reticle, settings screen (key N).
- **Quality/accessibility:** LOW/MEDIUM/ULTRA presets; particle intensity (including zero), glow, distortion, cinematic audio, letterbox; reduced motion and reduced flashing (default on) override quality.
- **Assets:** reproducible generators and validator under `tools/assets`; no third-party art or audio.
