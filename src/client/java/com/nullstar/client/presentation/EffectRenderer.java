package com.nullstar.client.presentation;

import com.nullstar.client.ClientStrikeState;
import com.nullstar.client.NullstarClient;
import com.nullstar.core.Phase;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.Vec3;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Tick-based, globally budgeted vanilla particles. Part 2 can register independent presentation layers. */
public final class EffectRenderer {
    private final Map<UUID,DustParticleOptions> colors=new HashMap<>();
    private int remaining;
    private int layerRemaining;
    public void tick(Minecraft mc,long tick) {
        remaining=NullstarClient.config().particleBudget;
        colors.keySet().retainAll(NullstarClient.STATE.strikes().keySet());
        for(var strike:NullstarClient.STATE.strikes().values()) {
            var state=strike.state();Vec3 target=Vec3.atCenterOf(state.target()).add(0,1,0);
            layerRemaining=(int)(NullstarClient.config().particleBudget*.7/Math.max(1,NullstarClient.STATE.strikes().size())*state.particleScale());
            var dust=colors.computeIfAbsent(state.id(),id->new DustParticleOptions(state.color(),1.7f));
            Phase phase=state.phase();double progress=strike.phaseProgress(tick);
            if(phase==Phase.COMPLETE||phase==Phase.COOLDOWN)continue;
            ring(mc,dust,target,2.5,12,tick*.1,16);
            if(state.weapon().equals("singularity")) {
                Vec3 center=target.add(0,4,0);double r=phase==Phase.IMPACT?.8:2+progress*3;
                ring(mc,dust,center,r,20,-tick*.15,12);
                for(int i=0;i<8;i++){double a=i*Math.PI/4+tick*.12;emit(mc,dust,center.add(Math.cos(a)*r*.6,Math.sin(a*2)*r*.5,Math.sin(a)*r*.6),Vec3.ZERO,12);}
                emit(mc,new DustParticleOptions(0x100019,4),center,Vec3.ZERO,8);
                if(phase==Phase.FIRING)for(int i=0;i<12;i++) {
                    double a=i*Math.PI/6+tick*.11,radius=state.radius()*(1-progress*.7);
                    Vec3 point=center.add(Math.cos(a)*radius,Math.sin(a*2)*3,Math.sin(a)*radius);
                    emit(mc,ParticleTypes.REVERSE_PORTAL,point,center.subtract(point).scale(.05),20);
                }
            } else if(phase==Phase.CHARGING||phase==Phase.ORBITAL_ALIGNMENT||phase==Phase.ACQUIRING) {
                Vec3 source=target.add(0,state.orbitalHeight(),0);double r=3+progress*5;
                ring(mc,dust,source,r,24,tick*.08,20);ring(mc,ParticleTypes.END_ROD,source.add(0,1,0),r*.65,12,-tick*.1,16);
                for(int i=0;i<8;i++)emit(mc,dust,source.add(Math.sin(tick+i)*1.3,Math.cos(tick*.8+i),Math.cos(tick+i)*1.3),Vec3.ZERO,14);
            } else if(state.weapon().equals("helios")&&(phase==Phase.FIRING||phase==Phase.ATMOSPHERIC_ENTRY||phase==Phase.IMPACT)) {
                double bottom=phase==Phase.FIRING?state.orbitalHeight()*(1-progress*.55):phase==Phase.ATMOSPHERIC_ENTRY?state.orbitalHeight()*.45*(1-progress):0;
                int points=48;
                for(int i=0;i<points;i++) {
                    double y=bottom+(state.orbitalHeight()-bottom)*i/(points-1);
                    double a=tick*.25+i*.8;
                    emit(mc,dust,target.add(Math.cos(a)*.8,y,Math.sin(a)*.8),Vec3.ZERO,8);
                    if(i%3==0)emit(mc,ParticleTypes.END_ROD,target.add(0,y,0),Vec3.ZERO,8);
                }
            }
            if(phase==Phase.AFTERMATH)for(int i=0;i<6;i++) {
                double a=tick*.1+i,r=state.radius()*.4;
                emit(mc,state.weapon().equals("singularity")?ParticleTypes.REVERSE_PORTAL:ParticleTypes.LARGE_SMOKE,
                        target.add(Math.cos(a)*r,0,Math.sin(a)*r),new Vec3(0,.04,0),30);
            }
        }
        layerRemaining=remaining;
        for(var cue:NullstarClient.STATE.cues()) {
            var strike=NullstarClient.STATE.strikes().get(cue.payload().strike());if(strike==null)continue;
            Vec3 target=Vec3.atCenterOf(cue.payload().position()).add(0,1,0);
            var dust=colors.get(cue.payload().strike());double p=Math.min(1,(tick-cue.received())/(double)cue.payload().duration());
            if(cue.payload().effect().equals("impact")) {
                ring(mc,dust,target,p*cue.payload().radius()*2,40,tick*.03,14);
                if(p<.1 && !NullstarClient.config().reducedFlashing)emit(mc,ParticleTypes.EXPLOSION,target,Vec3.ZERO,10);
            } else {
                Vec3 head=target.add(8*(1-p),strike.state().orbitalHeight()*(1-p),-6*(1-p));
                emit(mc,ParticleTypes.END_ROD,head,Vec3.ZERO,14);
                for(int i=0;i<10;i++)emit(mc,dust,head.add(i*.14,i*.8,-i*.1),Vec3.ZERO,14);
            }
        }
        if(NullstarClient.holdingController(mc)) {
            layerRemaining=remaining;
            var pos=NullstarClient.targetPreview();if(pos!=null)ring(mc,new DustParticleOptions(0x55ddff,1),Vec3.atCenterOf(pos).add(0,1,0),1.7,8,tick*.1,10);
        }
    }
    private void ring(Minecraft mc,ParticleOptions type,Vec3 center,double radius,int points,double angle,int lifetime) {
        for(int i=0;i<points;i++){double a=angle+i*Math.PI*2/points;emit(mc,type,center.add(Math.cos(a)*radius,0,Math.sin(a)*radius),Vec3.ZERO,lifetime);}
    }
    private void emit(Minecraft mc,ParticleOptions type,Vec3 position,Vec3 velocity,int lifetime) {
        if(remaining<=0||layerRemaining<=0||type==null)return;remaining--;layerRemaining--;
        var particle=mc.particleEngine.createParticle(type,position.x,position.y,position.z,velocity.x,velocity.y,velocity.z);
        if(particle!=null)particle.setLifetime(lifetime);
    }
    public void clear(){colors.clear();}
}
