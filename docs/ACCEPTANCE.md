# Acceptance

Statuses refer to actual checks in this repository. `PASS` is automated unless explicitly marked visual inspection. Final run details and versions are in `VALIDATION.md`.

Part 1.5B engineering readiness: **CONDITIONAL PASS**. All confirmed critical/high defects are resolved (none were reported); targeted repairs pass 27 JUnit tests, seven dedicated GameTests and the expanded real client scenario. Detailed finding classifications and narrow runtime gaps are in `SOL_QA_REMEDIATION.md`.

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
| Legal long-stage duration scaling and single application | PASS | long-stage and operator-override JUnit regressions |
| Unauthorized preview/destructive admission rejected | PASS | dedicated embedded-player authority fixture |
| Owner attribution and PvP-safe damage/forces | PASS | dedicated PvP off/on and source-attribution assertions |
| Concurrent owners survive an isolated event failure | PASS, embedded server players | injected timeline fault cancels one; other Glassfall completes a crater; independent graphical clients remain open |
| Reservation saturation/release/overwrite/overflow | PASS | direct manager JUnit tests and completed-job world accounting |
| Offhand aiming/designation/key activation | PASS | expanded real graphical Helios scenario |
| Transient reset retains cooldown | PASS | nonzero cooldown checked across reset before real dimension travel |
| Camera blend/presets/release/death/unloaded frame | PASS | deterministic frame-input JUnit tests plus real restoration scenario |
| Disabled terrain destruction stays inert | PASS | dedicated complete Glassfall cycle with terrain disabled |
| Reproducible artifact evidence and JDK guard | PASS | executable archive inspector; actual Java 21 rejection |

## Additional acceptance for Part 2

- [ ] Evaluate final authored visuals and custom sounds with the user.
- [ ] Verify concurrent strikes with multiple independent clients and late observers on a production dedicated server.
- [ ] Validate claim-mod adapters against the exact installed claim-mod versions.
- [ ] Evaluate renderer/shader compatibility and reduced-motion/flashing on the final effects.
- [ ] Manually test survival death, dimension travel and disconnect during each weapon phase.
- [ ] Measure server tick time and client frame time in dense real worlds at configured upper limits.
- [ ] Evaluate cave/water/near-border art, remaining vanilla fluid behavior and physical aftermath for the final presentation.

These additional scenarios concern integrations and final art/performance validation. They are not marked complete by a green compile or by the disposable singleplayer fixture.
