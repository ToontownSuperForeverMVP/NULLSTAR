# NULLSTAR Cataclysm Engine — Part 2.5B colossal presentation release report

Part 2.5B is the extreme-scale presentation pass on top of the validated Part 2.5A engineering layer. Records all
three weapons at colossal scale, and turns colossal Helios into a directed world-to-cosmos mini-film that ends in a
stellar lance and a planetary-scale devastation sequence. Server authority, wire schemas, destruction budgets,
loaded-only access, protection hooks, preview policy and cooldowns are unchanged.

## 1. Branch, revision and release state

| Item | Value |
| --- | --- |
| Branch | `codex/part25-colossal` |
| Part 2.5A baseline | `1c8e250` ("enforce Helios ninety-second floor after stage rounding") + docs `77d1da0` |
| Part 2.5B presentation | `6a32540` ("present all three attacks at colossal scale") — the revision every check in this report was run against |
| Part 2.5B documentation/evidence | `4d189a2` ("record the Part 2.5B colossal release and its measurements") plus the follow-up commit that carries this report |
| Part 2 recovery point | `codex/pre-part25-20261009` retains Part 2 `c5ef353`; `codex/part-2.5a-complete` retains 2.5A |
| Final handoff tag | `codex/part-2.5b-complete`, the documentation/evidence commit carrying this report (`git rev-parse codex/part-2.5b-complete`) |
| Versions | Minecraft 26.3, Temurin 25.0.4+7, Fabric Loader 0.19.5, Fabric API 0.162.0+26.3, Loom 1.18.3, Gradle 9.7.1, JUnit 5.11.4 |
| Test machine | AMD EPYC 9354, NVIDIA RTX 2000 Ada (driver 595.79), Windows 11, 854x480 test window, render distance 16 |
| Worlds | disposable only, under `build/run`; no Prism saves, mods or config were touched |

## 2. Colossal visual systems implemented

- **`PresentationScale` (new).** The single client-side mapping from the authoritative `_v2` descriptors to
  world-space presentation magnitudes. `cinematicScale`/`structureScale` pass through `log10/9`, so an astronomical
  descriptor can never be multiplied into raw world vertices. STANDARD is identity.

  | Quantity | STANDARD | TITANIC | APOCALYPSE |
  | --- | --- | --- | --- |
  | Machinery multiplier | 1 | ~2.9 | ~4.4 |
  | Emitter altitude | 72 blocks | ~216 blocks | ~331 blocks |
  | Cosmetic beam radius | 1.7 blocks | ~7.2 blocks | 27 blocks |
  | Shock/debris reach | 60 blocks | 512 blocks | 2,000 blocks |
  | Secondary reach | radius | ~146 blocks | 650 blocks |

- **`CosmicMontageScene` (new, full rewrite of the normalized scene).** The authored cosmic act: atmospheric ascent
  over the world's limb; a planetary body with cloud bands, terminator beads, night-side glints, a debris belt and a
  moon; a solar system with a contained star, corona, precessing orbital paths and tinted planets; a barred-spiral
  galaxy with dust lanes, a core bulge and polar jets (the world is one mote on an arm); an observable-universe
  filament web with redshifted boundary shells and emptiness beyond; and the star mothership — reactor keel,
  counter-rotating armoured collar rings, armoured segments, unfolding star-harvesting vanes with harvest streams,
  curved harvest shells, targeting pylons, an iris aperture opening around a contained star, and the colossal
  layered stellar lance with ejected edge streaks and a contact bloom on the mote of a world below. The composition
  recedes in depth and shrinks per stage, so the act keeps travelling outward.
- **Helios world presentation.** Monumental collector with reactor keel and crown shroud; layered stellar lance scaled
  from the beam descriptor; planetary-scale impact and aftermath rebuilt around the shock reach — contact flash,
  incandescent core, energy pillar, multiple concentric shock systems with re-emanating follow-up fronts, a
  ground-hugging pressure wall, debris towers, towering ash columns, secondary devastation rings, molten pools, ash
  domes, seared outer layers and drifting residual rings.
- **Singularity.** Sky-filling anomaly, an enormous counter-precessing outer torus plus two tilted ring systems,
  infalling matter streams with torn-up slabs and shards, prolonged attraction, a compressing core, a held-breath
  pause, a violent release, and a gravitational sink basin: inward-sloped collapsed shells over a fused floor,
  fracture rings to the secondary reach, release jets, molten residue and a settling dust dome.
- **Glassfall.** A monumental orbital bombardment platform (spine of reactor corridors, counter-rotating ring stacks
  with docking cradles, capacitor banks, aiming booms, tension struts) unfolding as the strike deploys; colossal
  crystalline shells with internal energy cores, pseudo-refractive rinds, bow-shock caps and long analytic trails
  that still terminate exactly on the authoritative cue positions; regional shock systems per sub-impact; a
  crystal-blast finale with pressure dome, taller pillar, ash column and concentric devastation rings.
- **Layer and budget separation.** Ten dedicated cosmic material slots with disjoint per-layer vertex budgets, and an
  order that puts the opaque bodies before the blended shells. Because submission order is draw order, the opaque
  planet/mothership hulls write depth first and every additive shell over them is occluded by the far side
  automatically — the same mechanism that lets the atmosphere and terminator read correctly.
- **Camera.** Thirteen new colossal shots (`helios_pursuit`, `helios_annihilation`, `helios_pressure`, `helios_crater`,
  `helios_sweep`, `helios_horizon`, `helios_monolith`, `singularity_sky`, `singularity_sink`, `singularity_sweep`,
  `glassfall_platform`, `glassfall_crater`, `glassfall_sweep`) plus `cosmic_montage` for the detached act. Every
  orbital distance is bounded so a shot stays inside the player's loaded region, and `EffectScene`'s culling box now
  follows emitter altitude, structure factor and shock reach instead of only the local crater.
- **Timeline.** A TITANIC/APOCALYPSE Helios holds the detached cosmic framing through `CHARGING`,
  `ORBITAL_ALIGNMENT` and `FIRING` — the lance is fired in the star-mothership framing — and cuts back to the world
  for `ATMOSPHERIC_ENTRY` and the devastation. For APOCALYPSE that is 270 ticks (13.5 s) of local holographic
  designation, 990 ticks (49.5 s) of planetary/cosmic montage and 666 ticks (33.3 s) of charge/alignment/firing:
  **96.3 s from first lock to stellar discharge inside a 100 s event**, with the whole 1,000-diameter basin, two
  devastation waves and the aftermath still to come.

## 3. Shader and material changes

- **No new GLSL program.** The compiled count stays at **18**, and the colossal pass is carried through new texture
  bindings on the existing pipelines: additive `energy` (`MODE_CORE` / `MODE_RIM` / `MODE_FLAT`), alpha-blended
  `smoke`, opaque lit `hull` (directional/sky/rim/specular, texture-alpha emissive seams driven by vertex energy)
  and heated `debris`.
- **Four new original procedural textures** from `tools/assets/generate_textures.py`: `effect/star.png` (star/point
  shells and core bodies), `effect/gas.png` (nebulae, galaxy dust, planetary cloud annuli), `effect/lance.png`
  (the layered stellar beam) and `machine/cosmic_hull.png` (mothership plating with emissive seams).
- **One deliberate material behaviour.** The cosmic backdrop's far plate is drawn with vertex alpha zero, which
  switches the hull shader's emissive seams off, giving an opaque depth-writing quad. That is required: only a
  depth-writing opaque quad actually replaces the local world behind the cinematic — an earlier translucent version
  let daylight through and made the world visible behind the montage.
- **Graceful fallbacks unchanged.** Every pipeline is still registered `registerOptional` and every blended pipeline
  still registers an OIT pipeline set (26.3 throws under improved transparency otherwise). A shader failure logs a
  warning and leaves the vanilla render types and the native particle layer carrying the presentation; the capture
  run reported `custom=true` (custom pipelines verified) in every frame with no fallback and no renderer exception.

## 4. Audio changes

- **13 new original synthesized events** (41 total, up from 28): `cosmic.ascend`, `cosmic.reveal`, `cosmic.vast`,
  `cosmic.mothership`, `cosmic.deploy`, `cosmic.charge`, `cosmic.align`, `cosmic.fire`, `cosmic.invoke`,
  `singularity.vacuum`, `singularity.release`, `glassfall.lattice_charge`, `glassfall.dome`.
- **Generator ordering is a correctness property.** `tools/assets/generate_audio.py` shares one seeded RNG stream, so
  the Part 2 set is emitted first and in its original order and the new events are appended afterwards. The Part 2
  assets are **byte-identical** to the Part 2 release; an earlier draft that inserted generators ahead of the
  interface sounds shifted that stream and altered two validated UI sounds, which the ordering now prevents.
- **Direction.** Helios gets deeper machinery, immense charging, cosmic-scale reveal ambience, a terrifying firing
  transient and a massive impact/aftershock; Singularity gets huge low-frequency pressure, unstable harmonic
  collapse, a vacuum-like implosion and a violent release; Glassfall gets bombardment machinery, lattice charging,
  crystal resonance and a region-wide dome finale.
- **Mix isolation.** `CinematicAudio` carries a separate colossal-only accent table, so the STANDARD mix is
  bit-identical to Part 2. Delivery, distance-delayed aftershocks and the instance cap are unchanged.
- Format validation: **41 sounds valid** (mono 44.1 kHz Vorbis, peaks −3.2..−1.2 dBFS), every one referenced by
  `sounds.json`, in `evidence/part25b/audio-validation.json`. Not approved by listening.

## 5. Weapon-by-weapon final status

| Weapon | Colossal status | Boundary |
| --- | --- | --- |
| **Helios Annihilator** | 20-stage APOCALYPSE sequence, 100 s, radius 500 / depth 96 (1,000-diameter basin) with two annular passes to 575 and 650 blocks. Directed cosmic act through the discharge, colossal lance, planetary-scale impact/aftermath. | Full 1,000×1,000 excavation in a fully loaded region is admitted/budget-tested, not completed; the cinematic is an authored illusion, not a sky replacement. |
| **Singularity Engine** | Regional spacetime failure: sky-filling anomaly, counter-precessing tori, infalling streams and slabs, prolonged attraction, held-breath pause, violent release, gravitational sink basin with fracture rings and molten residue. Radius 320 / depth 160, 90 s. | Distortion is layered geometric and depth-based, not true screen-space lensing. |
| **Glassfall Array** | Civilization-ending precision bombardment: monumental orbital platform, 16 peripheral projectiles plus a final central strike (19 reserved impacts), colossal crystalline shells with internal cores and refractive rinds on authoritative trajectories, regional shock systems, crystal-blast dome finale, long aftermath. Radius 360 / depth 80, 100 s. | Projectile visuals are stylized shells with pseudo-refractive layers, not real refraction. |

STANDARD profiles for all three weapons resolve to exactly the Part 2 numbers and the Part 2 presentation, so the
shipped default experience is unchanged.

## 6. Performance measurements

Deterministic client-side measurement, not an estimate: one synthetic APOCALYPSE Helios strike injected into the
presentation cache at every magnitude boundary, ULTRA quality, captured with per-frame renderer diagnostics
(`evidence/part25b/renderer-diagnostics.txt`, frames in `evidence/part25b/screenshots`, statistics in
`evidence/part25b/frame-metrics.json`). `collect` is NULLSTAR's own smoothed extraction time; `frame` is Minecraft's
measured render work for the frame.

| Beat | Scene | Vertices | Dropped | Particles | Collect | Frame |
| --- | --- | --- | --- | --- | --- | --- |
| Ascent | cosmic | 3,468 | 0 | 0 | 0.304 ms | 1.17 ms |
| Planet | cosmic | 16,844 | 0 | 0 | 0.415 ms | 1.36 ms |
| Solar system | cosmic | 11,200 | 0 | 0 | 0.290 ms | 1.41 ms |
| Galaxy | cosmic | 2,964 | 0 | 0 | 0.266 ms | 1.10 ms |
| Observable universe | cosmic | 2,412 | 0 | 0 | 0.198 ms | 0.84 ms |
| Mothership reveal | cosmic | 17,472 | 0 | 0 | 0.307 ms | 1.46 ms |
| Deployment | cosmic | 17,472 | 0 | 0 | 0.295 ms | 1.49 ms |
| Charging | cosmic | 17,816 | 0 | 78 | 0.313 ms | 1.34 ms |
| Stellar lance | cosmic | 18,616 | 0 | 90 | 0.346 ms | 1.46 ms |
| Descent | world | 19,692 | 0 | 243 | 0.390 ms | 1.73 ms |
| Impact | world | 23,264 | 0 | 229 (peak 583) | 0.414 ms | 1.84 ms |
| Pressure wall | world | 23,348 | 0 | 188 (peak 583) | 0.396 ms | 1.54 ms |
| Aftermath | world | 17,380 | 0 | 172 (peak 583) | 0.315 ms | 1.18 ms |

The reported FPS in the unfocused test window is pinned at the 30 FPS background limit (29–30 in this table), so it is
not a frame-rate measurement; the same beats measured 111–117 FPS in a focused run with essentially identical frame
times (1.13–2.03 ms). Server tick time during the colossal beats stayed at 0.51–0.78 ms (idle world, synthetic
strike). For
reference, the Part 2 STANDARD capture on the same machine measured 3k–22k vertices and 0.08–0.45 ms collect for a
single strike and 40k–52k vertices with the cosmetic particle cap held at 1,024 for four concurrent strikes. Peak
cosmetic particles across the whole final capture session were 583 of the 1,024 ULTRA cap. The
colossal act therefore costs the same order of magnitude as the Part 2 presentation while presenting at ~4.4x
machinery scale, because the cosmic stages are depth-composed billboard shells rather than high-poly geometry, and
every layer keeps its pre-existing budget. Zero primitives were dropped in any colossal beat, and zero were dropped
across all 69 strike frames of the whole capture session (STANDARD Helios peaked at 25,500 vertices, Glassfall at
13,212, Singularity at 7,160). These are
single-machine measurements on a flat diorama with one client, not guarantees for dense worlds or weaker GPUs.

## 7. Tests run

| Check | Result | Evidence |
| --- | --- | --- |
| `./gradlew clean build` (JUnit + dedicated GameTests) | PASS: **61 JUnit, 0 failures/errors**; **all 10 dedicated GameTests passed** | `evidence/part25b/clean-build.log`, `evidence/part25b/unit-tests/` |
| `./gradlew runClientGameTest` (regression + dedicated + capture + colossal) | PASS | `evidence/part25b/client-suite.log` |
| Capture scenes | helios, singularity, glassfall, colossal, modes, stress — including the new colossal measurement pass | `evidence/part25b/screenshots/` (103 frames) |
| Dedicated GameTests | radius-500 admission/confirmation/replay, complete 2,000-tick inert preview, exact global budgets and changed-block cap, bounded scan with no corner chunk loading | `evidence/part25b/clean-build.log` |
| Real client lifecycle | cosmic scenes, late one-snapshot reconstruction, native K skip, skip after commitment, controlled diameter-64 real crater, inert nominal bounds, actual server close/reopen of an unfinished job | `evidence/part25b/client-suite.log` |
| Custom pipelines | 18 programs compiled; `custom=true` in every captured frame; no fallback, no renderer exception | `client-suite.log`, `renderer-diagnostics.txt` |
| Audio assets | 41 valid mono 44.1 kHz Vorbis, peaks −3.2..−1.2 dBFS | `evidence/part25b/audio-validation.json` |
| Production artifact | PASS: exact 26.3, class major 69, both production entrypoints, **0 packaged test classes** | `evidence/part25b/artifact-validation.json` |

New JUnit coverage in this pass (`ColossalPresentationTest`, 5 tests): the colossal magnitudes derived from the real
authoritative profiles, the UNCHANGED STANDARD identity, the starship scene holding through
charge/alignment/discharge while every other weapon and phase stays in the world, the colossal timeline selecting
the cosmic act and the devastation sweeps, and every generated profile camera name being registered.

The full suite logs two required Part 2.5 proof lines (cosmic scenes/late reconstruction/native skip/diameter-64
crater, and the restart-recovered job resumed at a bounded cursor). The one `Timeline cannot rewind` stack trace in
the dedicated run is the intentional injected isolation fixture that cancels exactly one strike; it is expected
evidence, not an unhandled defect. Windows OSHI/Perflib warnings and development Realms placeholder-auth notices
remain environment noise and appear in every historical run.

## 8. Captured evidence paths

- `docs/evidence/part25b/clean-build.log` — release build, 61 JUnit and 10 dedicated GameTests.
- `docs/evidence/part25b/unit-tests/TEST-*.xml` — 12 JUnit result files.
- `docs/evidence/part25b/client-suite.log` — full real-client suite on the final revision.
- `docs/evidence/part25b/renderer-diagnostics.txt` — per-frame `strikes/vertices/dropped/particles/collect` plus
  measured frame time and server tick time for every captured beat.
- `docs/evidence/part25b/screenshots/` — the colossal measurement beats, the three colossal weapon sets, the
  Part 2 reference scenes, the five normalized cosmic-act frames and the modes/stress captures.
- `docs/evidence/part25b/frame-metrics.json` — per-frame luminance, contrast, content and palette statistics used to
  check that no colossal stage collapsed into a flat or empty frame.
- `docs/evidence/part25b/artifact-validation.json`, `docs/evidence/part25b/audio-validation.json`.
- `docs/evidence/part25/` — Part 2.5A engineering evidence, left untouched as the historical record.

## 9. Open limitations

- **Artistic and audio approval is not claimed.** Frames were checked for structure, contrast and content statistics
  and inspected for composition hazards, but a human must judge whether the scale fantasy lands. Audio was validated
  by format, peak and RMS only — never by listening.
- **The cosmic act is an authored illusion composed in view-local space in front of the gameplay camera**, not an
  astronomical simulation, not a sky/level replacement and not a teleport. The local world is hidden by an opaque
  depth-writing backdrop quad, so geometry nearer than that quad (for example terrain directly below the eye) can
  still be visible at the frame's edge.
- **Colossal crater framing is deliberately bounded.** A 1,000-diameter basin is far wider than the client's render
  and simulation distance, so colossal shots frame the crater with nearby terrain, machine and dust as the scale
  reference instead of pulling the camera outside the loaded region. The crater does not fit in one frame by design.
- **The full nominal 1,000-diameter crater is supported, admitted and budget-tested, not completely excavated in a
  fully loaded 1,000×1,000 region.** Unloaded cells are skipped permanently and no chunk is force-loaded.
- **Pacing in the automated runs is accelerated.** Test-thread engine advancement drives the 100 s timeline quickly,
  so the client suites prove transitions, rendering, networking, lifecycle, bounded terrain and restoration, **not
  real-time pacing** or huge-region TPS.
- Multiple independent graphical clients, dense-world frame/tick timing, third-party render/shader/claim mod
  integration, power-loss transaction recovery, and survival-owner death in every phase remain unverified.
- Colossal performance numbers come from one synthetic strike on a flat diorama on one machine.

## 10. Final artifact

```text
build/libs/nullstar-1.0.0.jar
```

Exact version `=26.3`, Java class major 69, both production entrypoints present, zero packaged test classes; current
SHA-256 in `docs/evidence/part25b/artifact-validation.json`. The `nullstar-test` test mod is a separate source set and
is not distributed.
