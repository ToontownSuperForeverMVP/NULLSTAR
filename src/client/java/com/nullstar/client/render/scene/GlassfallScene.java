package com.nullstar.client.render.scene;

import com.nullstar.Nullstar;
import com.nullstar.client.ClientConfig;
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
import org.joml.Vector3f;
import java.util.ArrayList;
import java.util.List;
import static com.nullstar.client.render.Canvas.argb;
import static com.nullstar.client.render.StrikeView.range;
import static com.nullstar.client.render.StrikeView.smooth;
import static com.nullstar.client.render.StrikeView.smoother;

/**
 * GLASSFALL ARRAY: an orbital bombardment lattice that unfolds, aims one launch rail per authoritative impact
 * and fires crystalline projectiles along deterministic curves that arrive exactly at the server's impact cues.
 *
 * <p>Colossal profiles turn it into a regional bombardment platform: the lattice grows through
 * {@link PresentationScale#structure}, hangs at {@link PresentationScale#altitude}, and its aftermath reads out to
 * {@link PresentationScale#shockReach}. STANDARD resolves to the unchanged Part 2 geometry and the bundled
 * five-projectile pattern.
 */
public final class GlassfallScene implements WeaponScene {
    static final int CYAN=0x56e6ff,ICE=0xd8fbff,DEEP=0x1a7fb0,FROST=0x9fc4d8;
    private static final float[][] DEFAULT_OFFSETS={{-9,-7},{8,-6},{-7,9},{9,8},{0,0}};
    private static Mesh hub,lens,octagon,square,arm,rail,crystal,crystalLarge,spike;
    private static Mesh outerRing,outerRingFine,spine,cradle,capacitor,boom,ringSmall;
    /** Structure factor: 1 STANDARD, ~2.9 TITANIC, ~4.4 APOCALYPSE. */
    private static float factor(StrikeView v){return PresentationScale.structure(v);}
    /** Distance from the origin to the launch rails and the platform framing in rail space. */
    private static final float MOUNT_RADIUS=8.5f,HELD_DEPTH=-5.5f;
    private final Matrix4f root=new Matrix4f(),m=new Matrix4f();
    private final Vector3f tmp=new Vector3f();

    private static void build() {
        if(hub!=null)return;
        hub=new Mesh.Builder().frustum(3.6f,3.6f,-2f,2f,8,true).frustum(2.4f,1.2f,2f,4.5f,8,true).ring(2.2f,3.9f,-2.8f,-2.2f,0,Math.PI*2,8)
                .box(-.25f,4.5f,-.25f,.25f,8f,.25f).build();
        lens=new Mesh.Builder().ring(1.4f,2.6f,-.3f,.3f,0,Math.PI*2,6).build();
        octagon=new Mesh.Builder().ring(10.8f,12.2f,-.7f,.7f,Math.PI/8,Math.PI*2+Math.PI/8,8).ring(11.2f,11.8f,-1.5f,1.5f,Math.PI/8,Math.PI/8+Math.PI/4*.2,1).build();
        Mesh.Builder sq=new Mesh.Builder().ring(15.2f,16.8f,-.55f,.55f,Math.PI/4,Math.PI*2+Math.PI/4,4);
        for(int i=0;i<4;i++)sq.push().rotateY((float)(i*Math.PI/2+Math.PI/4)).box(15.6f,-.9f,-.9f,17.6f,.9f,.9f).frustum(.5f,.2f,.9f,3.2f,4,true).pop();
        for(int i=0;i<4;i++)sq.push().rotateY((float)(i*Math.PI/2)).box(9.5f,-.35f,-.35f,15.4f,.35f,.35f).pop();
        square=sq.build();
        Mesh.Builder a=new Mesh.Builder();
        a.box(0,-.35f,-.9f,1,.35f,-.5f).box(0,-.35f,.5f,1,.35f,.9f);
        for(float x=.1f;x<1;x+=.25f)a.box(x,-.25f,-.6f,x+.06f,.25f,.6f);
        arm=a.build();
        rail=new Mesh.Builder().box(-.35f,-10f,-.9f,.35f,0,-.55f).box(-.35f,-10f,.55f,.35f,0,.9f).box(-.8f,-1.2f,-1.3f,.8f,0,1.3f)
                .box(-.6f,-10.6f,-1.1f,.6f,-9.6f,1.1f).build();
        crystal=new Mesh.Builder().crystal(.8f,2.4f,3.6f,6,.25f).build();
        crystalLarge=new Mesh.Builder().crystal(1.7f,4.4f,7.5f,6,.3f).push().rotateY(.5f).translate(0,-1,0).crystal(1.2f,3.2f,5.6f,6,.4f).pop().build();
        spike=new Mesh.Builder().crystal(.7f,.1f,4.5f,5,0).build();
        // Outer bombardment lattice: a heavy ring of docking cradles and aiming booms around the inner frames.
        Mesh.Builder or=new Mesh.Builder().ring(25.5f,27.5f,-1f,1f,0,Math.PI*2,32).ring(24.4f,25.5f,-2.4f,2.4f,0,Math.PI*2,20);
        for(int i=0;i<16;i++)or.push().rotateY((float)(i*Math.PI/8)).box(27f,-.55f,-.7f,30.2f,.55f,.7f).box(28.4f,1f,-1.5f,29.8f,3.4f,1.5f).pop();
        outerRing=or.build();
        // Second, finer counter-rotating ring that only reads at MEDIUM quality and above.
        Mesh.Builder or2=new Mesh.Builder().ring(36f,38.6f,-.6f,.6f,0,Math.PI*2,40);
        for(int i=0;i<24;i++)or2.push().rotateY((float)(i*Math.PI/12)).box(38.2f,-1.6f,-1.1f,40.6f,1.6f,1.1f).pop();
        for(int i=0;i<6;i++)or2.push().rotateY((float)(i*Math.PI/3+.26)).box(30f,-2.6f,-1.4f,38.4f,2.6f,1.4f).pop();
        outerRingFine=or2.build();
        // Spine: reactor corridors running through the platform, with collar rings and a sensor mast.
        spine=new Mesh.Builder().frustum(4.6f,3.4f,-16f,-2f,10,true).frustum(2.6f,1.4f,4.5f,11f,10,true)
                .ring(4.4f,5.6f,-13f,-12f,0,Math.PI*2,12).ring(4.4f,5.6f,-6f,-5f,0,Math.PI*2,12)
                .box(-.5f,11f,-.5f,.5f,16f,.5f).build();
        cradle=new Mesh.Builder().box(-1.6f,-1.1f,-2.6f,1.6f,1.1f,2.6f).box(-1.1f,1.1f,-1.1f,1.1f,2.2f,1.1f).build();
        capacitor=new Mesh.Builder().box(-.9f,-1.4f,-2.4f,.9f,1.4f,2.4f).box(-1.1f,-.6f,-2.6f,1.1f,.6f,2.6f).box(-.3f,1.4f,-.5f,.3f,2.6f,.5f).build();
        boom=new Mesh.Builder().box(-.55f,-12f,-.55f,.55f,0,.55f).box(-1.1f,-12.5f,-1.1f,1.1f,-11.2f,1.1f).build();
        ringSmall=new Mesh.Builder().ring(6.4f,7.2f,-.4f,.4f,0,Math.PI*2,20).build();
    }
    static float[][] offsets() {
        var catalog=Nullstar.catalog();
        if(catalog==null)return DEFAULT_OFFSETS;
        List<float[]> list=new ArrayList<>();
        for(var cue:catalog.get("glassfall").events())if(cue.effect().equals("projectile"))list.add(new float[]{(float)cue.x(),(float)cue.z()});
        return list.isEmpty()?DEFAULT_OFFSETS:list.toArray(float[][]::new);
    }
    static float[][] offsets(StrikeView view) {
        if(view.scale().tier()==com.nullstar.core.ScaleTier.STANDARD)return offsets();
        int count=view.scale().subimpacts();float[][] offsets=new float[count+1][2];
        // The profile footprint is independent of safety-clipped crater radii.
        int radius=view.scale().footprintRadius();
        for(int i=0;i<count;i++) {
            double angle=i*Math.PI*2/Math.max(1,count),distance=radius*(i%2==0?.65:.92);
            var offset=com.nullstar.core.ImpactFootprint.offset(Math.cos(angle)*distance,Math.sin(angle)*distance,radius,radius,
                    Math.max(1,(int)Math.round(view.radius()*.22)),view.scale().secondaryRadius());
            offsets[i][0]=offset.x();offsets[i][1]=offset.z();
        }
        for(var event:view.events())if(!event.impact()) {
            int index=0;for(var prior:view.events()){if(prior==event)break;if(!prior.impact())index++;}
            if(index<offsets.length){offsets[index][0]=(float)(event.position().x-view.target().x);offsets[index][1]=(float)(event.position().z-view.target().z);}
        }
        return offsets;
    }
    public static float latticeY(StrikeView v) {
        double descend=smoother(range(v.through(Phase.CHARGING),0,.6));
        double ascend=smoother(range(v.through(Phase.AFTERMATH),.2,1));
        return (float)(PresentationScale.altitude(v)+(1-descend)*30+ascend*44);
    }
    /** Rail mount (origin-relative) for projectile index i. Colossal platforms grow their mount radius. */
    static Vector3f railMount(StrikeView v,float[][] offsets,int i,float y) {
        float s=factor(v);
        float[] o=offsets[Math.min(i,offsets.length-1)];
        float len=(float)Math.hypot(o[0],o[1]);
        if(len<.5f)return new Vector3f(0,y-3.2f*s,0);
        return new Vector3f(o[0]/len*MOUNT_RADIUS*s,y-1.2f*s,o[1]/len*MOUNT_RADIUS*s);
    }
    /** Origin-relative launch point of a rail's held projectile (the head of the curve at k=0). */
    private static Vector3f launchPoint(StrikeView v,float[][] offsets,int i,float ly,Matrix4f scratch) {
        return railMatrix(v,offsets,i,ly,1,scratch).transformPosition(new Vector3f(0,HELD_DEPTH*factor(v),0));
    }
    /** Curve bend, shared by the drawn flight and the camera so both follow the same path. */
    private static double bendFor(StrikeView v,int i,boolean last) {
        return (v.hash(1000+i)-.5)*(last?4:10)*(1+.6*PresentationScale.colossal(v));
    }

    /** Rail transform: hinged at its mount, its -Y axis swings from straight down to the impact direction. */
    static Matrix4f railMatrix(StrikeView v,float[][] offsets,int i,float ly,double lock,Matrix4f out) {
        Vector3f mount=railMount(v,offsets,i,ly);
        float[] o=offsets[Math.min(i,offsets.length-1)];
        float dx=o[0]-mount.x,dy=-mount.y,dz=o[1]-mount.z,len=(float)Math.sqrt(dx*dx+dy*dy+dz*dz);
        float tilt=(float)(lock*Math.acos(Math.max(-1,Math.min(1,-dy/len))));
        return out.translation(mount).rotateY((float)Math.atan2(dx,dz)).rotateX(-tilt);
    }
    // ------------------------------------------------------------------ projectile flight (shared with the camera)
    /** Position along a launched projectile's curve at progress k (0..1), origin-relative. */
    static Vector3f flight(Vector3f start,Vec3 end,double k,double bend,Vector3f out) {
        double ex=end.x,ey=end.y,ez=end.z;
        double mx=(start.x+ex)*.5,mz=(start.z+ez)*.5,dx=ex-start.x,dz=ez-start.z,len=Math.max(1e-3,Math.hypot(dx,dz));
        double cx=mx-dz/len*bend,cy=(start.y+ey)*.5+6,cz=mz+dx/len*bend;
        double a=(1-k)*(1-k),b=2*(1-k)*k,c=k*k;
        return out.set((float)(a*start.x+b*cx+c*ex),(float)(a*start.y+b*cy+c*ey),(float)(a*start.z+b*cz+c*ez));
    }
    static double flightProgress(double raw){return Math.pow(Math.max(0,Math.min(1,raw)),1.35);}

    /** Camera focus: tracks the incoming crystal along its real flight curve, then its impact, else the target. */
    public static Vec3 cameraFocus(StrikeView v) {
        Vec3 focus=v.target().add(0,2,0);
        float[][] offsets=offsets(v);
        int projectiles=0;
        for(var e:v.events())if(!e.impact())projectiles++;
        for(int i=v.events().size()-1;i>=0;i--) {
            var e=v.events().get(i);
            if(!e.impact()) {
                projectiles--;
                double raw=e.progress(v.now());
                if(raw>=1)continue;
                boolean last=projectiles>=offsets.length-1||e.payload().radius()>=v.radius()*.8;
                Vector3f start=launchPoint(v,offsets,projectiles,latticeY(v),new Matrix4f());
                Vector3f head=flight(start,e.position().subtract(v.target()),flightProgress(raw),bendFor(v,projectiles,last),new Vector3f());
                // Lead slightly toward the ground so the impact site is framed before contact.
                return v.target().add(head.x,head.y,head.z).lerp(e.position(),.35);
            }
            if(e.age(v.now())<14)return focus.lerp(e.position(),.45);
        }
        return focus;
    }

    // ------------------------------------------------------------------ rendering
    @Override public void render(StrikeView v,Canvas c,ClientConfig.Quality q) {
        build();
        double t=v.age();
        float[][] offsets=offsets(v);
        float ly=latticeY(v);
        boolean visible=!v.before(Phase.CHARGING)&&!v.atOrAfter(Phase.COOLDOWN)&&!v.atOrAfter(Phase.COMPLETE);
        if(visible)drawLattice(v,c,q,t,ly,offsets);
        drawProjectiles(v,c,q,t,ly,offsets);
        drawImpacts(v,c,q,t);
        if(v.in(Phase.ACQUIRING)||v.in(Phase.CHARGING)) {
            double appear=v.in(Phase.ACQUIRING)?v.progress():1-v.progress()*.7;
            // Colossal profiles mark their real devastation footprint, not just the primary crater.
            float mark=PresentationScale.secondaryReach(v);
            for(float[] o:offsets)Fx.shockRing(c,o[0],.12f,o[1],(o[0]==0&&o[1]==0?mark:mark*.35f)*(float)smooth(Math.min(1,v.age()/16)),.4f,CYAN,.6*appear,(float)(t*.04),q);
        }
    }

    private void drawLattice(StrikeView v,Canvas c,ClientConfig.Quality q,double t,float ly,float[][] offsets) {
        double charge=v.through(Phase.CHARGING),align=v.through(Phase.ORBITAL_ALIGNMENT),after=v.through(Phase.AFTERMATH);
        double unfold=smoother(range(charge,.25,1))*(1-.6*smoother(range(after,.1,.6)));
        double lock=smoother(align);
        double energy=.15+.4*charge+.45*align;
        if(v.atOrAfter(Phase.AFTERMATH))energy=Math.max(.05,1-after*2);
        int hull=argb(Math.min(1,energy),0xa0a8b0);
        float s=factor(v);
        boolean detail=q.level>=1;
        // Every structure added for the bombardment platform is colossal-only, so STANDARD keeps the Part 2 lattice.
        boolean big=PresentationScale.colossal(v)>0;
        float yaw=(float)(v.hash(5)*Math.PI*.5);
        // The root carries the colossal structure factor, so every framed component scales together.
        root.translation(0,ly,0).scale(s,s,s);
        if(big)c.mesh(Layer.HULL_GLASSFALL,spine,new Matrix4f(root).rotateY(yaw),hull,0,0);
        c.mesh(Layer.HULL_GLASSFALL,hub,new Matrix4f(root).rotateY(yaw),hull,0,0);
        // Interlocking frames counter-rotate during the unfold and snap into register on lock.
        float spinA=(float)((1-lock)*(t*.03)+lock*Math.PI/8+yaw),spinB=(float)(-(1-lock)*(t*.02)+yaw);
        float scaleFrames=(float)(.55+.45*unfold);
        m.set(root).rotateY(spinA).scale(scaleFrames,1,scaleFrames);
        c.mesh(Layer.HULL_GLASSFALL,octagon,m,hull,0,0);
        m.set(root).rotateY(spinB).rotateX((float)((1-lock)*.25)).scale(scaleFrames,1,scaleFrames);
        c.mesh(Layer.HULL_GLASSFALL,square,m,hull,.3f,0);
        // Outer bombardment lattice: extends outward as the array unfolds, then counter-rotates freely.
        double ringExtend=.42+.58*unfold,ringExtend2=.36+.64*unfold;
        float spinOut=(float)((1-lock)*(t*.018)+lock*(yaw+.18));
        if(big) {
            m.set(root).rotateY(spinOut).scale((float)ringExtend,(float)(.5+.5*unfold),(float)ringExtend);
            c.mesh(Layer.HULL_GLASSFALL,outerRing,m,hull,0,0);
            if(detail) {
                m.set(root).rotateY((float)(-spinOut*.55)).rotateX((float)((1-lock)*.12)).scale((float)ringExtend2,1,(float)ringExtend2);
                c.mesh(Layer.HULL_GLASSFALL,outerRingFine,m,hull,.15f,0);
            }
        }
        // Docking cradles, capacitor banks and aiming booms mounted around the outer ring.
        int mounts=detail?12:8;
        if(big)for(int i=0;i<mounts;i++) {
            double a=spinOut+i*Math.PI*2/mounts;
            float cx=(float)(Math.cos(a)*29.2f*ringExtend),cz=(float)(Math.sin(a)*29.2f*ringExtend);
            float bob=(float)(Math.sin(t*.07+i)*.6);
            m.set(root).translate(cx,1.4f+bob,cz).rotateY((float)a);
            c.mesh(Layer.HULL_GLASSFALL,cradle,m,hull,i*.11f,0);
            if(detail) {m.set(root).translate(cx*.86f,-3.4f+bob,cz*.86f).rotateY((float)(-a*.5));c.mesh(Layer.HULL_GLASSFALL,capacitor,m,hull,0,0);}
            // Aiming booms point down toward the barrage footprint.
            m.set(root).translate(cx,-2.2f,cz).rotateY((float)(a+Math.PI)).rotateX((float)(.22*Math.sin(t*.05+i)));
            c.mesh(Layer.HULL_GLASSFALL,boom,m,argb(Math.min(1,energy*.8),0xa0a8b0),0,0);
        }
        // Telescoping truss arms from the hub to the square frame's corners.
        float reach=(float)(3.6+(15.4-3.6)*unfold);
        for(int i=0;i<4;i++) {
            m.set(root).rotateY(spinB+(float)(i*Math.PI/2+Math.PI/4)).translate(3.4f,0,0).scale(reach*scaleFrames-3.4f*scaleFrames+.01f,1,1);
            c.mesh(Layer.HULL_GLASSFALL,arm,m,hull,i*.2f,0);
            if(detail) {
                double a=-(spinB+i*Math.PI/2+Math.PI/4);
                float ex=(float)(Math.cos(a)*reach*scaleFrames*s),ez=(float)(Math.sin(a)*reach*scaleFrames*s);
                c.tube(Layer.CORE_FLOW,ex,ly+.45f*s,ez,(float)(Math.cos(a)*3.6f*s),ly+.45f*s,(float)(Math.sin(a)*3.6f*s),.18f*s,.18f*s,5,argb(energy*.8,CYAN),argb(energy,ICE),1,0,(float)(t*.15),(float)(t*.15)+2);
            }
        }
        // Tension struts from the spine out to the outer lattice, tightening with the deployment.
        int struts=detail?8:4;
        if(big)for(int i=0;i<struts;i++) {
            double a=spinOut*.6+i*Math.PI*2/struts;
            float ex=(float)(Math.cos(a)*26f*ringExtend),ez=(float)(Math.sin(a)*26f*ringExtend);
            c.tube(Layer.HULL_GLASSFALL,(float)(Math.cos(a)*2.2f*s),ly,(float)(Math.sin(a)*2.2f*s),ex,ly+2.2f*s,ez,.28f*s,.14f*s,5,argb(Math.min(1,energy),0x8c98a4),argb(Math.min(1,energy),0x8c98a4),2,(float)i,0,1);
        }
        // Launch rails: hinge from stowed to aiming along each authoritative impact direction.
        int launched=launchedCount(v);
        for(int i=0;i<offsets.length;i++) {
            railMatrix(v,offsets,i,ly,lock,m);
            c.mesh(Layer.HULL_GLASSFALL,rail,new Matrix4f(m).scale(s),hull,i*.37f,0);
            if(i>=launched&&v.before(Phase.IMPACT)) {
                boolean last=i==offsets.length-1;
                float pulse=(float)(.75+.25*Math.sin(t*.6+i));
                Matrix4f held=new Matrix4f(m).scale(s).translate(0,HELD_DEPTH,0).scale(last?.8f:1);
                c.mesh(Layer.CORE_CRYSTAL,last?crystalLarge:crystal,held,argb(.55+.4*energy*pulse,ICE),(float)(t*.01),0);
                if(q.level>=1)c.mesh(Layer.RIM_CRYSTAL,last?crystalLarge:crystal,new Matrix4f(held).scale(1.3f),argb(.35*energy*pulse,CYAN),0,(float)(t*.02));
                held.transformPosition(tmp.set(0,0,0));
                c.billboard(Layer.GLOW,tmp.x,tmp.y,tmp.z,(last?4.5f:2.6f)*(big?s:1),0,argb(.5*energy*pulse,CYAN));
            }
        }
        // Central focusing mechanism: a counter-rotating lens stack with a glowing crystal focus.
        for(int k=0;k<3;k++){m.set(root).translate(0,-3.4f-k*.9f,0).rotateY((float)(t*.05*(k%2==0?1:-1))).scale(1-k*.18f,1,1-k*.18f);c.mesh(Layer.HULL_GLASSFALL,lens,m,hull,0,0);}
        if(big)for(int k=0;k<3;k++){m.set(root).translate(0,-6.2f-k*1.4f,0).rotateY((float)(-t*.04*(k%2==0?1:-1)*(1-lock))).scale(1-k*.22f,1,1-k*.22f);c.mesh(Layer.HULL_GLASSFALL,ringSmall,m,hull,0,0);}
        c.billboard(Layer.GLOW,0,ly-4.5f*s,0,(3.5f+(float)energy*3)*s,(float)t*.02f,argb(.4+.4*energy,CYAN));
        if(v.in(Phase.ORBITAL_ALIGNMENT)||v.in(Phase.CHARGING)) {
            double p=Fx.frac(t/12.0);
            Fx.shockRing(c,0,ly,0,(float)(5+p*30)*s,2*s,CYAN,(1-p)*.4*energy,0,q);
        }
    }
    private static int launchedCount(StrikeView v){int n=0;for(var e:v.events())if(!e.impact())n++;return n;}

    private void drawProjectiles(StrikeView v,Canvas c,ClientConfig.Quality q,double t,float ly,float[][] offsets) {
        int index=0;
        float s=factor(v);
        double colossal=PresentationScale.colossal(v);
        for(var e:v.events()) {
            if(e.impact())continue;
            int i=index++;
            double raw=e.progress(v.now());
            if(raw>=1)continue;
            boolean last=i>=offsets.length-1||e.payload().radius()>=v.radius()*.8;
            Vector3f start=launchPoint(v,offsets,i,ly,new Matrix4f());
            Vec3 end=e.position().subtract(v.target());
            double bend=bendFor(v,i,last);
            double k=flightProgress(raw);
            Vector3f head=flight(start,end,k,bend,new Vector3f()),ahead=flight(start,end,Math.min(1,k+.02),bend,new Vector3f());
            Vector3f dir=new Vector3f(ahead).sub(head);if(dir.lengthSquared()<1e-8f)dir.set(0,-1,0);dir.normalize();
            // Every enlargement below is colossal-only, so a STANDARD projectile stays exactly the Part 2 crystal.
            boolean big=colossal>0;
            float size=(last?2.1f:1.35f)*(big?s:1);
            float glowScale=big?s*.55f:1f,flashScale=big?s*.6f:1f;
            // Crystal oriented along its flight tangent (mesh tip is -Y).
            // rotateTowards aligns +Z with the tangent (up hint X avoids the vertical singularity); -90 deg about X then maps the -Y tip onto +Z.
            m.translation(head).rotateTowards(dir.x,dir.y,dir.z,1,0,0).rotateX((float)(-Math.PI/2)).rotateY((float)(t*.4)).scale(size);
            // Layered shell: an internal energy core, the crystalline body, then a pseudo-refractive outer rind.
            if(big&&q.level>=1)c.sphere(Layer.CORE_PLASMA,head.x,head.y,head.z,size*.42f,argb(.9,ICE),(float)(t*.03),0,false);
            c.mesh(Layer.CORE_CRYSTAL,last?crystalLarge:crystal,m,argb(.95,ICE),(float)(t*.02),0);
            if(q.level>=1) {
                c.mesh(Layer.RIM_CRYSTAL,last?crystalLarge:crystal,new Matrix4f(m).scale(1.3f),argb(.55,CYAN),0,(float)(t*.02));
                if(big) {
                    c.mesh(Layer.RIM_CRYSTAL,last?crystalLarge:crystal,new Matrix4f(m).scale(1.62f),argb(.3,DEEP),0,(float)(t*.02));
                    // Refractive rim: a camera-facing annulus around the shell, wide enough to read at distance.
                    Vec3 cam=c.cameraLocal();double cl=Math.max(1e-4,cam.length());
                    c.annulus(Layer.RIM_CRYSTAL,head.x,head.y,head.z,(float)(cam.x/cl),(float)(cam.y/cl),(float)(cam.z/cl),
                            size*.95f,size*(1.5f+.3f*(float)Math.sin(t*.5)),10,argb(.5,ICE),argb(0,CYAN),2,(float)t*.06f,0,1);
                }
            }
            c.billboard(Layer.GLOW,head.x,head.y,head.z,(last?9:5.5f)*glowScale,0,argb(.8,CYAN));
            c.billboard(Layer.GLOW,head.x,head.y,head.z,(last?3:2f)*glowScale,0,argb(.95,ICE));
            // Trail: analytic samples behind the head, so it never drifts from the authoritative endpoint.
            int samples=big?(q.level==2?36:q.level==1?22:12):(q.level==2?28:q.level==1?18:10);float[] pts=new float[samples*3];
            Vector3f p=new Vector3f();
            for(int n=0;n<samples;n++){double kk=Math.max(0,k-n*(last?.012:.009)*(1+.4*colossal));flight(start,end,kk,bend,p);pts[n*3]=p.x;pts[n*3+1]=p.y;pts[n*3+2]=p.z;}
            c.ribbon(Layer.CORE_FLOW,pts,samples,size*.7f,size*.05f,argb(.85,ICE),argb(0,DEEP),(float)(-t*.3));
            if(q.level>=1)for(int f=0;f<2;f++) {
                float[] fil=new float[samples*3];
                for(int n=0;n<samples;n++) {
                    double a=t*.5+n*.6+f*Math.PI;float r=size*.7f*(1-n/(float)samples);
                    fil[n*3]=pts[n*3]+(float)Math.cos(a)*r;fil[n*3+1]=pts[n*3+1]+(float)Math.sin(a)*r*.5f;fil[n*3+2]=pts[n*3+2]+(float)Math.sin(a)*r;
                }
                c.ribbon(Layer.GLOW,fil,samples,.08f*s,.02f,argb(.7,ICE),argb(0,CYAN),0);
            }
            // Atmospheric entry: a compressed shock cap riding the crystal and a flattened bow ring.
            if(big&&q.level>=1) {
                float w=size*(.9f+.3f*(float)(.5+.5*Math.sin(raw*3.1)));
                c.tube(Layer.RIM_FLOW,head.x,head.y,head.z,head.x+dir.x*size*4,head.y+dir.y*size*4,head.z+dir.z*size*4,w,w*.35f,Fx.segments(q,12),argb(.6*k,ICE),argb(.05*k,CYAN),2,(float)t*.2f,0,1);
                Fx.shockRing(c,head.x,head.y,head.z,dir.x,dir.y,dir.z,size*2.4f,size*.7f,ICE,.45*k,8,(float)(t*.08));
            }
            // Atmospheric streaks shed around the head as it accelerates.
            int streaks=Fx.scaled(q,last?24:12);
            for(int n=0;n<streaks;n++) {
                double h=v.hash(1200+i*50+n),pp=Fx.frac(t*.15+h);
                float rr=size*(1.5f+2.5f*(float)v.hash(1300+i*50+n));double a=h*Math.PI*11;
                float ox=(float)Math.cos(a)*rr,oz=(float)Math.sin(a)*rr;
                float bx=head.x-dir.x*(float)(pp*14*s)+ox,by=head.y-dir.y*(float)(pp*14*s),bz=head.z-dir.z*(float)(pp*14*s)+oz;
                c.streak(Layer.GLOW,bx,by,bz,bx-dir.x*4*s,by-dir.y*4*s,bz-dir.z*4*s,.06f*s,.02f,argb((1-pp)*.7*k,ICE),argb(0,CYAN),0,.5f,1,.5f);
            }
            if(raw<.18)Fx.flash(c,start.x,start.y,start.z,(last?14:8)*(float)(1-raw/.18)*flashScale,ICE,(1-raw/.18));
        }
    }
    private void drawImpacts(StrikeView v,Canvas c,ClientConfig.Quality q,double t) {
        int n=0;
        float reach=PresentationScale.shockReach(v),secondary=PresentationScale.secondaryReach(v);
        boolean colossal=PresentationScale.colossal(v)>0;
        for(var e:v.events()) {
            if(!e.impact())continue;
            int i=n++;
            double s=e.age(v.now());
            if(s>(colossal?260:140))continue;
            Vec3 at=e.position().subtract(v.target());
            float x=(float)at.x,y=(float)at.y,z=(float)at.z,radius=e.payload().radius();
            boolean fin=radius>=v.radius()*.8;
            double seedA=v.hash(1500+i)*Math.PI;
            // Authored spike height follows the real crater size, so a colossal basin erupts with colossal shards.
            float geom=Math.max(1f,Math.min(24f,radius/16f));
            if(s<5)Fx.flash(c,x,y+1.5f,z,radius*(fin?2.2f:1.6f)*(float)(1-s/6),ICE,1-s/5);
            if(colossal) {
                // Regional pressure system: a white-hot leading edge with slower, redder trailing rings.
                Fx.colossalShock(c,x,y,z,fin?reach:reach*.3f,s,fin?ICE:CYAN,(fin?.95:.8)*(1-Math.min(1,s/(colossal?180:120))),fin?6:2,(float)seedA,q);
                Fx.dustDome(c,x,y+1,z,radius*(fin?2.6f:1.5f),radius*(fin?1.1f:.5f),fin?FROST:0x8fb6c8,(fin?.42:.28)*(1-Math.min(1,s/200)),fin?3:1,q);
            } else {
                // Sharp angular shockwaves: hexagonal rings, plus tilted fragments for the final strike.
                Fx.shockRing(c,x,y+.4f,z,0,1,0,radius*(fin?3.2f:2.6f)*Fx.easeOut(s/12),fin?5:2.5f,ICE,(1-s/12)*.95,6,(float)seedA);
                Fx.shockRing(c,x,y+.25f,z,0,1,0,radius*(fin?2.2f:1.7f)*Fx.easeOut((s-2)/18),fin?3:1.6f,CYAN,(s>2?1:0)*(1-(s-2)/18)*.8,fin?8:6,(float)(seedA+.4));
            }
            if(fin) {
                for(int k=0;k<(colossal?7:5);k++) {
                    double a=seedA+k*1.3,tilt=.25+.15*k;
                    float nx=(float)(Math.sin(tilt)*Math.cos(a)),ny=(float)Math.cos(tilt),nz=(float)(Math.sin(tilt)*Math.sin(a));
                    double d=(s-k*1.5)/(16+k*3);
                    float ringR=colossal?radius*(1.2f+.55f*k)*Fx.easeOut(d):radius*(1.2f+.35f*k)*Fx.easeOut(d);
                    Fx.shockRing(c,x,y+1+k*1.6f,z,nx,ny,nz,ringR,1.6f,k%2==0?ICE:CYAN,(d>0?1:0)*(1-d)*.7,3+k%3,(float)a);
                }
                // Expanding energy dome / pressure shell over the final strike.
                if(s<(colossal?40:26)) {
                    double d=colossal?40:26;
                    float dome=(colossal?reach*.22f:radius*1.4f)*Fx.easeOut((float)(s/16));
                    c.sphere(Layer.RIM_CRYSTAL,x,y,z,dome,argb((1-s/d)*.85,CYAN),(float)(t*.02),0,true);
                    if(colossal)c.sphere(Layer.RIM_CRYSTAL,x,y,z,dome*.62f,argb((1-s/d)*.5,ICE),0,(float)(t*.03),false);
                    c.sphere(Layer.CORE_PLASMA,x,y,z,dome*.55f*(float)(1-s/d),argb((1-s/d)*.6,ICE),0,(float)(t*.02),q.level>=1);
                }
                if(s<20)c.tube(Layer.CORE_FLOW,x,y,z,x,y+(colossal?radius*.9f:70),z,radius*.25f*(float)(1-s/20),.1f,Fx.segments(q,16),argb((1-s/20)*.9,ICE),argb(0,CYAN),1,0,(float)(t*.1),(float)(t*.1)+3);
                if(colossal&&s<90)Fx.ashColumn(c,x,y,z,radius*.35f,radius*1.5f,CYAN,Math.max(0,1-s/90)*.35,(float)(t*.05),5,q);
            } else {
                if(s<10)c.sphere(Layer.RIM_CRYSTAL,x,y,z,radius*.9f*Fx.easeOut(s/6),argb((1-s/10)*.7,CYAN),0,0,false);
            }
            // Crystal spikes erupt from the impact and slowly dissolve (client-only, never real blocks).
            int spikes=fin?(q.level==2?16:q.level==1?10:5):(q.level==2?6:q.level==1?4:2);
            double grow=Fx.easeOut(s/5),fade=1-smooth(range(s,40,fin?130:80));
            if(fade>0)for(int k=0;k<spikes;k++) {
                double a=v.hash(1600+i*31+k)*Math.PI*2,rr=radius*(fin?.95:.85)*(.75+.35*v.hash(1700+i*31+k));
                float h=(float)((fin?2.2:1.4)*(.6+.8*v.hash(1800+i*31+k))*grow*fade*geom);
                float sx=x+(float)Math.cos(a)*(float)rr,sz=z+(float)Math.sin(a)*(float)rr;
                m.translation(sx,y-.6f,sz).rotateY((float)a).rotateZ((float)(.35+.4*v.hash(1900+i*31+k))).rotateX((float)(Math.PI)).scale(h);
                c.mesh(Layer.CORE_CRYSTAL,spike,m,argb(.8*fade,ICE),0,(float)(t*.005));
            }
            Fx.groundGlow(c,x,y+.08f,z,radius*(fin?1.3f:1),CYAN,Math.max(0,1-s/(fin?100:50))*.45);
        }
        if(v.atOrAfter(Phase.AFTERMATH)&&v.before(Phase.COMPLETE)) {
            double a=v.through(Phase.AFTERMATH);
            // Colossal only: the sub-impact footprints, then the regional pressure boundary. The sub-impact rings are
            // capped so a nineteen-impact barrage can never exceed the shared shock vertex budget in one frame.
            if(colossal) {
                int shown=0;
                for(var e:v.events()) {
                    if(!e.impact()||shown>=7)continue;
                    shown++;
                    Vec3 at=e.position().subtract(v.target());
                    float r=e.payload().radius();
                    Fx.shockRing(c,(float)at.x,(float)at.y+.2f,(float)at.z,0,1,0,r*(1.5f+(float)a*1.1f),1.6f,CYAN,(1-a)*.3,6,(float)(t*.01));
                }
            }
            if(colossal)for(int i=0;i<3;i++) {
                Fx.shockRing(c,0,.35f,0,(secondary*(.55f+.45f*i)+(float)a*secondary*.35f),Math.max(.8f,2.4f-Fx.easeOut(a)*1.6f),i%2==0?ICE:FROST,(1-a)*.26,(float)(t*.004*(i%2==0?1:-1)),q);
                Fx.shockRing(c,0,.6f,0,(reach*(.35f+.3f*i)+(float)a*reach*.2f),Math.max(.8f,3f-Fx.easeOut(a)*2f),i==0?CYAN:DEEP,(1-a)*.16,(float)(t*.003),q);
            }
            Fx.groundGlow(c,0,-v.depth()*.5f,0,v.radius()*.9f,DEEP,(1-a)*.35);
            if(colossal) {
                Fx.moltenPool(c,0,-v.depth()*.6f,0,v.radius()*1.05f,CYAN,(1-a)*.5,(float)(t*.01f),q.level==2?7:4);
                Fx.dustDome(c,0,-v.depth()*.25f,0,secondary*.55f,v.depth()*1.6f+60,FROST,(1-a)*.3,q.level==2?3:2,q);
            }
        }
    }
    @Override public void tick(StrikeView v,CosmeticParticles p,ClientLevel level,ClientConfig.Quality q) {
        if(v.in(Phase.AFTERMATH)&&p.random()<(1-v.progress())*1.2) {
            Vec3 target=v.target();
            double a=p.random()*Math.PI*2,r=v.radius()*p.random();
            p.spawn(CosmeticParticles.MOTE,Fx.owner(v),target.x+Math.cos(a)*r,target.y+3+p.random()*10,target.z+Math.sin(a)*r,
                    p.signed()*.02,-.05,p.signed()*.02,60+p.random()*40,.18f+p.random()*.15f,0xe0000000|ICE);
        }
        // Colossal basins keep shedding drifting crystal shards out of the ruptured floor.
        if(PresentationScale.colossal(v)>0&&(v.in(Phase.AFTERMATH)||v.in(Phase.DEVASTATION_WAVE_1)||v.in(Phase.DEVASTATION_WAVE_2))
                &&p.random()<(1-v.progress())*.8) {
            Vec3 target=v.target();double a=p.random()*Math.PI*2,r=v.radius()*(.3+p.random()*.8);
            p.spawn(CosmeticParticles.SHARD,Fx.owner(v),target.x+Math.cos(a)*r,target.y+v.depth()*.2,target.z+Math.sin(a)*r,
                    p.signed()*.05,.12+p.random()*.2,p.signed()*.05,70+p.random()*50,.6f+p.random()*1.4f,0xe0000000|ICE);
        }
    }

    @Override public void event(StrikeView v,StrikeView.Event e,CosmeticParticles p,ClientLevel level,ClientConfig.Quality q) {
        if(!e.impact())return;
        int owner=Fx.owner(v);Vec3 at=e.position();float radius=e.payload().radius();
        boolean fin=radius>=v.radius()*.8;
        float s=factor(v);
        int ground=Fx.groundColor(level,at);
        Fx.debrisBurst(p,owner,at,Fx.scaled(q,fin?110:30),fin?1.3f:.9f,fin?1.15f:.8f,ground,.25f,fin?1.2f:.7f,.15f);
        for(int i=0;i<Fx.scaled(q,fin?60:18);i++) {
            double a=p.random()*Math.PI*2,sp=(fin?.7:.45)+p.random()*.9;
            p.spawn(CosmeticParticles.SHARD,owner,at.x,at.y+.8,at.z,Math.cos(a)*sp*s,.45+p.random()*(fin?1.1:.7)*s,Math.sin(a)*sp*s,50+p.random()*50,.4f+p.random()*(fin?.9f:.5f)*s,0xf0000000|ICE);
        }
        Fx.sparks(p,owner,at,Fx.scaled(q,fin?50:20),fin?1.7f:1.2f,ICE);
        Fx.dustRing(p,owner,at,Fx.scaled(q,fin?40:12),radius,fin?.8f:.5f,Canvas.lerpRgb(ground,0xb8c8d0,.35),fin?4:2.5f);
    }
}
