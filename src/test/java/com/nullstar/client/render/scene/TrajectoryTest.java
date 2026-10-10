// Copyright (C) 2026 ToontownSuperForeverMVP and NULLSTAR contributors
// SPDX-License-Identifier: GPL-3.0-only

package com.nullstar.client.render.scene;

import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Cosmetic projectiles must start at their rail and arrive exactly on the server's authoritative impact point. */
class TrajectoryTest {
    @Test void flightEndsExactlyAtAuthoritativeImpact() {
        Vector3f start=new Vector3f(6,58,-4);
        for(float[] offset:new float[][]{{-9,-7},{8,-6},{-7,9},{9,8},{0,0}}) {
            Vec3 end=new Vec3(offset[0],1.5,offset[1]);
            for(double bend:new double[]{-5,0,5}) {
                Vector3f a=GlassfallScene.flight(start,end,0,bend,new Vector3f()),b=GlassfallScene.flight(start,end,1,bend,new Vector3f());
                assertEquals(0,a.distance(start),1e-4);
                assertEquals(end.x,b.x,1e-4);assertEquals(end.y,b.y,1e-4);assertEquals(end.z,b.z,1e-4);
            }
        }
        assertEquals(0,GlassfallScene.flightProgress(-1));assertEquals(1,GlassfallScene.flightProgress(2));
        assertTrue(GlassfallScene.flightProgress(.5)<.5,"projectiles accelerate toward the ground");
    }
    @Test void shakeSignalIsBoundedAndSegmentsScaleWithQuality() {
        for(var quality:com.nullstar.client.ClientConfig.Quality.values()) {
            int seg=Fx.segments(quality,24);
            assertTrue(seg>=6&&seg<=24);
        }
        assertTrue(Fx.segments(com.nullstar.client.ClientConfig.Quality.LOW,24)<Fx.segments(com.nullstar.client.ClientConfig.Quality.ULTRA,24));
        assertEquals(0,Fx.easeOut(0));assertEquals(1,Fx.easeOut(1),1e-6);assertEquals(1,Fx.easeOut(5),1e-6);
    }
}
