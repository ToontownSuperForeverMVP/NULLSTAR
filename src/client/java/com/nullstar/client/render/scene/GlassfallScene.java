package com.nullstar.client.render.scene;

import com.nullstar.Nullstar;
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
import java.util.ArrayList;
import java.util.List;
import static com.nullstar.client.render.Canvas.argb;
import static com.nullstar.client.render.StrikeView.range;
import static com.nullstar.client.render.StrikeView.smooth;
import static com.nullstar.client.render.StrikeView.smoother;

/**
 * GLASSFALL ARRAY: an angular orbital lattice that unfolds, aims five launch rails and fires crystalline
 * projectiles along deterministic curves that arrive exactly at the server's authoritative impact cues.
 */
public final class GlassfallScene implements WeaponScene {
    static final int CYAN=0x56e6ff,ICE=0xd8fbff,DEEP=0x1a7fb0;
    private static final float[][] DEFAULT_OFFSETS={{-9,-7},{8,-6},{-7,9},{9,8},{0,0}};
    private static Mesh hub,lens,octagon,square,arm,rail,crystal,crystalLarge,spike;
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
    }
    static float[][] offsets() {
        var catalog=Nullstar.catalog();
        if(catalog==null)return DEFAULT_OFFSETS;
        List<float[]> list=new ArrayList<>();
        for(var cue:catalog.get("glassfall").events())if(cue.effect().equals("projectile"))list.add(new float[]{(float)cue.x(),(float)cue.z()});
        return list.isEmpty()?DEFAULT_OFFSETS:list.toArray(float[][]::new);
    }
    static float latticeY(StrikeView v) {
        double descend=smoother(range(v.through(Phase.CHARGING),0,.6));
        double ascend=smoother(range(v.through(Phase.AFTERMATH),.2,1));
        return (float)(v.height()+(1-descend)*30+ascend*44);
    }
    /** Rail mount (origin-relative) for projectile index i. */
    static Vector3f railMount(StrikeView v,float[][] offsets,int i,float y) {
        float[] o=offsets[Math.min(i,offsets.length-1)];
        float len=(float)Math.hypot(o[0],o[1]);
        if(len<.5f)return new Vector3f(0,y-3.2f,0);
        return new Vector3f(o[0]/len*8.5f,y-1.2f,o[1]/len*8.5f);
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
        float[][] offsets=offsets();
        int projectiles=0;
        for(var e:v.events())if(!e.impact())projectiles++;
        for(int i=v.events().size()-1;i>=0;i--) {
            var e=v.events().get(i);
            if(!e.impact()) {
                projectiles--;
                double raw=e.progress(v.now());
                if(raw>=1)continue;
                boolean last=projectiles>=offsets.length-1||e.payload().radius()>=v.radius()*.8;
                Vector3f start=railMatrix(v,offsets,projectiles,latticeY(v),1,new Matrix4f()).transformPosition(new Vector3f(0,-5.5f,0));
                double bend=(v.hash(1000+projectiles)-.5)*(last?4:10);
                Vector3f head=flight(start,e.position().subtract(v.target()),flightProgress(raw),bend,new Vector3f());
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
        float[][] offsets=offsets();
        float ly=latticeY(v);
        boolean visible=!v.before(Phase.CHARGING)&&!v.atOrAfter(Phase.COOLDOWN)&&!v.atOrAfter(Phase.COMPLETE);
        if(visible)drawLattice(v,c,q,t,ly,offsets);
        drawProjectiles(v,c,q,t,ly,offsets);
        drawImpacts(v,c,q,t);
        if(v.in(Phase.ACQUIRING)||v.in(Phase.CHARGING)) {
            double appear=v.in(Phase.ACQUIRING)?v.progress():1-v.progress()*.7;
            for(float[] o:offsets)Fx.shockRing(c,o[0],.12f,o[1],(o[0]==0&&o[1]==0?v.radius():v.radius()*.35f)*(float)smooth(Math.min(1,v.age()/16)),.4f,CYAN,.6*appear,(float)(t*.04),q);
        }
    }

    private void drawLattice(StrikeView v,Canvas c,ClientConfig.Quality q,double t,float ly,float[][] offsets) {
        double charge=v.through(Phase.CHARGING),align=v.through(Phase.ORBITAL_ALIGNMENT),after=v.through(Phase.AFTERMATH);
        double unfold=smoother(range(charge,.25,1))*(1-.6*smoother(range(after,.1,.6)));
        double lock=smoother(align);
        double energy=.15+.4*charge+.45*align;
        if(v.atOrAfter(Phase.AFTERMATH))energy=Math.max(.05,1-after*2);
        int hull=argb(Math.min(1,energy),0xa0a8b0);
        float yaw=(float)(v.hash(5)*Math.PI*.5);
        root.translation(0,ly,0);
        c.mesh(Layer.HULL_GLASSFALL,hub,new Matrix4f(root).rotateY(yaw),hull,0,0);
        // Interlocking frames counter-rotate during the unfold and snap into register on lock.
        float spinA=(float)((1-lock)*(t*.03)+lock*Math.PI/8+yaw),spinB=(float)(-(1-lock)*(t*.02)+yaw);
        float scaleFrames=(float)(.55+.45*unfold);
        m.set(root).rotateY(spinA).scale(scaleFrames,1,scaleFrames);
        c.mesh(Layer.HULL_GLASSFALL,octagon,m,hull,0,0);
        m.set(root).rotateY(spinB).rotateX((float)((1-lock)*.25)).scale(scaleFrames,1,scaleFrames);
        c.mesh(Layer.HULL_GLASSFALL,square,m,hull,.3f,0);
        // Telescoping truss arms from the hub to the square frame's corners.
        float reach=(float)(3.6+(15.4-3.6)*unfold);
        for(int i=0;i<4;i++) {
            m.set(root).rotateY(spinB+(float)(i*Math.PI/2+Math.PI/4)).translate(3.4f,0,0).scale(reach*scaleFrames-3.4f*scaleFrames+.01f,1,1);
            c.mesh(Layer.HULL_GLASSFALL,arm,m,hull,i*.2f,0);
            if(q.level>=1) {
                double a=-(spinB+i*Math.PI/2+Math.PI/4);
                float ex=(float)Math.cos(a)*reach*scaleFrames,ez=(float)Math.sin(a)*reach*scaleFrames;
                c.tube(Layer.CORE_FLOW,ex,ly+.45f,ez,(float)Math.cos(a)*3.6f,ly+.45f,(float)Math.sin(a)*3.6f,.18f,.18f,5,argb(energy*.8,CYAN),argb(energy,ICE),1,0,(float)(t*.15),(float)(t*.15)+2);
            }
        }
        // Launch rails: hinge from stowed to aiming along each authoritative impact direction.
        int launched=launchedCount(v);
        for(int i=0;i<offsets.length;i++) {
            railMatrix(v,offsets,i,ly,lock,m);
            c.mesh(Layer.HULL_GLASSFALL,rail,m,hull,i*.37f,0);
            if(i>=launched&&v.before(Phase.IMPACT)) {
                boolean last=i==offsets.length-1;
                float pulse=(float)(.75+.25*Math.sin(t*.6+i));
                Matrix4f held=new Matrix4f(m).translate(0,-5.5f,0).scale(last?.8f:1);
                c.mesh(Layer.CORE_CRYSTAL,last?crystalLarge:crystal,held,argb(.55+.4*energy*pulse,ICE),(float)(t*.01),0);
                held.transformPosition(tmp.set(0,0,0));
                c.billboard(Layer.GLOW,tmp.x,tmp.y,tmp.z,last?4.5f:2.6f,0,argb(.5*energy*pulse,CYAN));
            }
        }
        // Central focusing mechanism: a counter-rotating lens stack with a glowing crystal focus.
        for(int k=0;k<3;k++){m.set(root).translate(0,-3.4f-k*.9f,0).rotateY((float)(t*.05*(k%2==0?1:-1))).scale(1-k*.18f,1,1-k*.18f);c.mesh(Layer.HULL_GLASSFALL,lens,m,hull,0,0);}
        c.billboard(Layer.GLOW,0,ly-4.5f,0,3.5f+(float)energy*3,(float)t*.02f,argb(.4+.4*energy,CYAN));
        if(v.in(Phase.ORBITAL_ALIGNMENT)||v.in(Phase.CHARGING)) {
            double p=Fx.frac(t/12.0);
            Fx.shockRing(c,0,ly,0,(float)(5+p*30),2,CYAN,(1-p)*.4*energy,0,q);
        }
    }
    private static int launchedCount(StrikeView v){int n=0;for(var e:v.events())if(!e.impact())n++;return n;}

    private void drawProjectiles(StrikeView v,Canvas c,ClientConfig.Quality q,double t,float ly,float[][] offsets) {
        int index=0;
        for(var e:v.events()) {
            if(e.impact())continue;
            int i=index++;
            double raw=e.progress(v.now());
            if(raw>=1)continue;
            boolean last=i>=offsets.length-1||e.payload().radius()>=v.radius()*.8;
            Vector3f start=railMatrix(v,offsets,i,ly,1,new Matrix4f()).transformPosition(new Vector3f(0,-5.5f,0));
            Vec3 end=e.position().subtract(v.target());
            double bend=(v.hash(1000+i)-.5)*(last?4:10);
            double k=flightProgress(raw);
            Vector3f head=flight(start,end,k,bend,new Vector3f()),ahead=flight(start,end,Math.min(1,k+.02),bend,new Vector3f());
            Vector3f dir=new Vector3f(ahead).sub(head);if(dir.lengthSquared()<1e-8f)dir.set(0,-1,0);dir.normalize();
            float size=last?2.1f:1.35f;
            // Crystal oriented along its flight tangent (mesh tip is -Y).
            // rotateTowards aligns +Z with the tangent (up hint X avoids the vertical singularity); -90 deg about X then maps the -Y tip onto +Z.
            m.translation(head).rotateTowards(dir.x,dir.y,dir.z,1,0,0).rotateX((float)(-Math.PI/2)).rotateY((float)(t*.4)).scale(size);
            c.mesh(Layer.CORE_CRYSTAL,last?crystalLarge:crystal,m,argb(.95,ICE),(float)(t*.02),0);
            if(q.level>=1){m.scale(1.3f);c.mesh(Layer.RIM_CRYSTAL,last?crystalLarge:crystal,m,argb(.55,CYAN),0,(float)(t*.02));}
            c.billboard(Layer.GLOW,head.x,head.y,head.z,(last?9:5.5f),0,argb(.8,CYAN));
            c.billboard(Layer.GLOW,head.x,head.y,head.z,(last?3:2f),0,argb(.95,ICE));
            // Trail: analytic samples behind the head, so it never drifts from the authoritative endpoint.
            int samples=q.level==2?28:q.level==1?18:10;float[] pts=new float[samples*3];
            Vector3f p=new Vector3f();
            for(int s=0;s<samples;s++){double kk=Math.max(0,k-s*(last?.012:.009));flight(start,end,kk,bend,p);pts[s*3]=p.x;pts[s*3+1]=p.y;pts[s*3+2]=p.z;}
            c.ribbon(Layer.CORE_FLOW,pts,samples,size*.7f,size*.05f,argb(.85,ICE),argb(0,DEEP),(float)(-t*.3));
            if(q.level>=1)for(int f=0;f<2;f++) {
                float[] fil=new float[samples*3];
                for(int s=0;s<samples;s++) {
                    double a=t*.5+s*.6+f*Math.PI;float r=size*.7f*(1-s/(float)samples);
                    fil[s*3]=pts[s*3]+(float)Math.cos(a)*r;fil[s*3+1]=pts[s*3+1]+(float)Math.sin(a)*r*.5f;fil[s*3+2]=pts[s*3+2]+(float)Math.sin(a)*r;
                }
                c.ribbon(Layer.GLOW,fil,samples,.08f,.02f,argb(.7,ICE),argb(0,CYAN),0);
            }
            // Atmospheric streaks shed around the head as it accelerates.
            int streaks=Fx.scaled(q,last?24:12);
            for(int s=0;s<streaks;s++) {
                double h=v.hash(1200+i*50+s),pp=Fx.frac(t*.15+h);
                float rr=size*(1.5f+2.5f*(float)v.hash(1300+i*50+s));double a=h*Math.PI*11;
                float ox=(float)Math.cos(a)*rr,oz=(float)Math.sin(a)*rr;
                float bx=head.x-dir.x*(float)(pp*14)+ox,by=head.y-dir.y*(float)(pp*14),bz=head.z-dir.z*(float)(pp*14)+oz;
                c.streak(Layer.GLOW,bx,by,bz,bx-dir.x*4,by-dir.y*4,bz-dir.z*4,.06f,.02f,argb((1-pp)*.7*k,ICE),argb(0,CYAN),0,.5f,1,.5f);
            }
            if(raw<.18)Fx.flash(c,start.x,start.y,start.z,(last?14:8)*(float)(1-raw/.18),ICE,(1-raw/.18));
        }
    }

    private void drawImpacts(StrikeView v,Canvas c,ClientConfig.Quality q,double t) {
        int n=0;
        for(var e:v.events()) {
            if(!e.impact())continue;
            int i=n++;
            double s=e.age(v.now());
            if(s>140)continue;
            Vec3 at=e.position().subtract(v.target());
            float x=(float)at.x,y=(float)at.y,z=(float)at.z,radius=e.payload().radius();
            boolean fin=radius>=v.radius()*.8;
            double seedA=v.hash(1500+i)*Math.PI;
            if(s<5)Fx.flash(c,x,y+1.5f,z,radius*(fin?2.2f:1.6f)*(float)(1-s/6),ICE,1-s/5);
            // Sharp angular shockwaves: hexagonal rings, plus tilted fragments for the final strike.
            Fx.shockRing(c,x,y+.4f,z,0,1,0,radius*(fin?3.2f:2.6f)*Fx.easeOut(s/12),fin?5:2.5f,ICE,(1-s/12)*.95,6,(float)seedA);
            Fx.shockRing(c,x,y+.25f,z,0,1,0,radius*(fin?2.2f:1.7f)*Fx.easeOut((s-2)/18),fin?3:1.6f,CYAN,(s>2?1:0)*(1-(s-2)/18)*.8,fin?8:6,(float)(seedA+.4));
            if(fin) {
                for(int k=0;k<5;k++) {
                    double a=seedA+k*1.3,tilt=.25+.15*k;
                    float nx=(float)(Math.sin(tilt)*Math.cos(a)),ny=(float)Math.cos(tilt),nz=(float)(Math.sin(tilt)*Math.sin(a));
                    double d=(s-k*1.5)/(16+k*3);
                    Fx.shockRing(c,x,y+1+k*1.6f,z,nx,ny,nz,radius*(1.2f+.35f*k)*Fx.easeOut(d),1.6f,k%2==0?ICE:CYAN,(d>0?1:0)*(1-d)*.7,3+k%3*1,(float)(a));
                }
                // Expanding cyan energy dome.
                if(s<26) {
                    float dome=radius*1.4f*Fx.easeOut(s/16);
                    c.sphere(Layer.RIM_CRYSTAL,x,y,z,dome,argb((1-s/26)*.85,CYAN),(float)(t*.02),0,true);
                    c.sphere(Layer.CORE_PLASMA,x,y,z,dome*.55f*(float)(1-s/26),argb((1-s/26)*.6,ICE),0,(float)(t*.02),q.level>=1);
                }
                if(s<20)c.tube(Layer.CORE_FLOW,x,y,z,x,y+70,z,radius*.25f*(float)(1-s/20),.1f,Fx.segments(q,16),argb((1-s/20)*.9,ICE),argb(0,CYAN),1,0,(float)(t*.1),(float)(t*.1)+3);
            } else {
                if(s<10)c.sphere(Layer.RIM_CRYSTAL,x,y,z,radius*.9f*Fx.easeOut(s/6),argb((1-s/10)*.7,CYAN),0,0,false);
            }
            // Crystal spikes erupt from the impact and slowly dissolve (client-only, never real blocks).
            int spikes=fin?(q.level==2?16:q.level==1?10:5):(q.level==2?6:q.level==1?4:2);
            double grow=Fx.easeOut(s/5),fade=1-smooth(range(s,40,fin?130:80));
            if(fade>0)for(int k=0;k<spikes;k++) {
                double a=v.hash(1600+i*31+k)*Math.PI*2,rr=radius*(fin?.95:.85)*(.75+.35*v.hash(1700+i*31+k));
                float h=(float)((fin?2.2:1.4)*(.6+.8*v.hash(1800+i*31+k))*grow*fade);
                float sx=x+(float)Math.cos(a)*(float)rr,sz=z+(float)Math.sin(a)*(float)rr;
                m.translation(sx,y-.6f,sz).rotateY((float)a).rotateZ((float)(.35+.4*v.hash(1900+i*31+k))).rotateX((float)(Math.PI)).scale(h);
                c.mesh(Layer.CORE_CRYSTAL,spike,m,argb(.8*fade,ICE),0,(float)(t*.005));
            }
            Fx.groundGlow(c,x,y+.08f,z,radius*(fin?1.3f:1),CYAN,Math.max(0,1-s/(fin?100:50))*.45);
        }
        if(v.atOrAfter(Phase.AFTERMATH)&&v.before(Phase.COMPLETE)) {
            double a=v.through(Phase.AFTERMATH);
            Fx.groundGlow(c,0,-v.depth()*.5f,0,v.radius()*.9f,DEEP,(1-a)*.35);
        }
    }

    @Override public void tick(StrikeView v,CosmeticParticles p,ClientLevel level,ClientConfig.Quality q) {
        if(v.in(Phase.AFTERMATH)&&p.random()<(1-v.progress())*1.2) {
            Vec3 target=v.target();
            double a=p.random()*Math.PI*2,r=v.radius()*p.random();
            p.spawn(CosmeticParticles.MOTE,Fx.owner(v),target.x+Math.cos(a)*r,target.y+3+p.random()*10,target.z+Math.sin(a)*r,
                    p.signed()*.02,-.05,p.signed()*.02,60+p.random()*40,.18f+p.random()*.15f,0xe0000000|ICE);
        }
    }

    @Override public void event(StrikeView v,StrikeView.Event e,CosmeticParticles p,ClientLevel level,ClientConfig.Quality q) {
        if(!e.impact())return;
        int owner=Fx.owner(v);Vec3 at=e.position();float radius=e.payload().radius();
        boolean fin=radius>=v.radius()*.8;
        int ground=Fx.groundColor(level,at);
        Fx.debrisBurst(p,owner,at,Fx.scaled(q,fin?110:30),fin?1.3f:.9f,fin?1.15f:.8f,ground,.25f,fin?1.2f:.7f,.15f);
        for(int i=0;i<Fx.scaled(q,fin?60:18);i++) {
            double a=p.random()*Math.PI*2,s=(fin?.7:.45)+p.random()*.9;
            p.spawn(CosmeticParticles.SHARD,owner,at.x,at.y+.8,at.z,Math.cos(a)*s,.45+p.random()*(fin?1.1:.7),Math.sin(a)*s,50+p.random()*50,.4f+p.random()*(fin?.9f:.5f),0xf0000000|ICE);
        }
        Fx.sparks(p,owner,at,Fx.scaled(q,fin?50:20),fin?1.7f:1.2f,ICE);
        Fx.dustRing(p,owner,at,Fx.scaled(q,fin?40:12),radius,fin?.8f:.5f,Canvas.lerpRgb(ground,0xb8c8d0,.35),fin?4:2.5f);
    }
}
