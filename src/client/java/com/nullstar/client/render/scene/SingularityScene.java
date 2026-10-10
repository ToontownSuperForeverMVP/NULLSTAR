// Copyright (C) 2026 ToontownSuperForeverMVP and NULLSTAR contributors
// SPDX-License-Identifier: GPL-3.0-only

package com.nullstar.client.render.scene;

import com.nullstar.client.ClientConfig;
import com.nullstar.client.NullstarClient;
import com.nullstar.client.render.Canvas;
import com.nullstar.client.render.CosmeticParticles;
import com.nullstar.client.render.Layer;
import com.nullstar.client.render.Mesh;
import com.nullstar.client.render.PresentationScale;
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
 *
 * <p>At colossal scale the body is lifted into the sky and enlarged, enormous tilted accretion structures orbit it,
 * rock and debris are visibly torn upward and inward, the world holds its breath, and the collapse leaves a
 * gravitational sink basin rather than a bowl. Every magnitude below is derived from the authoritative colossal
 * descriptors, so STANDARD resolves to exactly the Part 2 composition.
 */
public final class SingularityScene implements WeaponScene {
    static final int VIOLET=0xaa44ff,DEEP=0x4a1490,PALE=0xe6ccff,HOT=0xfff0ff;
    static final float CORE_Y=4;
    private final Map<UUID,Integer> groundTint=new HashMap<>();
    private final Matrix4f m=new Matrix4f();
    private final SingularityCollapse collapse=new SingularityCollapse();
    private static final Mesh SHARD=new Mesh.Builder().uvScale(.5f).crystal(.5f,.9f,1.2f,5,.6f).build();
    private static final Mesh SLAB=new Mesh.Builder().uvScale(.4f).box(-.6f,-.16f,-.9f,.6f,.16f,.9f).build();

    // ------------------------------------------------------------------ colossal scale mapping
    /**
     * Local height of the anomaly body above the target. STANDARD keeps the validated Part 2 core; a colossal
     * collapse hangs its body far up in the sky where it reads against the horizon.
     */
    public static float coreY(StrikeView v) {
        // Part 3: a colossal anomaly hangs low enough (about 170 blocks for APOCALYPSE) that a camera inside the
        // loaded area sees it fill the sky above the real horizon instead of a speck far overhead.
        double c=PresentationScale.colossal(v);
        return (float)(CORE_Y+(60+110*c-CORE_Y)*c);
    }
    /** Horizon/disk base multiplier: 1 STANDARD, ~14 TITANIC, 25 APOCALYPSE (an event horizon ~65 blocks across). */
    static float body(StrikeView v){return (float)(1+24*PresentationScale.colossal(v));}
    /** Outer orbital structure multiplier: 1 STANDARD, ~3.8 TITANIC, ~6 APOCALYPSE. */
    static float sprawl(StrikeView v){return (float)(1+5*PresentationScale.colossal(v));}
    static boolean colossal(StrikeView v){return PresentationScale.colossal(v)>0;}

    /** Event horizon radius for a given moment; zero once the anomaly has collapsed. */
    static float coreRadius(StrikeView v) {
        float base;
        if(v.in(Phase.ACQUIRING))base=(float)(.15+.35*v.progress());
        else if(v.in(Phase.CHARGING))base=(float)(.5+1.7*smoother(v.progress()));
        // Colossal profiles insert an alignment stage between the charge and the collapse: hold the charged horizon.
        else if(v.in(Phase.ORBITAL_ALIGNMENT))base=2.2f;
        else if(v.atOrAfter(Phase.FIRING)&&v.before(Phase.IMPACT)) {
            double p=collapse(v);
            base=(float)((2.2+.4*smooth(range(p,0,.6)))*(1-.86*smoother(range(p,.74,1))));
        } else base=0;
        return base<=.01f?0:base*body(v);
    }
    static double instability(StrikeView v){return v.atOrAfter(Phase.FIRING)&&v.before(Phase.IMPACT)?smoother(range(collapse(v),.55,1)):0;}
    /** The whole collapse act: the prolonged attraction, the compression, the hush and the release. */
    static boolean collapsing(StrikeView v){return v.atOrAfter(Phase.FIRING)&&v.before(Phase.IMPACT);}
    /** Collapse progress 0..1. STANDARD maps its FIRING progress exactly as Part 2 did. */
    static double collapse(StrikeView v){return v.in(Phase.FIRING)?v.progress():v.in(Phase.ATMOSPHERIC_ENTRY)?1:0;}
    /** The breath held just before the release: everything hushes and stops moving. */
    static double hush(StrikeView v){
        if(!collapsing(v))return 0;
        double p=collapse(v);
        return smooth(range(p,.5,.68))*(1-smooth(range(p,.74,.88)));
    }

    @Override public void render(StrikeView v,Canvas c,ClientConfig.Quality q) {
        double t=v.age();
        if(!v.atOrAfter(Phase.IMPACT))drawAnomaly(v,c,q,t);
        if(colossal(v)) {
            // Part 3: a distinct gravitational collapse and sink-basin aftermath (never Helios in violet).
            collapse.render(v,c,q,t,groundTint.getOrDefault(v.state().id(),0x7a7068));
            return;
        }
        drawImplosion(v,c,q,t);
        if(v.atOrAfter(Phase.AFTERMATH)&&v.before(Phase.COMPLETE))drawAftermath(v,c,q,t);
    }

    private void drawAnomaly(StrikeView v,Canvas c,ClientConfig.Quality q,double t) {
        float rc=coreRadius(v);if(rc<=.01f)return;
        double form=v.through(Phase.CHARGING),inst=instability(v);
        double contraction=collapsing(v)?smoother(range(collapse(v),.74,1)):0;
        double hold=hush(v);
        double dim=(1-.6*smoother(range(v.through(Phase.FIRING),.86,1)))*(1-.8*hold);
        float cy=coreY(v),bd=body(v),sp=sprawl(v);
        boolean big=colossal(v);
        double motion=1-.7*hold;
        // Tilted disk plane with slow precession that wobbles as the anomaly destabilizes.
        double tilt=.42+.25*inst*Math.sin(t*.9),prec=v.hash(3)*Math.PI*2+t*.01*motion;
        float nx=(float)(Math.sin(tilt)*Math.cos(prec)),ny=(float)Math.cos(tilt),nz=(float)(Math.sin(tilt)*Math.sin(prec));
        // 1. Opaque event horizon: occludes the far side of the disk because it writes depth.
        c.sphere(Layer.DEBRIS,0,cy,0,rc,0x00000000,0,0,true);
        c.billboard(Layer.VOID,0,cy,0,rc*1.22f,0,argb(.85,0x000000));
        if(NullstarClient.config().screenDistortion&&q.level>=1&&!big)c.billboard(Layer.VOID,0,cy,0,rc*(3.6f+(float)form)*(1+.6f*(bd-1)),0,argb(.22*dim,0x08000f));
        // 2. Accretion disk with Keplerian shear and Doppler-brightened approaching side.
        drawDisk(c,v,q,t,rc,cy,nx,ny,nz,dim*(.4+.6*form),big?rc*.45f:sp);
        // 3. Gravitationally lensed image of the disk's far side, bent over and under the horizon.
        drawLensedArcs(c,v,q,t,rc,cy,dim*(.3+.7*form));
        // 4. Uneven luminous boundary (photon ring) and refractive shells.
        c.billboard(Layer.HALO,0,cy,0,rc*1.5f,(float)(t*.03*motion),argb(.95*dim,PALE));
        c.billboard(Layer.HALO,0,cy,0,rc*1.62f,(float)(-t*.021+1.3),argb(.5*dim,VIOLET));
        if(q.level>=1) {
            c.sphere(Layer.RIM_PLASMA,0,cy,0,rc*2.1f,argb(.28*dim,VIOLET),(float)(t*.01*motion),(float)(t*.02*motion),true);
            if(q.level==2)c.sphere(Layer.RIM_FLOW,0,cy,0,rc*2.9f,argb(.12*dim,DEEP),(float)(-t*.02*motion),0,false);
        }
        c.billboard(Layer.GLOW,0,cy,0,rc*5,0,argb(.25*dim*(.5+.5*form),DEEP));
        // 5. Accretion rings: three tilted, unevenly rotating dashed rings that shake apart before the collapse.
        drawAccretionRings(v,c,q,t,cy,rc,dim*(.5+.5*form),inst,big,big?1:sp);
        // Part 3: the anomaly eats daylight. A fog-free falloff darkens the sky around it so the horizon, the disk
        // and the lensed arcs read against a bruised sky instead of washing out in full sun.
        if(big) {
            c.billboard(Layer.VOID_DEEP,0,cy,0,rc*(4.2f+2f*(float)form),(float)(t*.002),argb(.36*dim*(.35+.65*form),0x05000a));
            c.billboard(Layer.VOID_DEEP,0,cy,0,rc*(8f+3f*(float)form),(float)(-t*.0013),argb(.18*dim*(.3+.7*form),0x0a0414));
        }
        // 6. Colossal accretion architecture: a slow precessing outer torus plus broad tilted ring systems that
        //    make the anomaly the largest object on the horizon.
        if(big)drawOuterStructures(v,c,q,t,cy,rc,form,inst,contraction,hold,dim);
        // 7. Violet energy ribbons spiralling inward along logarithmic paths.
        if(form>.3) {
            int ribbons=q.level==2?5:q.level==1?3:2,n=26;float[] spiral=new float[n*3];
            for(int k=0;k<ribbons;k++) {
                double base=k*Math.PI*2/ribbons+t*.08*(1+inst)*motion;
                for(int i=0;i<n;i++) {
                    double s=i/(double)(n-1),radius=rc*1.15+(v.radius()*.9*sp+6)*(1-s)*(1-s);
                    double a=base+s*Math.PI*2.2;
                    spiral[i*3]=(float)(Math.cos(a)*radius);spiral[i*3+1]=(float)(cy+(1-s)*Math.sin(a*1.5+k)*1.6*sp);spiral[i*3+2]=(float)(Math.sin(a)*radius);
                }
                c.ribbon(Layer.CORE_FLOW,spiral,n,.12f*sp,.45f*sp,argb(0,DEEP),argb(.7*dim*form,VIOLET),(float)(-t*.08*motion));
            }
        }
        // 8. Inward-moving motes: the visible signature of the server's attraction pulses.
        int motes=Fx.scaled(q,collapsing(v)?(big?90:120):(big?60:50));
        for(int i=0;i<motes;i++) {
            double h=v.hash(200+i),h2=v.hash(400+i),speed=(.006+.012*h2+.02*inst)*motion;
            double p=Fx.frac(t*speed+h);double reach=v.radius()*(collapsing(v)?2:1.2)*(1-contraction*.6)*sp;
            double radius=rc*1.1+reach*Math.pow(1-p,1.4),a=h*Math.PI*2+(1-radius/reach)*3.5+t*.02;
            double prevR=rc*1.1+reach*Math.pow(1-Math.max(0,p-.04),1.4),prevA=h*Math.PI*2+(1-prevR/reach)*3.5+t*.02;
            float y=(float)(cy+(h2-.5)*radius*.5);
            c.streak(Layer.GLOW,(float)(Math.cos(prevA)*prevR),y,(float)(Math.sin(prevA)*prevR),(float)(Math.cos(a)*radius),y,(float)(Math.sin(a)*radius),
                    .05f+.02f*(sp-1),.12f+.05f*(sp-1),argb(0,VIOLET),argb(Math.min(1,p*2)*.85*dim,PALE),0,.5f,1,.5f);
        }
        // 9. Fracture arcs crawling over the horizon.
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
        // 10. Ground-level debris torn loose and spiralling into the core during attraction.
        if(collapsing(v))drawAttraction(v,c,q,t,rc,contraction,big);
        // 11. The hush: the anomaly swallows its own light and implodes a dark shell inward.
        if(hold>.05) {
            float hr=(float)(rc*(3.2f+2.4f*(big?1:sp))*(1-.55*hold));
            c.sphere(Layer.RIM_PLASMA,0,cy,0,hr,argb(.1*hold,0x120018),(float)(-t*.02),0,false);
            c.billboard(Layer.VOID,0,cy,0,hr*1.35f,0,argb(.55*hold,0x000000));
        }
        // 12. The collapse: layered rings racing inward in the last moments.
        if(collapsing(v)&&collapse(v)>.82) {
            double k=range(collapse(v),.82,1);
            Fx.shockRing(c,0,cy,0,(float)(v.radius()*1.4*(1-smooth(k))+rc),1.5f,PALE,.8*k,(float)t*.05f,q);
        }
        if(v.in(Phase.ACQUIRING)) {
            c.streak(Layer.GLOW,0,0,0,0,cy,0,.25f,.05f,argb(.6*v.progress(),VIOLET),argb(0,VIOLET),0,.5f,1,.5f);
            Fx.shockRing(c,0,.15f,0,v.radius()*(float)smooth(v.progress()),.6f,VIOLET,.6,(float)t*.03f,q);
        }
    }

    /** Three tilted dashed accretion ring systems, receding outward as the event grows. */
    private void drawAccretionRings(StrikeView v,Canvas c,ClientConfig.Quality q,double t,float cy,float rc,double strength,double inst,boolean big,float sp) {
        if(strength<=0)return;
        int pts=q.level==2?40:q.level==1?28:18;float[] buffer=new float[pts*3];
        int systems=big?4:3;
        for(int k=0;k<systems;k++) {
            double radius=rc*(2.4+k*1.1*sp)*(1+.15*Math.sin(t*.13+k));
            double speed=(.06-.012*k)*(1+3*inst)*(k%2==0?1:-1);
            double ta=.6+k*.7+inst*.5*Math.sin(t*(1.3+k)),tb=k*2.1+t*.004*(k+1);
            int arcs=big?4:3;
            for(int arc=0;arc<arcs;arc++) {
                double start=t*speed+arc*Math.PI*2/arcs+k,span=Math.PI*2/arcs*(.55+.25*Math.sin(t*.05+arc+k));
                for(int i=0;i<pts;i++) {
                    double a=start+span*i/(pts-1);
                    double x=Math.cos(a)*radius,z=Math.sin(a)*radius;
                    double y1=-z*Math.sin(ta),z1=z*Math.cos(ta);
                    double x2=x*Math.cos(tb)-z1*Math.sin(tb),z2=x*Math.sin(tb)+z1*Math.cos(tb);
                    buffer[i*3]=(float)x2;buffer[i*3+1]=(float)(cy+y1);buffer[i*3+2]=(float)z2;
                }
                c.ribbon(Layer.CORE_PLASMA,buffer,pts,rc*.08f,rc*.04f,argb(.55*strength,0xc890ff),argb(.45*strength,VIOLET),(float)(t*.05));
            }
        }
    }

    /**
     * Colossal accretion architecture: a broad tilted outer torus with visible thickness, two enormous ring
     * systems beyond the disk, and long infalling streams. Sized from the authoritative secondary reach so the
     * collapse is legible as a regional event, and compressed hard as the anomaly contracts.
     */
    private void drawOuterStructures(StrikeView v,Canvas c,ClientConfig.Quality q,double t,float cy,float rc,double form,double inst,double contraction,double hold,double dim) {
        float reach=(float)Math.min(1400,PresentationScale.secondaryReach(v))*(float)(1-.3*contraction);
        double motion=1-.7*hold;
        // Outside this radius the structures are already torn away by the contraction.
        float alive=(float)Math.max(0,1-contraction*1.35);
        if(alive<=.02f)return;
        float spin=(float)(t*.012*motion);
        int seg=Fx.segments(q,72);
        // Broad tilted torus with a visible section, counter-precessing against the disk.
        double tilt=.62+.18*Math.sin(t*.07)*motion,prec=v.hash(11)*Math.PI*2-t*.008*motion;
        float tnx=(float)(Math.sin(tilt)*Math.cos(prec)),tny=(float)Math.cos(tilt),tnz=(float)(Math.sin(tilt)*Math.sin(prec));
        for(int band=0;band<(q.level==2?3:2);band++) {
            float r0=reach*(.42f+.19f*band),r1=reach*(.5f+.19f*band);
            double a=dim*alive*(1-band*.22)*(.16+.14*form);
            c.annulus(Layer.CORE_FLOW,0,cy,0,tnx,tny,tnz,r0,r1,seg,argb(a*.7,0x9a5cff),argb(a,VIOLET),3,spin,0,1);
            c.annulus(Layer.RIM_FLOW,0,cy,0,tnx,tny,tnz,r1,r1+reach*.02f,seg,argb(a*.5,PALE),argb(0,VIOLET),2,spin+.4f,0,1);
        }
        // Two enormous flat ring systems further out, each on its own slow precession.
        for(int k=0;k<2;k++) {
            double tk=.34+k*.42+inst*.22*Math.sin(t*(.9+k)),pk=k*2.1+t*.0045*(k+1)*motion;
            float knx=(float)(Math.sin(tk)*Math.cos(pk)),kny=(float)Math.cos(tk),knz=(float)(Math.sin(tk)*Math.sin(pk));
            float radius=reach*(.72f+.26f*k);
            double a=dim*alive*(.2-.06*k);
            c.annulus(Layer.RIM_PLASMA,0,cy,0,knx,kny,knz,radius*.94f,radius,seg,argb(a*.6,0xc890ff),argb(a*.9,VIOLET),3,(float)(-t*.01*(k+1)),.1f,.9f);
        }
        // Long infalling streams: matter dragged in from the whole region during the attraction.
        if(collapsing(v)&&contraction<.75) {
            int streams=q.level==2?10:q.level==1?7:4;int n=22;float[] buf=new float[n*3];
            for(int s=0;s<streams;s++) {
                double seed=v.hash(1200+s)*Math.PI*2,start=reach*(.75+.35*v.hash(1250+s));
                double end=rc*(1.1+.4*v.hash(1300+s)),twist=3.5+3*v.hash(1350+s);
                double p=range(v.progress(),v.hash(1400+s)*.5,1);
                if(p<=0)continue;
                double outer=end+(start-end)*(1-Math.pow(p,1.5));
                for(int i=0;i<n;i++) {
                    double f=i/(double)(n-1),r=end+(outer-end)*f,a2=seed+twist*f+p*2.4;
                    double y=cy+(f-.5)*reach*.08*Math.sin(t*.4+s);
                    buf[i*3]=(float)(Math.cos(a2)*r);buf[i*3+1]=(float)y;buf[i*3+2]=(float)(Math.sin(a2)*r);
                }
                c.ribbon(Layer.CORE_FLOW,buf,n,reach*.006f,reach*.002f,argb(dim*p*.5,VIOLET),argb(0,DEEP),(float)(-t*.06));
            }
            // A slow rising ash column under the anomaly as the region is pulled apart.
            if(q.level>=1)Fx.ashColumn(c,0,-v.depth()*.4f,0,v.radius()*.5f,v.radius()*.85f,0x2a1636,dim*.16*form,(float)(t*.02),3,q);
        }
    }

    private void drawDisk(Canvas c,StrikeView v,ClientConfig.Quality q,double t,float rc,float cy,float nx,float ny,float nz,double strength,float sp) {
        if(strength<=0)return;
        int bands=q.level==2?6:q.level==1?4:2,segments=Fx.segments(q,colossal(v)?72:56);
        float inner=rc*1.55f,outer=rc*1.55f+((2.2f+3.2f*(float)v.through(Phase.CHARGING))*sp);
        Vec3 cam=c.cameraLocal().subtract(0,cy,0).normalize();
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
                c.quad(Layer.CORE_PLASMA,d0x*r0,cy+d0y*r0,d0z*r0,d1x*r0,cy+d1y*r0,d1z*r0,d1x*r1,cy+d1y*r1,d1z*r1,d0x*r1,cy+d0y*r1,d0z*r1,
                        u0,(float)b/bands,u1,(float)(b+1)/bands,col,col,edge,edge,nx,ny,nz);
            }
        }
    }

    private void drawLensedArcs(Canvas c,StrikeView v,ClientConfig.Quality q,double t,float rc,float cy,double strength) {
        if(q.level==0||strength<=0)return;
        int n=24;float[] pts=new float[n*3];
        float rx=c.rightX(),ry=c.rightY(),rz=c.rightZ(),ux=c.upX(),uy=c.upY(),uz=c.upZ();
        for(int arc=0;arc<2;arc++) {
            float radius=rc*(arc==0?1.32f:1.18f);double from=arc==0?.12:Math.PI+.35,to=arc==0?Math.PI-.12:Math.PI*2-.35;
            for(int i=0;i<n;i++) {
                double a=from+(to-from)*i/(n-1);
                float cx=(float)Math.cos(a)*radius,cyy=(float)Math.sin(a)*radius*(arc==0?1.05f:.9f);
                pts[i*3]=rx*cx+ux*cyy;pts[i*3+1]=cy+ry*cx+uy*cyy;pts[i*3+2]=rz*cx+uz*cyy;
            }
            double alpha=strength*(arc==0?.75:.4);
            c.ribbon(Layer.CORE_PLASMA,pts,n,rc*(arc==0?.26f:.16f),rc*(arc==0?.26f:.16f),argb(alpha,PALE),argb(alpha,VIOLET),(float)(t*.02));
        }
    }

    private void drawAttraction(StrikeView v,Canvas c,ClientConfig.Quality q,double t,float rc,double contraction,boolean big) {
        double p=collapse(v);
        int tint=groundTint.getOrDefault(v.state().id(),0x7a7068);
        // STANDARD keeps the validated Part 2 release radius; colossal events tear matter in from far wider out.
        float spread=sprawl(v)-1;
        // Small fragments.
        int count=Fx.scaled(q,big?60:44);
        for(int i=0;i<count;i++) {
            double start=v.hash(600+i)*.62,duration=.22+.18*v.hash(650+i);
            double k=range(p,start,start+duration);if(k<=0||k>=.97||contraction>.6)continue;
            double a0=v.hash(700+i)*Math.PI*2,r0=v.radius()*(.4+1.3*v.hash(750+i))*(1+spread*.8);
            double spin=(v.hash(800+i)>.5?1:-1)*(2.5+3*v.hash(850+i));
            double radius=rc*1.2+(r0-rc*1.2)*(1-Math.pow(k,1.7));
            double a=a0+spin*k*k;
            double lift=Math.sin(Math.min(1,k*3)*Math.PI*.5);
            float cy=coreY(v);
            float y=(float)((1-smooth(k))*.5+cy*smooth(k)+lift*2*(1-k));
            float size=(float)(.35+.9*v.hash(900+i))*(float)(1-.7*k)*(1+.5f*(body(v)-1));
            float x=(float)(Math.cos(a)*radius),z=(float)(Math.sin(a)*radius);
            // Tidal stretching toward the horizon as each fragment is pulled in.
            float stretch=1+(float)(k*k*3);
            m.translation(x,y,z).rotateY((float)-a).rotateXYZ((float)(t*.2+i),(float)(t*.13),0).scale(size*stretch,size/(float)Math.sqrt(stretch),size/(float)Math.sqrt(stretch));
            if(i%5==0)c.mesh(Layer.CORE_CRYSTAL,SHARD,m,argb(.8,VIOLET),0,0);
            else c.mesh(Layer.DEBRIS,Mesh.UNIT_CUBE,m,argb(0,tint),0,0);
            if(q.level>=1)c.streak(Layer.GLOW,x,y,z,(float)(Math.cos(a-.25*spin*.1)*radius*1.08),y-.2f,(float)(Math.sin(a-.25*spin*.1)*radius*1.08),.05f,size*.4f,argb(.5*k,VIOLET),argb(0,DEEP),0,.5f,1,.5f);
        }
        if(!big)return;
        // Whole slabs of terrain torn upward and inward, spinning as they climb toward the horizon.
        int slabs=Fx.scaled(q,26);
        for(int i=0;i<slabs;i++) {
            double start=v.hash(2000+i)*.45,duration=.34+.2*v.hash(2050+i);
            double k=range(p,start,start+duration);if(k<=0||k>=.98||contraction>.5)continue;
            double a0=v.hash(2100+i)*Math.PI*2,r0=v.radius()*(.55+1.1*v.hash(2150+i))*(1+spread*1.2);
            double radius=rc*1.3+(r0-rc*1.3)*(1-Math.pow(k,1.5));
            double a=a0+(v.hash(2200+i)>.5?1:-1)*(3.4+2.6*v.hash(2250+i))*k*k;
            float cy=coreY(v);
            float y=(float)((1-smooth(k))*.4+cy*smooth(k)+Math.sin(Math.min(1,k*3.4)*Math.PI*.5)*3.4*(1-k));
            float size=(float)(1.1+2.2*v.hash(2300+i))*(float)(1-.55*k);
            float x=(float)(Math.cos(a)*radius),z=(float)(Math.sin(a)*radius);
            m.translation(x,y,z).rotateY((float)-a).rotateXYZ((float)(t*.11+i*.7),(float)(t*.07),0).scale(size*1.6f,size*.35f,size*.8f);
            c.mesh(Layer.DEBRIS,SLAB,m,argb(0,tint),0,0);
            if(i%3==0)c.mesh(Layer.CORE_CRYSTAL,SHARD,m,argb(.7,VIOLET),0,0);
        }
    }

    private void drawImplosion(StrikeView v,Canvas c,ClientConfig.Quality q,double t) {
        double s=v.sinceImpact();
        if(s<0) {
            if(!v.atOrAfter(Phase.IMPACT))return;
            s=v.in(Phase.IMPACT)?v.progress()*15:15+v.through(Phase.AFTERMATH)*100;
        }
        float radius=v.radius(),cy=coreY(v);
        boolean big=colossal(v);
        float shellScale=(float)PresentationScale.structure(v);
        if(s<3)Fx.flash(c,0,cy,0,(float)(5+s*6)*shellScale,HOT,1-s/3);
        if(s<18) {
            float shell=radius*1.9f*shellScale*Fx.easeOut(s/14);
            c.sphere(Layer.RIM_PLASMA,0,cy,0,shell,argb((1-s/18)*.45,VIOLET),(float)(t*.03),0,q.level>=1);
            c.sphere(Layer.CORE_PLASMA,0,cy,0,Math.max(.1f,shell*.35f*(float)(1-s/18)),argb((1-s/18)*.9,PALE),0,(float)(t*.03),q.level>=1);
        }
        // Dark pressure ring followed by violet shock rings racing over the ground.
        if(s<26||big) {
            float r=radius*2.6f*shellScale*Fx.easeOut(s/24);
            c.annulus(Layer.VOID,0,.4f,0,0,1,0,Math.max(0,r-6),r,Fx.segments(q,64),argb(0,0),argb(.55*(1-s/26),0x0a0014),1,0,.2f,.8f);
        }
        if(big) {
            // Layered collapse system out to the authoritative shock reach: the region, not the block, fails.
            Fx.colossalShock(c,0,.5f,0,PresentationScale.shockReach(v),s,VIOLET,(1-s/24)*.95,q.level==2?6:4,(float)(s*.02),q);
            Fx.ashColumn(c,0,-v.depth()*.5f,0,radius*.8f,radius*1.5f,0x2e1540,(1-s/30)*.5,(float)(t*.03),q.level==2?4:3,q);
            if(s<40)Fx.dustDome(c,0,-v.depth()*.2f,0,PresentationScale.secondaryReach(v)*.6f,v.depth()*1.6f,0x1c1030,(1-s/40)*.4,q.level==2?3:2,q);
        }
        Fx.shockRing(c,0,.5f,0,radius*3.2f*shellScale*Fx.easeOut(s/18),5,VIOLET,(1-s/18)*.9,(float)(s*.02),q);
        Fx.shockRing(c,0,.3f,0,radius*2.1f*shellScale*Fx.easeOut((s-4)/24),3,PALE,(s>4?1:0)*(1-(s-4)/24)*.7,1.7f,q);
        Fx.shockWall(c,0,0,0,radius*1.5f*shellScale*Fx.easeOut(s/16),3.5f*shellScale*(float)(1-s/16),VIOLET,(1-s/16)*.35,q,(float)(t*.05));
        // Fracture rings propagating across the whole basin.
        if(big&&s<44) {
            float fr=PresentationScale.secondaryReach(v);
            for(int k=0;k<3;k++) {
                double d=(s-k*4)/38;
                if(d<=0)continue;
                Fx.shockRing(c,0,.35f,0,fr*(.25f+.75f*Fx.easeOut(d)),2.4f,k%2==0?VIOLET:PALE,(1-d)*.5*(1-k*.15),(float)(t*.006*(k%2==0?1:-1)),q);
            }
        }
        // Brief twin release jets along the collapse axis.
        if(s<14) {
            float len=40*shellScale*Fx.easeOut(s/5),w=1.6f*shellScale*(float)(1-s/14);
            c.tube(Layer.CORE_PLASMA,0,cy,0,0,cy+len,0,w,w*.2f,Fx.segments(q,16),argb((1-s/14),PALE),argb(0,VIOLET),1,0,(float)(t*.1),(float)(t*.1)+2);
            c.tube(Layer.CORE_PLASMA,0,cy,0,0,cy-Math.min(len,cy+v.depth()),0,w,w*.3f,Fx.segments(q,16),argb((1-s/14)*.8,PALE),argb(0,VIOLET),1,0,(float)(t*.1),(float)(t*.1)+1);
        }
    }

    private void drawAftermath(StrikeView v,Canvas c,ClientConfig.Quality q,double t) {
        double a=v.through(Phase.AFTERMATH);
        float radius=v.radius(),depth=v.depth(),cy=coreY(v);
        // Residual anomaly flickers out above the new basin.
        if(a<.35) {
            float r=(float)(.35*(1-a/.35)*(.7+.3*Math.sin(t*2.3)))*body(v);
            c.sphere(Layer.DEBRIS,0,cy-depth*.3f,0,r,0x00000000,0,0,false);
            c.billboard(Layer.HALO,0,cy-depth*.3f,0,r*1.6f,(float)t*.1f,argb(.8*(1-a/.35),PALE));
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
        if(!colossal(v))return;
        // Gravitational sink basin: concentric inward-sloped collapsed shells dropping to a fused floor.
        int shells=q.level==2?4:q.level==1?3:2;
        float floor=-depth*.85f;
        for(int i=0;i<shells;i++) {
            double k=(i+1.0)/shells;
            float rr=(float)(radius*(1.0+.22*i));
            float y=(float)(floor*(1-k)-depth*.06f*k);
            double alpha=(1-a)*.55*(.75-k*.4);
            if(alpha<=0)continue;
            c.annulus(Layer.SMOKE,0,y,0,0,1,0,Math.max(0,rr*.55f),rr,Fx.segments(q,48),
                    argb(alpha*.6,DEEP),argb(0,DEEP),3,(float)(t*.01*(i%2==0?1:-1)),.2f,.9f);
        }
        Fx.moltenPool(c,0,floor*.95f,0,radius*.75f,0x8a2ec4,(1-a)*.6,(float)(t*.01),q.level==2?6:4);
        Fx.dustDome(c,0,-depth*.15f,0,radius*1.25f,depth*1.5f,0x241436,(1-a)*.4,q.level==2?3:2,q);
        if(a<.6)Fx.colossalShock(c,0,0,0,PresentationScale.shockReach(v)*.7f,a*26,VIOLET,(1-a/.6)*.45,q.level==2?4:3,(float)(t*.004),q);
    }

    @Override public void tick(StrikeView v,CosmeticParticles p,ClientLevel level,ClientConfig.Quality q) {
        int owner=Fx.owner(v);Vec3 target=v.target();
        groundTint.keySet().retainAll(NullstarClient.STATE.strikes().keySet());
        groundTint.computeIfAbsent(v.state().id(),id->Fx.groundColor(level,target));
        float sp=sprawl(v);boolean big=colossal(v);
        if(collapsing(v)) {
            // Dust dragged across the ground toward the anomaly's footprint, from far wider out at colossal scale.
            int n=p.random()<.5?Fx.scaled(q,1):0;
            for(int i=0;i<n;i++) {
                double a=p.random()*Math.PI*2,r=v.radius()*sp*(.6+p.random()*.8);
                double tx=-Math.sin(a),tz=Math.cos(a);
                p.spawn(CosmeticParticles.DUST,owner,target.x+Math.cos(a)*r,target.y+.3,target.z+Math.sin(a)*r,
                        -Math.cos(a)*.18+tx*.12,.04,-Math.sin(a)*.18+tz*.12,40+p.random()*25,(1.6f+p.random()*1.6f)*(1+.4f*(sp-1)),
                        0x58000000|Canvas.lerpRgb(groundTint.get(v.state().id()),0x5a4a6a,.4));
                p.tune(0,.02f);
            }
            // Colossal events also tear whole dust sheets upward into the column under the anomaly.
            if(big&&p.random()<.35) {
                double a=p.random()*Math.PI*2,r=v.radius()*(.3+p.random()*.7);
                p.spawn(CosmeticParticles.DUST,owner,target.x+Math.cos(a)*r,target.y+.5,target.z+Math.sin(a)*r,
                        -Math.cos(a)*.3,.28+p.random()*.5,-Math.sin(a)*.3,60+p.random()*40,(3+p.random()*3)*(1+.5f*(sp-1)),
                        0x60000000|Canvas.lerpRgb(groundTint.get(v.state().id()),0x3a2450,.5));
                p.tune(0,.06f);
            }
        }
        if(v.in(Phase.AFTERMATH)&&p.random()<(1-v.progress())) {
            double a=p.random()*Math.PI*2,r=v.radius()*(.8+p.random()*.6)*sp;
            p.spawn(CosmeticParticles.MOTE,owner,target.x+Math.cos(a)*r,target.y+1+p.random()*4,target.z+Math.sin(a)*r,
                    -Math.cos(a)*.08,-.02,-Math.sin(a)*.08,40+p.random()*30,.2f+p.random()*.2f,0xd0000000|PALE);
        }
    }

    @Override public void event(StrikeView v,StrikeView.Event e,CosmeticParticles p,ClientLevel level,ClientConfig.Quality q) {
        if(!e.impact())return;
        int owner=Fx.owner(v);Vec3 at=e.position();float radius=e.payload().radius();
        int ground=Fx.groundColor(level,at);
        boolean big=colossal(v);
        float sp=big?Math.min(3f,PresentationScale.structure(v)):1;
        Fx.debrisBurst(p,owner,at,Fx.scaled(q,big?150:110),1.25f*sp,1.1f,ground,.3f,1.2f*sp,0);
        for(int i=0;i<Fx.scaled(q,big?48:30);i++) {
            double a=p.random()*Math.PI*2,s=(.6+p.random()*1.1)*sp;
            p.spawn(CosmeticParticles.SHARD,owner,at.x,at.y+coreY(v)*.25f,at.z,Math.cos(a)*s,.3+p.random()*.8,Math.sin(a)*s,60+p.random()*40,(.5f+p.random()*.6f)*sp,0xe0000000|VIOLET);
        }
        Fx.sparks(p,owner,at.add(0,coreY(v)*.25,0),Fx.scaled(q,50),1.4f*sp,PALE);
        Fx.dustRing(p,owner,at,Fx.scaled(q,big?48:30),radius*sp,.9f,Canvas.lerpRgb(ground,0x4a3a5a,.45),3.5f*sp);
    }
}
