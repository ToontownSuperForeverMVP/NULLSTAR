package com.nullstar.client.camera;

import com.nullstar.client.NullstarClient;
import com.nullstar.client.ClientStrikeState;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.UnaryOperator;

/** Frame-local camera override. Player, view options, key state and camera entity are never replaced. */
public final class CinematicCamera {
    public record Pose(Vec3 position,float yaw,float pitch,float fov,float roll) {
        public Pose(Vec3 position,float yaw,float pitch,float fov){this(position,yaw,pitch,fov,0);}
    }
    private final Map<String,CameraPreset> presets=new HashMap<>();
    private final Set<UUID> skipped=new HashSet<>();
    private Pose last;
    // Position and look-at are smoothed separately so fast subjects stay framed while the dolly glides.
    private Vec3 camPos,camLook;
    private float camFov,camRoll;
    private double lastSample,releaseStart=-1;
    private boolean applied;
    public CinematicCamera() {
        register("orbital",ctx->{
            double angle=.4+ctx.strike().age(ctx.tick())*.008,r=ctx.strike().state().radius()*1.8+12;
            return new CameraPreset.Frame(ctx.target().add(Math.cos(angle)*r,ctx.strike().state().orbitalHeight()*.55,Math.sin(angle)*r),
                    ctx.target().add(0,ctx.strike().state().orbitalHeight()*.9,0),50);
        });
        register("beam",ctx->{
            double progress=ease(ctx.progress()),height=ctx.strike().state().orbitalHeight();
            double head=ctx.strike().state().phase()==com.nullstar.core.Phase.FIRING?height*(1-progress*.55):height*.45*(1-progress);
            return new CameraPreset.Frame(ctx.target().add(ctx.strike().state().radius()+12,
                    Math.max(18,head+8),ctx.strike().state().radius()+18),ctx.target().add(0,head+5,0),65);
        });
        register("impact",ctx->new CameraPreset.Frame(ctx.target().add(ctx.strike().state().radius()*2+12,18,ctx.strike().state().radius()+15),ctx.target(),72));
    }
    public void register(String id,CameraPreset preset){presets.put(id,preset);}
    public boolean has(String id){return presets.containsKey(id);}
    public void skip(){skip(NullstarClient.STATE,Minecraft.getInstance().player==null?null:Minecraft.getInstance().player.getUUID());}
    public void skip(ClientStrikeState state,UUID owner){state.strikes().values().stream().filter(s->s.state().owner().equals(owner)).forEach(s->skipped.add(s.state().id()));}
    public void reset(){last=null;camPos=null;camLook=null;skipped.clear();releaseStart=-1;applied=false;}
    public boolean applied(){return applied;}
    /** Current blended roll (degrees) for the camera seam; zero when no cinematic pose is applied. */
    public float roll(){return applied&&last!=null?last.roll():0;}
    public Pose sample(Vec3 vanilla,float yaw,float pitch,float fov,double tick) {
        Minecraft mc=Minecraft.getInstance();
        boolean alive=mc.level!=null&&mc.player!=null&&mc.player.isAlive();
        var config=NullstarClient.config();
        return sample(vanilla,yaw,pitch,fov,tick,NullstarClient.STATE,alive?mc.player.getUUID():null,alive,
                config.cinematicCamera&&!config.reducedMotion&&mc.gui.screen()==null,
                config.shakeAllowed()?config.cameraShakeIntensity:0,
                position->mc.level!=null&&mc.level.hasChunk((int)Math.floor(position.x)>>4,(int)Math.floor(position.z)>>4),
                CinematicCamera::clearance);
    }
    /** Explicit frame inputs allow deterministic testing without launching a Minecraft client. */
    public Pose sample(Vec3 vanilla,float yaw,float pitch,float fov,double tick,ClientStrikeState state,UUID owner,
                       boolean alive,boolean enabled,double shakeIntensity,java.util.function.Predicate<Vec3> loaded) {
        return sample(vanilla,yaw,pitch,fov,tick,state,owner,alive,enabled,shakeIntensity,loaded,UnaryOperator.identity());
    }
    /** @param clear lifts a desired camera position out of solid terrain; identity in tests. */
    public Pose sample(Vec3 vanilla,float yaw,float pitch,float fov,double tick,ClientStrikeState state,UUID owner,
                       boolean alive,boolean enabled,double shakeIntensity,java.util.function.Predicate<Vec3> loaded,UnaryOperator<Vec3> clear) {
        applied=false;
        if(!alive || owner==null){reset();return null;}
        skipped.retainAll(state.strikes().keySet());
        ClientStrikeState.Strike strike=state.strikes().values().stream().filter(s->s.state().owner().equals(owner)
                && !skipped.contains(s.state().id()) && presets.containsKey(s.state().camera())).findFirst().orElse(null);
        CameraPreset.Frame frame=null;
        if(enabled&&strike!=null) {
            Vec3 target=Vec3.atCenterOf(strike.state().target());
            frame=presets.get(strike.state().camera()).sample(new CameraPreset.Context(strike,tick,target,strike.phaseProgress(tick)));
            if(frame!=null&&!(valid(frame.position())&&valid(frame.lookAt())&&loaded.test(frame.position())))frame=null;
        }
        if(frame!=null) {
            releaseStart=-1;
            Vec3 desiredPos=clear.apply(frame.position());
            if(last==null||camPos==null) {
                camPos=vanilla;camLook=vanilla.add(forward(yaw,pitch).scale(12));camFov=fov;camRoll=0;
            }
            double delta=Math.max(.01,Math.min(2,tick-lastSample));
            double kp=1-Math.exp(-delta*.18),kl=1-Math.exp(-delta*.45);
            camPos=camPos.lerp(desiredPos,kp);camLook=camLook.lerp(frame.lookAt(),kl);
            camFov+=(float)((frame.fov()-camFov)*kp);camRoll+=(float)((frame.roll()-camRoll)*kp);
            lastSample=tick;
            double shake=Math.min(1,impactShake(state,strike,tick)+frame.rumble()*.35)*shakeIntensity;
            double punch=impactPunch(state,strike,tick)*Math.min(1,shakeIntensity*2);
            Vec3 offset=new Vec3(noise(tick,1.7,4.1),noise(tick,2.3,3.3)*.6,noise(tick,1.3,5.2)).scale(shake);
            Vec3 direction=camLook.subtract(camPos);
            float lookYaw=(float)(Math.toDegrees(Math.atan2(-direction.x,direction.z)));
            float lookPitch=(float)(-Math.toDegrees(Math.atan2(direction.y,Math.hypot(direction.x,direction.z))));
            last=new Pose(camPos.add(offset),lookYaw+(float)(noise(tick,3.1,2.2)*shake*1.6),lookPitch+(float)(noise(tick,2.7,1.9)*shake),
                    camFov+(float)(punch*7),camRoll+(float)(noise(tick,1.1,3.7)*shake*2.2));
            applied=true;return last;
        }
        if(last!=null) {
            if(releaseStart<0)releaseStart=tick;
            double t=(tick-releaseStart)/12;
            if(t>=1){last=null;camPos=null;camLook=null;releaseStart=-1;return null;}
            applied=true;return blend(last,new Pose(vanilla,yaw,pitch,fov),ease(t));
        }
        return null;
    }
    private static boolean valid(Vec3 v){return Double.isFinite(v.x)&&Double.isFinite(v.y)&&Double.isFinite(v.z);}
    /** Deterministic band-limited shake signal in [-1,1]. */
    static double noise(double tick,double a,double b){return (Math.sin(tick*a)+Math.sin(tick*b+1.3)*.6+Math.sin(tick*(a+b)*.71+2.1)*.4)/2;}
    /** Impact cues shake proportionally to their authoritative radius relative to the strike. */
    private static double impactShake(ClientStrikeState state,ClientStrikeState.Strike strike,double tick) {
        double shake=0;
        for(var cue:state.cues()) {
            if(!cue.payload().strike().equals(strike.state().id())||!cue.payload().effect().equals("impact"))continue;
            double age=tick-cue.received();if(age<0||age>40)continue;
            double scale=Math.min(1,cue.payload().radius()/(double)Math.max(1,strike.state().radius()));
            shake=Math.max(shake,Math.exp(-age/9)*(.35+.65*scale));
        }
        return shake;
    }
    private static double impactPunch(ClientStrikeState state,ClientStrikeState.Strike strike,double tick) {
        double punch=0;
        for(var cue:state.cues()) {
            if(!cue.payload().strike().equals(strike.state().id())||!cue.payload().effect().equals("impact"))continue;
            double age=tick-cue.received();if(age<0||age>20)continue;
            punch=Math.max(punch,Math.sin(Math.min(1,age/3)*Math.PI*.5)*Math.exp(-age/6));
        }
        return punch;
    }
    /** Raises a camera position out of solid blocks (bounded search), avoiding clipping into terrain or craters' rims. */
    static Vec3 clearance(Vec3 position) {
        Minecraft mc=Minecraft.getInstance();
        if(mc.level==null)return position;
        Vec3 out=position;
        for(int i=0;i<32;i++) {
            BlockPos pos=BlockPos.containing(out.x,out.y,out.z);
            if(!mc.level.hasChunkAt(pos)||mc.level.getBlockState(pos).getCollisionShape(mc.level,pos).isEmpty()
                    &&mc.level.getBlockState(pos.below()).getCollisionShape(mc.level,pos.below()).isEmpty())break;
            out=out.add(0,1,0);
        }
        return out;
    }
    static Vec3 forward(float yaw,float pitch) {
        double y=Math.toRadians(yaw),p=Math.toRadians(pitch);
        return new Vec3(-Math.sin(y)*Math.cos(p),-Math.sin(p),Math.cos(y)*Math.cos(p));
    }
    public static double ease(double t){t=Math.max(0,Math.min(1,t));return t*t*(3-2*t);}
    static Pose blend(Pose a,Pose b,double t) {
        float yawDelta=((b.yaw-a.yaw)%360+540)%360-180;
        return new Pose(a.position.lerp(b.position,t),a.yaw+(float)(yawDelta*t),a.pitch+(float)((b.pitch-a.pitch)*t),
                a.fov+(float)((b.fov-a.fov)*t),a.roll+(float)((b.roll-a.roll)*t));
    }
}
