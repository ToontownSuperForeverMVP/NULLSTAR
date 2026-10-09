package com.nullstar.client.camera;

import com.nullstar.client.ClientStrikeState;
import com.nullstar.core.Phase;
import com.nullstar.network.StatePayload;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class CinematicCameraTest {
    private final UUID owner=UUID.randomUUID(),id=UUID.randomUUID();
    private final CinematicCamera camera=new CinematicCamera();
    private final ClientStrikeState state=new ClientStrikeState();
    private StatePayload snapshot(UUID strike,UUID user,String preset) {
        return new StatePayload(strike,user,"minecraft:overworld","helios","HELIOS",Phase.CHARGING,20,0,100,20,
                new BlockPos(0,64,0),20,13,42,false,0,0xffdd33,72,1,preset,false);
    }
    private CinematicCamera.Pose sample(double tick,boolean alive,boolean enabled) {
        return camera.sample(Vec3.ZERO,0,0,90,tick,state,owner,alive,enabled,0,p->true);
    }
    @Test void blendUsesShortestYawArcAndEaseHasBoundedEndpoints() {
        var a=new CinematicCamera.Pose(Vec3.ZERO,179,10,90);
        var b=new CinematicCamera.Pose(new Vec3(10,20,30),-179,20,60);
        assertEquals(a,CinematicCamera.blend(a,b,0));
        assertEquals(180,CinematicCamera.blend(a,b,.5).yaw());
        assertEquals(0,CinematicCamera.ease(-1));assertEquals(1,CinematicCamera.ease(2));assertEquals(.5,CinematicCamera.ease(.5));
        assertEquals(-540,CinematicCamera.blend(new CinematicCamera.Pose(Vec3.ZERO,-539,0,90),new CinematicCamera.Pose(Vec3.ZERO,179,0,90),.5).yaw());
    }
    @Test void releaseHandsBackToVanillaWithinTwelveTicks() {
        state.snapshot(snapshot(id,owner,"orbital"),0);
        assertNotNull(sample(1,true,true));assertTrue(camera.applied());
        var start=sample(2,true,false);var halfway=sample(8,true,false);
        assertTrue(halfway.position().length()<start.position().length());
        assertNull(sample(14,true,false));assertFalse(camera.applied());
        assertNotNull(sample(15,true,true));assertNull(sample(16,false,true));assertFalse(camera.applied());
    }
    @Test void skipIsOwnerScopedAndNewStrikesRemainEligible() {
        state.snapshot(snapshot(id,owner,"beam"),0);
        var otherId=UUID.randomUUID();var other=UUID.randomUUID();state.snapshot(snapshot(otherId,other,"beam"),0);
        camera.skip(state,owner);assertNull(sample(1,true,true));
        assertNotNull(camera.sample(Vec3.ZERO,0,0,90,2,state,other,true,true,0,p->true));
        camera.reset();camera.skip(state,owner);state.clear();state.snapshot(snapshot(UUID.randomUUID(),owner,"impact"),3);
        assertNotNull(sample(4,true,true));
    }
    @Test void presetsStayFiniteAndUnloadedFramesAreRejected() {
        for(String preset:new String[]{"orbital","beam","impact"}) {
            camera.reset();state.clear();state.snapshot(snapshot(id,owner,preset),0);
            var pose=sample(1,true,true);assertNotNull(pose);
            assertTrue(Double.isFinite(pose.position().length()));assertTrue(Float.isFinite(pose.yaw()));
            assertTrue(pose.fov()>0&&pose.fov()<180);
            camera.reset();assertNull(camera.sample(Vec3.ZERO,0,0,90,1,state,owner,true,true,0,p->false));
        }
    }
}
