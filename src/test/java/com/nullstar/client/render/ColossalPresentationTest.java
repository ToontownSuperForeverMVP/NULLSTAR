// Copyright (C) 2026 ToontownSuperForeverMVP and NULLSTAR contributors
// SPDX-License-Identifier: GPL-3.0-only

package com.nullstar.client.render;

import com.nullstar.client.ClientStrikeState;
import com.nullstar.client.camera.CameraScripts;
import com.nullstar.client.camera.CinematicCamera;
import com.nullstar.core.CinematicScene;
import com.nullstar.core.ColossalMetadata;
import com.nullstar.core.Phase;
import com.nullstar.core.ScaleCatalog;
import com.nullstar.core.ScaleProfile;
import com.nullstar.core.ScaleTier;
import com.nullstar.core.WeaponCatalog;
import com.nullstar.core.WeaponDefinition;
import com.nullstar.network.StatePayload;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Part 2.5B presentation contracts: the colossal magnitudes the client derives from the authoritative descriptors,
 * the cosmic act reaching through the discharge, and every camera preset a generated profile can select existing
 * in the shot vocabulary.
 */
class ColossalPresentationTest {
    private static WeaponCatalog weapons() throws Exception {return new WeaponCatalog(Path.of("build/test-overrides"));}

    /** The authoritative Helios profile for a tier, so the presentation magnitudes are read from real operator data. */
    private static ScaleProfile helios(ScaleTier tier) {
        return ScaleCatalog.defaults().stream().filter(p->p.weapon().equals("helios")&&p.tier()==tier).findFirst().orElseThrow();
    }
    private static ColossalMetadata metadata(ScaleProfile profile) {
        return new ColossalMetadata(profile.tier(),profile.tier()==ScaleTier.APOCALYPSE?CinematicScene.STELLAR_MOTHERSHIP:CinematicScene.PLANET,
                profile.cinematicScale(),profile.structureScale(),profile.beamWidth(),profile.shockwaveRadius(),profile.secondaryRadius(),
                profile.blastRadius(),profile.entityRadius(),profile.script(),profile.anchor(),profile.alignmentNodes(),
                profile.subimpacts(),profile.radius(),profile.waves(),0,profile.durationTicks(),false);
    }
    private static StrikeView view(ScaleTier tier, Phase phase, int radius, int depth, double progress) {
        var metadata=tier==ScaleTier.STANDARD?ColossalMetadata.standard(radius):metadata(helios(tier));
        var payload=new StatePayload(UUID.randomUUID(),UUID.randomUUID(),"minecraft:overworld","helios","HELIOS",phase,400,
                (int)(progress*100),100,900,new BlockPos(0,80,0),radius,depth,7,false,0,0xffaa00,72,1,"none",false,metadata);
        var strike=new ClientStrikeState.Strike(payload,0);
        return new StrikeView(strike,payload,phase,progress,400,Vec3.atBottomCenterOf(new BlockPos(0,80,0)).add(0,1,0),
                radius,depth,72,0xffaa00,7,true,List.of(),400);
    }
    @Test void standardKeepsThePart2MagnitudesExactly() {
        var view=view(ScaleTier.STANDARD,Phase.FIRING,20,13,.5);
        assertEquals(0,PresentationScale.colossal(view),1e-9);
        assertEquals(1,PresentationScale.structure(view),1e-6);
        assertEquals(1.7f,PresentationScale.beamRadius(view),1e-6);
        assertEquals(72,PresentationScale.altitude(view),1e-6);
        assertEquals(60,PresentationScale.shockReach(view),1e-6);
        assertEquals(20,PresentationScale.secondaryReach(view),1e-6);
        assertEquals(3,PresentationScale.nodes(view));
    }

    @Test void colossalMagnitudesGrowWithTheAuthoritativeDescriptors() {
        ScaleProfile titanicProfile=helios(ScaleTier.TITANIC);
        ScaleProfile apocalypseProfile=helios(ScaleTier.APOCALYPSE);
        var titanic=view(ScaleTier.TITANIC,Phase.FIRING,titanicProfile.radius(),titanicProfile.depth(),.5);
        assertEquals(Math.log10(titanicProfile.cinematicScale())/9,PresentationScale.colossal(titanic),1e-6);
        assertEquals(5/9.0,PresentationScale.colossal(titanic),1e-6);
        assertEquals(1+3.4*(5/9.0),PresentationScale.structure(titanic),1e-4);
        assertEquals(72*(1+3.6*(5/9.0)),PresentationScale.altitude(titanic),1e-4);
        assertEquals(1.7+10*.55,PresentationScale.beamRadius(titanic),1e-4);
        assertEquals(512,PresentationScale.shockReach(titanic),1e-4);
        assertEquals(128,titanicProfile.radius());assertEquals(500,apocalypseProfile.radius());assertEquals(48,apocalypseProfile.beamWidth(),1e-9);
        var apocalypse=view(ScaleTier.APOCALYPSE,Phase.FIRING,apocalypseProfile.radius(),apocalypseProfile.depth(),.5);
        assertEquals(1,PresentationScale.colossal(apocalypse),1e-9);
        assertEquals(4.4f,PresentationScale.structure(apocalypse),1e-4);
        assertEquals(72*4.6f,PresentationScale.altitude(apocalypse),1e-3);
        assertEquals(27f,PresentationScale.beamRadius(apocalypse),1e-3);
        assertEquals(2000,PresentationScale.shockReach(apocalypse),1e-4);
        // 650 and 2000 are the documented APOCALYPSE ring/reach ceilings, taken from the profile's own rings.
        assertEquals(650,apocalypseProfile.secondaryRadius(),1e-9);
        assertEquals(650,PresentationScale.secondaryReach(apocalypse),1e-4);
        assertEquals(12,PresentationScale.nodes(apocalypse));
        // The astronomical descriptors must never leak into a raw world magnitude.
        assertTrue(PresentationScale.altitude(apocalypse)<1000&&PresentationScale.beamRadius(apocalypse)<64);
    }

    @Test void colossalHeliosHoldsTheCosmicActThroughTheDischarge() {
        assertEquals(CinematicScene.STELLAR_MOTHERSHIP,CinematicScene.forPhase(Phase.CHARGING,"helios",ScaleTier.APOCALYPSE));
        assertEquals(CinematicScene.STELLAR_MOTHERSHIP,CinematicScene.forPhase(Phase.ORBITAL_ALIGNMENT,"helios",ScaleTier.TITANIC));
        assertEquals(CinematicScene.STELLAR_MOTHERSHIP,CinematicScene.forPhase(Phase.FIRING,"helios",ScaleTier.APOCALYPSE));
        assertEquals(CinematicScene.WORLD,CinematicScene.forPhase(Phase.CHARGING,"helios",ScaleTier.STANDARD));
        // No other weapon and no standard event may be dragged into the virtual scenes.
        assertEquals(CinematicScene.WORLD,CinematicScene.forPhase(Phase.CHARGING,"singularity",ScaleTier.APOCALYPSE));
        assertEquals(CinematicScene.WORLD,CinematicScene.forPhase(Phase.FIRING,"glassfall",ScaleTier.APOCALYPSE));
        assertEquals(CinematicScene.WORLD,CinematicScene.forPhase(Phase.ATMOSPHERIC_ENTRY,"helios",ScaleTier.APOCALYPSE));
        assertEquals(CinematicScene.PLANET,CinematicScene.forPhase(Phase.PLANETARY_VIEW,"helios",ScaleTier.APOCALYPSE));
    }

    @Test void colossalTimelineSelectsTheCosmicActAndTheDevastationSweeps() throws Exception {
        var apocalypse=ScaleCatalog.defaults().stream()
                .filter(p->p.weapon().equals("helios")&&p.tier()==ScaleTier.APOCALYPSE).findFirst().orElseThrow();
        var def=apocalypse.apply(weapons().get("helios"));
        assertEquals(3600,def.duration());
        assertEquals("cosmic_montage",camera(def,Phase.CHARGING));
        assertEquals("cosmic_montage",camera(def,Phase.ORBITAL_ALIGNMENT));
        assertEquals("cosmic_montage",camera(def,Phase.FIRING));
        assertEquals("helios_pursuit",camera(def,Phase.ATMOSPHERIC_ENTRY));
        assertEquals("helios_annihilation",camera(def,Phase.IMPACT));
        assertEquals("helios_pressure",camera(def,Phase.DEVASTATION_WAVE_1));
        assertEquals("helios_crater",camera(def,Phase.DEVASTATION_WAVE_2));
        assertEquals("helios_sweep",camera(def,Phase.AFTERMATH));
        // Part 3 long-form beds: one per phase group, continuation phases silent, every bed a registered event.
        assertEquals("nullstar:cosmic.crucible",sound(def,Phase.CHARGING));
        assertEquals("nullstar:cosmic.lance",sound(def,Phase.FIRING));
        assertEquals("nullstar:cosmic.invoke",sound(def,Phase.IMPACT));
        assertEquals("nullstar:helios.ruin",sound(def,Phase.AFTERMATH));
        assertEquals("",sound(def,Phase.SOLAR_SYSTEM_VIEW));
        var registered=java.util.Set.of(com.nullstar.NullstarSounds.IDS);
        for(var w:java.util.List.of("helios","singularity","glassfall"))
            for(var tier:java.util.List.of(ScaleTier.TITANIC,ScaleTier.APOCALYPSE)) {
                var profile=ScaleCatalog.defaults().stream().filter(p->p.weapon().equals(w)&&p.tier()==tier).findFirst().orElseThrow();
                for(var stage:profile.apply(weapons().get(w)).phases())
                    if(!stage.sound().isEmpty())assertTrue(registered.contains(stage.sound().replace("nullstar:","")),stage.sound());
            }
        // STANDARD keeps the bundled authoring untouched.
        var standard=weapons().get("helios");
        assertEquals("helios_discharge",camera(standard,Phase.FIRING));
        assertEquals("nullstar:helios.fire",sound(standard,Phase.FIRING));
        assertEquals("helios_impact",camera(standard,Phase.IMPACT));
    }

    @Test void everyGeneratedCameraNameIsRegistered() throws Exception {
        var camera=new CinematicCamera();
        CameraScripts.register(camera);
        var catalog=weapons();
        for(var profile:ScaleCatalog.defaults()) {
            var def=profile.apply(catalog.get(profile.weapon()));
            for(var stage:def.phases()) {
                if(stage.camera().equals("none"))continue;
                assertTrue(camera.has(stage.camera()),
                        () -> "Unregistered camera preset '"+stage.camera()+"' for "+profile.weapon()+" "+profile.tier());
            }
        }
    }

    private static String camera(WeaponDefinition def,Phase phase) {
        return def.phases().stream().filter(s->s.phase()==phase).map(WeaponDefinition.Stage::camera).findFirst().orElse("missing");
    }
    private static String sound(WeaponDefinition def,Phase phase) {
        return def.phases().stream().filter(s->s.phase()==phase).map(WeaponDefinition.Stage::sound).findFirst().orElse("missing");
    }
}
