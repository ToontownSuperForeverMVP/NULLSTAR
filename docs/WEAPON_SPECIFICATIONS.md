# Weapon specifications

The three identities and their implementations are original. Part 1 uses native particle sprites, a native placeholder controller icon and existing sounds. No reference-mod code, models, music or cinematic sequences are incorporated.

## Shared gameplay

Hold the Orbital Controller in the main hand. Sneak-use or V selects a weapon. Aim at a loaded surface and use/G to designate it; use/G again launches the confirmed strike. Operators are required for destructive activation by default. Designations expire in ten seconds. The engine limits one active event per owner and four simultaneous events by default. Leaving the dimension, disconnecting, dying or unloading the target cancels remaining work.

All timings below are ticks at multiplier 1 and a nominal 20 TPS. Cooldown begins at authorization and survives cancellation/disconnect during the current server session. Phase `COOLDOWN` is the final presentation phase; the cooldown gate can remain active longer. The event stays registered until terrain jobs finish.

| Weapon | Range | Default radius/depth | Timeline | Cooldown |
| --- | --- | --- | --- | --- |
| Helios Annihilator | 128 | 20 / 13 | 300 ticks / 15 s | 600 ticks / 30 s |
| Singularity Engine | 112 | 13 / 18 | 355 ticks / 17.75 s | 500 ticks / 25 s |
| Glassfall Array | 128 | 16 / 10, subimpacts at 35% | 395 ticks / 19.75 s | 700 ticks / 35 s |

## HELIOS ANNIHILATOR

Technical sequence: acquiring 20, charging 60, orbital alignment 40, firing 20, atmospheric entry 30, impact 10, aftermath 100, cooldown 20. Impact cue occurs at absolute tick 170. A gold energy source appears 72 blocks above the target. Rings and a luminous core build during charging/alignment; a descending gold/white column leads into an expanding radial shockwave, warm impact accent and smoke. A rounded, irregular bowl removes the terrain asynchronously. Supported exposed shell materials become basalt, blackstone or coal.

Damage is radial, up to 60 at the center, falling to zero at 1.5 times the effective radius; knockback scale is 1.7. Creative/spectator players are excluded. Radius and depth obey the server caps. Default full candidate volume is 42,025, distinct from actual changed blocks; crater seed affects the changed count.

Art direction for Part 2: a compact asymmetric orbital collector with segmented solar vanes, a compressed honey-gold source, deliberate alignment motion and a sharply readable downward energy column. Keep the terrain target identifiable throughout. Develop a rising tonal charge, focused firing transient and delayed low-frequency impact. Final models, shaders, atmosphere and authored audio are future art work.

## SINGULARITY ENGINE

Technical sequence: acquiring 20, charging 80, firing/attraction 120, impact 15, aftermath 100, cooldown 20. Implosion cue at tick 220. A violet ring surrounds a dark core four blocks above the ground, with inward spiral particles. During firing, the server attracts eligible living entities inside twice the radius every five ticks; velocity is clamped. Implosion creates a deeper ellipsoidal excavation, radius 13 and vertical axis 18, with obsidian/blackstone shell transformations. Damage cap is 45 and knockback 0.7.

Art direction: a restrained black center with violet lensing and uneven orbiting fracture arcs. Inward debris should make attraction readable; the implosion should feel compressed and heavy. Optional advanced distortion must respect reduced motion and flashing preferences. Current particles provide the playable gravity/implosion foundation; optical lensing and physical debris are Part 2 additions.

## GLASSFALL ARRAY

Technical sequence: acquiring 20, charging 50, alignment 20, firing/barrage 170, final impact 15, aftermath 100, cooldown 20. Five launches/impacts are authoritative timeline cues. Four smaller projectiles launch at ticks 90/125/160/195, impacting 30 ticks later at offsets (-9,-7), (8,-6), (-7,9), (9,8). A larger center projectile launches at 230 and impacts at 260. Each offset gets a bounded local surface probe. Unloaded offset chunks are skipped without generation; the reserved job slot is released.

Visual trajectories are client-only cyan/white particle streaks; no independently simulated projectile can drift away from the server impact schedule. Four subcraters use 35% of radius/depth/damage, then the center bowl uses radius 16/depth 10. Sand at scorch boundaries becomes glass; stone becomes basalt. Five impact jobs are reserved before launch so a barrage cannot overcommit halfway through.

Art direction: an angular orbital lattice, staggered crystalline rods, cold cyan streaks with thin refractive shells, and a deliberate final center strike. Preserve the staggered impacts and server event timestamps when replacing the primitive trails with authored meshes and impact animation.

## Data and extension rules

Edit bundled JSON or place validated per-ID overrides in `config/nullstar/weapons`. Stage fields are phase/ticks/camera/sound; cues have absolute tick/effect/x/z/radiusScale. Impact fields define geometry, radial damage, knockback and scorch. Presentation fields define color, orbital height, particle quality scale and sound pitch. See `WeaponDefinition` for the exact validation limits.

`/nullstar reload` only succeeds when no strike is active. Invalid combined timing/config values retain the prior settings. The shared server behavior dispatch currently recognizes three behavior IDs; new behaviors should use reusable timeline cues and queue APIs rather than duplicate terrain/network code.
