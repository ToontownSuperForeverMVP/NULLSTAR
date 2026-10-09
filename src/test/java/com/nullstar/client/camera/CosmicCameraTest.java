package com.nullstar.client.camera;

import com.nullstar.client.ClientStrikeState;
import com.nullstar.core.*;
import com.nullstar.network.StatePayload;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class CosmicCameraTest {
    @Test void detachedSceneNeverQueriesTerrainAndSkipRestoresGameplay() {
        var owner=UUID.randomUUID();var id=UUID.randomUUID();var state=new ClientStrikeState();var camera=new CinematicCamera();
        camera.register("cosmic_montage",ctx->null);
        var metadata=new ColossalMetadata(ScaleTier.APOCALYPSE,CinematicScene.GALAXY,1e9,1e6,48,2000,650,128,128,"helios_colossal","stellar",12,0,500,3,7,2000,false);
        state.snapshot(new StatePayload(id,owner,"minecraft:overworld","helios","HELIOS",Phase.GALACTIC_VIEW,800,10,90,100,
                new BlockPos(100,80,100),500,96,42,false,0,0xffaa00,72,1,"cosmic_montage",false,metadata),0);
        Vec3 vanilla=new Vec3(12,85,18);
        var pose=camera.sample(vanilla,31,20,90,1,state,owner,true,true,0,p->{fail("Detached camera queried a chunk");return false;},p->{fail("Detached camera probed terrain");return p;});
        assertEquals(vanilla,pose.position());assertEquals(31,pose.yaw(),.001);assertTrue(camera.detached(state.strikes().get(id)));
        camera.skip(state,owner);camera.sample(vanilla,31,20,90,2,state,owner,true,true,0,p->true);
        assertTrue(camera.virtualScene(),"The release must still exclude the self-avatar while the camera is at eye position");
        assertNull(camera.sample(vanilla,31,20,90,15,state,owner,true,true,0,p->true));assertFalse(camera.applied());assertEquals(1,state.strikes().size());
        camera.reset();assertNull(camera.sample(vanilla,31,20,90,16,state,UUID.randomUUID(),true,true,0,p->true));
        assertNull(camera.sample(vanilla,31,20,90,17,state,owner,true,false,0,p->true));
        state.tick(101,"minecraft:overworld");assertTrue(state.strikes().isEmpty());
    }
}
