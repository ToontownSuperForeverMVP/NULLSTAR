package com.nullstar.client.camera;

import com.nullstar.client.NullstarClient;
import com.nullstar.client.ClientStrikeState;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Frame-local camera override. Player, view options, key state and camera entity are never replaced. */
public final class CinematicCamera {
    public record Pose(Vec3 position,float yaw,float pitch,float fov) {}
    private final Map<String,CameraPreset> presets=new HashMap<>();
    private final Set<UUID> skipped=new HashSet<>();
    private Pose last;
    private double lastSample,releaseStart=-1;
    private boolean applied;
    public CinematicCamera() {
        register("orbital",ctx->{
            double angle=.4+ctx.strike().age(ctx.tick())*.008,r=ctx.strike().state().radius()*1.8+12;
            return new CameraPreset.Frame(ctx.target().add(Math.cos(angle)*r,ctx.strike().state().orbitalHeight()*.55,Math.sin(angle)*r),
                    ctx.target().add(0,ctx.strike().state().orbitalHeight()*.25,0),50);
        });
        register("beam",ctx->{
            double progress=ease(ctx.progress());
            return new CameraPreset.Frame(ctx.target().add(ctx.strike().state().radius()+12,
                    14+ctx.strike().state().orbitalHeight()*(1-progress)*.4,ctx.strike().state().radius()+18),ctx.target().add(0,8,0),65);
        });
        register("impact",ctx->new CameraPreset.Frame(ctx.target().add(ctx.strike().state().radius()*2+12,18,ctx.strike().state().radius()+15),ctx.target(),72));
    }
    public void register(String id,CameraPreset preset){presets.put(id,preset);}
    public void skip(){skipped.addAll(NullstarClient.STATE.strikes().keySet());}
    public void reset(){last=null;skipped.clear();releaseStart=-1;applied=false;}
    public boolean applied(){return applied;}
    public Pose sample(Vec3 vanilla,float yaw,float pitch,float fov,double tick) {
        Minecraft mc=Minecraft.getInstance();applied=false;
        if(mc.level==null||mc.player==null||!mc.player.isAlive()){reset();return null;}
        skipped.retainAll(NullstarClient.STATE.strikes().keySet());
        ClientStrikeState.Strike strike=NullstarClient.STATE.strikes().values().stream().filter(s->s.state().owner().equals(mc.player.getUUID())
                && !skipped.contains(s.state().id()) && presets.containsKey(s.state().camera())).findFirst().orElse(null);
        boolean enabled=NullstarClient.config().cinematicCamera&&!NullstarClient.config().reducedMotion&&mc.gui.screen()==null;
        Pose desired=null;
        if(enabled&&strike!=null) {
            Vec3 target=Vec3.atCenterOf(strike.state().target());
            var frame=presets.get(strike.state().camera()).sample(new CameraPreset.Context(strike,tick,target,strike.phaseProgress(tick)));
            if(mc.level.hasChunk((int)Math.floor(frame.position().x)>>4,(int)Math.floor(frame.position().z)>>4)) {
                Vec3 direction=frame.lookAt().subtract(frame.position());
                float targetYaw=(float)(Math.toDegrees(Math.atan2(-direction.x,direction.z)));
                float targetPitch=(float)(-Math.toDegrees(Math.atan2(direction.y,Math.hypot(direction.x,direction.z))));
                double shake=0;
                for(var cue:NullstarClient.STATE.cues())if(cue.payload().strike().equals(strike.state().id())&&cue.payload().effect().equals("impact"))
                    shake=Math.max(shake,Math.max(0,1-(tick-cue.received())/20));
                shake*=NullstarClient.config().cameraShakeIntensity;
                desired=new Pose(frame.position().add(Math.sin(tick*3)*shake,Math.cos(tick*2.7)*shake*.5,0),
                        targetYaw+(float)(Math.sin(tick*4)*shake),targetPitch,frame.fov());
            }
        }
        if(desired!=null) {
            releaseStart=-1;
            if(last==null)last=new Pose(vanilla,yaw,pitch,fov);
            double delta=Math.max(.01,Math.min(2,tick-lastSample));
            last=blend(last,desired,1-Math.exp(-delta*.18));lastSample=tick;applied=true;return last;
        }
        if(last!=null) {
            if(releaseStart<0)releaseStart=tick;
            double t=(tick-releaseStart)/12;
            if(t>=1){last=null;releaseStart=-1;return null;}
            applied=true;return blend(last,new Pose(vanilla,yaw,pitch,fov),ease(t));
        }
        return null;
    }
    public static double ease(double t){t=Math.max(0,Math.min(1,t));return t*t*(3-2*t);}
    private static Pose blend(Pose a,Pose b,double t) {
        float yawDelta=(b.yaw-a.yaw+540)%360-180;
        return new Pose(a.position.lerp(b.position,t),a.yaw+(float)(yawDelta*t),a.pitch+(float)((b.pitch-a.pitch)*t),a.fov+(float)((b.fov-a.fov)*t));
    }
}
