# Part 2.5A — Colossal scale engineering

October 9, 2026. Branch: `codex/part25-colossal`. Recovery branch: `codex/pre-part25-20261009` at the unchanged Part 2 baseline `c5ef353`. Historical `part2-release` and `codex/part-1.5b-complete` tags are preserved. This stage extends the existing engine and keeps the Part 2 renderer/assets. Final extreme presentation belongs to Part 2.5B.

## 1. Implementation

Added three scale classes, six validated operator profiles, a 20-stage Helios sequence, broader Singularity collapse and Glassfall barrage plans, chunk-aligned constant-memory terrain traversal, staged wave dependencies, planned-volume progress, change limits, bounded diagnostics, optional restart recovery, v2 spectacle snapshots, normalized cosmic scene hooks and operator commands. Existing controller authorization, server targeting, replay/cooldown gates, protections, loaded-only access and global candidate budgets remain authoritative.

Read the required engineering/Part 2 documents and actual source before editing. Baseline `gradlew build` passed its existing dedicated tests; JUnit was up-to-date. Sources for the exact 26.3 camera, command and world-storage APIs were inspected locally. Java 25, Loader 0.19.5, API 0.162.0+26.3 and Loom 1.18.3 are unchanged. No new runtime dependency, mapping, GLFW call or Mixin was added.

## 2. Scale architecture

`ScaleTier` selects STANDARD, TITANIC or APOCALYPSE. STANDARD uses the original validated definition unchanged. `ScaleCatalog` creates/loads `config/nullstar/scales.json`; its six `ScaleProfile` records validate identifiers, finite independent physical/scene dimensions, rings, nodes, subimpact count and timelines before publication. Invalid reload retains the old catalog/config.

Profiles independently define cinematic scale, structure scale, beam width, shockwave radius, primary crater radius/depth, blast radius, attraction/entity radius, barrage footprint, rings, cinematic script reference, anchor descriptor and alignment nodes. Astronomical values describe presentation space; they are never passed to chunk access or physical camera travel. Per-event snapshots freeze the resolved definition and safety caps.

Profile duration scaling applies once to the generated sequence. Apocalypse Helios floors its duration multiplier so its complete timeline remains at least 1,800 ticks. Individual stages retain the legal 2,400-tick cap; cues scale proportionally within actual stages. STANDARD retains its existing single catalog scaling behavior.

## 3. Geometry and limits

| Tier / weapon | Primary radius / depth | Nominal diameter | Timeline at multiplier 1 | Physical impact jobs |
| --- | --- | --- | --- | --- |
| STANDARD Helios | 20 / 13 | 40 | 300 ticks / 15 s | 1 |
| STANDARD Singularity | 13 / 18 | 26 | 355 ticks / 17.75 s | 1 |
| STANDARD Glassfall | 16 / 10 | 32 | 395 ticks / 19.75 s | 5 |
| TITANIC Helios | 128 / 48 | 256 | 1,400 ticks / 70 s | 3 |
| TITANIC Singularity | 96 / 64 | 192 | 1,200 ticks / 60 s | 3 |
| TITANIC Glassfall | 112 / 40 | 224 | 1,400 ticks / 70 s | 11 |
| APOCALYPSE Helios | 500 / 96 | **1,000** | **2,000 ticks / 100 s** | 3 |
| APOCALYPSE Singularity | 320 / 160 | 640 | 1,800 ticks / 90 s | 3 |
| APOCALYPSE Glassfall | 360 / 80 | 720 | 2,000 ticks / 100 s | 19 |

Physical values above require private-world policy/caps permitting them. Public mode clips colossal radius to 48 and depth to the existing standard cap by default. Apocalypse physical damage is disabled by default. Geometry absolute bounds are radius 1,024 and depth 384; sphere radius is additionally clipped to the permitted vertical extent on activation. The maximum configurable colossal diameter is 2,048, depth 384, waves 8, changed blocks per event 1 billion, simultaneous composite colossal events 8, impact slots 32, and candidates/tick 8,192. Defaults are more conservative.

`basin` adds a broad, deep floor with seeded concentric irregularity rather than just scaling a small bowl. Ellipsoids and existing bowls/spheres remain supported. `ring` excludes an explicit inner radius and cuts/scorches a surrounding annulus, giving a fractured/scorched rim without unbudgeted block placement. Default rings have outer/inner scales 1.15/0.88 and 1.30/1.08, with depth scales 0.30 and 0.12. Helios's default rings extend nominally to 575 and 650 blocks. Noise never escapes the chosen outer radius; the primary Helios boundary is within 4% of its radius-500 envelope. Protected cells, missing chunks and build/border limits can leave gaps; no fabricated guarantee of a solid crater in unloaded land is made.

Glassfall reserves sixteen smaller peripheral impacts, one large center strike and two secondary rings. Its alternate rings of subimpact centers lie at 65% and 92% of the configured footprint radius. Each launch is paired with an authoritative impact; the server probes each local surface and releases unavailable impact reservations without loading chunks. Safety clipping scales/bounds both centers and child craters inside the permitted colossal physical footprint; unrestricted private profiles retain their original pattern. Astronomical presentation scale remains independent of this clipping. `ImpactFootprint` shares the integer-safe bounds between execution, previews and client reconstruction.

## 4. Work scheduling and recovery

`DestructionJob` traverses world-aligned 16×16 chunk tiles, clipped to the exact rectangular envelope, with contiguous vertical columns. It stores a handful of counters, geometry and origin coordinates, never a candidate list or retained chunk. Unloaded columns fast-forward their logical cursor while charging **every skipped candidate** to the same global budget. Loaded air, geometry misses, protected blocks and skipped build/border cells also consume budget. Scan order is deterministic and checkpoint cross-checks prevent a corrupt cursor from rewinding work.

`DestructionManager` retains round-robin 64-candidate slices and the cooperative four-millisecond deadline across dimensions. The deadline is not a hard real-time guarantee. Primary/subimpact work is wave 0; a later wave waits until all lower-wave queued jobs for that event finish. Block updates keep the previous no-loot/no-neighbor-shape-propagation flags. All impact slots are reserved before launch, preventing partial admission of a large barrage. Progress uses planned candidate volume, including future impact jobs, and credits skipped surface probes; adding later jobs cannot make its fraction move backwards.

The radius-500/depth-96 main basin alone has **146,292,146 candidate cells**, not changed blocks. At 8,192 candidates/tick its theoretical full-budget minimum is about 893 seconds; actual work can take longer due to the deadline/protection costs. Rings add more work. At the default 1,536 candidates/tick this is approximately 79 minutes for the main envelope alone. These are arithmetic lower bounds, **not measured performance**. The engineering deliberately allows terrain aftermath to continue after the 100-second cinematic.

Optional `persistDestructionJobs` saves committed queued jobs/cursors/statistics under the current world's `data/nullstar-jobs-v1.json`, every 200 engine ticks and at SERVER_STOPPING. It uses a bounded, versioned, atomically replaced file, validates current radius/depth/wave/change/concurrency policy on reload, and retains rejected files for diagnosis. Recovered jobs wait for their owner to be online, alive, authorized under current operator policy and in the correct dimension; unloaded cells are still skipped. They release capacity when complete and support owner cancellation or operator abort.

Only already-enqueued terrain jobs recover. Future uncommitted cues, cinematic timelines and cooldowns do not resume. Graceful world close/reopen is tested. This is **not** a crash-consistent transaction journal synchronized with Minecraft chunk saves: power loss between a cursor write and a chunk save can lose/revisit work. Atomic JSON replacement prevents a partial document, not world-level atomicity. Cancellation never rolls back blocks. Emergency/owner/operator cancellation checkpoints the remaining queue immediately when persistence is enabled.

## 5. Authoritative timelines

Apocalypse Helios supports acquisition, confirmation, prelude, ascent, exoatmospheric transition, planet, solar system, galaxy, cosmic boundary, mothership reveal, deployment, charging, alignment, firing, descent/entry, impact, two devastation phases, aftermath and cooldown. The primary impact is at tick 1,756 in the default 2,000-tick profile. Additional rings begin after the impact phase; actual terrain wave eligibility depends on queue completion, not an assumption that the crater finished in a cinematic tick.

Singularity has prolonged formation, alignment, attraction, release, deep excavation and collapse rings. Glassfall has a longer firing stage with sixteen synchronized projectile/impact pairs, a final center projectile and concentric devastation. Existing registered phase sounds are retained where phases match the base definition; new cosmic phases intentionally leave room for Part 2.5B audio direction. All cues are delivered once across delayed tick intervals. An event stays registered after its timeline ends until its queued terrain completes.

The first actual terrain enqueue sets `committed`. Cinematic skip never modifies that flag, the timeline, reservations or damage. Owner death/disconnect/dimension change or unloaded central target still cancels the live event's remaining work with a recorded cause; already changed terrain remains. Recovered terrain has no live cinematic event and waits for owner availability instead.

## 6. Wire schema and presentation hooks

`state_v2` replaces `state_v1` because enum ordering, larger geometry and new metadata change the schema. `visual_v2` replaces `visual_v1` for enlarged cue radii/index range. Unchanged client authority/status contracts remain `request_v1` / `status_v1`. Install the same new mod on both ends; v1 clients are not a supported mixed-version deployment.

The state appends bounded `ColossalMetadata`: tier, normalized scene ID, cinematic/structure scales, beam width, shockwave radius, secondary/blast/entity radii, script and anchor, node/subimpact counts, footprint radius, wave count, checkpoint, full duration and commitment. Round-trip tests keep representative packets under 512 bytes. No serialized geometry, meshes or giant scene lists are sent. Snapshot cadence remains phase changes plus one Hz, and recipients remain same-dimension observers plus owner; colossal observer range is configurable 256..4,096 (default 2,048). Transient cues are not replayed on join.

`StrikeView.scale()` / `cinematicScene()` expose the immutable metadata. `CosmicMontageScene` provides a minimal deterministic view-local composition for the cosmic stages. Its imagery is a foundation, **not final planetary or mothership art**. `Canvas.sceneBillboard` maps scene coordinates through the current camera basis. Owner-only `cosmic_montage`, registered in `CameraScripts`, keeps camera position at the normal gameplay camera and changes framing/FOV. It never queries remote terrain. The existing single Mixin seam stays before projection/culling; virtual scenes retain first-person avatar exclusion while suppressing held items through frame-local extracted options. World shots remain intact.

Reduced motion, disabled cinematics, skip, screens, death, cancellation, stale state, level transition and disconnect return to ordinary gameplay. Observers keep their own view and see world effects. Glassfall derives its enlarged rail footprint from compact server metadata and uses actual cue positions when available. Colossal HUD progress uses authoritative full duration rather than assuming the old bundled timeline.

## 7. Operator tools and configuration

Commands (coordinates optional unless shown):

```text
/nullstar scale standard|titanic|apocalypse
/nullstar bounds helios apocalypse [x y z]
/nullstar preview helios apocalypse [x y z]
/nullstar fire helios apocalypse [x y z]
/nullstar confirm
/nullstar status
/nullstar diagnostics
/nullstar cancel
/nullstar abort <strike-uuid>
/nullstar emergency_abort
/nullstar reload
```

Existing `/fire <weapon> [x y z]` and `/preview <weapon> [x y z]` use the owner's selected scale. New tier-taking paths use the same engine validation. Selection, bounds, destructive fire, confirmation, diagnostics, abort and emergency abort require operator commands; preview retains server `operatorsOnly` policy. Pending confirmation binds weapon, tier, dimension and exact server-validated target for 200 ticks, consumes no destructive capacity/cooldown and is single-use. `/confirm` rechecks current targeting, authority, caps, capacity and cooldown. Cancel clears pending confirmation and owned recovered jobs as well as active events.

Bounds inspection is inert: it computes per-pass envelopes, inner/outer ring radii, candidate volume and subimpact centers without activating an event or mutating terrain. It uses the same `CraterGeometry.forCue` as execution/accounting. Heights for secondary impacts remain relative to the eventual local surface, not an assertion that all land is flat. Status includes event UUID/progress/change/backlog/time; diagnostics dumps recent bounded history and current jobs to chat and server log.

Important `server.json` additions: `defaultScale`, `apocalypseEnabled`, `publicServerMode`, `colossalCinematicOnly`, `requireColossalConfirmation`, `maximumColossalDiameter`, `maximumColossalDepth`, `publicMaximumRadius`, `maximumDevastationWaves`, `maximumSimultaneousColossalJobs`, `maximumChangedBlocks`, `maximumEntityRadius`, `colossalObserverRange`, `persistDestructionJobs`. The simultaneous setting counts composite colossal events, including detached recovered work; `maximumJobs` independently limits their child impacts/reservations. Reload rejects active events or queued recovered work. Examples: `docs/config-examples`.

## 8. Automated coverage

New unit suites cover scale data/overrides, unchanged STANDARD, all larger profiles, 1,000 diameter, long phase boundaries/one-time cues, distributed launches/impacts, public/private/config clamps, invalid metadata, basin/ellipse/annulus bounds, exact chunk traversal/checkpoint replay, corrupted cursor rejection, radius-500 synthetic budgeting/cancellation, compact v2 serialization, detached camera/no terrain queries/owner/skip/stale behavior and manager persistence/current-policy rejection. Prior replay, cooldown, authorization, geometry, client, camera and renderer suites remain.

Three new dedicated GameTests cover full-size admission and explicit confirmation/replay; queue saturation; a complete inert 2,000-tick preview; real smaller staged world mutations; graceful file recovery; completed recovery releasing capacity; strict changed-block caps; radius-500 bounded traversal/cancellation with no corner chunk loading. Existing protection/PvP/concurrency/failure isolation tests remain. Historical hard-bound tests now test the expanded absolute bounds, not the intentionally removed radius-48 geometry cap.

`ColossalClientGameTest` adds real graphical cosmic-stage captures, single-snapshot late reconstruction, native skip, a controlled 64-diameter real crater, committed-damage skip, inert full-size bounds and actual integrated-server close/reopen of an unfinished job. Its timeline is accelerated by test-thread server advancement; it does not measure real-time 100-second pacing or performance. The original regression, real dedicated connection and Part 2 capture suites remain selected by default.

## 9. Runtime evidence

Focused graphical suite passed after correcting an exposed self-avatar in virtual scenes and a falling creative test fixture. It captured five cosmic stages, reconstructed the mothership scene from a current snapshot, restored gameplay on skip, completed a controlled diameter-64 crater and reopened a world with the same unfinished job at 272 / 92,950 candidates in the initial successful run. This is an actual server restart, not only JSON round-trip validation. Final release commands/counts, logs and curated captures are recorded in `VALIDATION.md` and `docs/evidence/part25`.

## 10. Evidence boundaries

The full nominal 1,000-diameter physical crater was admitted and algorithmically/budget-tested, but **was not excavated to completion in a fully loaded real 1,000×1,000 region**. No huge-crater TPS, completion-time, independent multi-client or dense entity claim is made. Default loaded-only scanning means unloaded chunks are skipped permanently, not queued for eventual generation or retried when explored. Public caps must be deliberately changed for true private-world mega-craters. Memory stays bounded even if many cells are unavailable.

Manual cosmic pacing, final planet/galaxy/universe/mothership visuals, extreme shaders, long-form audio and artistic approval remain Part 2.5B work. Independent clients, third-party protection/render mods, every survival-death stage, power-loss recovery and worst-case dense-world timing remain unverified. Existing audio assets were not changed or re-authored in this engineering phase.

## 11. Handoff

Read `HANDOFF_TO_OPUS_PART25.md` for exact source seams and invariants. Attach final immense art to `StrikeView`/normalized scene identifiers, extend `CameraScripts`/per-profile script references and existing optional/OIT-aware pipelines, and preserve the queue, confirmation, v2 schemas, loaded-only/protection policy and STANDARD behavior. Keep astronomical scales in normalized cinematic space. Do not replace the radius-500 job with a cosmetic imitation or bypass candidate accounting to make it appear instantaneous.
