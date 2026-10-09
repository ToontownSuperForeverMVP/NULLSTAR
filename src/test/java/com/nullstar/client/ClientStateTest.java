package com.nullstar.client;

import com.nullstar.core.Phase;
import com.nullstar.network.StatePayload;
import com.nullstar.network.VisualPayload;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class ClientStateTest {
    private final UUID id=UUID.randomUUID(),owner=UUID.randomUUID();
    private StatePayload state(long serverTick,Phase phase,boolean terminal) {
        return new StatePayload(id,owner,"minecraft:overworld","helios","HELIOS ANNIHILATOR",phase,150,10,30,serverTick,
                new BlockPos(0,64,0),20,13,42,false,.5,0xffdd33,72,1,"beam",terminal);
    }
    @Test void lateObserverStartsAtAuthoritativeAgeAndRejectsOlderSnapshots() {
        var cache=new ClientStrikeState();cache.snapshot(state(150,Phase.ATMOSPHERIC_ENTRY,false),100);
        assertEquals(150,cache.strikes().get(id).age(100));assertEquals(160,cache.strikes().get(id).age(110));
        assertEquals(2.0/3,cache.strikes().get(id).phaseProgress(110),.000001);
        cache.snapshot(state(149,Phase.FIRING,false),110);
        assertEquals(Phase.ATMOSPHERIC_ENTRY,cache.strikes().get(id).state().phase());
    }
    @Test void duplicateCuesAndTerminalCleanup() {
        var cache=new ClientStrikeState();cache.snapshot(state(150,Phase.IMPACT,false),100);
        var cue=new VisualPayload(id,1,"impact",BlockPos.ZERO,20,40);cache.cue(cue,100);cache.cue(cue,101);
        assertEquals(1,cache.cues().size());cache.snapshot(state(151,Phase.COMPLETE,true),102);
        assertTrue(cache.strikes().isEmpty());assertTrue(cache.cues().isEmpty());
        cache.cue(cue,103);assertTrue(cache.cues().isEmpty());
    }
    @Test void staleConnectionsAndDimensionChangesClearEffects() {
        var cache=new ClientStrikeState();cache.snapshot(state(150,Phase.CHARGING,false),100);
        cache.tick(201,"minecraft:overworld");assertTrue(cache.strikes().isEmpty());
        cache.snapshot(state(150,Phase.CHARGING,false),300);
        cache.tick(301,"minecraft:the_nether");assertTrue(cache.strikes().isEmpty());
    }
}
