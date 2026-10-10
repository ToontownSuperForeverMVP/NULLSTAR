// Copyright (C) 2026 ToontownSuperForeverMVP and NULLSTAR contributors
// SPDX-License-Identifier: GPL-3.0-only

package com.nullstar.client.render.scene;

import com.nullstar.client.ClientConfig;
import com.nullstar.client.render.Canvas;
import com.nullstar.client.render.Layer;
import com.nullstar.client.render.Mesh;
import com.nullstar.client.render.StrikeView;
import com.nullstar.core.Phase;
import org.joml.Matrix4f;
import static com.nullstar.client.render.Canvas.argb;
import static com.nullstar.client.render.StrikeView.range;
import static com.nullstar.client.render.StrikeView.smooth;
import static com.nullstar.client.render.StrikeView.smoother;

/**
 * The HELIOS stellar mothership: a civilization-scale star-harvesting weapon modelled as a hierarchy of reusable
 * meshes posed by the authoritative timeline.
 *
 * <p>Silhouette, rear to front along the firing axis (-Z): a contained star inside an armillary crucible of three
 * gimballed rings; eight harvesting petals hinged on the crucible collar (a closed bud that blooms into a dish
 * whose golden inner faces drink the star); a conduit neck; the armoured main hull with sliding armour plates over a
 * glowing core; two colossal collector rings on spokes; a tapered accelerator barrel ringed with coils; four
 * targeting pylons that swing forward and converge on a focus point; and a twelve-blade aperture iris.
 *
 * <p>The pose is a pure function of the phase and its progress, so a late observer sees the machine in its current
 * state and replay is deterministic. Mechanisms move in an ordered causal chain with eased, slightly over-damped
 * motion and short locking settles instead of free-running spins: armour retracts, petals unfold, rings swing into
 * alignment and counter-rotate, pylons reposition, energy streams converge, containment closes, the iris opens and the
 * barrel locks. Static topology is built once; frames only transform it.
 */
final class StellarMothership {
    // Palette.
    static final int HULL=0x8f8a84,HULL_DARK=0x5c5a5e,GOLD=0xffcf7a,STAR=0xffb24a,STAR_HOT=0xfff1d6,ENERGY=0xffa040,COIL=0xff7a2a,
            TEAL=0x7fd6ff,BLOOD=0xff4a2a;
    // Layout along the firing axis (virtual units). The ship spans roughly 1,900 units.
    static final float STAR_Z=620,STAR_R=96,COLLAR_Z=520,HULL_FRONT=-360,HULL_BACK=260,BARREL_END=-820,APERTURE_Z=-840,FOCUS_Z=-1320;

    private static Mesh hullCore,armorPlate,petal,crucibleRing,collector,collectorSmall,spoke,barrel,coil,pylon,blade,neck,node,drone,collar;
    private final Matrix4f m=new Matrix4f(),base=new Matrix4f(),child=new Matrix4f();

    /** The timeline-derived pose. Every value is 0..1 unless noted. */
    static final class Pose {
        double armor,petals,rings,ringAlign,pylons,streams,containment,iris,lock,charge,instability,fire,recoil,ringSpin,crucibleSpin;
    }

    static void build() {
        if(hullCore!=null)return;
        // Main hull: an octagonal armoured body with stepped bands, built along +Y then laid on the Z axis.
        Mesh.Builder h=new Mesh.Builder().uvScale(1/40f);
        h.frustum(96,118,-300,-220,8,true).frustum(118,124,-220,120,8,false).frustum(124,104,120,200,8,true)
                .frustum(70,96,-360,-300,8,true);
        for(int i=0;i<6;i++){float y=-200+i*56;h.ring(118,134,y,y+12,0,Math.PI*2,8);}
        for(int i=0;i<8;i++)h.push().rotateY((float)(i*Math.PI/4+Math.PI/8)).box(120,-240,-7,140,140,7).pop();
        hullCore=h.build();
        // One armour plate: a curved shell patch with a raised rib, opened outward by the pose.
        armorPlate=new Mesh.Builder().uvScale(1/36f).ring(126,140,-46,46,-.36,.36,4).box(138,-40,-8,148,40,8).build();
        // Harvesting petal: tapered spine with ribs and edge armour, hinge at the origin, extending along +Y.
        Mesh.Builder p=new Mesh.Builder().uvScale(1/30f);
        int ribs=7;
        for(int i=0;i<ribs;i++) {
            float y0=40+i*92f,y1=y0+88,w0=150-i*16f,w1=150-(i+1)*16f;
            p.quad(new float[]{-w0*.5f,y0,0},new float[]{w0*.5f,y0,0},new float[]{w1*.5f,y1,0},new float[]{-w1*.5f,y1,0},0,y0/30f,w0/30f,y1/30f);
            p.quad(new float[]{-w1*.5f,y1,-7},new float[]{w1*.5f,y1,-7},new float[]{w0*.5f,y0,-7},new float[]{-w0*.5f,y0,-7},0,y0/30f,w0/30f,y1/30f);
            p.box(-w0*.5f-6,y0,-12,-w0*.5f+4,y1,5).box(w0*.5f-4,y0,-12,w0*.5f+6,y1,5);
            p.box(-w0*.5f,y0-3,-14,w0*.5f,y0+5,-6);
        }
        p.box(-9,0,-20,9,ribs*92f+40,-6).box(-30,0,-26,30,46,8);
        petal=p.build();
        // Crucible gimbal ring with bearing nodes.
        Mesh.Builder r=new Mesh.Builder().uvScale(1/20f).ring(150,162,-9,9,0,Math.PI*2,48);
        for(int i=0;i<6;i++)r.push().rotateY((float)(i*Math.PI/3)).box(156,-16,-14,174,16,14).pop();
        crucibleRing=r.build();
        Mesh.Builder c=new Mesh.Builder().uvScale(1/40f).ring(420,452,-14,14,0,Math.PI*2,72);
        for(int i=0;i<12;i++)c.push().rotateY((float)(i*Math.PI/6)).box(440,-30,-26,476,30,26).box(452,-10,-60,470,10,60).pop();
        collector=c.build();
        Mesh.Builder cs=new Mesh.Builder().uvScale(1/40f).ring(318,342,-11,11,0,Math.PI*2,60);
        for(int i=0;i<9;i++)cs.push().rotateY((float)(i*Math.PI*2/9)).box(330,-24,-20,358,24,20).pop();
        collectorSmall=cs.build();
        spoke=new Mesh.Builder().uvScale(1/30f).box(0,-6,-10,1,6,10).build();
        // Accelerator barrel and its coils.
        barrel=new Mesh.Builder().uvScale(1/40f).frustum(96,64,0,460,8,false).frustum(64,80,460,480,8,true).build();
        coil=new Mesh.Builder().uvScale(1/24f).ring(78,98,-8,8,0,Math.PI*2,24).build();
        // Targeting pylon: a long boom with a knuckle, a sensor head and a glowing tip housing; extends along -Z.
        Mesh.Builder py=new Mesh.Builder().uvScale(1/24f);
        py.box(-10,-10,-520,10,10,0).box(-18,-18,-40,18,18,20).box(-24,-14,-560,24,14,-500).box(-6,-26,-500,6,26,-180);
        pylon=py.build();
        blade=new Mesh.Builder().uvScale(1/24f).box(0,-4,-24,84,4,24).push().translate(84,0,0).rotateY(.5f).box(0,-3,-16,30,3,16).pop().build();
        neck=new Mesh.Builder().uvScale(1/30f).frustum(26,26,0,260,8,false).build();
        node=new Mesh.Builder().uvScale(1/12f).box(-12,-12,-12,12,12,12).build();
        drone=new Mesh.Builder().uvScale(1/6f).box(-4,-1.5f,-9,4,1.5f,9).box(-11,-.5f,-2,11,.5f,4).build();
        collar=new Mesh.Builder().uvScale(1/24f).ring(108,134,-16,16,0,Math.PI*2,32).build();
    }

    /** Ease with a gentle over-damped settle, giving heavy machinery a sense of mass when it arrives. */
    static double settle(double t) {
        t=Math.max(0,Math.min(1,t));
        double e=smoother(t);
        return e+Math.sin(t*Math.PI)*Math.pow(1-t,2)*.06;
    }

    /** Derives the pose from the authoritative phase: reveal (idle) -> deployment -> charging -> alignment -> firing. */
    static Pose pose(StrikeView v,Pose out) {
        double p=v.progress();
        double d=v.in(Phase.DEPLOYMENT)?p:v.atOrAfter(Phase.CHARGING)?1:0;
        double c=v.in(Phase.CHARGING)?p:v.atOrAfter(Phase.ORBITAL_ALIGNMENT)?1:0;
        double a=v.in(Phase.ORBITAL_ALIGNMENT)?p:v.atOrAfter(Phase.FIRING)?1:0;
        double f=v.in(Phase.FIRING)?p:v.atOrAfter(Phase.ATMOSPHERIC_ENTRY)?1:0;
        // Deployment chain: armour -> petals -> rings swing in -> rings align/spin up -> pylons reposition.
        out.armor=settle(range(d,0,.22));
        out.petals=settle(range(d,.14,.62));
        out.rings=settle(range(d,.4,.72));
        out.ringAlign=settle(range(d,.58,.86));
        out.pylons=settle(range(d,.7,1));
        // Charge: streams converge, then containment closes around the star.
        out.streams=smooth(range(c,0,.45))*(1-.3*range(f,.6,1));
        out.containment=settle(range(c,.45,.9));
        out.charge=smooth(c)*.75+smooth(a)*.25;
        // Alignment: iris opens, then the barrel locks with a short jolt and a held breath before discharge.
        out.iris=settle(range(a,.1,.6));
        out.lock=range(a,.62,.7);
        out.instability=Math.min(1,smooth(range(c,.5,1))*.6+smooth(range(a,.55,1))*.4+(f>0&&f<.2?.3:0));
        out.fire=f;
        out.recoil=f>0?Math.exp(-Math.max(0,f-.12)*12)*range(f,.1,.13):0;
        // Ring spin accumulates deterministically from the phase timeline (no free-running clock): slow in reveal,
        // spinning up through deployment and charge, decelerating to a locked stop at the end of alignment.
        double age=v.age();
        double spinRate=(.0006+.0024*out.ringAlign+.0045*out.charge)*(1-out.lock*.92);
        out.ringSpin=age*spinRate+out.ringAlign*1.3;
        out.crucibleSpin=age*(.0016+.004*out.charge)*(1-out.containment*.85);
        return out;
    }

    /**
     * Renders the ship at the virtual origin set on the canvas, firing axis along -Z, posed by {@code pose}.
     *
     * @param light light rig in camera-relative virtual coordinates (the star position is set here)
     */
    void render(Canvas c,Pose pose,Canvas.Light light,ClientConfig.Quality q,double t,StrikeView v,float ox,float oy,float oz) {
        build();
        c.virtualOrigin(ox,oy,oz);
        float rec=(float)(pose.recoil*28);
        // Star light: warm, brightening with charge, pulsing with instability.
        float pulse=(float)(1+.12*pose.instability*Math.sin(t*.9)+.05*Math.sin(t*.23));
        light.point(ox-c.eyeX(),oy-c.eyeY(),oz+STAR_Z+rec-c.eyeZ(),STAR,(float)(3.0+2.0*pose.charge)*pulse,900);
        double seam=.25+.75*pose.charge;
        int hullAlbedo=HULL,dark=HULL_DARK;
        // ---- main hull, nudged back by the discharge recoil.
        base.identity().translate(0,0,rec);
        m.set(base).rotateX((float)(Math.PI/2));
        c.litMesh(Layer.COSMIC_BODY,hullCore,m,light,Canvas.Shading.BAKED,hullAlbedo,seam,0,0);
        // Inner core glow revealed as the armour slides away.
        if(pose.armor>.02)for(int i=0;i<8;i++) {
            float a=(float)(i*Math.PI/4);
            float x=(float)Math.cos(a)*126,y=(float)Math.sin(a)*126;
            c.streak(Layer.COSMIC_GLOW,x,y,-180+rec,x,y,100+rec,9,9,argb(.55*pose.armor*(.4+.6*pose.charge),ENERGY),argb(.55*pose.armor*(.4+.6*pose.charge),ENERGY),0,0,1,1);
        }
        // ---- armour plates: 3 bands x 8, sliding outward and back in sequence.
        for(int band=0;band<3;band++)for(int i=0;i<8;i++) {
            double local=range(pose.armor,band*.18,band*.18+.64);
            float open=(float)settle(local);
            m.set(base).translate(0,0,-150+band*110).rotateZ((float)(i*Math.PI/4+Math.PI/8))
                    .translate(34*open,0,46*open).rotateY(open*.28f).rotateX((float)(Math.PI/2));
            c.litMesh(Layer.COSMIC_BODY,armorPlate,m,light,Canvas.Shading.BAKED,dark,seam*.6,band*.3f,i*.2f);
        }
        // ---- conduit neck from the hull to the crucible collar.
        for(int i=0;i<4;i++) {
            float a=(float)(i*Math.PI/2+Math.PI/4);
            m.set(base).translate((float)Math.cos(a)*56,(float)Math.sin(a)*56,190).rotateX((float)(Math.PI/2));
            c.litMesh(Layer.COSMIC_BODY,neck,m,light,Canvas.Shading.BAKED,dark,seam,i*.25f,0);
        }
        m.set(base).translate(0,0,COLLAR_Z).rotateX((float)(Math.PI/2));
        c.litMesh(Layer.COSMIC_BODY,collar,m,light,Canvas.Shading.BAKED,hullAlbedo,seam,0,0);
        // ---- crucible: three gimballed rings, precessing until containment locks them into one plane.
        double lockAngle=1-pose.containment;
        for(int i=0;i<3;i++) {
            float spin=(float)(pose.crucibleSpin*(i==1?-1.3:1)+i*1.1);
            float tilt=(float)((i-1)*1.05*lockAngle+Math.PI/2);
            float s=1+i*.12f;
            m.set(base).translate(0,0,STAR_Z).rotateZ(spin).rotateX(tilt).rotateY(spin*.7f*(float)lockAngle).scale(s,s,s);
            c.litMesh(Layer.COSMIC_BODY,crucibleRing,m,light,Canvas.Shading.BAKED,i==1?GOLD:hullAlbedo,.4+.6*pose.charge,i*.33f,0);
        }
        // ---- harvesting petals: closed bud wrapping the star, blooming into a golden dish.
        int petals=q.level==0?6:8;
        for(int i=0;i<petals;i++) {
            double stagger=range(pose.petals,i%2==0?0:.08,i%2==0?.92:1);
            double open=settle(stagger);
            float hinge=(float)(-.13+1.28*open);        // closed: folded back over the star; open: a dish facing it
            float az=(float)(i*Math.PI*2/petals+Math.PI/petals);
            m.set(base).translate(0,0,COLLAR_Z).rotateZ(az).translate(0,120,0).rotateX((float)(Math.PI/2-hinge));
            c.litMesh(Layer.COSMIC_BODY,petal,m,light,Canvas.Shading.BAKED,GOLD,.3+.7*pose.charge*open,i*.17f,0);
        }
        // ---- collector rings on spokes: stowed edge-on along the hull, swinging out into alignment then spinning.
        float swing=(float)((1-pose.rings)*Math.PI/2);
        float grow=(float)(.45+.55*pose.rings);
        for(int r=0;r<2;r++) {
            float z=(r==0?-40:-210)+rec;
            float spin=(float)(pose.ringSpin*(r==0?1:-1.4));
            child.set(base).translate(0,0,z-rec).rotateY(swing*(r==0?1:-1)).rotateX((float)(Math.PI/2)).rotateY(spin).scale(grow,1,grow);
            c.litMesh(Layer.COSMIC_BODY,r==0?collector:collectorSmall,child,light,Canvas.Shading.BAKED,r==0?hullAlbedo:GOLD,seam,r*.5f,0);
            int spokes=r==0?4:3;
            float outer=r==0?430:325;
            for(int k=0;k<spokes;k++) {
                m.set(child).rotateY((float)(k*Math.PI*2/spokes)).translate(110,0,0).scale(outer-110,1,1);
                c.litMesh(Layer.COSMIC_BODY,spoke,m,light,Canvas.Shading.BAKED,dark,seam*.5,0,0);
            }
            // Charge circulating the ring: a travelling glow along its rim once it is aligned.
            if(pose.ringAlign>.05&&pose.rings>.97) {
                int beads=q.level==2?12:q.level==1?8:5;
                for(int k=0;k<beads;k++) {
                    double a=k*Math.PI*2/beads+spin;
                    float rr=(r==0?436:330)*grow;
                    float x=(float)(Math.cos(a)*rr),y=(float)(Math.sin(a)*rr);
                    c.fxSprite(Layer.COSMIC_FX,Canvas.FX_FLARE,0,x,y,z,24+40*(float)pose.charge,24+40*(float)pose.charge,0,
                            argb((.3+.6*pose.charge)*pose.ringAlign,ENERGY));
                }
            }
        }
        // ---- accelerator barrel with coils that light front-to-back as the charge builds.
        m.set(base).translate(0,0,HULL_FRONT).rotateX((float)(-Math.PI/2));
        c.litMesh(Layer.COSMIC_BODY,barrel,m,light,Canvas.Shading.BAKED,hullAlbedo,seam,0,0);
        int coils=7;
        for(int i=0;i<coils;i++) {
            float z=HULL_FRONT-40-i*60;
            double lit=range(pose.charge*1.3-(coils-1-i)*.08,0,.3)/.3;
            float s=(float)(1-.06*i);
            m.set(base).translate(0,0,z).rotateX((float)(Math.PI/2)).scale(s,1,s);
            c.litMesh(Layer.COSMIC_BODY,coil,m,light,Canvas.Shading.BAKED,lit>.1?COIL:dark,.15+.85*lit,i*.1f,0);
            if(lit>.05)c.annulus(Layer.COSMIC_GLOW,0,0,z+rec,0,0,1,82*s,104*s,24,argb(.45*lit,COIL),argb(0,COIL),1,0,0,1);
        }
        // ---- targeting pylons: folded back along the hull, swinging forward to converge on the focus point.
        for(int i=0;i<4;i++) {
            float az=(float)(i*Math.PI/2+Math.PI/4);
            double swingOpen=settle(range(pose.pylons,i*.08,.76+i*.08));
            float swingAngle=(float)((1-swingOpen)*Math.PI*.92-.125*swingOpen);
            m.set(base).translate(0,0,-300).rotateZ(az).translate(0,128,0).rotateX(swingAngle);
            c.litMesh(Layer.COSMIC_BODY,pylon,m,light,Canvas.Shading.BAKED,dark,.3+.7*pose.charge,i*.2f,0);
            // Tip emitter and the alignment beam to the focus point.
            m.transformPosition(tip.set(0,0,-560));
            if(pose.charge>.1)c.fxSprite(Layer.COSMIC_FX,Canvas.FX_FLARE,0,tip.x,tip.y,tip.z,30+50*(float)pose.charge,30+50*(float)pose.charge,0,argb(.4+.5*pose.charge,BLOOD));
            if(pose.iris>.05) {
                c.tube(Layer.COSMIC_FX,tip.x,tip.y,tip.z,0,0,FOCUS_Z+rec,4,2,6,argb(.55*pose.iris*(1-pose.fire*.7),ENERGY),argb(.8*pose.iris,STAR_HOT),
                        1,Canvas.fxU(.1),Canvas.fxV(Canvas.FX_BEAM),Canvas.fxV(Canvas.FX_BEAM)+3);
            }
        }
        // ---- aperture iris.
        int blades=12;
        for(int i=0;i<blades;i++) {
            float az=(float)(i*Math.PI*2/blades);
            float open=(float)(pose.iris*62);
            m.set(base).translate(0,0,APERTURE_Z).rotateZ(az).translate(10+open,0,0).rotateZ((float)(.55-.45*pose.iris)).rotateX((float)(Math.PI/2));
            c.litMesh(Layer.COSMIC_BODY,blade,m,light,Canvas.Shading.BAKED,dark,.5*pose.charge,i*.1f,0);
        }
        if(pose.iris>.02) {
            float core=(float)(30+60*pose.iris);
            c.fxSprite(Layer.COSMIC_FX,Canvas.FX_CORONA,pose.instability,0,0,APERTURE_Z-8+rec,core*2.2f,core*2.2f,(float)(t*.01),argb(.45*pose.iris+.4*pose.instability,ENERGY));
            c.fxSprite(Layer.COSMIC_FX,Canvas.FX_FLARE,0,0,0,APERTURE_Z-12+rec,core*1.4f,core*1.4f,0,argb(.6*pose.iris,STAR_HOT));
        }
        // ---- the contained star and its machinery of light.
        star(c,pose,q,t,v,rec);
        // ---- scale references: tiny service craft working the hull.
        int drones=q.level==2?26:q.level==1?16:8;
        for(int i=0;i<drones;i++) {
            double h=v.hash(5100+i),orbit=180+h*520,speed=.0025+.004*v.hash(5200+i);
            double a=t*speed+h*Math.PI*2;
            float z=(float)(-620+v.hash(5300+i)*1180);
            float x=(float)(Math.cos(a)*orbit),y=(float)(Math.sin(a)*orbit*.7);
            m.identity().translate(x,y,z).rotateZ((float)a).rotateY((float)(Math.PI/2));
            c.litMesh(Layer.COSMIC_BODY,drone,m,light,Canvas.Shading.BAKED,HULL,.9,0,0);
            c.fxSprite(Layer.COSMIC_FX,Canvas.FX_FLARE,0,x,y,z+9,7,7,0,argb(.7,i%3==0?BLOOD:TEAL));
        }
    }
    private final org.joml.Vector3f tip=new org.joml.Vector3f();

    private void star(Canvas c,Pose pose,ClientConfig.Quality q,double t,StrikeView v,float rec) {
        float z=STAR_Z+rec;
        double charge=pose.charge,inst=pose.instability;
        // Compression just before discharge, then a violent swell on firing that collapses as the star is spent.
        double squeeze=1-.18*smooth(range(pose.lock,0,1))*(1-pose.fire);
        double spent=pose.fire>.25?1-.55*smooth(range(pose.fire,.25,1)):1;
        float r=(float)(STAR_R*(.85+.3*charge)*squeeze*spent);
        m.identity().translate(0,0,z).rotateY((float)(t*.004)).rotateX(.3f).scale(r);
        float du=Canvas.fxU(inst);
        c.mesh(Layer.COSMIC_FX,Mesh.UNIT_SPHERE,m,argb(Math.min(1,.9+.4*charge),STAR),du,Canvas.fxV(Canvas.FX_STAR));
        c.fxSprite(Layer.COSMIC_FX,Canvas.FX_CORONA,inst,0,0,z,r*3.6f,r*3.6f,(float)(t*.003),argb(.55+.4*charge,STAR));
        c.fxSprite(Layer.COSMIC_FX,Canvas.FX_CORONA,Math.min(1,inst+.3),0,0,z,r*7f,r*7f,(float)(-t*.002),argb(.22+.3*charge,ENERGY));
        // Magnetic confinement: field arcs from the crucible ring nodes over the star, tightening with containment.
        int arcs=q.level==2?12:q.level==1?8:5;
        for(int i=0;i<arcs;i++) {
            double a=i*Math.PI*2/arcs+pose.crucibleSpin;
            float ring=160,lift=(float)(r*(1.35-.25*pose.containment));
            float x0=(float)Math.cos(a)*ring,y0=(float)Math.sin(a)*ring;
            float x1=(float)Math.cos(a+Math.PI*.7)*ring,y1=(float)Math.sin(a+Math.PI*.7)*ring;
            pts[0]=x0;pts[1]=y0;pts[2]=z;
            float mx=(x0+x1)*.5f,my=(y0+y1)*.5f,ml=(float)Math.sqrt(mx*mx+my*my)+1e-3f;
            pts[3]=mx/ml*lift*.6f;pts[4]=my/ml*lift*.6f;pts[5]=z+(i%2==0?1:-1)*lift;
            pts[6]=x1;pts[7]=y1;pts[8]=z;
            c.ribbon(Layer.COSMIC_GLOW,pts,3,3.5f,3.5f,argb(.35+.4*pose.containment,TEAL),argb(.35+.4*pose.containment,TEAL),0);
        }
        // Prominences: looping eruptions off the limb, more violent as the star destabilises.
        int proms=q.level==2?7:q.level==1?5:3;
        for(int i=0;i<proms;i++) {
            double h=v.hash(5400+i),cycle=Fx.frac(t*(.004+.006*h)+h);
            double a=h*Math.PI*2+t*.001;
            float height=(float)(r*(.3+1.1*inst)*Math.sin(cycle*Math.PI));
            for(int k=0;k<5;k++) {
                double s=k/4.0,ang=a+(s-.5)*.5;
                float rr=r+(float)(Math.sin(s*Math.PI)*height);
                arc[k*3]=(float)Math.cos(ang)*rr;arc[k*3+1]=(float)Math.sin(ang)*rr;arc[k*3+2]=z+(float)((s-.5)*r*.3);
            }
            c.ribbon(Layer.COSMIC_FX,arc,5,r*.09f,r*.05f,argb(.6*(1-cycle*.5),ENERGY),argb(.3,STAR_HOT),0);
        }
        // Extraction streams: spiralling plasma pulled off the star into the collar and down the neck conduits.
        if(pose.streams>.02) {
            int streams=4;
            for(int s=0;s<streams;s++) {
                double base0=s*Math.PI/2+Math.PI/4;
                int n=9;
                for(int k=0;k<n;k++) {
                    double f=k/(double)(n-1);
                    double ang=base0+f*2.2+t*.01;
                    float rr=(float)(r*1.05+(56-r*1.05)*f);
                    stream[k*3]=(float)Math.cos(ang)*rr;stream[k*3+1]=(float)Math.sin(ang)*rr;stream[k*3+2]=(float)(z-f*(z-HULL_BACK+20));
                }
                c.ribbon(Layer.COSMIC_FX,stream,n,14,6,argb(.7*pose.streams,STAR_HOT),argb(.5*pose.streams,ENERGY),0);
            }
            // Straight flow down the neck to the hull core.
            for(int i=0;i<4;i++) {
                float a=(float)(i*Math.PI/2+Math.PI/4);
                float x=(float)Math.cos(a)*56,y=(float)Math.sin(a)*56;
                c.tube(Layer.COSMIC_FX,x,y,450+rec,x,y,190+rec,9,9,6,argb(.6*pose.streams,ENERGY),argb(.5*pose.streams,STAR_HOT),
                        1,Canvas.fxU(.05),Canvas.fxV(Canvas.FX_BEAM),Canvas.fxV(Canvas.FX_BEAM)+4);
            }
        }
    }
    private final float[] pts=new float[9],arc=new float[15],stream=new float[27];
}
