package com.nullstar.core;

import java.util.ArrayList;
import java.util.List;

/** Validated operator data. Physical dimensions and scene dimensions are independent. */
public record ScaleProfile(String weapon, ScaleTier tier, int radius, int depth, int durationTicks,
        double cinematicScale, double structureScale, double beamWidth, double shockwaveRadius,
        int blastRadius, int entityRadius, int subimpacts, String script, String anchor, int alignmentNodes,
        List<Ring> rings) {
    public record Ring(double outerScale, double innerScale, double depthScale) {
        public Ring {
            if (!Double.isFinite(outerScale) || !Double.isFinite(innerScale) || !Double.isFinite(depthScale)
                    || outerScale < 1 || outerScale > 2 || innerScale < 0 || innerScale >= outerScale
                    || depthScale <= 0 || depthScale > 1) throw new IllegalArgumentException("Invalid devastation ring");
        }
    }
    public ScaleProfile {
        if (!WeaponCatalog.IDS.contains(weapon) || tier == null || radius < 1 || radius > 1024
                || depth < 1 || depth > 384 || durationTicks < 1 || durationTicks > 72000
                || !finite(cinematicScale, 1, 1e12) || !finite(structureScale, 1, 1e9)
                || !finite(beamWidth, .1, 2048) || !finite(shockwaveRadius, 1, 8192)
                || blastRadius < 1 || blastRadius > 512 || entityRadius < 1 || entityRadius > 512
                || subimpacts < 0 || subimpacts > 24 || script == null || !script.matches("[a-z0-9_]{1,32}")
                || anchor == null || !List.of("orbital", "stellar", "cosmic").contains(anchor)
                || alignmentNodes < 1 || alignmentNodes > 64 || rings == null || rings.size() > 7)
            throw new IllegalArgumentException("Unsafe colossal scale profile");
        rings = List.copyOf(rings);
        if (tier == ScaleTier.APOCALYPSE && weapon.equals("helios") && durationTicks < 1800)
            throw new IllegalArgumentException("Apocalypse Helios requires at least 90 seconds");
        if (subimpacts + rings.size() + 1 > 32) throw new IllegalArgumentException("Too many reserved impacts");
    }
    private static boolean finite(double n, double low, double high) { return Double.isFinite(n) && n >= low && n <= high; }
    /**
     * Authored colossal shot vocabulary. Every name here is registered in `CameraScripts`; an unregistered name
     * simply declines to the player's own view, so this stays a presentation choice rather than a hard dependency.
     */
    private String cameraFor(Phase phase) {
        if (weapon.equals("helios")) return switch (phase) {
            case ACQUIRING, TARGET_CONFIRMATION, PRELUDE -> "helios_establish";
            case ASCENT, EXOATMOSPHERIC_TRANSITION, PLANETARY_VIEW, SOLAR_SYSTEM_VIEW, GALACTIC_VIEW,
                 COSMIC_BOUNDARY_VIEW, MOTHERSHIP_REVEAL, DEPLOYMENT, CHARGING, ORBITAL_ALIGNMENT,
                 FIRING -> "cosmic_montage";
            case ATMOSPHERIC_ENTRY -> "helios_pursuit";
            case IMPACT -> "helios_annihilation";
            case DEVASTATION_WAVE_1 -> "helios_pressure";
            case DEVASTATION_WAVE_2 -> "helios_crater";
            case AFTERMATH -> "helios_sweep";
            default -> "none";
        };
        if (weapon.equals("singularity")) return switch (phase) {
            case ACQUIRING -> "singularity_form";
            case CHARGING -> "singularity_sky";
            case ORBITAL_ALIGNMENT, FIRING -> "singularity_pull";
            case IMPACT -> "singularity_implode";
            case DEVASTATION_WAVE_1 -> "singularity_sink";
            case DEVASTATION_WAVE_2 -> "singularity_aftermath";
            case AFTERMATH -> "singularity_sweep";
            default -> "none";
        };
        return switch (phase) {
            case ACQUIRING -> "glassfall_reveal";
            case CHARGING -> "glassfall_platform";
            case ORBITAL_ALIGNMENT -> "glassfall_lattice";
            case FIRING, ATMOSPHERIC_ENTRY -> "glassfall_barrage";
            case IMPACT -> "glassfall_final";
            case DEVASTATION_WAVE_1 -> "glassfall_crater";
            case DEVASTATION_WAVE_2, AFTERMATH -> "glassfall_sweep";
            default -> "none";
        };
    }
    /**
     * New colossal-scale audio direction for TITANIC/APOCALYPSE. Only phases that genuinely need a larger sound
     * are listed; every other phase keeps the validated Part 2 sound for its phase.
     */
    private java.util.Map<Phase,String> colossalSounds() {
        var map = new java.util.EnumMap<Phase,String>(Phase.class);
        boolean apocalypse = tier==ScaleTier.APOCALYPSE;
        switch (weapon) {
            case "helios" -> {
                map.put(Phase.ASCENT, "nullstar:cosmic.ascend");
                map.put(Phase.EXOATMOSPHERIC_TRANSITION, "nullstar:cosmic.ascend");
                map.put(Phase.PLANETARY_VIEW, "nullstar:cosmic.reveal");
                map.put(Phase.SOLAR_SYSTEM_VIEW, "nullstar:cosmic.reveal");
                map.put(Phase.GALACTIC_VIEW, "nullstar:cosmic.vast");
                map.put(Phase.COSMIC_BOUNDARY_VIEW, "nullstar:cosmic.vast");
                map.put(Phase.MOTHERSHIP_REVEAL, "nullstar:cosmic.mothership");
                map.put(Phase.DEPLOYMENT, "nullstar:cosmic.deploy");
                map.put(Phase.CHARGING, "nullstar:cosmic.charge");
                map.put(Phase.ORBITAL_ALIGNMENT, "nullstar:cosmic.align");
                map.put(Phase.FIRING, "nullstar:cosmic.fire");
                map.put(Phase.IMPACT, "nullstar:cosmic.invoke");
                if (apocalypse) map.put(Phase.AFTERMATH, "nullstar:cosmic.invoke");
            }
            case "singularity" -> {
                map.put(Phase.FIRING, "nullstar:singularity.vacuum");
                map.put(Phase.IMPACT, "nullstar:singularity.release");
            }
            case "glassfall" -> {
                map.put(Phase.ORBITAL_ALIGNMENT, "nullstar:glassfall.lattice_charge");
                map.put(Phase.IMPACT, "nullstar:glassfall.dome");
            }
            default -> { }
        }
        return map;
    }
    public int waves() { return 1 + rings.size(); }
    public int secondaryRadius() { return Math.max(subimpacts>0?(int)Math.ceil(radius*1.14):radius,rings.stream().mapToInt(r -> (int)Math.ceil(radius * r.outerScale())).max().orElse(radius)); }

    public static ScaleProfile standard(WeaponDefinition definition) {
        var impact = definition.impact();
        return new ScaleProfile(definition.id(), ScaleTier.STANDARD, impact.radius(), impact.depth(), definition.duration(),
                1, 1, 2, impact.radius()*3, Math.min(512,impact.radius()), Math.min(512,impact.radius()*2), 0, "standard", "orbital", 3, List.of());
    }
    /** Builds a compact authoritative timeline; no scene meshes or block lists are serialized. */
    public WeaponDefinition apply(WeaponDefinition base) {
        if (tier == ScaleTier.STANDARD) return base;
        var stages = new ArrayList<WeaponDefinition.Stage>();
        boolean helios = weapon.equals("helios");
        if (helios) return heliosStages(base);
        stages.add(new WeaponDefinition.Stage(Phase.ACQUIRING, durationTicks/12, cameraFor(Phase.ACQUIRING), ""));
        // The colossal Helios holds the detached cosmic act through charge, alignment and discharge: the stellar
        // lance is fired in the star-mothership framing and only cuts back to the world for the descent.
        stages.add(new WeaponDefinition.Stage(Phase.CHARGING, durationTicks/8, cameraFor(Phase.CHARGING), ""));
        stages.add(new WeaponDefinition.Stage(Phase.ORBITAL_ALIGNMENT, durationTicks/12, cameraFor(Phase.ORBITAL_ALIGNMENT), ""));
        int firingStart = stages.stream().mapToInt(WeaponDefinition.Stage::ticks).sum();
        int firingLength = durationTicks/8;
        stages.add(new WeaponDefinition.Stage(Phase.FIRING, firingLength, cameraFor(Phase.FIRING), ""));
        stages.add(new WeaponDefinition.Stage(Phase.ATMOSPHERIC_ENTRY, durationTicks/20, cameraFor(Phase.ATMOSPHERIC_ENTRY), ""));
        int impactTick = stages.stream().mapToInt(WeaponDefinition.Stage::ticks).sum();
        stages.add(new WeaponDefinition.Stage(Phase.IMPACT, durationTicks/40, cameraFor(Phase.IMPACT), ""));
        stages.add(new WeaponDefinition.Stage(Phase.DEVASTATION_WAVE_1, durationTicks/30, cameraFor(Phase.DEVASTATION_WAVE_1), ""));
        stages.add(new WeaponDefinition.Stage(Phase.DEVASTATION_WAVE_2, durationTicks/30, cameraFor(Phase.DEVASTATION_WAVE_2), ""));
        int remaining = Math.max(21, durationTicks-stages.stream().mapToInt(WeaponDefinition.Stage::ticks).sum());
        // Split unusually long aftermaths into legal bounded stages by rejecting overlong profiles at apply time.
        stages.add(new WeaponDefinition.Stage(Phase.AFTERMATH, remaining-20, cameraFor(Phase.AFTERMATH), ""));
        stages.add(new WeaponDefinition.Stage(Phase.COOLDOWN, 20, "none", ""));
        var cues = new ArrayList<WeaponDefinition.Cue>();
        for (int i=0; i<subimpacts; i++) {
            double angle=i*Math.PI*2/Math.max(1,subimpacts), distance=radius*(i%2==0?.65:.92);
            int launch=firingStart+i*Math.max(1,(firingLength-31)/Math.max(1,subimpacts));
            cues.add(new WeaponDefinition.Cue(launch,"projectile",Math.cos(angle)*distance,Math.sin(angle)*distance,.22));
            cues.add(new WeaponDefinition.Cue(launch+30,"impact",Math.cos(angle)*distance,Math.sin(angle)*distance,.22));
        }
        if(weapon.equals("glassfall"))cues.add(new WeaponDefinition.Cue(impactTick-30,"projectile",0,0,1));
        cues.add(new WeaponDefinition.Cue(impactTick,"impact",0,0,1));
        for (int i=0;i<rings.size();i++) {
            Ring ring=rings.get(i);
            cues.add(new WeaponDefinition.Cue(impactTick+durationTicks/40+1+i*Math.max(1,(durationTicks/30)*2/Math.max(1,rings.size())),"impact",0,0,
                    ring.outerScale(),ring.innerScale(),ring.depthScale(),"ring",i+1));
        }
        cues.sort(java.util.Comparator.comparingInt(WeaponDefinition.Cue::tick));
        var old = base.impact();
        var impact = new WeaponDefinition.Impact(weapon.equals("singularity")?"ellipsoid":"basin", radius,depth,
                weapon.equals("helios")?Math.min(.04,old.irregularity()):old.irregularity(),old.damage(),old.knockback(),old.scorch());
        var colossalSounds = colossalSounds();
        stages.replaceAll(stage->new WeaponDefinition.Stage(stage.phase(),stage.ticks(),stage.camera(),
                colossalSounds.getOrDefault(stage.phase(),base.phases().stream()
                .filter(original->original.phase()==stage.phase()).map(WeaponDefinition.Stage::sound).findFirst().orElse(""))));
        return new WeaponDefinition(base.id(),base.name(),base.behavior(),base.targeting(),stages,cues,impact,base.presentation(),base.cooldownTicks());
    }
    /**
     * Part 3 Helios pacing, as per-mille-of-3600 weights so every operator duration keeps the same dramatic shape:
     * dread (15 s at 180 s), the outward voyage (37 s), reveal and deployment (30 s), containment and charge (26 s),
     * discharge and descent (17 s), impact and both devastation passes (26 s), and an aftermath that receives
     * everything left (about 28 s). The previous uniform split left a two-second aftermath.
     */
    static final Object[][] HELIOS_PACING = {
            {Phase.ACQUIRING, 100}, {Phase.TARGET_CONFIRMATION, 100}, {Phase.PRELUDE, 100},
            {Phase.ASCENT, 140}, {Phase.EXOATMOSPHERIC_TRANSITION, 80}, {Phase.PLANETARY_VIEW, 140},
            {Phase.SOLAR_SYSTEM_VIEW, 120}, {Phase.GALACTIC_VIEW, 140}, {Phase.COSMIC_BOUNDARY_VIEW, 120},
            {Phase.MOTHERSHIP_REVEAL, 240}, {Phase.DEPLOYMENT, 360}, {Phase.CHARGING, 360}, {Phase.ORBITAL_ALIGNMENT, 160},
            {Phase.FIRING, 200}, {Phase.ATMOSPHERIC_ENTRY, 140}, {Phase.IMPACT, 120},
            {Phase.DEVASTATION_WAVE_1, 200}, {Phase.DEVASTATION_WAVE_2, 200}};
    private WeaponDefinition heliosStages(WeaponDefinition base) {
        var stages = new ArrayList<WeaponDefinition.Stage>();
        int impactTick = 0, impactLength = 0, waveLength = 0;
        for (Object[] entry : HELIOS_PACING) {
            Phase phase = (Phase) entry[0];
            int ticks = Math.max(1, (int) Math.round(durationTicks * (Integer) entry[1] / 3600.0));
            if (phase == Phase.IMPACT) { impactTick = stages.stream().mapToInt(WeaponDefinition.Stage::ticks).sum(); impactLength = ticks; }
            if (phase == Phase.DEVASTATION_WAVE_1) waveLength = ticks;
            stages.add(new WeaponDefinition.Stage(phase, ticks, cameraFor(phase), ""));
        }
        int remaining = Math.max(21, durationTicks - stages.stream().mapToInt(WeaponDefinition.Stage::ticks).sum());
        stages.add(new WeaponDefinition.Stage(Phase.AFTERMATH, remaining - 20, cameraFor(Phase.AFTERMATH), ""));
        stages.add(new WeaponDefinition.Stage(Phase.COOLDOWN, 20, "none", ""));
        var cues = new ArrayList<WeaponDefinition.Cue>();
        cues.add(new WeaponDefinition.Cue(impactTick, "impact", 0, 0, 1));
        // Each annular devastation pass starts with its own wave stage.
        for (int i = 0; i < rings.size(); i++) {
            Ring ring = rings.get(i);
            cues.add(new WeaponDefinition.Cue(impactTick + impactLength + 1 + i * waveLength, "impact", 0, 0,
                    ring.outerScale(), ring.innerScale(), ring.depthScale(), "ring", i + 1));
        }
        cues.sort(java.util.Comparator.comparingInt(WeaponDefinition.Cue::tick));
        var old = base.impact();
        var impact = new WeaponDefinition.Impact("basin", radius, depth, Math.min(.04, old.irregularity()), old.damage(), old.knockback(), old.scorch());
        var colossalSounds = colossalSounds();
        stages.replaceAll(stage -> new WeaponDefinition.Stage(stage.phase(), stage.ticks(), stage.camera(),
                colossalSounds.getOrDefault(stage.phase(), base.phases().stream()
                        .filter(original -> original.phase() == stage.phase()).map(WeaponDefinition.Stage::sound).findFirst().orElse(""))));
        return new WeaponDefinition(base.id(), base.name(), base.behavior(), base.targeting(), stages, cues, impact, base.presentation(), base.cooldownTicks());
    }
    public WeaponDefinition apply(WeaponDefinition base,double multiplier) {
        if(tier==ScaleTier.STANDARD)return base;
        var generated=apply(base);
        var scaled=generated.scaled(weapon.equals("helios")&&tier==ScaleTier.APOCALYPSE?Math.max(1800.0/generated.duration(),multiplier):multiplier);
        if(weapon.equals("helios")&&tier==ScaleTier.APOCALYPSE&&scaled.duration()<1800) {
            // Per-stage integer rounding can land one or more ticks below the global 90-second floor.
            var stages=new ArrayList<>(scaled.phases());int deficit=1800-scaled.duration();
            for(int i=0;i<stages.size();i++)if(stages.get(i).phase()==Phase.AFTERMATH) {
                var stage=stages.get(i);stages.set(i,new WeaponDefinition.Stage(stage.phase(),stage.ticks()+deficit,stage.camera(),stage.sound()));break;
            }
            scaled=new WeaponDefinition(scaled.id(),scaled.name(),scaled.behavior(),scaled.targeting(),stages,scaled.events(),scaled.impact(),scaled.presentation(),scaled.cooldownTicks());
        }
        return scaled;
    }
}
