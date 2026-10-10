package com.nullstar.core;

import org.junit.jupiter.api.Test;
import java.util.HashSet;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

/** Part 3: unloaded tiles are remembered and excavated once loaded, with every probe and cell still charged. */
class DestructionDeferralTest {
    private static CraterGeometry geometry(){return new CraterGeometry("basin",40,10,.04,99);}
    /** A tiny world: removed cells are remembered, so a re-visit changes nothing, exactly like real air. */
    private static final class World {
        final Set<Long> removed=new HashSet<>();
        boolean visit(int x,int y,int z,boolean remove,boolean scorch){return remove&&removed.add((((long)x&0xffff)<<32)|(((long)y&0xffff)<<16)|((long)z&0xffff));}
    }
    /** Chunk-aligned like the real loaded test: world chunks west of x=0 are loaded (job origin x=3). */
    private static boolean westLoaded(int x,int z){return ((x+3)>>4)<0;}

    @Test void deferredTilesAreExcavatedOnceLoadedAndMatchAFullyLoadedRun() {
        World reference=new World();
        var full=new DestructionJob(geometry(),3,-5);
        while(!full.done())full.step(500,reference::visit);

        World world=new World();
        var job=new DestructionJob(geometry(),3,-5).deferUnloaded();
        int guard=0;
        while(!job.mainPassDone()&&guard++<100000)assertTrue(job.step(500,world::visit,DestructionDeferralTest::westLoaded)<=500);
        assertTrue(job.mainPassDone());
        assertTrue(job.deferredTiles()>0,"east half deferred");
        assertFalse(job.done(),"a deferring job is not done while regions wait");
        assertTrue(world.removed.size()<reference.removed.size());
        // Still unloaded: a waiting job only spends bounded probes per step.
        int waiting=job.step(500,world::visit,DestructionDeferralTest::westLoaded);
        assertTrue(waiting>0&&waiting<=DestructionJob.PROBES_PER_STEP,"probes "+waiting);
        // The east half loads: the job finishes the crater.
        guard=0;
        while(!job.done()&&guard++<100000)assertTrue(job.step(500,world::visit,(x,z)->true)<=500);
        assertTrue(job.done());
        assertEquals(0,job.deferredTiles());
        assertEquals(reference.removed,world.removed,"deferred excavation equals the fully loaded crater");
        assertEquals(job.total(),job.scanned(),"the planned total is unchanged by replay");
    }

    @Test void legacyJobsWithoutDeferralStillSkipForGood() {
        World world=new World();
        var job=new DestructionJob(geometry(),3,0);
        while(!job.done())job.step(700,world::visit,DestructionDeferralTest::westLoaded);
        assertEquals(0,job.deferredTiles());
        assertTrue(job.done());
    }

    @Test void abandoningAndCheckpointRestoreAreBounded() {
        World world=new World();
        var job=new DestructionJob(geometry(),3,0).deferUnloaded();
        while(!job.mainPassDone())job.step(600,world::visit,DestructionDeferralTest::westLoaded);
        int pending=job.deferredTiles();
        var checkpoint=job.checkpoint();
        assertNotNull(checkpoint.deferred());
        var restored=DestructionJob.restore(checkpoint);
        assertEquals(pending,restored.deferredTiles(),"deferred tiles survive a restart");
        assertEquals(pending,restored.abandonDeferred());
        assertTrue(restored.done());
        // A pre-Part-3 checkpoint (no deferred field) restores as a plain job.
        var legacy=new DestructionJob.Checkpoint(checkpoint.geometry(),checkpoint.originX(),checkpoint.originZ(),checkpoint.cursor(),checkpoint.changed(),
                checkpoint.tileX(),checkpoint.tileZ(),checkpoint.x(),checkpoint.z(),checkpoint.y());
        assertFalse(DestructionJob.restore(legacy).deferring());
        // Out-of-range bits are rejected.
        var hostile=new DestructionJob.Checkpoint(checkpoint.geometry(),checkpoint.originX(),checkpoint.originZ(),checkpoint.cursor(),checkpoint.changed(),
                checkpoint.tileX(),checkpoint.tileZ(),checkpoint.x(),checkpoint.z(),checkpoint.y(),new long[]{0,0,0,0,0,0,0,0,1L<<62});
        assertThrows(IllegalArgumentException.class,()->DestructionJob.restore(hostile));
    }
}
