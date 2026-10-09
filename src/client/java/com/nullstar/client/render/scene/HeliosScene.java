package com.nullstar.client.render.scene;

import com.nullstar.client.ClientConfig;
import com.nullstar.client.render.Canvas;
import com.nullstar.client.render.CosmeticParticles;
import com.nullstar.client.render.Layer;
import com.nullstar.client.render.Mesh;
import com.nullstar.client.render.StrikeView;
import com.nullstar.core.Phase;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import static com.nullstar.client.render.Canvas.argb;
import static com.nullstar.client.render.StrikeView.range;
import static com.nullstar.client.render.StrikeView.smooth;
import static com.nullstar.client.render.StrikeView.smoother;

/**
 * HELIOS ANNIHILATOR: an asymmetric orbital collector that descends, unfolds, aligns its rings, opens an armoured
 * reactor and discharges a layered stellar beam onto the server-confirmed target.
 */
public final class HeliosScene implements WeaponScene {
    static final int GOLD=0xffa62a,WHITE_GOLD=0xfff0c8,EMBER=0xff4a12,HOT=0xfff8ec;
    /** Local Y of the emitter nozzle tip below the collector origin. */
    static final float NOZZLE=-8.6f;
    static final float[] VANE_ANGLE={.15f,1.25f,2.55f,3.65f,5.0f},VANE_LENGTH={14,18,11,16,9},VANE_WIDTH={5.5f,6.5f,4.5f,6f,3.6f};
    static final float[] POD_ANGLE={.7f,2.9f,4.4f};
    private static Mesh petal,cap,nozzle,irisBlade,ringA,ringB,segment,spoke,pod;
    private static final Mesh[] VANES=new Mesh[VANE_LENGTH.length];
    private final Matrix4f root=new Matrix4f(),m=new Matrix4f(),frame=new Matrix4f();

    private static void build() {
        if(petal!=null)return;
        petal=new Mesh.Builder().shellPatch(4.2f,5.0f,-.34,.34,.38,2.2,3,5)
                .push().translate(4.95f,-.2f,0).box(0,-1.4f,-.5f,.45f,1.4f,.5f).pop().build();
        cap=new Mesh.Builder().frustum(2.6f,1.5f,4.4f,7.0f,10,true).frustum(.9f,.6f,7f,9.5f,6,true)
                .ring(1.0f,1.9f,8.2f,8.6f,0,Math.PI*2,12).box(-.2f,9.5f,-.2f,.2f,13.5f,.2f)
                .push().translate(0,12f,0).ring(.25f,1.3f,0,.15f,0,Math.PI*1.4,8).pop().build();
        nozzle=new Mesh.Builder().frustum(2.8f,1.6f,-4.4f,NOZZLE,14,false).ring(1.7f,3.1f,-6.3f,-5.7f,0,Math.PI*2,16)
                .ring(1.6f,2.3f,-8.2f,-7.8f,0,Math.PI*2,14).build();
        irisBlade=new Mesh.Builder().box(0,-.12f,-.75f,2.1f,.12f,.75f).build();
        Mesh.Builder a=new Mesh.Builder().ring(6.0f,6.9f,-.55f,.55f,0,Math.PI*2,48);
        for(int i=0;i<12;i++)a.push().rotateY((float)(i*Math.PI/6)).box(6.8f,-.35f,-.4f,7.4f,.35f,.4f).pop();
        ringA=a.build();
        Mesh.Builder b=new Mesh.Builder().ring(8.0f,8.7f,-.4f,.4f,0,Math.PI*2,56);
        for(int i=0;i<3;i++)b.push().rotateY((float)(i*Math.PI*2/3+.3)).box(7.6f,-.9f,-1.1f,9.3f,.9f,1.1f).pop();
        ringB=b.build();
        segment=new Mesh.Builder().ring(14.6f,17.2f,-1.0f,1.0f,-.22,.22,6).ring(13.8f,14.6f,-.35f,.35f,-.25,.25,6)
                .push().rotateY(0).box(15.2f,1.0f,-1.4f,16.9f,1.6f,1.4f).box(15.5f,-1.7f,-.6f,16.4f,-1.0f,.6f).pop().build();
        spoke=new Mesh.Builder().box(5.2f,-.55f,-.8f,14.8f,.55f,.8f).box(6f,.55f,-.35f,14f,.95f,.35f)
                .box(9.5f,-1.3f,-1.2f,11f,1.3f,1.2f).build();
        for(int v=0;v<VANES.length;v++) {
            float len=VANE_LENGTH[v],w=VANE_WIDTH[v];
            Mesh.Builder vb=new Mesh.Builder().box(0,-.25f,-.45f,len,.25f,.45f);
            int panels=Math.max(3,(int)(len/3.4f));
            for(int i=0;i<panels;i++) {
                float x0=1.2f+i*(len-1.2f)/panels,x1=x0+(len-1.2f)/panels-.35f,taper=1-.35f*i/panels;
                vb.box(x0,-.12f,-w*.5f*taper,x1,.12f,-.55f).box(x0,-.12f,.55f,x1,.12f,w*.5f*taper);
            }
            vb.box(len-.6f,-.5f,-.8f,len+.4f,.5f,.8f);
            VANES[v]=vb.build();
        }
        pod=new Mesh.Builder().box(-.3f,0,-.3f,.3f,3.2f,.3f).push().translate(0,0,0).frustum(.95f,.55f,-1.7f,0,8,true).pop()
                .ring(.95f,1.25f,-.7f,-.3f,0,Math.PI*2,8).build();
    }

    // ------------------------------------------------------------------ choreography state
    /** Collector state derived from the current phase, all values 0..1 unless stated. */
    private record Rig(double descend,double ringExtend,double frameAngle,double[] vane,double focusTilt,double focusSpin,
                       double petals,double iris,double charge,double energy,double aim,double recoil,double jitter,double ascend,boolean visible) {}

    private Rig rig(StrikeView v) {
        double t=v.age();
        double charge=v.through(Phase.CHARGING),align=v.through(Phase.ORBITAL_ALIGNMENT),fire=v.through(Phase.FIRING);
        double after=v.through(Phase.AFTERMATH);
        double descend=smoother(range(charge,0,.55));
        double ringExtend=smoother(range(align,0,.35));
        // Frame rotates during charging, then decelerates and locks at the end of the first alignment beat.
        double frameAngle=charge*1.2+smoother(range(align,0,.35))*.9;
        double[] vane=new double[VANE_LENGTH.length];
        for(int i=0;i<vane.length;i++)vane[i]=smoother(range(align,.12+i*.07,.42+i*.07))*(1-.55*smooth(range(after,.15,.8)));
        double focusTilt=1-smoother(range(align,.2,.82));
        double focusSpin=t*.12*(1-smoother(range(align,.55,.9)))+smoother(range(align,.2,.9))*2.4;
        double petals=smoother(range(align,.58,.9))*(1-smoother(range(after,.05,.35)));
        double iris=smoother(range(align,.72,.98))*(1-smoother(range(after,.1,.3)));
        double coreCharge=.18+.32*charge+.5*smoother(range(align,.6,1));
        if(v.atOrAfter(Phase.FIRING))coreCharge=1;
        if(v.atOrAfter(Phase.AFTERMATH))coreCharge=Math.max(0,1-after*3);
        double energy=.1+.35*charge+.55*align;
        if(v.atOrAfter(Phase.AFTERMATH))energy=Math.max(.05,1-after*2.2);
        double aim=smoother(range(align,.08,.4));
        double recoil=v.in(Phase.FIRING)?Math.sin(Math.min(1,fire*5)*Math.PI)*(1-fire)*2.2:0;
        double jitter=(v.in(Phase.ORBITAL_ALIGNMENT)?align*.12:0)+(v.in(Phase.FIRING)||v.in(Phase.ATMOSPHERIC_ENTRY)?.1:0);
        double ascend=smoother(range(after,.25,1));
        boolean visible=!v.before(Phase.CHARGING)&&!v.atOrAfter(Phase.COOLDOWN)&&!v.atOrAfter(Phase.COMPLETE);
        return new Rig(descend,ringExtend,frameAngle,vane,focusTilt,focusSpin,petals,iris,coreCharge,energy,aim,recoil,jitter,ascend,visible);
    }
    /** Local Y of the collector origin (relative to the target). */
    static float collectorY(StrikeView v,double descend,double recoil,double ascend){return (float)(v.height()+(1-descend)*34+recoil+ascend*46);}
    /** Collector height above the target for camera framing; matches the rendered rig exactly. */
    public static double collectorHeight(StrikeView v) {
        double descend=smoother(range(v.through(Phase.CHARGING),0,.55));
        double fire=v.through(Phase.FIRING);
        double recoil=v.in(Phase.FIRING)?Math.sin(Math.min(1,fire*5)*Math.PI)*(1-fire)*2.2:0;
        return collectorY(v,descend,recoil,smoother(range(v.through(Phase.AFTERMATH),.25,1)));
    }
    /** Height of the descending beam's leading edge above the target. */
    public static float beamHead(StrikeView v) {
        float top=(float)collectorHeight(v)+NOZZLE,H=v.height();
        if(v.before(Phase.FIRING))return top;
        if(v.in(Phase.FIRING))return top-(float)(smooth(v.progress())*(top-H*.45f));
        if(v.in(Phase.ATMOSPHERIC_ENTRY))return H*.45f*(1-Fx.easeIn(v.progress()));
        return 0;
    }

    // ------------------------------------------------------------------ rendering
    @Override public void render(StrikeView v,Canvas c,ClientConfig.Quality q) {
        build();
        Rig r=rig(v);double t=v.age();
        float H=v.height(),radius=v.radius();
        float cy=collectorY(v,r.descend,r.recoil,r.ascend);
        double jx=Math.sin(t*2.7)*r.jitter,jz=Math.cos(t*3.1)*r.jitter;
        float yaw=(float)(v.hash(1)*Math.PI*2);
        if(r.visible) {
            root.translation((float)jx,cy,(float)jz).rotateY(yaw);
            drawCollector(v,c,q,r,t);
            drawCore(v,c,q,r,t,cy,yaw);
        }
        drawGround(v,c,q,r,t);
        drawBeam(v,c,q,r,t,cy);
        drawImpact(v,c,q,t);
        if(v.before(Phase.CHARGING)||v.in(Phase.CHARGING))drawMarker(v,c,q,t);
    }

    private void drawCollector(StrikeView v,Canvas c,ClientConfig.Quality q,Rig r,double t) {
        int tint=Canvas.lerpRgb(0xa0a0a0,0xffd9a8,r.charge*.45);
        int hull=argb(Math.min(1,r.energy),tint);
        // Gimballed reactor housing: cap, nozzle and eight armoured petals that hinge open around the core.
        c.mesh(Layer.HULL_HELIOS,cap,root,hull,0,0);
        c.mesh(Layer.HULL_HELIOS,nozzle,root,hull,0,0);
        for(int i=0;i<8;i++) {
            float a=(float)(i*Math.PI/4);float open=(float)(r.petals*.95);
            float px=(float)(5*Math.sin(.38)),py=(float)(5*Math.cos(.38));
            m.set(root).rotateY(a).translate(px,py,0).rotateZ(-open).translate(-px,-py,0);
            c.mesh(Layer.HULL_HELIOS,petal,m,hull,i*.13f,0);
        }
        for(int i=0;i<6;i++) {
            float a=(float)(i*Math.PI/3+r.iris*.9);
            m.set(root).translate(0,NOZZLE-.05f,0).rotateY(a).translate((float)(r.iris*1.9),0,0).rotateY((float)(-.5+r.iris*.5));
            c.mesh(Layer.HULL_HELIOS,irisBlade,m,hull,0,0);
        }
        // Focusing rings counter-rotate on independent gimbals and settle level as alignment completes.
        float tiltA=(float)(r.focusTilt*.85),tiltB=(float)(r.focusTilt*-.65);
        m.set(root).rotateY((float)r.focusSpin).rotateX(tiltA).rotateZ(tiltA*.4f);
        c.mesh(Layer.HULL_HELIOS,ringA,m,argb(Math.min(1,r.energy*1.2),tint),0,0);
        m.set(root).rotateY((float)-r.focusSpin*1.3f).rotateZ(tiltB).rotateX(tiltB*.3f);
        c.mesh(Layer.HULL_HELIOS,ringB,m,argb(Math.min(1,r.energy*1.2),tint),0,0);
        // Rotating frame: spokes, twelve collector segments extending outward, and five asymmetric solar vanes.
        frame.set(root).rotateY((float)r.frameAngle);
        for(int i=0;i<3;i++){m.set(frame).rotateY((float)(i*Math.PI*2/3+.5));c.mesh(Layer.HULL_HELIOS,spoke,m,hull,0,0);}
        float retract=(float)((1-r.ringExtend)*4.5);
        for(int i=0;i<12;i++) {
            if(i==4)continue;// deliberate gap where the long vane's maintenance boom sits
            double stagger=(1-r.ringExtend)*(i%2==0?.35:-.25);
            m.set(frame).rotateY((float)(i*Math.PI/6+stagger)).translate(-retract,(float)((1-r.ringExtend)*(i%3-1)*1.5),0);
            c.mesh(Layer.HULL_HELIOS,segment,m,hull,i*.31f,0);
        }
        for(int i=0;i<VANES.length;i++) {
            float deploy=(float)r.vane[i];
            float fold=(float)Math.toRadians(82*(1-deploy)-7*deploy);
            m.set(frame).rotateY(VANE_ANGLE[i]).translate(17.0f-retract,0,0).rotateZ(fold).rotateX((float)(.18*Math.sin(i*1.7)));
            c.mesh(Layer.HULL_HELIOS,VANES[i],m,argb(Math.min(1,r.energy*(.6+.4*deploy)),tint),i*.2f,0);
        }
        // Targeting pods swivel from stowed to tracking the confirmed ground target.
        Vector3f at=new Vector3f(),lens=new Vector3f();
        for(int i=0;i<POD_ANGLE.length;i++) {
            m.set(root).rotateY(POD_ANGLE[i]).translate(11.5f,-1.2f,0).getTranslation(at);
            // The canvas origin is the target, so -at points from the pod pivot to the ground target.
            float len=Math.max(1e-3f,at.length());
            float yawToTarget=(float)Math.atan2(-at.x,-at.z);
            float tilt=(float)(r.aim*Math.acos(Math.max(-1,Math.min(1,at.y/len))));
            m.translation(at).rotateY(yawToTarget).rotateX(-tilt);
            c.mesh(Layer.HULL_HELIOS,pod,m,hull,0,0);
            double sight=range(r.aim,.95,1)*(1-range(r.petals,.85,1)*.6)*(v.atOrAfter(Phase.FIRING)?0:1);
            if(sight>0) {
                m.transformPosition(lens.set(0,-1.8f,0));
                float flick=(float)(.75+.25*Math.sin(t*1.7+i*2.1));
                c.streak(Layer.GLOW,lens.x,lens.y,lens.z,0,.15f,0,.22f,.08f,argb(sight*flick*.85,0xff5a28),argb(sight*flick*.6,0xffb070),0,.5f,1,.5f);
                c.billboard(Layer.GLOW,lens.x,lens.y,lens.z,1.1f,0,argb(sight*.9,0xff8040));
            }
        }
    }

    private void drawCore(StrikeView v,Canvas c,ClientConfig.Quality q,Rig r,double t,float cy,float yaw) {
        float rc=(float)(1.1+1.6*r.charge);
        double pulse=.85+.15*Math.sin(t*1.9)+(v.in(Phase.CHARGING)?.25*Math.max(0,1-Fx.frac((t-26)/10.0)*3):0);
        float x=0,y=cy,z=0;
        boolean detailed=q.level>=1;
        float scroll=(float)(t*.03);
        double vis=.35+.65*Math.max(r.petals,.25);
        c.sphere(Layer.CORE_PLASMA,x,y,z,rc*.55f,argb(1,HOT),scroll,scroll*2,detailed);
        c.sphere(Layer.CORE_PLASMA,x,y,z,rc,argb(.9*pulse,GOLD),-scroll,scroll*1.3f,detailed);
        if(q.level>=1) {
            c.sphere(Layer.RIM_FLOW,x,y,z,rc*1.3f,argb(.55*vis,0xffd27a),scroll*2,-scroll,detailed);
            c.sphere(Layer.RIM_PLASMA,x,y,z,rc*1.62f,argb(.35*vis,EMBER),-scroll*1.5f,scroll*.5f,false);
        }
        c.billboard(Layer.GLOW,x,y,z,rc*(3.6f+(float)r.petals*2.2f),(float)(t*.02),argb((.35+.45*vis)*pulse,GOLD));
        c.billboard(Layer.GLOW,x,y,z,rc*1.6f,0,argb(.8,HOT));
        // Inward-moving charge motes converge from the vanes and rings into the reactor.
        if(r.charge>.3&&v.before(Phase.AFTERMATH)) {
            int count=Fx.scaled(q,64);
            double strength=range(r.charge,.3,1);
            for(int i=0;i<count;i++) {
                double h=v.hash(100+i),h2=v.hash(300+i);
                double p=Fx.frac(t*(.012+.02*h2)*(1+strength)+h);
                double a=h*Math.PI*2+yaw,elev=(h2-.5)*1.2;
                double dist=rc+(22-rc)*(1-p)*(1-p);
                float px=(float)(Math.cos(a)*Math.cos(elev)*dist),py=(float)(cy+Math.sin(elev)*dist*.5),pz=(float)(Math.sin(a)*Math.cos(elev)*dist);
                float k=(float)(1-.15);
                c.streak(Layer.GLOW,px,py,pz,px*k,cy+(py-cy)*k,pz*k,.06f,.18f,argb(0,GOLD),argb(strength*p*.9,WHITE_GOLD),0,.5f,1,.5f);
            }
        }
        if(r.petals>.4&&v.before(Phase.AFTERMATH)&&q.level>=1) {
            int arcs=q.level==2?6:3;long step=(long)(t/1.5);
            for(int i=0;i<arcs;i++) {
                java.util.Random rnd=new java.util.Random(v.seed()^(step*977+i));
                double a=rnd.nextDouble()*Math.PI*2,e=(rnd.nextDouble()-.5)*1.6;
                float sx=(float)(Math.cos(a)*Math.cos(e)),sy=(float)Math.sin(e),sz=(float)(Math.sin(a)*Math.cos(e));
                c.arc(Layer.GLOW,sx*rc,cy+sy*rc,sz*rc,sx*4.3f,cy+sy*4.3f,sz*4.3f,.8f,7,.12f,argb(.85*r.petals,0xfff0c0),rnd.nextLong());
            }
            // Prominences: short plasma loops escaping the containment field.
            float[] pts=new float[3*12];
            for(int k=0;k<2;k++) {
                double life=Fx.frac(t*.05+k*.5);if(life>.6)continue;
                double a=v.hash(900+(int)(t*.05+k*.5)*7+k)*Math.PI*2;float height=(float)(rc*(.6+1.6*Math.sin(life/.6*Math.PI)));
                for(int i=0;i<12;i++) {
                    double s=i/11.0,ang=a+(s-.5)*.9;
                    float rr=(float)(rc+Math.sin(s*Math.PI)*height);
                    pts[i*3]=(float)Math.cos(ang)*rr;pts[i*3+1]=cy+(float)((s-.5)*rc*.8);pts[i*3+2]=(float)Math.sin(ang)*rr;
                }
                c.ribbon(Layer.CORE_PLASMA,pts,12,.5f,.5f,argb(.8,GOLD),argb(.8,EMBER),(float)t*.05f);
            }
        }
        // Energy conduits along the spokes pulse inward during the charge.
        if(r.energy>.2&&q.level>=1) {
            for(int i=0;i<3;i++) {
                double a=r.frameAngle+i*Math.PI*2/3+.5+yaw;
                float ca=(float)Math.cos(a),sa=(float)Math.sin(a);
                c.tube(Layer.CORE_FLOW,ca*14.5f,cy+1.05f,-sa*14.5f,ca*5.5f,cy+1.05f,-sa*5.5f,.32f,.32f,6,argb(r.energy*.8,GOLD),argb(r.energy,WHITE_GOLD),1,0,(float)(t*.12),(float)(t*.12)+1.5f);
            }
        }
        // Distant charging pulses expand from the collector in step with the reactor's audio pulses.
        if(v.in(Phase.CHARGING)||v.in(Phase.ORBITAL_ALIGNMENT)) {
            for(int k=0;k<2;k++) {
                double p=Fx.frac((t-6)/10.0+k*.5);
                Fx.shockRing(c,0,cy,0,(float)(6+p*38),3,GOLD,(1-p)*.35*r.energy,(float)(t*.01),q);
            }
        }
    }

    private void drawGround(StrikeView v,Canvas c,ClientConfig.Quality q,Rig r,double t) {
        if(v.atOrAfter(Phase.IMPACT))return;
        double charge=v.through(Phase.CHARGING),entry=v.through(Phase.ATMOSPHERIC_ENTRY);
        double warm=.12*charge+.18*v.through(Phase.ORBITAL_ALIGNMENT)+.35*entry;
        float radius=v.radius();
        Fx.groundGlow(c,0,.08f,0,radius*(1.1f+(float)entry*.6f),0xff9a40,warm*(.85+.15*Math.sin(t*.8)));
        Fx.groundGlow(c,0,.1f,0,radius*.35f,0xffe2a0,warm*.8);
        if(q.level>=1)for(int i=0;i<3;i++) {
            double p=Fx.frac(t/40.0+i/3.0);
            Fx.shockRing(c,0,.12f,0,radius*(.3f+(float)p*1.1f),1.2f,GOLD,warm*.6*(1-p),(float)t*.02f,q);
        }
    }

    private void drawMarker(StrikeView v,Canvas c,ClientConfig.Quality q,double t) {
        double appear=v.in(Phase.ACQUIRING)?v.progress():1-v.through(Phase.CHARGING)*.6;
        float h=v.height()*(float)Math.min(1,v.age()/20.0);
        c.streak(Layer.GLOW,0,0,0,0,h,0,.35f,.08f,argb(.55*appear,GOLD),argb(0,GOLD),0,.5f,1,.5f);
        for(int i=0;i<3;i++)Fx.shockRing(c,0,.15f,0,v.radius()*(.5f+i*.35f)*(float)smooth(Math.min(1,v.age()/15.0)),.5f,GOLD,.5*appear,(float)(t*.03*(i%2==0?1:-1)),q);
    }

    private void drawBeam(StrikeView v,Canvas c,ClientConfig.Quality q,Rig r,double t,float cy) {
        if(v.before(Phase.FIRING))return;
        float H=v.height(),top=cy+NOZZLE;
        float head=beamHead(v);double intensity=1;float width=1.7f;
        if(v.in(Phase.ATMOSPHERIC_ENTRY))width=1.7f+.6f*(float)v.progress();
        if(v.in(Phase.IMPACT))width=2.6f-(float)v.progress()*.4f;
        if(v.atOrAfter(Phase.AFTERMATH)) {
            double a=range(v.through(Phase.AFTERMATH),0,.16);
            if(a>=1)return;
            intensity=1-a;width=2.2f*(float)(1-a*.7);top=top-(top-0)*(float)smooth(a);
        }
        if(v.atOrAfter(Phase.COOLDOWN))return;
        Fx.beam(c,0,top,head,0,width,t,HOT,GOLD,EMBER,intensity,q,v.seed());
        // Firing transient at the aperture.
        if(v.in(Phase.FIRING)) {
            double p=v.progress();
            Fx.flash(c,0,top,0,(float)(26*(1-p)+8),WHITE_GOLD,(1-p)*1.1);
            Fx.shockRing(c,0,top,0,(float)(4+Fx.easeOut(p*1.6)*46),5,GOLD,(1-Math.min(1,p*1.6))*.9,0,q);
            Fx.shockRing(c,0,top-2,0,(float)(3+Fx.easeOut(p*2.2)*28),3,WHITE_GOLD,(1-Math.min(1,p*2.2))*.8,1.3f,q);
        }
        // Head of the descending column: compressed plasma sheath and fast luminous streaks.
        if(v.in(Phase.FIRING)||v.in(Phase.ATMOSPHERIC_ENTRY)) {
            double speed=v.in(Phase.ATMOSPHERIC_ENTRY)?1:.6;
            c.tube(Layer.RIM_FLOW,0,head,0,0,head+12,0,width*2.1f,width*1.1f,Fx.segments(q,24),argb(.75*speed,0xffd27a),argb(0,EMBER),2,(float)(t*.2),0,1);
            c.tube(Layer.CORE_FLOW,0,head-.5f,0,0,head+5,0,width*1.9f,width*.9f,Fx.segments(q,20),argb(.9,WHITE_GOLD),argb(.2,GOLD),1,(float)(t*.3),0,.6f);
            Fx.flash(c,0,head,0,width*5,WHITE_GOLD,.8);
            int streaks=Fx.scaled(q,36);
            for(int i=0;i<streaks;i++) {
                double h=v.hash(500+i);double p=Fx.frac(t*(.06+.05*v.hash(700+i))+h);
                double a=h*Math.PI*14;float rr=(float)(width*(2.5+3*v.hash(800+i)));
                float y=head+(float)(p*30);float len=(float)(3+5*speed);
                c.streak(Layer.GLOW,(float)Math.cos(a)*rr,y,(float)Math.sin(a)*rr,(float)Math.cos(a)*rr*1.05f,y+len,(float)Math.sin(a)*rr*1.05f,
                        .08f,.16f,argb((1-p)*.8*speed,0xffe6b0),argb(0,EMBER),0,.5f,1,.5f);
            }
        }
    }

    private void drawImpact(StrikeView v,Canvas c,ClientConfig.Quality q,double t) {
        double s=v.sinceImpact();
        if(s<0) {
            // Late joiners never receive the transient cue; reconstruct impact time from the authoritative phase.
            if(!v.atOrAfter(Phase.IMPACT))return;
            s=v.in(Phase.IMPACT)?v.progress()*10:10+v.through(Phase.AFTERMATH)*100;
        }
        float radius=v.radius(),depth=v.depth();
        // 1. Contact flash and incandescent core.
        if(s<8)Fx.flash(c,0,2,0,radius*2.2f*(float)(1-s/10),HOT,(1-s/8)*1.2);
        if(s<34) {
            float core=radius*.75f*Fx.easeOut(s/7);
            int col=Canvas.lerpRgb(Canvas.lerpRgb(HOT,GOLD,s/10),EMBER,(s-10)/24);
            c.sphere(Layer.CORE_PLASMA,0,core*.25f,0,core,argb((1-s/34)*.95,col),(float)(t*.02),0,q.level>=1);
            c.sphere(Layer.CORE_PLASMA,0,core*.2f,0,core*.6f,argb((1-s/34),HOT),0,(float)(t*.03),false);
            c.billboard(Layer.GLOW,0,core*.4f,0,core*2.2f,0,argb((1-s/34)*.55,col));
        }
        // 2. Vertical energy pillar.
        if(s<22)c.tube(Layer.CORE_FLOW,0,0,0,0,90,0,radius*.32f*(float)(1-s/22),radius*.05f,Fx.segments(q,20),argb((1-s/22)*.9,WHITE_GOLD),argb(0,GOLD),1,0,(float)(t*.1),(float)(t*.1)+3);
        // 3. Layered shockwaves and the ground-hugging pressure wall.
        Fx.shockRing(c,0,.6f,0,radius*3.4f*Fx.easeOut(s/16),6,WHITE_GOLD,(1-s/16)*.95,(float)(s*.01),q);
        Fx.shockRing(c,0,.4f,0,radius*2.3f*Fx.easeOut((s-3)/26),4,GOLD,(1-(s-3)/26)*.8*(s>3?1:0),1.1f,q);
        Fx.shockRing(c,0,3,0,radius*1.7f*Fx.easeOut((s-1)/20),3,EMBER,(1-(s-1)/20)*.6*(s>1?1:0),2.3f,q);
        Fx.shockWall(c,0,0,0,radius*1.8f*Fx.easeOut(s/20),7*(float)(1-s/20),0xffc070,(1-s/20)*.7,q,(float)(t*.05));
        // 4. Heat shimmer shell (subtle, opt-in through the distortion preference).
        if(com.nullstar.client.NullstarClient.config().screenDistortion&&s<40&&q.level==2)
            c.billboard(Layer.RIM_PLASMA,0,radius*.4f,0,radius*(1.2f+(float)s/30),(float)(t*.01),argb((1-s/40)*.12,0xffd9a0));
        // 5. Aftermath: fading rings and lingering plasma pooling in the crater floor.
        if(v.atOrAfter(Phase.AFTERMATH)&&v.before(Phase.COMPLETE)) {
            double a=v.through(Phase.AFTERMATH);
            for(int i=0;i<2;i++)Fx.shockRing(c,0,.3f,0,radius*(1.05f+.25f*i+(float)a*.6f),1.5f,GOLD,.28*(1-a)*(i==0?1:.6),(float)(t*.004*(i==0?1:-1)),q);
            int pools=q.level==2?7:q.level==1?4:2;
            for(int i=0;i<pools;i++) {
                double ang=v.hash(40+i)*Math.PI*2,rr=radius*.45*v.hash(60+i);
                float flick=(float)(.7+.3*Math.sin(t*.4+i*1.7));
                Fx.groundGlow(c,(float)(Math.cos(ang)*rr),-depth*.55f,(float)(Math.sin(ang)*rr),radius*(.25f+.2f*(float)v.hash(80+i)),EMBER,(1-a)*.55*flick);
            }
            Fx.groundGlow(c,0,-depth*.6f,0,radius*.6f,0xff7a28,(1-a)*.45);
        }
    }

    // ------------------------------------------------------------------ particles
    @Override public void tick(StrikeView v,CosmeticParticles p,ClientLevel level,ClientConfig.Quality q) {
        int owner=Fx.owner(v);Vec3 target=v.target();
        if(v.in(Phase.CHARGING)||v.in(Phase.ORBITAL_ALIGNMENT)||v.in(Phase.ATMOSPHERIC_ENTRY)) {
            // Small ascending charged particles at the target.
            int n=Math.max(0,Fx.scaled(q,v.in(Phase.ATMOSPHERIC_ENTRY)?6:3));
            for(int i=0;i<n;i++) {
                double a=p.random()*Math.PI*2,rr=v.radius()*p.random();
                p.spawn(CosmeticParticles.MOTE,owner,target.x+Math.cos(a)*rr,target.y+.3,target.z+Math.sin(a)*rr,0,.08+p.random()*.12,0,40+p.random()*30,.15f+p.random()*.2f,0xd0ffc870);
            }
        }
        if(v.in(Phase.ATMOSPHERIC_ENTRY)&&v.progress()>.6) {
            // Pressure from the incoming column lifts dust before contact.
            Fx.dustRing(p,owner,target,Fx.scaled(q,2),v.radius()*.8f,.25f,Canvas.lerpRgb(Fx.groundColor(level,target),0x9a8a78,.4),2.5f);
        }
        if(v.in(Phase.AFTERMATH)) {
            double a=v.progress();
            int tint=Canvas.lerpRgb(Fx.groundColor(level,target),0x6a625c,.55);
            if(p.random()<(1-a)*.9)Fx.column(p,owner,target.add(0,-v.depth()*.4,0),1,v.radius()*.4f,.18f,tint,4.5f);
            if(p.random()<(1-a))Fx.embers(p,owner,target.add(0,-v.depth()*.5,0),Math.max(1,Fx.scaled(q,2)),v.radius()*.5f,.12f,0xff8a30);
        }
    }

    @Override public void event(StrikeView v,StrikeView.Event e,CosmeticParticles p,ClientLevel level,ClientConfig.Quality q) {
        if(!e.impact())return;
        int owner=Fx.owner(v);Vec3 at=e.position();float radius=e.payload().radius();
        int ground=Fx.groundColor(level,at);
        Fx.debrisBurst(p,owner,at,Fx.scaled(q,140),1.3f,1.05f,ground,.35f,1.4f,1);
        Fx.sparks(p,owner,at,Fx.scaled(q,60),1.6f,0xffd27a);
        Fx.embers(p,owner,at,Fx.scaled(q,80),radius*.6f,.35f,0xffa040);
        int dust=Canvas.lerpRgb(ground,0x9a8f84,.45);
        Fx.dustRing(p,owner,at,Fx.scaled(q,48),radius*.9f,.75f,dust,4.5f);
        Fx.column(p,owner,at,Fx.scaled(q,36),radius*.35f,.55f,Canvas.lerpRgb(dust,0x5a524c,.4),6);
    }
}
