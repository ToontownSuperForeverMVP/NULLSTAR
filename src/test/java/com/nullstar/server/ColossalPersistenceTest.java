package com.nullstar.server;

import com.nullstar.config.ServerConfig;
import com.nullstar.core.CraterGeometry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class ColossalPersistenceTest {
    private net.minecraft.resources.ResourceKey<Level> dimension(){return net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,net.minecraft.resources.Identifier.parse("minecraft:overworld"));}
    @Test void managerCheckpointRoundTripsJobsAndHonorsNewSafetyPolicy(@TempDir Path path) throws Exception {
        var original=new DestructionManager();var strike=UUID.randomUUID();original.reserve(strike,3,3);
        var primary=new CraterGeometry("basin",500,96,.04,123);var ring=new CraterGeometry("ring",650,12,0,123,540);
        original.plan(strike,new com.nullstar.core.DestructionJob(primary).total()+new com.nullstar.core.DestructionJob(ring).total());
        original.enqueue(strike,UUID.randomUUID(),dimension(),new BlockPos(-31,80,26),"helios",true,primary,0,com.nullstar.core.ScaleTier.APOCALYPSE);
        original.enqueue(strike,UUID.randomUUID(),dimension(),new BlockPos(-31,80,26),"helios",true,ring,1,com.nullstar.core.ScaleTier.APOCALYPSE);
        var file=path.resolve("jobs.json");original.save(file);assertTrue(Files.size(file)<8192);
        var restored=new DestructionManager();var config=new ServerConfig();config.publicServerMode=false;config.apocalypseEnabled=true;
        restored.load(file,config);assertEquals(original.jobs().size(),restored.jobs().size());assertEquals(original.diagnostics(strike).total(),restored.diagnostics(strike).total());
        assertTrue(restored.jobs().stream().allMatch(DestructionManager.JobStatus::recovered));
        assertFalse(restored.reserve(UUID.randomUUID(),31,32));
        config.publicServerMode=true;assertThrows(java.io.IOException.class,()->restored.load(file,config));assertEquals(2,restored.queued());
        restored.cancel(strike,"operator abort");assertEquals("operator abort",restored.diagnostics(strike).cause());restored.save(file);
        var empty=new DestructionManager();empty.load(file,config);assertEquals(0,empty.queued());
    }
    @Test void partialOrUnknownCheckpointCannotReplaceAnExistingQueue(@TempDir Path path) throws Exception {
        var manager=new DestructionManager();var id=UUID.randomUUID();manager.reserve(id,1,1);
        manager.enqueue(id,UUID.randomUUID(),dimension(),BlockPos.ZERO,"helios",true,new CraterGeometry("crater",2,2,0,1));
        var file=path.resolve("jobs.json");Files.writeString(file,"{\"version\":99,\"jobs\":[],\"statistics\":[]}");
        assertThrows(java.io.IOException.class,()->manager.load(file,new ServerConfig()));assertEquals(1,manager.queued());
    }
}
