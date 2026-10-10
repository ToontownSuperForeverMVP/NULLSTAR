package com.nullstar.core;

import com.nullstar.config.ServerConfig;
import com.nullstar.config.ConfigFiles;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ColossalEngineTest {
    private WeaponCatalog weapons() throws Exception {return new WeaponCatalog(Path.of("build/test-overrides"));}
    @Test void allScaleProfilesAreValidatedAndStandardIsUnchanged(@TempDir Path path) throws Exception {
        var weapons=weapons();var scales=new ScaleCatalog(path.resolve("scales.json"),weapons);
        for(var base:weapons.all()) {
            assertSame(base,scales.get(base.id(),ScaleTier.STANDARD,base).apply(base));
            for(var tier:List.of(ScaleTier.TITANIC,ScaleTier.APOCALYPSE)) {
                var profile=scales.get(base.id(),tier,base);var large=profile.apply(base);
                assertTrue(large.impact().radius()>base.impact().radius()*4);assertTrue(large.duration()>base.duration()*3);
                assertEquals(large,ConfigFiles.JSON.fromJson(ConfigFiles.JSON.toJson(large),WeaponDefinition.class));
                assertEquals(3,profile.waves());
                for(double factor:new double[]{.25,1,4})assertTrue(large.scaled(factor).events().stream().allMatch(c->c.tick()<large.scaled(factor).duration()));
            }
        }
    }
    @Test void heliosHasThousandDiameterAndNinetySecondCosmicTimeline() throws Exception {
        var profile=ScaleCatalog.defaults().get(1);var def=profile.apply(weapons().get("helios"));
        assertEquals(500,def.impact().radius());assertEquals(3600,def.duration());assertTrue(def.phases().size()>=20);
        assertTrue(def.phases().stream().anyMatch(s->s.phase()==Phase.COSMIC_BOUNDARY_VIEW));
        var timeline=new Timeline(def);int start=0;
        for(var stage:def.phases()) {
            assertEquals(stage.phase(),timeline.at(start).phase());
            assertEquals(stage.phase(),timeline.at(start+stage.ticks()-1).phase());start+=stage.ticks();
        }
        assertEquals(Phase.COMPLETE,timeline.at(start).phase());
        var cues=timeline.advance(start).cues();assertEquals(def.events().size(),cues.size());assertTrue(timeline.advance(start).cues().isEmpty());
        assertTrue(def.events().stream().filter(c->c.effect().equals("impact")).map(WeaponDefinition.Cue::wave).toList().equals(List.of(0,1,2)));
    }
    @Test void glassfallHasSixteenDistributedImpactsAndFinalCenter() throws Exception {
        var def=ScaleCatalog.defaults().get(5).apply(weapons().get("glassfall"));
        assertEquals(19,def.events().stream().filter(c->c.effect().equals("impact")).count());
        assertEquals(17,def.events().stream().filter(c->c.effect().equals("projectile")).count());
        assertTrue(def.events().stream().anyMatch(c->Math.hypot(c.x(),c.z())>300));
        for(var launch:def.events())if(launch.effect().equals("projectile"))assertTrue(def.events().stream().anyMatch(c->c.effect().equals("impact")&&c.tick()==launch.tick()+30&&c.x()==launch.x()&&c.z()==launch.z()));
    }
    @Test void privateAndPublicCapsAndInvalidNumbersFailClosed() {
        var config=new ServerConfig();assertFalse(config.apocalypseEnabled);assertTrue(config.requireColossalConfirmation);
        assertEquals(48,config.radiusCap(ScaleTier.APOCALYPSE));config.publicServerMode=false;assertEquals(768,config.radiusCap(ScaleTier.APOCALYPSE));
        config.maximumColossalDiameter=Integer.MAX_VALUE;config.maximumColossalDepth=-1;config.maximumChangedBlocks=Long.MAX_VALUE;config.maximumDevastationWaves=99;
        config.validate();assertEquals(2048,config.maximumColossalDiameter);assertEquals(1,config.maximumColossalDepth);assertEquals(1_000_000_000L,config.maximumChangedBlocks);assertEquals(8,config.maximumDevastationWaves);
        assertThrows(IllegalArgumentException.class,()->new ColossalMetadata(ScaleTier.APOCALYPSE,CinematicScene.PLANET,Double.NaN,1,2,2,500,100,100,"helios","stellar",12,0,500,3,0,2000,false));
    }
    @Test void megaGeometryNeverEscapesItsBoundsAndRingExcludesTheCore() {
        var basin=new CraterGeometry("basin",500,96,.04,42);var ellipse=new CraterGeometry("ellipsoid",320,160,.12,42);
        assertTrue(basin.removes(0,-70,0));assertFalse(basin.removes(0,-97,0));assertFalse(basin.removes(500,0,0));
        assertTrue(ellipse.removes(0,-150,0));assertFalse(ellipse.removes(321,0,0));
        var ring=new CraterGeometry("ring",650,12,0,42,540);
        assertFalse(ring.removes(539,0,0));assertTrue(ring.removes(600,-5,0));assertFalse(ring.removes(650,0,0));
        for(int i=0;i<720;i++) {
            double a=i*Math.PI/360;assertFalse(basin.removes((int)Math.round(Math.cos(a)*502),0,(int)Math.round(Math.sin(a)*502)));
        }
        assertThrows(IllegalArgumentException.class,()->new CraterGeometry("ring",500,10,0,1,500));
    }
    @Test void chunkTraversalIsCompleteAndCheckpointRestoresExactOrder() {
        var geometry=new CraterGeometry("basin",19,8,.1,42);var full=new DestructionJob(geometry,-7,29);
        List<String> expected=new ArrayList<>();while(!full.done())full.step(127,(x,y,z,r,s)->{expected.add(x+","+y+","+z+","+r);return true;});
        assertEquals(new HashSet<>(expected).size(),expected.size());
        var split=new DestructionJob(geometry,-7,29);List<String> actual=new ArrayList<>();
        split.step(373,(x,y,z,r,s)->{actual.add(x+","+y+","+z+","+r);return true;});
        var restored=DestructionJob.restore(ConfigFiles.JSON.fromJson(ConfigFiles.JSON.toJson(split.checkpoint()),DestructionJob.Checkpoint.class));
        while(!restored.done())restored.step(91,(x,y,z,r,s)->{actual.add(x+","+y+","+z+","+r);return true;});
        assertEquals(expected,actual);assertEquals(full.total(),restored.scanned());assertEquals(full.changed(),restored.changed());assertEquals(1,restored.progress());
    }
    @Test void hugeSkippedScanChargesEveryCandidateAndUsesConstantMemory() {
        var job=new DestructionJob(new CraterGeometry("basin",500,96,.04,123),123,-45);
        assertEquals(146_292_146L,job.total());
        for(int i=0;i<50;i++)assertEquals(8192,job.step(8192,(x,y,z,r,s)->fail("Unloaded cell was visited"),(x,z)->false));
        assertEquals(409600,job.scanned());assertEquals(0,job.changed());assertTrue(job.progress()>0&&job.progress()<1);
        var restored=DestructionJob.restore(job.checkpoint());assertEquals(job.scanned(),restored.scanned());
        job.cancel();assertEquals(0,job.step(8192,(x,y,z,r,s)->fail()));
    }
    @Test void corruptCursorCannotRewindAJob() {
        var job=new DestructionJob(new CraterGeometry("crater",4,3,0,1));job.step(17,(x,y,z,r,s)->false);
        var c=job.checkpoint();assertThrows(IllegalArgumentException.class,()->DestructionJob.restore(new DestructionJob.Checkpoint(c.geometry(),c.originX(),c.originZ(),c.cursor()-1,c.changed(),c.tileX(),c.tileZ(),c.x(),c.z(),c.y())));
    }
    @Test void profileOverridesRejectDuplicatesAndShortApocalypse(@TempDir Path path) throws Exception {
        var defaults=new ScaleCatalog.Data();defaults.profiles=Collections.nCopies(6,ScaleCatalog.defaults().getFirst());
        java.nio.file.Files.writeString(path.resolve("scales.json"),ConfigFiles.JSON.toJson(defaults));
        assertThrows(IllegalArgumentException.class,()->new ScaleCatalog(path.resolve("scales.json"),weapons()));
        var p=ScaleCatalog.defaults().get(1);
        assertThrows(IllegalArgumentException.class,()->new ScaleProfile(p.weapon(),p.tier(),p.radius(),p.depth(),1799,p.cinematicScale(),p.structureScale(),p.beamWidth(),p.shockwaveRadius(),p.blastRadius(),p.entityRadius(),0,p.script(),p.anchor(),p.alignmentNodes(),p.rings()));
    }
    @Test void footprintPreviewSharesExactGeometryAndNeverVisitsAWorld() throws Exception {
        var profile=ScaleCatalog.defaults().get(1);var def=profile.apply(weapons().get("helios"));
        var impact=def.events().stream().filter(c->c.effect().equals("impact")&&c.wave()==0).findFirst().orElseThrow();
        var main=CraterGeometry.forCue(def.impact(),500,96,768,42,impact);
        assertEquals(500,main.radius());assertEquals(96,main.floor());assertEquals(48,main.ceiling());
        var outer=def.events().stream().filter(c->c.wave()==2).findFirst().orElseThrow();
        var ring=CraterGeometry.forCue(def.impact(),500,96,768,42,outer);
        assertEquals(650,ring.radius());assertEquals(540,ring.innerRadius());assertFalse(ring.removes(0,0,0));
        assertNull(CraterGeometry.forCue(def.impact(),48,24,48,42,outer));
        assertEquals(0,new DestructionJob(main).scanned()); // Bounds construction alone admits and scans nothing.
    }
    @Test void cachedColumnsMatchTheOriginalContinuousGeometryMath() {
        for(String shape:List.of("crater","basin","sphere","ellipsoid","ring")) {
            var geometry=new CraterGeometry(shape,23,17,.12,123,shape.equals("ring")?15:0);
            for(int x=-24;x<=24;x+=2)for(int z=-24;z<=24;z+=2)for(int y=-25;y<=25;y++) {
                assertEquals(geometry.intensity(x,y,z)>0,geometry.column(x,z).removes(y),shape+" at "+x+","+y+","+z);
                assertEquals(geometry.shell(x,y,z),geometry.column(x,z).shell(y));
            }
        }
    }
    @Test void entireColossalBarrageFitsPublicAndPrivateSafetyEnvelopes() throws Exception {
        var profile=ScaleCatalog.defaults().get(5);var def=profile.apply(weapons().get("glassfall"));
        for(int cap:new int[]{1,2,24,48,128,400,768}) {
            int radius=Math.min(cap,profile.radius());
            for(var cue:def.events())if(cue.effect().equals("impact")) {
                var geometry=CraterGeometry.forCue(def.impact(),radius,80,cap,42,cue);if(geometry==null)continue;
                var offset=ImpactFootprint.offset(cue.x(),cue.z(),profile.radius(),radius,geometry.radius(),cap);
                assertTrue(Math.hypot(offset.x(),offset.z())+geometry.radius()<=cap,"Child crater escaped the admitted footprint");
                if(cap==768&&cue.x()!=0)assertEquals(Math.round(cue.x()),offset.x(),"Unclipped private footprint changed");
            }
        }
    }
    @Test void apocalypseHeliosCannotRoundBelowNinetySeconds() throws Exception {
        var profile=ScaleCatalog.defaults().get(1);var base=weapons().get("helios");
        for(double multiplier:new double[]{.25,.333,.899,1,1.333,4}) {
            var def=profile.apply(base,multiplier);assertTrue(def.duration()>=1800);
            assertTrue(def.events().stream().allMatch(c->c.tick()<def.duration()));
        }
        assertEquals(1800,profile.apply(base,.25).duration());
    }
}
