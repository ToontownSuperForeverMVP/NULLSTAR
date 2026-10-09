package com.nullstar.client.camera;

import com.nullstar.client.NullstarClient;
import com.nullstar.client.render.StrikeView;
import com.nullstar.client.render.scene.GlassfallScene;
import com.nullstar.client.render.scene.HeliosScene;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import static com.nullstar.client.render.StrikeView.range;
import static com.nullstar.client.render.StrikeView.smooth;
import static com.nullstar.client.render.StrikeView.smoother;

/**
 * Authored, reusable shot vocabulary (establishing, reveal, mechanical detail, discharge, follow, impact,
 * aftermath) registered as {@link CameraPreset}s. Weapon JSON selects a preset per phase, so shots stay data-driven
 * and the camera Mixin stays generic. Presets reuse the scenes' choreography so the camera frames exactly what
 * is drawn.
 */
public final class CameraScripts {
    private static final Map<UUID,Double> HEADING=new HashMap<>();
    /** Per-strike, per-phase camera lift that only grows while the subject is blocked, avoiding oscillation. */
    private static final Map<UUID,double[]> LIFT=new HashMap<>();
    private CameraScripts() {}

    public static void register(CinematicCamera camera) {
        // HELIOS ------------------------------------------------------------------------------------------
        camera.register("helios_establish",shot((v,a)->{
            double p=v.progress();
            Vec3 collector=v.target().add(0,HeliosScene.collectorHeight(v),0);
            Vec3 pos=around(v,a+.25*p,v.radius()*1.6+46,10+9*p);
            Vec3 look=lerp(v.target().add(0,2,0),v.target().add(0,(collector.y-v.target().y)*.62,0),smoother(range(p,.15,.75)));
            return new CameraPreset.Frame(pos,look,(float)(68-6*p),0,0);
        }));
        camera.register("helios_mechanism",shot((v,a)->{
            double p=v.progress();
            Vec3 collector=v.target().add(0,HeliosScene.collectorHeight(v),0);
            double radius=46-9*smooth(p);
            Vec3 pos=collector.add(Math.cos(a+.7+.55*p)*radius,-11+4*p,Math.sin(a+.7+.55*p)*radius);
            return new CameraPreset.Frame(pos,collector.add(0,-2.5,0),(float)(60-9*smooth(p)),(float)(-4*Math.sin(p*Math.PI)),(float)(p*.15));
        }));
        camera.register("helios_discharge",shot((v,a)->{
            double p=v.progress();
            Vec3 collector=v.target().add(0,HeliosScene.collectorHeight(v),0);
            Vec3 head=v.target().add(0,HeliosScene.beamHead(v),0);
            Vec3 pos=collector.add(Math.cos(a+1.25)*24,-24-6*p,Math.sin(a+1.25)*24);
            return new CameraPreset.Frame(pos,lerp(collector.add(0,-9,0),head,.35+.3*p),72,(float)(3*p),(float)(1-p)*.9f);
        }));
        camera.register("helios_follow",shot((v,a)->{
            double p=v.progress();
            double headY=HeliosScene.beamHead(v);
            double side=16+v.radius()*.35;
            Vec3 head=v.target().add(0,headY,0);
            Vec3 pos=head.add(Math.cos(a+1.45)*side,7,Math.sin(a+1.45)*side);
            if(pos.y<v.target().y+10+v.radius()*.25)pos=new Vec3(pos.x,v.target().y+10+v.radius()*.25,pos.z);
            Vec3 look=lerp(head.add(0,-9,0),v.target().add(0,1,0),smooth(range(p,.55,1)));
            return new CameraPreset.Frame(pos,look,(float)(72+8*p),(float)(5*Math.sin(p*Math.PI)),(float)(.25+.5*p));
        }));
        camera.register("helios_impact",shot((v,a)->{
            Vec3 pos=around(v,a+1.9,v.radius()*2.6+18,6+v.radius()*.35);
            return new CameraPreset.Frame(pos,v.target().add(0,v.radius()*.5,0),74,0,.2f);
        }));
        camera.register("helios_aftermath",shot((v,a)->{
            double p=smooth(v.progress());
            Vec3 pos=around(v,a+1.9+.6*p,(v.radius()*2.6+18)*(1-.18*p),6+v.radius()*.35+p*(v.radius()*1.4+10));
            Vec3 look=v.target().add(0,v.radius()*.5*(1-p)-v.depth()*.35*p,0);
            return new CameraPreset.Frame(pos,look,(float)(74-10*p),0,0);
        }));
        // SINGULARITY -------------------------------------------------------------------------------------
        camera.register("singularity_form",shot((v,a)->{
            double p=smooth(v.progress());
            Vec3 core=v.target().add(0,4,0);
            Vec3 pos=around(v,a+.3*p,v.radius()+20-5*p,9-1.5*p);
            return new CameraPreset.Frame(pos,core,(float)(60-8*p),0,0);
        }));
        camera.register("singularity_pull",shot((v,a)->{
            double p=v.progress();
            Vec3 core=v.target().add(0,4,0);
            // Final beat: a dolly zoom (moving in while widening) makes space itself feel like it is stretching.
            double zoom=smooth(range(p,.72,1));
            double distance=(v.radius()+16)*(1-.3*smooth(p))*(1-.38*zoom);
            Vec3 pos=around(v,a+.3+.95*p,distance,6-2*p);
            float fov=(float)(52+26*zoom);
            float roll=(float)(7*Math.sin(v.age()*.07)*p);
            return new CameraPreset.Frame(pos,core,fov,roll,(float)(.15+.55*p));
        }));
        camera.register("singularity_implode",shot((v,a)->{
            Vec3 core=v.target().add(0,4,0);
            Vec3 pos=around(v,a+1.25,v.radius()+12,8);
            return new CameraPreset.Frame(pos,core,70,0,.3f);
        }));
        camera.register("singularity_aftermath",shot((v,a)->{
            double p=smooth(v.progress());
            Vec3 pos=around(v,a+1.25+.35*p,v.radius()*1.5+10,12+p*(v.depth()+8));
            return new CameraPreset.Frame(pos,v.target().add(0,-v.depth()*.45*p+2*(1-p),0),(float)(66-6*p),0,0);
        }));
        // GLASSFALL ---------------------------------------------------------------------------------------
        camera.register("glassfall_reveal",shot((v,a)->{
            double p=v.progress();
            Vec3 lattice=v.target().add(0,v.height(),0);
            Vec3 pos=around(v,a+.2*p,v.radius()*1.8+40,9+5*p);
            Vec3 look=lerp(v.target().add(0,6,0),v.target().add(0,v.height()*.62,0),smoother(range(p,.1,.8)));
            return new CameraPreset.Frame(pos,look,(float)(68-4*p),0,0);
        }));
        camera.register("glassfall_lattice",shot((v,a)->{
            double p=v.progress();
            Vec3 lattice=v.target().add(0,v.height(),0);
            Vec3 pos=lattice.add(Math.cos(a+.6+.35*p)*36,-10,Math.sin(a+.6+.35*p)*36);
            return new CameraPreset.Frame(pos,lattice.add(0,-2,0),58,(float)(-3*p),.1f);
        }));
        camera.register("glassfall_barrage",shot((v,a)->{
            double p=v.progress();
            Vec3 focus=GlassfallScene.cameraFocus(v);
            double push=smooth(range(v.age(),226,262));
            Vec3 pos=around(v,a+.9+.4*p,(v.radius()*1.5+24)*(1-.2*push),12+v.radius()*.3-3*push);
            return new CameraPreset.Frame(pos,focus,(float)(66+4*push),0,(float)(.1+.3*push));
        }));
        camera.register("glassfall_final",shot((v,a)->{
            Vec3 pos=around(v,a+1.35,v.radius()*2.2+16,5+v.radius()*.3);
            return new CameraPreset.Frame(pos,v.target().add(0,v.radius()*.45,0),74,0,.2f);
        }));
        camera.register("glassfall_aftermath",shot((v,a)->{
            double p=smooth(v.progress());
            Vec3 pos=around(v,a+1.35+.5*p,(v.radius()*2.2+16)*(1-.1*p),5+v.radius()*.3+p*(v.radius()+10));
            return new CameraPreset.Frame(pos,v.target().add(0,v.radius()*.45*(1-p)-v.depth()*.3*p,0),(float)(74-8*p),0,0);
        }));
    }

    @FunctionalInterface interface Shot { CameraPreset.Frame frame(StrikeView view,double heading); }

    /** Wraps a shot with the shared view construction, stable per-strike heading and cave policy. */
    static CameraPreset shot(Shot shot) {
        return ctx->{
            Minecraft mc=Minecraft.getInstance();
            UUID player=mc.player==null?null:mc.player.getUUID();
            StrikeView view=NullstarClient.SCENE.view(ctx.strike(),ctx.tick(),player);
            // Underground targets cannot be framed from the sky without loading or faking terrain: keep the player's view.
            if(mc.level!=null&&!view.atOrAfter(com.nullstar.core.Phase.IMPACT)&&underground(view.target()))return null;
            HEADING.keySet().retainAll(NullstarClient.STATE.strikes().keySet());
            double heading=HEADING.computeIfAbsent(view.state().id(),id->{
                if(mc.player!=null) {
                    double dx=mc.player.getX()-view.target().x,dz=mc.player.getZ()-view.target().z;
                    if(dx*dx+dz*dz>4)return Math.atan2(dz,dx);
                }
                return view.hash(2)*Math.PI*2;
            });
            CameraPreset.Frame frame=shot.frame(view,heading);
            if(frame==null||mc.level==null)return frame;
            LIFT.keySet().retainAll(NullstarClient.STATE.strikes().keySet());
            double[] lift=LIFT.computeIfAbsent(view.state().id(),id->new double[]{-1,0});
            if(lift[0]!=view.phase().ordinal()){lift[0]=view.phase().ordinal();lift[1]=0;}
            Vec3 pos=frame.position().add(0,lift[1],0);
            if(lift[1]<28&&occluded(mc,pos,frame.lookAt()))lift[1]+=1.5;
            return new CameraPreset.Frame(pos,frame.lookAt(),frame.fov(),frame.roll(),frame.rumble());
        };
    }
    /** True when terrain or foliage blocks the line from the camera to most of the way to its subject. */
    static boolean occluded(Minecraft mc,Vec3 from,Vec3 to) {
        Vec3 end=from.lerp(to,.85);
        var hit=mc.level.clip(new net.minecraft.world.level.ClipContext(from,end,net.minecraft.world.level.ClipContext.Block.VISUAL,
                net.minecraft.world.level.ClipContext.Fluid.NONE,net.minecraft.world.phys.shapes.CollisionContext.empty()));
        return hit.getType()!=net.minecraft.world.phys.HitResult.Type.MISS;
    }
    /** A thick solid ceiling above the target means a cave or tunnel; leaves and thin roofs do not count. */
    static boolean underground(Vec3 target) {
        var level=Minecraft.getInstance().level;
        if(level==null)return false;
        int solid=0;
        for(int dy=1;dy<=48;dy++) {
            BlockPos pos=BlockPos.containing(target.x,target.y-.5+dy,target.z);
            if(level.isOutsideBuildHeight(pos.getY()))break;
            if(level.getBlockState(pos).isSolidRender()&&++solid>=6)return true;
        }
        return false;
    }
    static Vec3 around(StrikeView v,double angle,double distance,double height) {
        return v.target().add(Math.cos(angle)*distance,height,Math.sin(angle)*distance);
    }
    static Vec3 lerp(Vec3 a,Vec3 b,double t){return a.lerp(b,Math.max(0,Math.min(1,t)));}
}
