# Acceptance

Statuses refer to actual checks in this repository. `PASS` is automated unless explicitly marked visual inspection. Final run details and versions are in `VALIDATION.md`.

| Part 1 criterion | Status | Evidence |
| --- | --- | --- |
| Fabric 26.3 / Java 25 clean build | PASS | `gradlew clean build` |
| Usable production JAR; test mod excluded | PASS | `build/libs/nullstar-1.0.0.jar`, archive inspection |
| Mod initializes on a dedicated server | PASS | dedicated `runGameTest` startup and real-world tests |
| Client initializes and enters a world | PASS | `runClientGameTest` |
| Controller registration and targeting | PASS | real item use; registered controller/model visible in client screenshots |
| Server-authoritative activation through keybind | PASS | native key mapping after controller designation |
| Helios full phase loop and completion | PASS | client/server phase assertions and completed crater |
| Visible charging source and descending beam | PASS, visual inspection | charge/entry screenshots, `EffectRenderer` and camera framing |
| Visible impact/aftermath and changed landscape | PASS, visual inspection | aftermath/crater screenshots; queue/world assertions |
| Persistent crater after save/reopen | PASS | reopened disposable world block assertion |
| Singularity and Glassfall playable end-to-end | PASS | charge/fire/aftermath/completion plus terrain assertions for both |
| Gravity, damage and knockback foundation | PASS | dedicated living-entity GameTest |
| Bounded processing and cancellation | PASS | algorithm tests; dedicated manager tests and real cancel |
| Protected/unbreakable blocks and containers | PASS | dedicated world fixture; protection veto check |
| Loaded-only target reads | PASS | dedicated loaded-block-view test; no new chunk loaded |
| Radius limits, seeded geometry and falloff | PASS | pure geometry/configuration tests; server caps |
| Timeline transitions and one-time cues | PASS | pure monotonic/boundary/scaling tests |
| Wire serialization and invalid data rejection | PASS | current typed codec round-trip/malformed request tests |
| Duplicate activation prevention | PASS | per-owner/existing-event checks and replay/cooldown unit tests |
| Late/stale/dimension client cache behavior | PASS | pure client-state tests; real dimension transition |
| Camera skip and completion restoration | PASS | player camera entity/type/position assertions; real skip |
| Preview leaves terrain unchanged | PASS | full preview loop and stone block assertion; zero changed-block log |
| Full engineering handoff and installation README | PASS | required docs and coherent commits |

## Additional acceptance for Part 2

- [ ] Evaluate final authored visuals and custom sounds with the user.
- [ ] Verify concurrent strikes with multiple independent clients and late observers on a production dedicated server.
- [ ] Validate claim-mod adapters against the exact installed claim-mod versions.
- [ ] Evaluate renderer/shader compatibility and reduced-motion/flashing on the final effects.
- [ ] Manually test survival death, dimension travel and disconnect during each weapon phase.
- [ ] Measure server tick time and client frame time in dense real worlds at configured upper limits.
- [ ] Evaluate cave/water/near-border art, remaining vanilla fluid behavior and physical aftermath for the final presentation.

These additional scenarios concern integrations and final art/performance validation. They are not marked complete by a green compile or by the disposable singleplayer fixture.
