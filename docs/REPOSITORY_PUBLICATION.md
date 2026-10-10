# GitHub publication — 2026-10-10

Repository: <https://github.com/ToontownSuperForeverMVP/NULLSTAR>

This publication preserves the existing source history and includes the current
Part 3 implementation. Original NULLSTAR code, assets, and documentation are
GPL-3.0-only. The complete GPLv3 text is in the root LICENSE; NOTICE and the
licenses directory retain template/Gradle provenance. Production metadata carries
the SPDX identifier and source/issue links. Original Java and asset-generator
sources carry copyright/SPDX headers.

## Included material

Common/client source, weapon data, original textures/audio/shaders, Gradle wrapper
and build configuration, JUnit and development-only GameTests, asset generators,
artifact verification, engineering documentation, operator examples, and dated
evidence. Existing local cosmic-camera and capture-test edits are preserved.

Generated build outputs, development/test worlds, Gradle/Kotlin caches, reference
downloads, local agent tooling, logs outside curated evidence, private environment
files, keys, and backups are excluded. No binary release is implied by this source
publication. Production JARs are obtained by building or from successful CI artifacts.

## Validation of the publication source

- `gradlew.bat clean build`: passed; **68 JUnit tests**, zero failures/errors/skips,
  and **10 required dedicated GameTests** passed.
- `python -I tools/assets/validate_audio.py`: **60 sounds valid**, mono 44.1 kHz
  Vorbis, measured decoded peaks -3.2 to -1.2 dBFS.
- `pwsh -File scripts/verify-artifact.ps1 -Output build/artifact-validation.json`:
  passed; Java class major 69, Minecraft 26.3, GPL-3.0-only, both entrypoints,
  zero test classes, optional Mod Menu remains unbundled and nonmandatory,
  GPL license and NOTICE present.
- Production JAR SHA-256:
  `606816CBE81B9239F1A8762F6EF3D3960FC5B3CF51ED0CD42F375F5BE08D6D7C`.
- README rendered through GitHub's Markdown API; local links/section targets and
  SVG XML checked. The bespoke SVG banner was rasterized and visually inspected;
  the PNG is used for consistent README display, with editable SVG retained.
- Historical text blobs scanned for common GitHub/OpenAI token and private-key
  signatures: no matching files. This is a bounded signature scan, not a claim
  that every possible secret format has been detected.

- `gradlew.bat runClientGameTest "-Ptests=settings,regression,dedicated,capture,colossal"
  "-Pcapture=part3helios,part3singularity,part3glassfall,part3oit"`: passed in
  6m 42s. Includes settings, destructive weapon regression, dedicated networking,
  current cosmic/weapon presentation captures, Improved Transparency, and colossal
  state/skip/actual world-close-and-reopen unfinished-job recovery.
- Curated current frames were inspected and retained under
  [evidence/publication/frames](evidence/publication/frames). These Part 3 capture
  fixtures reconstruct client presentation states, not complete destructive strikes.
- Build, client-suite, audio and artifact evidence is retained under
  [evidence/publication](evidence/publication).

Human artistic/audio sign-off, independent multiple graphical clients, broad mod
compatibility, and complete maximum-footprint excavation remain separate acceptance
items. The historical destruction measurements are not rerun by this publication.
