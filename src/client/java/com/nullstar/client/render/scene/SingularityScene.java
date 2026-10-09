package com.nullstar.client.render.scene;

import com.nullstar.client.ClientConfig;
import com.nullstar.client.NullstarClient;
import com.nullstar.client.render.Canvas;
import com.nullstar.client.render.CosmeticParticles;
import com.nullstar.client.render.Layer;
import com.nullstar.client.render.Mesh;
import com.nullstar.client.render.StrikeView;
import com.nullstar.core.Phase;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import static com.nullstar.client.render.Canvas.argb;
import static com.nullstar.client.render.StrikeView.range;
import static com.nullstar.client.render.StrikeView.smooth;
import static com.nullstar.client.render.StrikeView.smoother;

/**
 * SINGULARITY ENGINE: a dark gravitational anomaly with a lensed accretion disk, unstable accretion rings and
 * inward spirals that make the server's attraction legible, then contracts into near-silence and implodes.
 */
public final class SingularityScene implements WeaponScene {
    static final int VIOLET=0xaa44ff,DEEP=0x4a1490,PALE=0xe6ccff,HOT=0xfff0ff;
    static final float CORE_Y=4;
    private final Map<UUID,Integer> groundTint=new HashMap<>();
    private final Matrix4f m=new Matrix4f();
    private static final Mesh SHARD=new Mesh.Builder().uvScale(.5f).crystal(.5f,.9f,1.2f,5,.6f).build();

    /** Event horizon radius for a given moment; zero once the anomaly has collapsed. */
    static float coreRadius(StrikeView v) {
        if(v.in(Phase.ACQUIRING))return (float)(.15+.35*v.progress());
        if(v.in(Phase.CHARGING))return (float)(.5+1.7*smoother(v.progress()));
        if(v.in(Phase.FIRING)) {
            double p=v.progress();
            return (float)((2.2+.4*smooth(range(p,0,.6)))*(1-.86*smoother(range(p,.74,1))));
        }
        return 0;
    }
    static double instability(StrikeView v){return v.in(Phase.FIRING)?smoother(range(v.progress(),.55,1)):0;}

    @Override public void render(StrikeView v,Canvas c,ClientConfig.Quality q) {
        double t=v.age();
        if(!v.atOrAfter(Phase.IMPACT))drawAnomaly(v,c,q,t);
        drawImplosion(v,c,q,t);
        if(v.atOrAfter(Phase.AFTERMATH)&&v.before(Phase.COMPLETE))drawAftermath(v,c,q,t);
    }

    private void drawAnomaly(StrikeView v,Canvas c,ClientConfig.Quality q,double t) {
        float rc=coreRadius(v);if(rc<=.01f)return;
        double form=v.through(Phase.CHARGING),inst=instability(v);
        double contraction=v.in(Phase.FIRING)?smoother(range(v.progress(),.74,1)):0;
        double dim=1-.6*smoother(range(v.through(Phase.FIRING),.86,1));
        float cy=CORE_Y;
        // Tilted disk plane with slow precession that wobbles as the anomaly destabilizes.
        double tilt=.42+.25*inst*Math.sin(t*.9),prec=v.hash(3)*Math.PI*2+t*.01;
        float nx=(float)(Math.sin(tilt)*Math.cos(prec)),ny=(float)Math.cos(tilt),nz=(float)(Math.sin(tilt)*Math.sin(prec));
        // 1. Opaque event horizon: occludes the far side of the disk because it writes depth.
        c.sphere(Layer.DEBRIS,0,cy,0,rc,0x00000000,0,0,true);
        c.billboard(Layer.VOID,0,cy,0,rc*1.22f,0,argb(.85,0x000000));
        if(NullstarClient.config().screenDistortion&&q.level>=1)c.billboard(Layer.VOID,0,cy,0,rc*(3.6f+(float)form),0,argb(.22*dim,0x08000f));
        // 2. Accretion disk with Keplerian shear and Doppler-brightened approaching side.
        drawDisk(c,v,q,t,rc,nx,ny,nz,dim*(.4+.6*form));
        // 3. Gravitationally lensed image of the disk's far side, bent over and under the horizon.
        drawLensedArcs(c,v,q,t,rc,dim*(.3+.7*form));
        // 4. Uneven luminous boundary (photon ring) and refractive shells.
        c.billboard(Layer.HALO,0,cy,0,rc*1.5f,(float)(t*.03),argb(.95*dim,PALE));
        c.billboard(Layer.HALO,0,cy,0,rc*1.62f,(float)(-t*.021+1.3),argb(.5*dim,VIOLET));
        if(q.level>=1) {
            c.sphere(Layer.RIM_PLASMA,0,cy,0,rc*2.1f,argb(.28*dim,VIOLET),(float)(t*.01),(float)(t*.02),true);
            if(q.level==2)c.sphere(Layer.RIM_FLOW,0,cy,0,rc*2.9f,argb(.12*dim,DEEP),(float)(-t*.02),0,false);
        }
        c.billboard(Layer.GLOW,0,cy,0,rc*5,0,argb(.25*dim*(.5+.5*form),DEEP));
        // 5. Accretion rings: three tilted, unevenly rotating dashed rings that shake apart before the collapse.
        int pts=q.level==2?40:q.level==1?28:18;float[] buffer=new float[pts*3];
        for(int k=0;k<3;k++) {
            double radius=rc*(2.4+k*1.1)*(1+.15*Math.sin(t*.13+k));
            double speed=(.06-.015*k)*(1+3*inst)*(k%2==0?1:-1);
            double ta=.6+k*.7+inst*.5*Math.sin(t*(1.3+k)),tb=k*2.1+t*.004*(k+1);
            for(int arc=0;arc<3;arc++) {
                double start=t*speed+arc*Math.PI*2/3+k,span=Math.PI*2/3*(.55+.25*Math.sin(t*.05+arc+k));
                for(int i=0;i<pts;i++) {
                    double a=start+span*i/(pts-1);
                    double x=Math.cos(a)*radius,z=Math.sin(a)*radius;
                    double y1=-z*Math.sin(ta),z1=z*Math.cos(ta);
                    double x2=x*Math.cos(tb)-z1*Math.sin(tb),z2=x*Math.sin(tb)+z1*Math.cos(tb);
                    buffer[i*3]=(float)x2;buffer[i*3+1]=(float)(cy+y1);buffer[i*3+2]=(float)z2;
                }
                c.ribbon(Layer.CORE_PLASMA,buffer,pts,rc*.08f,rc*.04f,argb(.55*dim,0xc890ff),argb(.45*dim,VIOLET),(float)(t*.05));
            }
        }
        // 6. Violet energy ribbons spiralling inward along logarithmic paths.
        if(form>.3) {
            int ribbons=q.level==2?5:q.level==1?3:2;int n=26;float[] spiral=new float[n*3];
            for(int k=0;k<ribbons;k++) {
                double base=k*Math.PI*2/ribbons+t*.08*(1+inst);
                for(int i=0;i<n;i++) {
                    double s=i/(double)(n-1),radius=rc*1.15+(v.radius()*.9+6)*(1-s)*(1-s);
                    double a=base+s*Math.PI*2.2;
                    spiral[i*3]=(float)(Math.cos(a)*radius);spiral[i*3+1]=(float)(cy+(1-s)*Math.sin(a*1.5+k)*1.6);spiral[i*3+2]=(float)(Math.sin(a)*radius);
                }
                c.ribbon(Layer.CORE_FLOW,spiral,n,.12f,.45f,argb(0,DEEP),argb(.7*dim*form,VIOLET),(float)(-t*.08));
            }
        }
        // 7. Inward-moving motes: the visible signature of the server's attraction pulses.
        int motes=Fx.scaled(q,v.in(Phase.FIRING)?120:50);
        for(int i=0;i<motes;i++) {
            double h=v.hash(200+i),h2=v.hash(400+i),speed=.006+.012*h2+.02*inst;
            double p=Fx.frac(t*speed+h);double reach=v.radius()*(v.in(Phase.FIRING)?2:1.2)*(1-contraction*.6);
            double radius=rc*1.1+reach*Math.pow(1-p,1.4),a=h*Math.PI*2+(1-radius/reach)*3.5+t*.02;
            double prevR=rc*1.1+reach*Math.pow(1-Math.max(0,p-.04),1.4),prevA=h*Math.PI*2+(1-prevR/reach)*3.5+t*.02;
            float y=(float)(cy+(h2-.5)*radius*.5);
            c.streak(Layer.GLOW,(float)(Math.cos(prevA)*prevR),y,(float)(Math.sin(prevA)*prevR),(float)(Math.cos(a)*radius),y,(float)(Math.sin(a)*radius),
                    .05f,.12f,argb(0,VIOLET),argb(Math.min(1,p*2)*.85*dim,PALE),0,.5f,1,.5f);
        }
        // 8. Fracture arcs crawling over the horizon.
        if(q.level>=1&&form>.5) {
            int arcs=q.level==2?6:3;long step=(long)(t/(2-inst));
            for(int i=0;i<arcs;i++) {
                java.util.Random r=new java.util.Random(v.seed()^(step*733+i));
                double a=r.nextDouble()*Math.PI*2,e=(r.nextDouble()-.5)*2.4,a2=a+(r.nextDouble()-.5)*1.4;
                float sx=(float)(Math.cos(a)*Math.cos(e)),sy=(float)Math.sin(e),sz=(float)(Math.sin(a)*Math.cos(e));
                float ex=(float)Math.cos(a2),ez=(float)Math.sin(a2);float reach=rc*(1.8f+r.nextFloat()*1.2f);
                c.arc(Layer.GLOW,sx*rc*1.05f,cy+sy*rc*1.05f,sz*rc*1.05f,ex*reach,cy+sy*reach*.6f,ez*reach,rc*.35f,7,rc*.06f,argb(.9*dim,HOT),r.nextLong());
            }
        }
        // 9. Ground-level debris torn loose and spiralling into the core during attraction.
        if(v.in(Phase.FIRING))drawAttraction(v,c,q,t,rc,contraction);
        // 10. The collapse: a ring of light rushing inward in the last moments.
        if(v.in(Phase.FIRING)&&v.progress()>.82) {
            double k=range(v.progress(),.82,1);
            Fx.shockRing(c,0,cy,0,(float)(v.radius()*1.4*(1-smooth(k))+rc),1.5f,PALE,.8*k,(float)t*.05f,q);
        }
        if(v.in(Phase.ACQUIRING)) {
            c.streak(Layer.GLOW,0,0,0,0,cy,0,.25f,.05f,argb(.6*v.progress(),VIOLET),argb(0,VIOLET),0,.5f,1,.5f);
            Fx.shockRing(c,0,.15f,0,v.radius()*(float)smooth(v.progress()),.6f,VIOLET,.6,(float)t*.03f,q);
        }
    }

    private void drawDisk(Canvas c,StrikeView v,ClientConfig.Quality q,double t,float rc,float nx,float ny,float nz,double strength) {
        if(strength<=0)return;
        int bands=q.level==2?6:q.level==1?4:2,segments=Fx.segments(q,56);
        float inner=rc*1.55f,outer=rc*1.55f+2.2f+3.2f*(float)v.through(Phase.CHARGING);
        Vec3 cam=c.cameraLocal().subtract(0,CORE_Y,0).normalize();
        float px,py,pz;
        if(Math.abs(ny)<.9f){px=nz;py=0;pz=-nx;}else{px=1;py=0;pz=0;}
        float pl=(float)Math.sqrt(px*px+py*py+pz*pz);px/=pl;py/=pl;pz/=pl;
        float qx=ny*pz-nz*py,qy=nz*px-nx*pz,qz=nx*py-ny*px;
        for(int b=0;b<bands;b++) {
            float r0=inner+(outer-inner)*b/bands,r1=inner+(outer-inner)*(b+1)/bands;
            double mid=(r0+r1)*.5,omega=.9/Math.pow(mid/rc,1.5);
            float scroll=(float)(t*omega*.05);
            double fall=b==0?.9:1-(double)b/bands*.75;
            int rgb=Canvas.lerpRgb(HOT,DEEP,(double)b/Math.max(1,bands-1));
            for(int i=0;i<segments;i++) {
                double a0=i*Math.PI*2/segments,a1=(i+1)*Math.PI*2/segments,am=(a0+a1)*.5;
                float c0=(float)Math.cos(a0),s0=(float)Math.sin(a0),c1=(float)Math.cos(a1),s1=(float)Math.sin(a1);
                float d0x=px*c0+qx*s0,d0y=py*c0+qy*s0,d0z=pz*c0+qz*s0,d1x=px*c1+qx*s1,d1y=py*c1+qy*s1,d1z=pz*c1+qz*s1;
                // Tangential velocity toward the camera brightens that side of the disk.
                double tx=-(px*Math.sin(am))+qx*Math.cos(am),ty=-(py*Math.sin(am))+qy*Math.cos(am),tz=-(pz*Math.sin(am))+qz*Math.cos(am);
                double doppler=1+.65*(tx*cam.x+ty*cam.y+tz*cam.z);
                int col=argb(Math.min(1,strength*fall*doppler*.75),rgb),edge=argb(Math.min(1,strength*fall*doppler*.75)*(b==bands-1?0:1),rgb);
                float u0=scroll+(float)i/segments*3,u1=scroll+(float)(i+1)/segments*3;
                c.quad(Layer.CORE_PLASMA,d0x*r0,CORE_Y+d0y*r0,d0z*r0,d1x*r0,CORE_Y+d1y*r0,d1z*r0,d1x*r1,CORE_Y+d1y*r1,d1z*r1,d0x*r1,CORE_Y+d0y*r1,d0z*r1,
                        u0,(float)b/bands,u1,(float)(b+1)/bands,col,col,edge,edge,nx,ny,nz);
            }
        }
    }

    private void drawLensedArcs(Canvas c,StrikeView v,ClientConfig.Quality q,double t,float rc,double strength) {
        if(q.level==0||strength<=0)return;
        int n=24;float[] pts=new float[n*3];
        float rx=c.rightX(),ry=c.rightY(),rz=c.rightZ(),ux=c.upX(),uy=c.upY(),uz=c.upZ();
        for(int arc=0;arc<2;arc++) {
            float radius=rc*(arc==0?1.32f:1.18f);double from=arc==0?.12:Math.PI+.35,to=arc==0?Math.PI-.12:Math.PI*2-.35;
            for(int i=0;i<n;i++) {
                double a=from+(to-from)*i/(n-1);
                float cx=(float)Math.cos(a)*radius,cyy=(float)Math.sin(a)*radius*(arc==0?1.05f:.9f);
                pts[i*3]=rx*cx+ux*cyy;pts[i*3+1]=CORE_Y+ry*cx+uy*cyy;pts[i*3+2]=rz*cx+uz*cyy;
            }
            double alpha=strength*(arc==0?.75:.4);
            c.ribbon(Layer.CORE_PLASMA,pts,n,rc*(arc==0?.26f:.16f),rc*(arc==0?.26f:.16f),argb(alpha,PALE),argb(alpha,VIOLET),(float)(t*.02));
        }
    }

    private void drawAttraction(StrikeView v,Canvas c,ClientConfig.Quality q,double t,float rc,double contraction) {
        int count=Fx.scaled(q,44);double p=v.progress();
        int tint=groundTint.getOrDefault(v.state().id(),0x7a7068);
        for(int i=0;i<count;i++) {
            double start=v.hash(600+i)*.62,duration=.22+.18*v.hash(650+i);
            double k=range(p,start,start+duration);if(k<=0||k>=.97||contraction>.6)continue;
            double a0=v.hash(700+i)*Math.PI*2,r0=v.radius()*(.4+1.3*v.hash(750+i));
            double spin=(v.hash(800+i)>.5?1:-1)*(2.5+3*v.hash(850+i));
            double radius=rc*1.2+(r0-rc*1.2)*(1-Math.pow(k,1.7));
            double a=a0+spin*k*k;
            double lift=Math.sin(Math.min(1,k*3)*Math.PI*.5);
            float y=(float)((1-smooth(k))*.5+CORE_Y*smooth(k)+lift*2*(1-k));
            float size=(float)(.35+.9*v.hash(900+i))*(float)(1-.7*k);
            float x=(float)(Math.cos(a)*radius),z=(float)(Math.sin(a)*radius);
            // Tidal stretching toward the horizon as each fragment is pulled in.
            float stretch=1+(float)(k*k*3);
            m.translation(x,y,z).rotateY((float)-a).rotateXYZ((float)(t*.2+i),(float)(t*.13),0).scale(size*stretch,size/(float)Math.sqrt(stretch),size/(float)Math.sqrt(stretch));
            if(i%5==0)c.mesh(Layer.CORE_CRYSTAL,SHARD,m,argb(.8,VIOLET),0,0);
            else c.mesh(Layer.DEBRIS,Mesh.UNIT_CUBE,m,argb(0,tint),0,0);
            if(q.level>=1)c.streak(Layer.GLOW,x,y,z,(float)(Math.cos(a-.25*spin*.1)*radius*1.08),y-.2f,(float)(Math.sin(a-.25*spin*.1)*radius*1.08),.05f,size*.4f,argb(.5*k,VIOLET),argb(0,DEEP),0,.5f,1,.5f);
        }
    }

    private void drawImplosion(StrikeView v,Canvas c,ClientConfig.Quality q,double t) {
        double s=v.sinceImpact();
        if(s<0) {
            if(!v.atOrAfter(Phase.IMPACT))return;
            s=v.in(Phase.IMPACT)?v.progress()*15:15+v.through(Phase.AFTERMATH)*100;
        }
        float radius=v.radius(),cy=CORE_Y;
        if(s<3)Fx.flash(c,0,cy,0,(float)(5+s*6),HOT,1-s/3);
        if(s<18) {
            float shell=radius*1.9f*Fx.easeOut(s/14);
            c.sphere(Layer.RIM_PLASMA,0,cy,0,shell,argb((1-s/18)*.45,VIOLET),(float)(t*.03),0,q.level>=1);
            c.sphere(Layer.CORE_PLASMA,0,cy,0,Math.max(.1f,shell*.35f*(float)(1-s/18)),argb((1-s/18)*.9,PALE),0,(float)(t*.03),q.level>=1);
        }
        // Dark pressure ring followed by violet shock rings racing over the ground.
        if(s<26) {
            float r=radius*2.6f*Fx.easeOut(s/24);
            c.annulus(Layer.VOID,0,.4f,0,0,1,0,Math.max(0,r-6),r,Fx.segments(q,64),argb(0,0),argb(.55*(1-s/26),0x0a0014),1,0,.2f,.8f);
        }
        Fx.shockRing(c,0,.5f,0,radius*3.2f*Fx.easeOut(s/18),5,VIOLET,(1-s/18)*.9,(float)(s*.02),q);
        Fx.shockRing(c,0,.3f,0,radius*2.1f*Fx.easeOut((s-4)/24),3,PALE,(s>4?1:0)*(1-(s-4)/24)*.7,1.7f,q);
        Fx.shockWall(c,0,0,0,radius*1.5f*Fx.easeOut(s/16),3.5f*(float)(1-s/16),VIOLET,(1-s/16)*.35,q,(float)(t*.05));
        // Brief twin release jets along the collapse axis.
        if(s<14) {
            float len=40*Fx.easeOut(s/5),w=1.6f*(float)(1-s/14);
            c.tube(Layer.CORE_PLASMA,0,cy,0,0,cy+len,0,w,w*.2f,Fx.segments(q,16),argb((1-s/14),PALE),argb(0,VIOLET),1,0,(float)(t*.1),(float)(t*.1)+2);
            c.tube(Layer.CORE_PLASMA,0,cy,0,0,cy-Math.min(len,cy+v.depth()),0,w,w*.3f,Fx.segments(q,16),argb((1-s/14)*.8,PALE),argb(0,VIOLET),1,0,(float)(t*.1),(float)(t*.1)+1);
        }
    }

    private void drawAftermath(StrikeView v,Canvas c,ClientConfig.Quality q,double t) {
        double a=v.through(Phase.AFTERMATH);
        float radius=v.radius(),depth=v.depth();
        // Residual anomaly flickers out.
        if(a<.35) {
            float r=(float)(.35*(1-a/.35)*(.7+.3*Math.sin(t*2.3)));
            c.sphere(Layer.DEBRIS,0,CORE_Y-depth*.3f,0,r,0x00000000,0,0,false);
            c.billboard(Layer.HALO,0,CORE_Y-depth*.3f,0,r*1.6f,(float)t*.1f,argb(.8*(1-a/.35),PALE));
        }
        int arcs=q.level==2?5:q.level==1?3:1;long step=(long)(t/3);
        for(int i=0;i<arcs;i++) {
            java.util.Random r=new java.util.Random(v.seed()^(step*409+i));
            if(r.nextDouble()>(1-a)*.9)continue;
            double a0=r.nextDouble()*Math.PI*2,a1=a0+.6+r.nextDouble()*1.4;
            float r0=radius*(.6f+r.nextFloat()*.4f);
            c.arc(Layer.GLOW,(float)Math.cos(a0)*r0,-depth*.15f,(float)Math.sin(a0)*r0,(float)Math.cos(a1)*r0*.4f,-depth*.55f,(float)Math.sin(a1)*r0*.4f,
                    2.5f,8,.14f,argb(.85*(1-a),PALE),r.nextLong());
        }
        Fx.groundGlow(c,0,-depth*.7f,0,radius*.6f,DEEP,(1-a)*.5);
        for(int i=0;i<2;i++)Fx.shockRing(c,0,.3f,0,radius*(1.1f+.3f*i+(float)a*.4f),1.2f,VIOLET,.25*(1-a),(float)(t*.005*(i==0?1:-1)),q);
    }

    @Override public void tick(StrikeView v,CosmeticParticles p,ClientLevel level,ClientConfig.Quality q) {
        int owner=Fx.owner(v);Vec3 target=v.target();
        groundTint.keySet().retainAll(NullstarClient.STATE.strikes().keySet());
        groundTint.computeIfAbsent(v.state().id(),id->Fx.groundColor(level,target));
        if(v.in(Phase.FIRING)) {
            // Dust dragged across the ground toward the anomaly's footprint.
            int n=p.random()<.5?Fx.scaled(q,1):0;
            for(int i=0;i<n;i++) {
                double a=p.random()*Math.PI*2,r=v.radius()*(1+p.random());
                double tx=-Math.sin(a),tz=Math.cos(a);
                p.spawn(CosmeticParticles.DUST,owner,target.x+Math.cos(a)*r,target.y+.3,target.z+Math.sin(a)*r,
                        -Math.cos(a)*.18+tx*.12,.04,-Math.sin(a)*.18+tz*.12,40+p.random()*25,1.6f+p.random()*1.6f,
                        0x58000000|Canvas.lerpRgb(groundTint.get(v.state().id()),0x5a4a6a,.4));
                p.tune(0,.02f);
            }
        }
        if(v.in(Phase.AFTERMATH)&&p.random()<(1-v.progress())) {
            double a=p.random()*Math.PI*2,r=v.radius()*(.8+p.random()*.6);
            p.spawn(CosmeticParticles.MOTE,owner,target.x+Math.cos(a)*r,target.y+1+p.random()*4,target.z+Math.sin(a)*r,
                    -Math.cos(a)*.08,-.02,-Math.sin(a)*.08,40+p.random()*30,.2f+p.random()*.2f,0xd0000000|PALE);
        }
    }

    @Override public void event(StrikeView v,StrikeView.Event e,CosmeticParticles p,ClientLevel level,ClientConfig.Quality q) {
        if(!e.impact())return;
        int owner=Fx.owner(v);Vec3 at=e.position();float radius=e.payload().radius();
        int ground=Fx.groundColor(level,at);
        Fx.debrisBurst(p,owner,at,Fx.scaled(q,110),1.25f,1.1f,ground,.3f,1.2f,0);
        for(int i=0;i<Fx.scaled(q,30);i++) {
            double a=p.random()*Math.PI*2,s=.6+p.random()*1.1;
            p.spawn(CosmeticParticles.SHARD,owner,at.x,at.y+CORE_Y,at.z,Math.cos(a)*s,.3+p.random()*.8,Math.sin(a)*s,60+p.random()*40,.5f+p.random()*.6f,0xe0000000|VIOLET);
        }
        Fx.sparks(p,owner,at.add(0,CORE_Y-.5,0),Fx.scaled(q,50),1.4f,PALE);
        Fx.dustRing(p,owner,at,Fx.scaled(q,30),radius,.9f,Canvas.lerpRgb(ground,0x4a3a5a,.45),3.5f);
    }
}
