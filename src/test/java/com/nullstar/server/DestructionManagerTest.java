package com.nullstar.server;

import com.nullstar.core.CraterGeometry;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.core.registries.Registries;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class DestructionManagerTest {
    @Test void reservationsCannotBeOverwrittenOrOvercommitted() {
        var manager=new DestructionManager();var id=UUID.randomUUID();
        assertTrue(manager.reserve(id,5,5));
        assertFalse(manager.reserve(id,0,5));
        assertFalse(manager.reserve(UUID.randomUUID(),1,5));
        manager.releaseReservation(id);
        assertTrue(manager.reserve(UUID.randomUUID(),1,5));
        assertFalse(manager.reserve(UUID.randomUUID(),Integer.MAX_VALUE,5));
        manager.cancel(id);assertTrue(manager.reserve(id,4,5));
    }
    @Test void exactlyReservedJobsCanBeEnqueuedAndCancellationFreesAllSlots() {
        var manager=new DestructionManager();var id=UUID.randomUUID();var owner=UUID.randomUUID();
        assertTrue(manager.reserve(id,2,2));
        var dimension=ResourceKey.create(Registries.DIMENSION,Identifier.parse("minecraft:overworld"));
        for(int i=0;i<2;i++)manager.enqueue(id,owner,dimension,BlockPos.ZERO,"helios",false,new CraterGeometry("crater",2,2,0,i));
        assertEquals(2,manager.queued());assertEquals(2,manager.progress(id).active());
        assertThrows(IllegalStateException.class,()->manager.enqueue(id,owner,dimension,BlockPos.ZERO,"helios",false,new CraterGeometry("crater",2,2,0,1)));
        assertFalse(manager.reserve(UUID.randomUUID(),1,2));
        manager.cancel(id);assertEquals(0,manager.queued());assertEquals(0,manager.progress(id).active());
        assertTrue(manager.reserve(UUID.randomUUID(),2,2));manager.clear();assertEquals(0,manager.lastTickOperations());
    }
}
