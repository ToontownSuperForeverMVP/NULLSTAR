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
        if (helios) {
            Phase[] preparation = {Phase.ACQUIRING, Phase.TARGET_CONFIRMATION, Phase.PRELUDE, Phase.ASCENT,
                    Phase.EXOATMOSPHERIC_TRANSITION, Phase.PLANETARY_VIEW, Phase.SOLAR_SYSTEM_VIEW,
                    Phase.GALACTIC_VIEW, Phase.COSMIC_BOUNDARY_VIEW, Phase.MOTHERSHIP_REVEAL, Phase.DEPLOYMENT};
            for (Phase phase : preparation) stages.add(new WeaponDefinition.Stage(phase, durationTicks/22,
                    CinematicScene.forPhase(phase)==CinematicScene.WORLD ? "helios_establish" : "cosmic_montage", ""));
        } else stages.add(new WeaponDefinition.Stage(Phase.ACQUIRING, durationTicks/12, weapon+"_"+(weapon.equals("singularity")?"form":"reveal"), ""));
        stages.add(new WeaponDefinition.Stage(Phase.CHARGING, durationTicks/8, weapon+"_"+(helios?"mechanism":weapon.equals("singularity")?"form":"lattice"), ""));
        stages.add(new WeaponDefinition.Stage(Phase.ORBITAL_ALIGNMENT, durationTicks/12, helios?"helios_mechanism":weapon.equals("singularity")?"singularity_pull":"glassfall_lattice", ""));
        int firingStart = stages.stream().mapToInt(WeaponDefinition.Stage::ticks).sum();
        int firingLength = durationTicks/8;
        stages.add(new WeaponDefinition.Stage(Phase.FIRING, firingLength, helios?"helios_discharge":weapon.equals("singularity")?"singularity_pull":"glassfall_barrage", ""));
        stages.add(new WeaponDefinition.Stage(Phase.ATMOSPHERIC_ENTRY, durationTicks/20, helios?"helios_follow":"none", ""));
        int impactTick = stages.stream().mapToInt(WeaponDefinition.Stage::ticks).sum();
        stages.add(new WeaponDefinition.Stage(Phase.IMPACT, durationTicks/40, weapon+"_"+(helios?"impact":weapon.equals("singularity")?"implode":"final"), ""));
        stages.add(new WeaponDefinition.Stage(Phase.DEVASTATION_WAVE_1, durationTicks/30, weapon+"_aftermath", ""));
        stages.add(new WeaponDefinition.Stage(Phase.DEVASTATION_WAVE_2, durationTicks/30, weapon+"_aftermath", ""));
        int remaining = Math.max(21, durationTicks-stages.stream().mapToInt(WeaponDefinition.Stage::ticks).sum());
        // Split unusually long aftermaths into legal bounded stages by rejecting overlong profiles at apply time.
        stages.add(new WeaponDefinition.Stage(Phase.AFTERMATH, remaining-20, weapon+"_aftermath", ""));
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
        stages.replaceAll(stage->new WeaponDefinition.Stage(stage.phase(),stage.ticks(),stage.camera(),base.phases().stream()
                .filter(original->original.phase()==stage.phase()).map(WeaponDefinition.Stage::sound).findFirst().orElse("")));
        return new WeaponDefinition(base.id(),base.name(),base.behavior(),base.targeting(),stages,cues,impact,base.presentation(),base.cooldownTicks());
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
