package com.nullstar.core;

import com.nullstar.config.ConfigFiles;
import com.nullstar.config.ServerConfig;
import org.junit.jupiter.api.Test;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class EngineTest {
    private WeaponCatalog catalog() throws Exception { return new WeaponCatalog(Path.of("build/test-overrides")); }
    @Test void bundledDefinitionsHaveCompleteLifecyclesAndRoundTrip() throws Exception {
        for (var def : catalog().all()) {
            assertEquals(def, ConfigFiles.JSON.fromJson(ConfigFiles.JSON.toJson(def), WeaponDefinition.class));
            assertEquals(Phase.COMPLETE, new Timeline(def).at(def.duration()).phase());
            assertTrue(def.events().stream().anyMatch(cue -> cue.effect().equals("impact")));
            for (double factor : new double[]{.25, .333, 1, 1.75, 4}) {
                var scaled = def.scaled(factor);
                assertTrue(scaled.events().stream().allMatch(e -> e.tick() < scaled.duration()));
            }
        }
    }
    @Test void timelineDeliversExactlyOnceAndCannotRewind() throws Exception {
        var def = catalog().get("glassfall"); var timeline = new Timeline(def);
        assertEquals(Phase.ACQUIRING, timeline.advance(0).state().phase());
        var result = timeline.advance(261);
        assertEquals(def.events().size(), result.cues().size());
        assertTrue(timeline.advance(261).cues().isEmpty());
        assertThrows(IllegalArgumentException.class, () -> timeline.advance(260));
        assertEquals(Phase.COMPLETE, timeline.advance(def.duration()).state().phase());
    }
    @Test void exactPhaseBoundariesAndCancellation() throws Exception {
        var def = catalog().get("helios"); var timeline = new Timeline(def);
        assertEquals(Phase.ACQUIRING, timeline.at(19).phase());
        assertEquals(Phase.CHARGING, timeline.at(20).phase());
        assertEquals(0, timeline.at(20).phaseAge());
        timeline.advance(0); timeline.cancel();
        var cancelled = timeline.advance(1000);
        assertEquals(Phase.CANCELLED, cancelled.state().phase()); assertTrue(cancelled.cues().isEmpty());
    }
    @Test void geometryIsSeededBoundedAndRounded() {
        var a = new CraterGeometry("crater", 20, 13, .15, 42);
        var b = new CraterGeometry("crater", 20, 13, .15, 42);
        assertTrue(a.removes(0, -12, 0)); assertFalse(a.removes(0, -14, 0));
        assertFalse(a.removes(20, 0, 0)); assertFalse(a.removes(19, -12, 0));
        assertTrue(a.shell(0, -14, 0));
        for (int x = -22; x <= 22; x++) for (int z = -22; z <= 22; z++) {
            assertEquals(a.boundary(x,z), b.boundary(x,z));
            assertTrue(a.boundary(x,z) <= 20 && a.boundary(x,z) >= 17);
        }
        assertNotEquals(a.boundary(4,7), new CraterGeometry("crater",20,13,.15,43).boundary(4,7));
    }
    @Test void spheresAndEllipsoidsHaveCorrectAxes() {
        var sphere = new CraterGeometry("sphere",10,4,0,1);
        assertTrue(sphere.removes(0,9,0)); assertFalse(sphere.removes(0,10,0));
        var ellipsoid = new CraterGeometry("ellipsoid",10,4,0,1);
        assertTrue(ellipsoid.removes(9,0,0)); assertTrue(ellipsoid.removes(0,3,0));
        assertFalse(ellipsoid.removes(0,4,0));
    }
    @Test void unsafeGeometryRejected() {
        assertThrows(IllegalArgumentException.class, () -> new CraterGeometry("crater",49,10,0,1));
        assertThrows(IllegalArgumentException.class, () -> new CraterGeometry("crater",10,49,0,1));
        assertThrows(IllegalArgumentException.class, () -> new CraterGeometry("crater",10,10,Double.NaN,1));
    }
    @Test void scanningBudgetIncludesEmptySpaceAndCancellationStopsAllWork() {
        var job = new DestructionJob(new CraterGeometry("crater",8,5,.1,1));
        var visits = new AtomicInteger();
        assertEquals(17, job.step(17, (x,y,z,r,s) -> {visits.incrementAndGet();return false;}));
        assertEquals(17, job.scanned()); assertTrue(visits.get() <= 17);
        assertEquals(0, job.step(0, (x,y,z,r,s) -> fail("No work allowed")));
        job.cancel(); assertTrue(job.done());
        assertEquals(0, job.step(1000, (x,y,z,r,s) -> fail("Cancelled visitor invoked")));
        assertTrue(job.progress() < 1);
    }
    @Test void queueCompletesWithoutDuplicateCoordinates() {
        var job = new DestructionJob(new CraterGeometry("crater",6,5,0,1));
        var positions = new java.util.HashSet<String>();
        while (!job.done()) assertTrue(job.step(31, (x,y,z,r,s) -> {assertTrue(positions.add(x+":"+y+":"+z));return true;}) <= 31);
        assertEquals(job.total(),job.scanned()); assertEquals(1,job.progress()); assertEquals(positions.size(),job.changed());
    }
    @Test void configurationClampsInvalidAndNonFiniteValues() {
        var cfg = new ServerConfig(); cfg.maximumRadius = Integer.MAX_VALUE; cfg.blockOperationsPerTick = -1;
        cfg.maximumJobs=10000; cfg.cinematicDurationMultiplier=Double.NaN; cfg.soundVolumeMultiplier=Double.POSITIVE_INFINITY;
        cfg.validate(); assertEquals(48,cfg.maximumRadius); assertEquals(16,cfg.blockOperationsPerTick);
        assertEquals(32,cfg.maximumJobs); assertEquals(1,cfg.cinematicDurationMultiplier); assertEquals(1,cfg.soundVolumeMultiplier);
    }
    @Test void replayGateCooldownAndReconnect() {
        var gate = new ActivationGate(); var id = UUID.randomUUID();
        assertFalse(gate.request(id,0,0)); assertTrue(gate.request(id,1,0));
        assertFalse(gate.request(id,1,10)); assertFalse(gate.request(id,2,1)); assertTrue(gate.request(id,2,4));
        gate.activate(id,4,600); assertFalse(gate.ready(id,603)); assertTrue(gate.ready(id,604));
        gate.disconnect(id); assertTrue(gate.request(id,1,10)); assertEquals(594,gate.remaining(id,10));
    }
    @Test void targetingRejectsNanInfinityAndOutOfRange() {
        assertTrue(TargetRules.withinRange(0,0,0,8,0,0,8));
        assertFalse(TargetRules.withinRange(0,0,0,8.01,0,0,8));
        assertFalse(TargetRules.withinRange(0,0,0,Double.NaN,0,0,8));
        assertFalse(TargetRules.withinRange(Double.POSITIVE_INFINITY,0,0,0,0,0,8));
        assertFalse(TargetRules.withinRange(0,0,0,0,0,0,-1));
    }
}
