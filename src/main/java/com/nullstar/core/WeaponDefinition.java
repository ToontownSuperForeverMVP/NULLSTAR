// Copyright (C) 2026 ToontownSuperForeverMVP and NULLSTAR contributors
// SPDX-License-Identifier: GPL-3.0-only

package com.nullstar.core;

import java.util.List;

/** Immutable wire-independent definition. All durations are server ticks. */
public record WeaponDefinition(String id, String name, String behavior, Targeting targeting,
        List<Stage> phases, List<Cue> events, Impact impact, Presentation presentation, int cooldownTicks) {
    public record Targeting(int range, boolean groundRequired) {}
    public record Stage(Phase phase, int ticks, String camera, String sound) {}
    public record Cue(int tick, String effect, double x, double z, double radiusScale,
                      double innerRadiusScale, double depthScale, String shape, int wave) {
        public Cue(int tick,String effect,double x,double z,double radiusScale) { this(tick,effect,x,z,radiusScale,0,1,null,0); }
        public double effectiveDepthScale() { return depthScale==0?1:depthScale; }
    }
    public record Impact(String shape, int radius, int depth, double irregularity, float damage,
                         double knockback, boolean scorch) {}
    public record Presentation(int color, double orbitalHeight, double particleScale, float soundPitch) {}

    public WeaponDefinition {
        if (id == null || !id.matches("[a-z][a-z0-9_]{0,31}") || name == null || name.isBlank() || name.length()>96
                || !List.of("helios", "singularity", "glassfall").contains(behavior)
                || targeting == null || targeting.range < 8 || targeting.range > 192
                || phases == null || phases.isEmpty() || phases.size() > 32
                || events == null || events.size() > 128 || impact == null || presentation == null
                || cooldownTicks < 20 || cooldownTicks > 72000) throw new IllegalArgumentException("Invalid weapon definition");
        phases = List.copyOf(phases);
        events = List.copyOf(events);
        if (phases.stream().anyMatch(s -> s.phase == null || s.ticks < 1 || s.ticks > 2400
                || s.camera == null || s.camera.length() > 32 || s.sound == null || s.sound.length() > 128
                || s.phase == Phase.COMPLETE || s.phase == Phase.CANCELLED)
                || phases.stream().map(Stage::phase).distinct().count() != phases.size())
            throw new IllegalArgumentException("Invalid or repeated phase");
        int duration = phases.stream().mapToInt(Stage::ticks).sum();
        if (events.stream().anyMatch(e -> e.tick < 0 || e.tick >= duration || e.effect == null
                || !List.of("impact", "projectile").contains(e.effect)
                || !Double.isFinite(e.x) || !Double.isFinite(e.z) || Math.abs(e.x) > 1024 || Math.abs(e.z) > 1024
                || !Double.isFinite(e.radiusScale) || e.radiusScale <= 0 || e.radiusScale > 2
                || !Double.isFinite(e.innerRadiusScale) || e.innerRadiusScale < 0 || e.innerRadiusScale >= e.radiusScale
                || !Double.isFinite(e.depthScale) || e.depthScale < 0 || e.depthScale > 1
                || e.wave < 0 || e.wave > 7 || e.shape != null && !List.of("ring","basin","crater","sphere","ellipsoid").contains(e.shape)))
            throw new IllegalArgumentException("Invalid timeline cue");
        if (!List.of("crater", "sphere", "ellipsoid", "basin").contains(impact.shape)
                || impact.radius < 1 || impact.radius > 1024 || impact.depth < 1 || impact.depth > 384
                || !Double.isFinite(impact.irregularity) || impact.irregularity < 0 || impact.irregularity > .2
                || !Float.isFinite(impact.damage) || impact.damage < 0 || impact.damage > 200
                || !Double.isFinite(impact.knockback) || impact.knockback < 0 || impact.knockback > 4
                || !Double.isFinite(presentation.orbitalHeight) || presentation.orbitalHeight < 8 || presentation.orbitalHeight > 128
                || !Double.isFinite(presentation.particleScale) || presentation.particleScale < 0 || presentation.particleScale > 2
                || !Float.isFinite(presentation.soundPitch) || presentation.soundPitch < .1 || presentation.soundPitch > 2)
            throw new IllegalArgumentException("Unsafe impact or presentation");
    }

    public int duration() { return phases.stream().mapToInt(Stage::ticks).sum(); }
    public WeaponDefinition scaled(double multiplier) {
        if (!Double.isFinite(multiplier) || multiplier < .25 || multiplier > 4)
            throw new IllegalArgumentException("Duration multiplier must be between .25 and 4");
        return new WeaponDefinition(id, name, behavior, targeting,
                phases.stream().map(s -> new Stage(s.phase, scaledLength(s, multiplier), s.camera, s.sound)).toList(),
                events.stream().map(e -> new Cue(scaleTick(e.tick, multiplier), e.effect, e.x, e.z, e.radiusScale,
                        e.innerRadiusScale,e.depthScale,e.shape,e.wave)).toList(),
                impact, presentation, cooldownTicks);
    }
    private static int scaledLength(Stage stage, double multiplier) {
        return Math.max(1, Math.min(2400, (int) Math.round(stage.ticks * multiplier)));
    }
    private int scaleTick(int tick, double multiplier) {
        int sourceStart = 0, destinationStart = 0;
        for (Stage stage : phases) {
            int length = scaledLength(stage, multiplier);
            if (tick < sourceStart + stage.ticks)
                return destinationStart + Math.min(length - 1, (int) ((long) (tick - sourceStart) * length / stage.ticks));
            sourceStart += stage.ticks; destinationStart += length;
        }
        throw new IllegalArgumentException("Cue outside timeline");
    }
}
