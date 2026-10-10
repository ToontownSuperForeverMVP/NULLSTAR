// Copyright (C) 2026 ToontownSuperForeverMVP and NULLSTAR contributors
// SPDX-License-Identifier: GPL-3.0-only

package com.nullstar.client.render;

import com.nullstar.client.ClientConfig;
import com.nullstar.client.ClientStrikeState;
import com.nullstar.core.Phase;
import com.nullstar.network.StatePayload;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class PresentationEngineTest {
    static StrikeView view(Phase phase,int phaseAge,int phaseDuration,long seed) {
        var state=new StatePayload(UUID.randomUUID(),UUID.randomUUID(),"minecraft:overworld","helios","HELIOS",phase,100,phaseAge,phaseDuration,100,
                new BlockPos(0,64,0),20,13,seed,false,0,0xffaa33,72,1,"none",false);
        var strike=new ClientStrikeState.Strike(state,0);
        return new StrikeView(strike,state,phase,strike.phaseProgress(0),strike.age(0),new Vec3(.5,65,.5),20,13,72,0xffaa33,seed,true,List.of(),0);
    }

    @Test void sinkBudgetDropsWholePrimitivesAndResets() {
        var sink=new VertexSink(10);
        assertTrue(sink.reserve(4));for(int i=0;i<4;i++)sink.put(0,0,0,0,0,0,0,1,0);
        assertTrue(sink.reserve(4));for(int i=0;i<4;i++)sink.put(0,0,0,0,0,0,0,1,0);
        assertFalse(sink.reserve(4),"capacity is rounded down to whole quads");
        assertEquals(8,sink.vertices());assertEquals(4,sink.dropped());
        sink.clear();assertEquals(0,sink.vertices());assertEquals(0,sink.dropped());assertTrue(sink.isEmpty());
    }

    @Test void canvasPrimitivesEmitQuadsAndRespectBudgets() {
        var canvas=new Canvas();
        canvas.begin(new Vec3(10,70,10),new Quaternionf(),1);
        canvas.origin(0,64,0);
        canvas.tube(Layer.CORE_FLOW,0,0,0,0,10,0,1,1,12,0xffffffff,0xffffffff,1,0,0,1);
        assertEquals(48,canvas.sink(Layer.CORE_FLOW).vertices());
        canvas.billboard(Layer.GLOW,0,0,0,2,0,0xffffffff);
        canvas.annulus(Layer.SHOCK,0,0,0,0,1,0,1,2,16,-1,-1,1,0,0,1);
        assertEquals(4,canvas.sink(Layer.GLOW).vertices());assertEquals(64,canvas.sink(Layer.SHOCK).vertices());
        for(int i=0;i<10000;i++)canvas.billboard(Layer.HALO,0,0,0,1,0,-1);
        assertEquals(Layer.HALO.budget,canvas.sink(Layer.HALO).vertices());
        assertTrue(canvas.droppedVertices()>0);
        for(Layer layer:Layer.values())assertEquals(0,canvas.sink(layer).vertices()%4,layer+" must never hold a partial quad");
        canvas.begin(Vec3.ZERO,new Quaternionf(),1);
        assertEquals(0,canvas.totalVertices());
    }

    @Test void glowPreferenceScalesEnergyAlphaButNotOpaqueMaterials() {
        var canvas=new Canvas();
        canvas.begin(Vec3.ZERO,new Quaternionf(),0);
        canvas.billboard(Layer.GLOW,0,0,0,1,0,0xffffffff);
        canvas.mesh(Layer.HULL_HELIOS,Mesh.UNIT_CUBE,new org.joml.Matrix4f(),0x80a0a0a0,0,0);
        assertEquals(24,canvas.sink(Layer.HULL_HELIOS).vertices());
        assertEquals(1,canvas.sink(Layer.GLOW).vertices()/4);
    }

    @Test void proceduralMeshesHaveExpectedTopologyAndUnitNormals() {
        assertEquals(24,Mesh.UNIT_CUBE.vertexCount());
        var crystal=new Mesh.Builder().crystal(1,2,3,6,0).build();
        assertEquals(6*8,crystal.vertexCount());
        var frustum=new Mesh.Builder().frustum(1,2,0,3,8,true).build();
        assertEquals(8*12,frustum.vertexCount());
        var shell=new Mesh.Builder().shellPatch(4,5,-.3,.3,.4,2,3,4).build();
        assertTrue(shell.vertexCount()>0&&shell.vertexCount()%4==0);
        for(Mesh mesh:List.of(Mesh.UNIT_SPHERE,crystal,frustum,shell))
            for(int i=0;i<mesh.vertices;i++) {
                float nx=mesh.data[i*8+5],ny=mesh.data[i*8+6],nz=mesh.data[i*8+7];
                assertEquals(1,Math.sqrt(nx*nx+ny*ny+nz*nz),1e-3);
            }
    }

    @Test void particlePoolIsBoundedAndOwnerScoped() {
        var pool=new CosmeticParticles();
        pool.setCap(8);
        for(int i=0;i<8;i++)assertTrue(pool.spawn(CosmeticParticles.EMBER,i<4?1:2,0,0,0,0,0,0,10,1,-1));
        assertFalse(pool.spawn(CosmeticParticles.EMBER,3,0,0,0,0,0,0,10,1,-1),"cap must reject further spawns");
        assertEquals(8,pool.active());
        pool.removeOwner(1);assertEquals(4,pool.active());
        for(int i=0;i<10;i++)pool.tick(null);
        assertEquals(0,pool.active(),"particles expire after their lifetime");
        pool.setCap(100000);assertTrue(pool.spawn(CosmeticParticles.DUST,1,0,0,0,0,0,0,5,1,-1));
        pool.setCap(0);assertFalse(pool.spawn(CosmeticParticles.DUST,1,0,0,0,0,0,0,5,1,-1),"zero-particle mode spawns nothing");
        pool.clear();assertEquals(0,pool.active());
    }

    @Test void phaseHelpersFollowTimelineOrder() {
        var view=view(Phase.ORBITAL_ALIGNMENT,10,40,7);
        assertEquals(.25,view.progress(),1e-9);
        assertEquals(1,view.through(Phase.CHARGING));
        assertEquals(.25,view.through(Phase.ORBITAL_ALIGNMENT),1e-9);
        assertEquals(0,view.through(Phase.IMPACT));
        assertTrue(view.before(Phase.FIRING)&&view.atOrAfter(Phase.ACQUIRING));
        assertEquals(view.hash(3),view(Phase.CHARGING,0,10,7).hash(3),"hashing is deterministic per seed");
        for(int i=0;i<100;i++){double h=view.hash(i);assertTrue(h>=0&&h<1);}
        assertEquals(1,view(Phase.CANCELLED,0,1,7).through(Phase.AFTERMATH));
        assertEquals(-1,view.sinceImpact());
    }

    @Test void clientConfigClampsPresentationPreferences() {
        var config=new ClientConfig();
        config.particleIntensity=9;config.glowIntensity=-3;config.cinematicAudio=Double.NaN;config.quality=null;config.cameraShakeIntensity=4;
        config.validate();
        assertEquals(2,config.particleIntensity);assertEquals(0,config.glowIntensity);assertEquals(1,config.cinematicAudio);
        assertEquals(ClientConfig.Quality.MEDIUM,config.quality);assertEquals(1,config.cameraShakeIntensity);
        config.particleIntensity=0;assertEquals(0,config.cosmeticParticles());
        config.particleIntensity=1;config.quality=ClientConfig.Quality.ULTRA;assertEquals(ClientConfig.Quality.ULTRA.cosmeticBudget,config.cosmeticParticles());
        assertTrue(config.reducedFlashing,"reduced flashing stays on by default");
        config.reducedMotion=true;assertFalse(config.shakeAllowed());
    }
}
