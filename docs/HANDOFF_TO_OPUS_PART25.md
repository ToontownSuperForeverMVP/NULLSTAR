# Part 2.5B handoff — extreme presentation

Optional Mod Menu integration is documented in [MOD_MENU_SUPPORT.md](MOD_MENU_SUPPORT.md). Keep its entrypoint client-only and its dependency optional; both the Mod Menu button and N use the same settings screen. The client test suite now includes `settings`; `-PwithoutModMenu` tests the standalone path.

Part 2.5A implements the colossal engineering layer on top of the preserved Part 2 release. Branch `codex/part25-colossal`; recovery `codex/pre-part25-20261009`. Read `PART25_ENGINEERING_REPORT.md`, `PROJECT_STATE.md`, `VALIDATION.md`, `ACCEPTANCE.md` and `AGENTS.md` first. The old `HANDOFF_TO_OPUS.md` remains historical.

## Implemented and stable

- STANDARD is the original definition/loop. TITANIC and APOCALYPSE resolve validated profiles from `config/nullstar/scales.json`. All physical authority stays in `WeaponEngine`.
- Helios apocalypse: primary radius 500, depth 96, a nominal 1,000-diameter basin, two concentric devastation passes and a 2,000-tick / 100-second default sequence. It never scales below 90 seconds. Singularity: radius 320/depth 160, prolonged attraction and collapse rings. Glassfall: sixteen peripheral impacts, one final center projectile/strike and two ring passes, primary radius 360/depth 80.
- The terrain cursor is constant-memory and aligned to world chunk tiles. Every candidate, including fast-skipped unloaded cells, consumes the global budget. Later waves wait for lower waves. Admission reserves all impacts. Protection/vanilla interaction/container/unbreakable restrictions and mutation flags remain intact.
- Optional graceful terrain-job restart recovery is implemented and tested through actual world close/reopen. It restores queued jobs, not future cues, cinematic timelines or cooldowns. Offline/unauthorized owners pause recovered jobs. No crash-consistent cross-file transaction guarantee exists.
- Commands/config include scale selection, inert bounds, full cinematic-only preview, explicit target-bound confirmation, progress, diagnostics, cancellation, per-ID abort and emergency abort. Apocalypse physical damage is disabled by default; public mode limits physical size.
- `state_v2` and `visual_v2` are the new compact presentation contracts. Request/status remain v1. Never mutate an existing wire schema under the same suffix.

## Gigantic scene hooks

| Seam | Attach Part 2.5B work here |
| --- | --- |
| `core/ScaleProfile` / `ScaleCatalog` | Independent structure/cinematic scale, beam width, shockwave extent, orbital/stellar/cosmic anchor, script reference, alignment nodes, rings and barrage footprint. Operator overrides are validated before admission. |
| `core/CinematicScene` | WORLD, ASCENT, PLANET, SOLAR_SYSTEM, GALAXY, COSMIC_BOUNDARY, STELLAR_MOTHERSHIP. These identify authored scenes, never physical astronomical destinations. |
| `core/ColossalMetadata`, `network/StatePayload` | Compact current scene, scale dimensions, total duration, checkpoint and commitment; late observers reconstruct from one snapshot. No giant scene serialization. |
| `client/render/StrikeView.scale()` / `cinematicScene()` | Read-only per-frame entry to all new metadata alongside phase/progress/age/seed/cues. |
| `client/render/scene/CosmicMontageScene` | Replace its minimal procedural composition with immense planet/solar-system/galaxy/universe/mothership art. It is deliberately a basic engineering placeholder. |
| `client/render/Canvas.sceneBillboard` | View-local basis composition; origin is near the current gameplay camera for normalized scenes. Use additional bounded mesh helpers here if needed. |
| `client/render/EffectScene.collect` | Owner virtual-scene routing before terrain frustum culling; ordinary Part 2 scenes remain intact for world stages/observers/fallback. |
| `client/camera/CameraScripts` / `CinematicCamera` | `cosmic_montage` keeps the camera at gameplay position with authored framing/FOV. Add presets here, keeping observer and accessibility behavior. |
| Existing client-only `CameraMixin` | Preserve the before-projection/culling seam. The only new generic distinction is virtual scenes excluding the self-avatar while suppressing held items through frame-local extracted options. Do not put weapon branches here. |
| `GlassfallScene.offsets(StrikeView)` | Deterministic broader lattice derived from compact server footprint/count, corrected to received authoritative cue positions. Final center projectile is present. |

Helios phase order: ACQUIRING → TARGET_CONFIRMATION → PRELUDE → ASCENT → EXOATMOSPHERIC_TRANSITION → PLANETARY_VIEW → SOLAR_SYSTEM_VIEW → GALACTIC_VIEW → COSMIC_BOUNDARY_VIEW → MOTHERSHIP_REVEAL → DEPLOYMENT → CHARGING → ORBITAL_ALIGNMENT → FIRING → ATMOSPHERIC_ENTRY → IMPACT → DEVASTATION_WAVE_1 → DEVASTATION_WAVE_2 → AFTERMATH → COOLDOWN. Default primary cue tick: 1,756. Full duration: 2,000. The phase enum preserves chronological helpers used by `StrikeView`; do not randomly reorder it without changing protocol/derived behavior.

## Visual intent and unfinished artistry

Helios should ascend through a stylized cosmic zoom montage, pass a planet/solar system/galaxy/observable-universe boundary, reveal an enormous star-controlled mechanical mothership, deploy and align many nodes, charge and fire an immense stellar beam, then descend into a readable regional apocalypse. Its physical basin is real queued terrain work; show expanding devastation/aftermath while it continues. The normalized scene foundation is not final artwork, a literal astrophysical simulation or a fully animated final mothership.

Singularity should have a much larger anomaly and distortion footprint, sustained attraction and catastrophic contraction/release, a deep ruined region and fracture rings. Glassfall should have a broad firing lattice, many synchronized colossal projectiles, linked craters and a huge center strike. Use the server subimpact/cue locations; retain exact trajectory endpoints.

Structure scale defaults are astronomical descriptors (1 million for Apocalypse), not numbers to multiply into world-space vertices blindly. Map them through authored/logarithmic normalized scenes. Beam width and shockwave metadata are independent of physical damage; do not tie accessibility or quality to server crater size. Owners may decline/skip cameras, observers keep normal view, and both still need readable world effects. New cosmic phases need long-form authored audio/checkpoint accents; existing Part 2 sounds are retained for corresponding phases but were not stretched/re-authored here.

## Verification and invariants

Use Java 25. Run `gradlew build` for JUnit/dedicated tests, `gradlew runClientGameTest` for all four client suites, and `gradlew clean build` for release. Focus: `gradlew runClientGameTest -Ptests=colossal`; PowerShell comma-separated selections need quoting, e.g. `'-Ptests=regression,dedicated,capture,colossal'`. Generate Minecraft sources in a separate invocation. Tests use only disposable `build/run` worlds.

Keep optional pipelines and required OIT variants; current per-layer vertex budgets and cosmetic particle cap still apply. Keep every protection and candidate-budget check, server-owned geometry, pre-reservations, changed-block caps, explicit confirmation and fallback path. Do not distribute `nullstar-test`. No Prism saves/mods have been changed.

Real graphical evidence includes five normalized scenes, late reconstruction, native skip/restoration, a controlled diameter-64 crater and actual restart recovery. Full radius-500 admission and bounded scans are tested; complete real excavation of an entire loaded 1,000×1,000 region is **not verified**. Cinematic timelines in the new fixture advance artificially; this is not real-time pacing or a colossal performance benchmark. See the final acceptance table for precise boundaries before making stronger claims.
