# NULLSTAR Cataclysm Engine — Part 3 release report ("The Final Cataclysm")

Part 3 is the visual reconstruction and production-hardening pass on top of Part 2.5B. It was driven by an
independent audit of what the previous release actually rendered, not by its reported vertex counts. Server
authority, the `_v1`/`_v2` wire schemas, protection hooks, budgets, caps, preview policy and the STANDARD timelines
are unchanged. Every claim below is labelled with one of four categories:

- **IMPLEMENTED** — source and assets exist.
- **RUNTIME VERIFIED** — observed in a running client/server (automated real-client suites, real terrain).
- **ARTISTICALLY REVIEWED** — the actual frames were inspected by the implementing model during iteration. This is
  not human approval; no human artistic or audio sign-off is claimed anywhere in this report.
- **UNVERIFIED** — not demonstrated in this environment.

## 1. Original deficiencies found (audit of `docs/evidence/part25b`)

| Deficiency | Observed cause (verified in source) |
| --- | --- |
| The whole cosmic act rendered against flat daytime-blue (or fog-grey) sky; the galaxy and observable-universe frames were empty | `hull.fsh`/`smoke.fsh` applied world fog to geometry placed hundreds of blocks out, so the "opaque backdrop" and every hull were fogged to exactly the sky colour; `energy.fsh` faded additive shells by 65 % |
| Composition changed with the direction the player faced | `sceneBillboard` used the camera basis but planets, mothership and lance were placed on raw world axes 32 blocks ahead |
| Mothership frames were a pile of black boxes | the scaled mesh (~49-block ring radius) enclosed the camera; hull lighting used the world sun direction, not the scene |
| Impact, pressure wall and aftermath were total white-outs | colossal world shots sat ~1,630 blocks out (outside loaded chunks) so presets declined and the camera fell back inside kilometre-wide additive layers; distant smoke domes were fogged to the sky colour |
| Zoom "popped" at every cosmic stage boundary | detached FOV was `65-25*ease(progress)`, resetting each phase |
| Apocalypse Helios gave the impact and aftermath almost no time | uniform `/22` stage split: impact at tick 1,756 of 2,000, **AFTERMATH 42 ticks (2.1 s)** |
| Singularity anomaly was a speck in daylight sky; it vanished during the colossal alignment stage | core radius ~9 blocks at ~331 blocks altitude; `coreRadius` had no case for ORBITAL_ALIGNMENT |
| Glassfall platform shots were inside the machine; finale and aftermath white-outs | 34–44-block orbit around a ~180-block-radius platform; 316-block additive crystal dome around the camera; fogged dust domes |
| 41 sounds of 0.8–8 s against phases of 4–39 s | long silent stretches in the extended timelines |
| Colossal craters silently incomplete beyond loaded chunks | unloaded cells are skipped permanently (measured: 27 % of chunk visits lost even at radius 160, see §9) |

## 2. Completed visual improvements

Current source and presentation are described in the [README](../README.md).
The [GitHub publication record](REPOSITORY_PUBLICATION.md) contains fresh build,
graphical-suite, audio, packaging and curated Part 3 capture evidence. This file
retains the historical audit above; it is not a complete final acceptance report.
