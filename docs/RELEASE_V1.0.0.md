# NULLSTAR v1.0.0 — 2026-10-10

Release: <https://github.com/ToontownSuperForeverMVP/NULLSTAR/releases/tag/v1.0.0>

- Tag/source commit: `v1.0.0` / `574b26e8bf25e28de6d83261126d95b750a1b162`.
- Build: <https://github.com/ToontownSuperForeverMVP/NULLSTAR/actions/runs/38070595033>.
- Build environment: Ubuntu 24.04, Temurin Java 25, pinned Gradle/Loom/dependencies.
- CI `clean build` passed, including the required dedicated GameTests.
- Production asset: `nullstar-1.0.0.jar`; SHA-256
  `fbc98708c03f20d9078908c5833e8b4f0ba03f3e78e0d3e44337f50f541bcded`.
- Developer asset: `nullstar-1.0.0-sources.jar`; not an installable mod.
- `SHA256SUMS.txt` covers both JARs and the accompanying `build-info.json`.
- Production packaging verification passed: version 1.0.0, Minecraft 26.3,
  Java class major 69, GPL-3.0-only, license/NOTICE present, correct entrypoints,
  zero test classes, optional Mod Menu unbundled and nonmandatory.
- Both JARs passed ZIP integrity checks. The developer source archive retains
  the expected unexpanded `${version}` resource template.
- The production asset was downloaded back from the published release and its
  SHA-256 matched the verified upload.

The same gameplay/presentation source passed 68 JUnit tests, 10 dedicated
GameTests, graphical suites, and validation of 60 sound assets during
[publication validation](REPOSITORY_PUBLICATION.md). The release source includes
the subsequent documentation-only README encoding correction, whose CI passed.
Different platform build hashes are recorded separately; the release hash above
identifies the actual Linux CI asset, not the earlier local Windows build.

Human artistic/audio approval, complete maximal-footprint excavation, worst-case
performance, independent multiple graphical clients, and broad third-party
compatibility remain unverified. Release publication does not close those items.
