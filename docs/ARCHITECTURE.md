# Architecture

## Build and ownership

The supplied template was compared with FabricMC's official 26.3 example branch and built before implementation. The project retains its Minecraft/API/Loader/Gradle versions, pins its resolved Loom snapshot to 1.18.3, and replaces unused Kotlin example stubs with Java 25 entrypoints. Kotlin runtime is no longer required. Source sets are split by environment.

`Nullstar` initializes validated config, immutable weapon definitions, item/creative registration, networking, commands and server lifecycle callbacks. `NullstarClient` owns key mappings, presentation state, client preferences and HUD. Client classes and the camera Mixin never load on the dedicated server. Production JAR excludes the `gametest` source set.

## Authoritative execution

`WeaponDefinition` declares targeting, phases, sound IDs, camera preset names, event ticks, impact geometry, presentation metadata and cooldown. Bundled files live in `data/nullstar/weapons`. Operator overrides in `config/nullstar/weapons/<id>.json` are validated before use; invalid data fails startup or retains the prior catalog on reload. Current IDs/behaviors are `helios`, `singularity`, `glassfall`. Adding a fourth behavior currently requires extending the behavior allowlist and dispatch in `WeaponEngine`.

`Timeline` is a pure monotonic server-tick state machine. It delivers cues once, including skipped tick intervals. `WeaponCatalog(Path,double)` scales and validates immutable definitions once at load; activation uses those snapshots without scaling again. The one-argument constructor retains multiplier 1. Scaled stages cap at 2400 ticks; cue offsets map proportionally within each actual scaled stage. `all()` is an unmodifiable view. `ActiveStrike` snapshots the validated definition, dimension key, target, geometry caps, seed, owner and start tick. Mutable state is confined to the server thread. Bundled `data/` JSON is loaded from the classpath, not through data-pack reload; only the named config files override it. Unknown override files and sound IDs produce diagnostics at load.

`WeaponEngine` shares activation between item, typed request and commands. It recomputes item targeting on the server, verifies loaded terrain, dimension, distance, border, owner permissions, protection and cooldown. It prevents overlapping activations by one owner and reserves future destruction jobs before accepting a strike. Preview/config-disabled destruction suppresses both damage and terrain changes.

Each tick advances active timelines, schedules phase sounds, applies Singularity attraction at five-tick intervals, publishes event cues and queues impacts. Once the timeline ends it waits for queued jobs before publishing terminal completion. Owner death/disconnect/dimension change, unloaded target chunk or missing level cancels remaining work. Completed terrain changes persist normally in world chunk storage. Cancellation is not an undo operation.

The controller first designates a target for 200 ticks, then confirms activation. Sneak-use selects the next weapon. Activation key uses the same confirmed-target flow. Commands with explicit coordinates still validate server-side distance, ground, loaded state and permissions.

Preview obeys the same `operatorsOnly` setting as destructive activation. It intentionally emits shared sound/visual cues and consumes the same bounded event slot and cooldown; it never reserves destructive jobs or mutates blocks/entities. Runtime invariant failures are contained per strike and discard its pending work; failed terrain steps still consume their scanned candidate budget. Re-reserving an admitted strike is rejected. Already changed terrain remains persistent after cancellation/failure.

## Terrain and entity safety

`CraterGeometry` provides deterministic angular noise, layered radial falloff, bowls, spheres and ellipsoids. Hard limits are radius/depth 48; no noise extends beyond the selected radius. `DestructionJob` scans a constant-memory bounding volume incrementally; its cursor has a deterministic ordering and every candidate consumes budget. No giant position list is allocated.

`DestructionManager` round-robins at most 64 candidates per slice across all dimensions within a global candidate cap and cooperative four-millisecond deadline. This deadline is cooperative, not a hard real-time guarantee. Individual block updates/protection callbacks can take longer. Chunk keys/positions are retained, not chunks. Unloaded cells are skipped, never force-loaded or retried indefinitely.

Mutation skips borders/build limits, negative destroy-speed blocks, `nullstar:protected_blocks`, all block entities, vanilla player interaction protection and vetoing protection adapters. `BlockTransformations` changes supported bowl-shell materials without loot generation. Neighbor shape propagation is suppressed to keep the work budget predictable; Part 2 must preserve that boundary if introducing fluid or debris simulations.

`ProtectionHooks.register` returns an `AutoCloseable` registration and vetoes both block and living-entity effects. Hook exceptions deny the operation. This is an integration seam; no third-party claim mod bridge is bundled. Register adapters before admitting player strikes. The configurable `operatorsOnly` default is true.

`EntityEffects` performs bounded-radius living-entity queries, falloff damage/knockback and bounded Singularity attraction. Creative/spectator players are excluded. The mutation count is bounded by `entityBudget`; the underlying vanilla query may still enumerate a dense population. Client packets cannot create damage or force vectors.

Explosion sources identify the online owner as both direct and causing entity. Other-player damage, knockback and attraction require the `pvp` rule and vanilla team permission; self-forces remain allowed, while self-damage follows vanilla attributed-explosion rules. Without a resolved owner, player effects fail closed, while mob effects remain environmental. Public claim hooks continue to veto both kinds of effect.

On server shutdown, outstanding jobs and strikes are safely discarded and already changed chunks are saved by Minecraft. Jobs do not resume after restart. Cooldowns are session-scoped, retained across disconnect within the same server session, and reset on server restart.

## Networking v1

All contracts use typed `CustomPacketPayload` and `StreamCodec<RegistryFriendlyByteBuf,...>`. Channel suffix `_v1` versions the schema. Decoders constrain strings/numbers. Round-trip tests bound each current packet below 512 bytes.

| Channel | Owner/direction | Fields and use |
| --- | --- | --- |
| `nullstar:request_v1` | client -> server | protocol=1, monotonic positive sequence, action 0 acquire/1 activate/2 select/3 cancel/4 skip, known weapon string, preview intent. No target, radius, dimension or damage fields. Four-tick replay/rate gate per connection. |
| `nullstar:status_v1` | server -> owner | selected weapon ID/display name, bounded message, confirmed-target flag/position, remaining cooldown ticks. Target is authoritative. |
| `nullstar:state_v1` | server -> observers | strike/owner UUIDs, dimension, weapon ID/display name/phase, overall and phase ages, phase duration, server tick, target, radius/depth, seed, preview, destruction progress, color/orbital height/particle scale/camera preset, terminal flag. |
| `nullstar:visual_v1` | server -> observers | strike UUID, monotonic cue index, impact/projectile type, server-resolved position, capped radius and duration. Client deduplicates; transient cues are not replayed on join. |

State goes out on phase changes and at most once per 20 server ticks, plus initial and terminal messages. Recipients are owner and same-dimension players within 256 blocks. Join and periodic state resync cover late observers and dimension entry. Owner status updates at one Hz after a selection. No per-block or per-render-frame custom packets. Vanilla chunk updates carry terrain changes.

Broadcast rejects payload types/unassociated strike IDs before enumerating recipients; join snapshots apply the same observer range. Transient client-level reset clears presentation/target but retains authoritative cooldown and its clock baseline. Actual disconnect clears the entire session. Controller aiming/effects/HUD accept either hand.

`ClientStrikeState` interpolates from reception ticks, caps cache size, rejects older state, deduplicates cues and expires state after 100 silent client ticks. During singleplayer pause client visual time pauses too. Disconnect and new client-level identity clear all transient presentation.

## Presentation and camera contracts

`EffectRenderer` uses tick-generated vanilla particles with one total client budget, divided between continuous strike layers and event cues. It shows targeting rings, orbital energy, Helios beam, shockwave, Singularity core/spirals and Glassfall projectile trails. It creates no world-authoritative projectile entities. HUD displays the server-supplied weapon display name, target, phase progress, terrain progress and cooldown, with a compact panel and rebinding-aware hints.

`SoundScheduler` sends vanilla sound packets on server phase transitions to same-dimension observers within 256 blocks. Charging sources use the orbital/core position and impact sources use the ground. Delivery deliberately bypasses the vanilla player-position distance filter: the render camera can be near the event while the player is far away. Native attenuation and the user's sound settings still apply. No custom audio asset is bundled.

`PresentationLayers.register(Layer)` allows new client animation/audio/render-state systems to consume read-only client strike snapshots. Layers must respect their own allocation and effect limits and clear resources in `reset`. For custom GPU rendering, verify 26.3 RenderPearl/extraction APIs directly; do not insert old immediate-mode OpenGL code here.

`CinematicCamera.register(id, CameraPreset)` registers generic position/look-at/FOV samplers. The controller provides easing, angular interpolation, smooth release and intensity-capped impact shake. Current presets are orbital reveal, beam follow and impact view. Only the owner's event activates camera effects; observers retain normal view.

The client-only Mixin applies a pose after vanilla alignment and FOV calculation but before view matrices, culling and perspective projection. It updates camera position, rotation and frame FOV, marks extracted cinematic views detached, and suppresses held-item rendering through that frame's extracted camera-type state. Vanilla computes the original state again on every frame. It never changes the player, camera entity, saved options, input state, server position or control authority. Skip stops only that client's camera; gameplay continues. Disabling camera/reduced motion, opening screens, death, cancellation, stale snapshots and level changes restore normal view.

Reduced-motion disables cinematic camera/shake. Reduced-flashing defaults true and disables the impact screen overlay/explosion particle flash. Particle budget can be zero. Shader/light/weather effects belong to Part 2.
