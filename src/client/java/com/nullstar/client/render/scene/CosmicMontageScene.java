package com.nullstar.client.render.scene;

import com.nullstar.client.ClientConfig;
import com.nullstar.client.render.Canvas;
import com.nullstar.client.render.Layer;
import com.nullstar.client.render.Mesh;
import com.nullstar.client.render.PresentationScale;
import com.nullstar.client.render.StrikeView;
import com.nullstar.core.CinematicScene;
import com.nullstar.core.Phase;
import org.joml.Matrix4f;
import static com.nullstar.client.render.Canvas.argb;
import static com.nullstar.client.render.StrikeView.range;
import static com.nullstar.client.render.StrikeView.smooth;
import static com.nullstar.client.render.StrikeView.smoother;

/**
 * The authored cosmic act of the colossal Helios annihilation, drawn entirely in view-local space in front of the
 * gameplay camera: the world recedes through atmosphere, planetary, solar-system, galactic and observable-universe
 * scale until an impossibly large star-harvesting mothership fills the frame, deploys every subsystem, contains a
 * star and fires a stellar lance back down at the world.
 *
 * <p>This is a staged cinematic illusion, not an astronomical simulation: every element is composed from
 * deterministic geometry and the read-only {@link StrikeView}, the normalized scene identifier decides what is
 * staged, and the composition recedes in depth as each stage plays so the act keeps travelling outward. Nothing
 * here reads the world, queries terrain, or feeds back into gameplay.
 */
public final class CosmicMontageScene {
    static final int STAR_RED=0xff9a76,STAR_GOLD=0xffdca8,STAR_BLUE=0xcfe0ff,STAR_WHITE=0xffffff;
    static final int DEEP_VOID=0x03040a,NIGHT=0x070b18,OCEAN=0x14355a,LAND=0x4a4234,LIMB=0x6fc0ff,LIMB_WARM=0xffd0a0;
    static final int SUN=0xfff0bc,SUN_CORE=0xfffff2,NEB_VIOLET=0x4a2a86,NEB_TEAL=0x11626e,NEB_ROSE=0x8a3a5c;
    static final int HULL_DARK=0x2b2930,GOLD_TRIM=0xffc86a,HARVEST=0xffa63a,APERTURE=0xfff2d8;

    /** The canvas origin sits this far in front of the eye; the deep backdrop is pushed well beyond it. */
    static final float BACKDROP=-46;

    private static Mesh ringA,ringB,ringC,plinth,vane,pylon,harvester,segment,iris;
    private final Matrix4f m=new Matrix4f(),sub=new Matrix4f();

    private static void build() {
        if(ringA!=null)return;
        ringA=new Mesh.Builder().uvScale(1/8f).ring(6.4f,7.6f,-1.5f,1.5f,0,Math.PI*2,26).build();
        Mesh.Builder a=new Mesh.Builder().uvScale(1/8f).ring(11.6f,13.0f,-1.1f,1.1f,0,Math.PI*2,36);
        for(int i=0;i<12;i++)a.push().rotateY((float)(i*Math.PI/6)).box(12.6f,-1.7f,-1.5f,13.8f,1.7f,1.5f).pop();
        ringB=a.build();
        Mesh.Builder b=new Mesh.Builder().uvScale(1/8f).ring(17.4f,18.8f,-.85f,.85f,0,Math.PI*2,44);
        for(int i=0;i<8;i++)b.push().rotateY((float)(i*Math.PI/4+.2)).box(18.4f,-2.2f,-2.4f,19.6f,2.2f,2.4f).pop();
        ringC=b.build();
        plinth=new Mesh.Builder().uvScale(1/5f).frustum(3.0f,2.2f,-10f,7f,14,true)
                .ring(2.2f,3.2f,-6.4f,-5.2f,0,Math.PI*2,14).ring(2.2f,3.2f,-1.2f,0,0,Math.PI*2,14)
                .ring(2.0f,2.6f,3.2f,4.2f,0,Math.PI*2,12).build();
        Mesh.Builder v=new Mesh.Builder().uvScale(1/9f).box(4.6f,-.5f,-5.4f,20f,.5f,5.4f);
        for(int i=0;i<5;i++){float x=6f+i*2.9f;v.box(x,-.9f,-4.6f,x+.6f,.9f,4.6f);}
        v.push().translate(20f,0,0).rotateZ(-.5f).box(0,-.4f,-3.6f,7f,.4f,3.6f).pop();
        vane=v.build();
        pylon=new Mesh.Builder().uvScale(1/9f).box(-.7f,-13.5f,-.7f,.7f,0,.7f).box(-1.4f,-13.9f,-1.4f,1.4f,-12.6f,1.4f)
                .frustum(1.5f,.5f,-16f,-14f,8,false).build();
        harvester=new Mesh.Builder().uvScale(1/7f).shellPatch(5.4f,6.6f,-.46,.46,.16,1.05,7,5)
                .push().rotateX(1.1f).box(-.5f,-6f,-.7f,.5f,0,.7f).pop().build();
        Mesh.Builder s=new Mesh.Builder().uvScale(1/7f).ring(4.2f,5.2f,-2.2f,2.2f,-.3,.3,18);
        s.push().rotateY(1.2f).ring(3.4f,4.2f,-1.4f,1.4f,-.22,.22,10).pop();
        segment=s.build();
        iris=new Mesh.Builder().uvScale(1/6f).box(0,-.3f,-1.5f,5.2f,.3f,1.5f).build();
    }

    // ------------------------------------------------------------------ staging
    /** How far the mothership has unfolded: reveal is partial, deployment completes it, firing holds it open. */
    private static double deploy(StrikeView v) {
        if(v.atOrAfter(Phase.CHARGING))return 1;
        if(v.in(Phase.DEPLOYMENT))return .4+.6*smoother(v.progress());
        if(v.in(Phase.MOTHERSHIP_REVEAL))return .1+.3*smoother(v.progress());
        return smoother(v.through(Phase.MOTHERSHIP_REVEAL))*.1;
    }
    /** Contained stellar core: grows through the charge, holds, and collapses after the discharge. */
    private static double charge(StrikeView v) {
        if(v.atOrAfter(Phase.FIRING))return 1;
        if(v.in(Phase.ORBITAL_ALIGNMENT))return .55+.45*smoother(v.progress());
        if(v.in(Phase.CHARGING))return .12+.88*smoother(v.progress());
        return deploy(v)*.12;
    }
    /** The stellar lance leaving the aperture, 0 until the discharge begins. */
    private static double lance(StrikeView v) {
        if(v.in(Phase.FIRING))return smooth(range(v.progress(),.12,.55));
        if(v.atOrAfter(Phase.ATMOSPHERIC_ENTRY))return 1;
        return 0;
    }

    public void render(StrikeView view,Canvas canvas,ClientConfig.Quality quality) {
        build();
        CinematicScene scene=view.state().scale().scene();
        if(scene==CinematicScene.WORLD)return;
        double t=view.age();
        double k=smoother(view.progress());
        // Each stage pushes the composition further away and shrinks it, so the act keeps travelling outward.
        float depth=(float)(-22*k),zoom=(float)(1-.36*k);
        backdrop(canvas,view,quality,scene,depth,zoom,t);
        switch(scene) {
            case ASCENT -> ascent(canvas,view,quality,depth,zoom,t,k);
            case PLANET -> planet(canvas,view,quality,depth,zoom,t,k);
            case SOLAR_SYSTEM -> solarSystem(canvas,view,quality,depth,zoom,t,k);
            case GALAXY -> galaxy(canvas,view,quality,depth,zoom,t,k);
            case COSMIC_BOUNDARY -> universe(canvas,view,quality,depth,zoom,t,k);
            case STELLAR_MOTHERSHIP -> mothership(canvas,view,quality,depth,zoom,t);
            default -> { }
        }
    }

    // ------------------------------------------------------------------ shared layers
    private void backdrop(Canvas c,StrikeView v,ClientConfig.Quality q,CinematicScene scene,float depth,float zoom,double t) {
        // Empty space behind the composition. The far plate is opaque (vertex alpha zero switches the hull
        // shader's emissive seams off) because only a depth-writing opaque quad actually replaces the local world
        // behind the cinematic; the nearer translucent layers then only add atmospheric falloff over it.
        c.sceneBillboard(Layer.COSMIC_HULL,0,0,depth,86,56,0,argb(0,DEEP_VOID));
        c.sceneBillboard(Layer.COSMIC_HULL,0,0,depth+BACKDROP,220,140,0,argb(0,DEEP_VOID));
        c.sceneBillboard(Layer.COSMIC_DARK,0,0,depth+BACKDROP+8,168,104,0,argb(.5,NIGHT));
        c.sceneBillboard(Layer.COSMIC_DARK,0,0,depth-12,100,64,(float)(-t*.0024),argb(.34,0x0b1020));
        int nebs=switch(q.level){case 2->4;case 1->3;default->1;};
        int[] tints={NEB_VIOLET,NEB_TEAL,NEB_ROSE,NEB_VIOLET};
        for(int i=0;i<nebs;i++) {
            double h=v.hash(700+i);
            float size=(scene==CinematicScene.GALAXY||scene==CinematicScene.COSMIC_BOUNDARY?36:24)*zoom*(.7f+(float)v.hash(760+i)*.7f);
            c.sceneBillboard(Layer.COSMIC_GAS,(float)((h-.5)*130),(float)((v.hash(720+i)-.5)*70),
                    depth+BACKDROP+9+(float)(v.hash(740+i)*16),size,size*.6f,
                    (float)(t*.02*(i%2==0?1:-1)+h*3),argb(.14+.1*v.hash(780+i),tints[i%tints.length]));
        }
        int stars=switch(scene) {
            case ASCENT -> q.level==2?220:q.level==1?140:60;
            case PLANET -> 300;case SOLAR_SYSTEM -> 320;case GALAXY -> 210;
            case COSMIC_BOUNDARY -> 150;default -> 380;
        };
        stars=(int)(stars*(q.level==2?1:q.level==1?.7:.4));
        starfield(c,v,depth,zoom,t,stars,scene==CinematicScene.ASCENT?.8f:1);
    }
    /** Deterministic twinkling star shell filling the frame; the additive backbone of every cosmic stage. */
    private void starfield(Canvas c,StrikeView v,float depth,float zoom,double t,int count,float brightness) {
        for(int i=0;i<count;i++) {
            double h1=v.hash(i*4+1),h2=v.hash(i*4+2),h3=v.hash(i*4+3),h4=v.hash(i*4+4);
            float x=(float)((h1-.5)*190*zoom),y=(float)((h2-.5)*104*zoom);
            float d=depth+BACKDROP+(float)(h3*28);
            float size=(.05f+(float)(h4*h4)*.2f)*zoom;
            float tw=(float)(.5+.5*Math.sin(t*(.5+h1*2.4)+h2*6.283));
            int rgb=h4<.09?STAR_RED:h4<.24?STAR_GOLD:h1<.35?STAR_BLUE:STAR_WHITE;
            c.sceneBillboard(Layer.COSMIC_STAR,x,y,d,size,size,0,argb((.32+.68*tw)*brightness,rgb));
        }
    }
    private void star(Canvas c,float x,float y,float depth,float size,float alpha,int rgb) {
        c.sceneBillboard(Layer.COSMIC_STAR,x,y,depth,size,size,0,argb(alpha,rgb));
    }
    /** Square soft glow, used wherever a corona, bloom or halo is laid over the composition. */
    private void glow(Canvas c,float x,float y,float depth,float size,double alpha,int rgb) {
        c.sceneBillboard(Layer.COSMIC_GLOW,x,y,depth,size,size,0,argb(alpha,rgb));
    }
    /**
     * A spherical body with its atmosphere, terminator crescent and surface detail. The opaque {@link Layer#COSMIC_HULL}
     * shell writes depth, so every additive shell drawn after it is naturally occluded by the far side.
     */
    private void world(Canvas c,StrikeView v,ClientConfig.Quality q,float x,float y,float depth,float radius,double t,
                       float lit,float zoom) {
        int bands=q.level==2?8:q.level==1?6:3;
        c.sphere(Layer.COSMIC_HULL,x,y,depth,radius,argb(1,OCEAN),0,0,true);
        // Cloud bands: annuli tilted toward the viewer so the sphere's depth buffer culls their far halves.
        for(int i=0;i<bands;i++) {
            double h=v.hash(200+i);
            float tilt=.34f+(float)h*.5f,roll=(float)(v.hash(220+i)*Math.PI*2+t*.004*(i%2==0?1:-1));
            float nx=(float)(Math.sin(tilt)*Math.cos(roll)),ny=(float)Math.cos(tilt),nz=(float)(Math.sin(tilt)*Math.sin(roll));
            float rr=radius*(.3f+.68f*(float)v.hash(240+i)),w=radius*(.05f+.05f*(float)v.hash(260+i));
            c.annulus(Layer.COSMIC_GAS,x,y+rr*(ny-.6f),depth+rr*.5f,nx,ny,nz,Math.max(.02f,rr-w),rr,Fx.segments(q,24),
                    argb(.42*zoom,0xd8e6f2),argb(.03,0xd8e6f2),3,(float)i,.4f,1);
        }
        // Atmosphere: a fresnel shell at the limb plus a broad soft halo.
        c.sphere(Layer.COSMIC_SHELL,x,y,depth,radius*1.045f,argb(.5,limp(lit)),0,(float)(t*.01),q.level>=1);
        c.sceneBillboard(Layer.COSMIC_GAS,x,y,depth+radius*.2f,radius*2.5f,radius*2.5f,(float)(t*.006),argb(.08,limp(lit)));
        // Sunlit limb: additive beads along the lit arc read as a real terminator from any distance.
        if(lit>.05f) {
            int beads=q.level==2?44:q.level==1?30:16;
            for(int i=0;i<beads;i++) {
                double a=Math.PI*(1.15+.7*Math.sin(i*.31+t*.002));
                float bx=x+(float)Math.cos(a)*radius*1.01f,by=y+(float)Math.sin(a)*radius*1.01f;
                float s=radius*(.045f+.03f*(float)v.hash(280+i));
                c.sceneBillboard(Layer.COSMIC_GLOW,bx,by,depth+radius*.1f,s,s,0,argb(lit*.5,LIMB_WARM));
            }
        }
    }
    private static int limp(float lit){return Canvas.lerpRgb(LIMB,LIMB_WARM,lit);}

    // ------------------------------------------------------------------ stage 3: ascent
    private void ascent(Canvas c,StrikeView v,ClientConfig.Quality q,float depth,float zoom,double t,double k) {
        // The world's limb fills the lower frame and falls away as the atmosphere thins.
        float radius=26f*zoom,cy=-radius*1.42f+(float)(k*2);
        world(c,v,q,0,cy,depth-14,radius,t,.55f,zoom);
        // Ground haze and the last cloud deck, stacked low and wide.
        int decks=q.level==2?5:q.level==1?4:2;
        for(int i=0;i<decks;i++) {
            float y=cy+radius*(1.02f+.1f*i);
            c.sceneBillboard(Layer.COSMIC_GAS,0,y,depth-14+i*1.6f,58f*zoom*(1+.06f*i),9f*zoom,
                    (float)(t*.004*(i%2==0?1:-1)),argb(.14-.03f*i,Canvas.lerpRgb(LIMB,LIMB_WARM,.7f)));
        }
        c.sceneBillboard(Layer.COSMIC_GLOW,0,cy+radius,depth-16,44f*zoom,2.6f*zoom,0,argb(.3,LIMB_WARM));
        // Weather glints on the night side keep the surface alive as it recedes.
        int glints=q.level==0?0:q.level==1?6:12;
        for(int i=0;i<glints;i++) {
            double h=v.hash(320+i),s2=v.hash(340+i);
            float a=(float)(h*Math.PI*2),rr=radius*(.4f+.5f*(float)s2);
            star(c,(float)Math.cos(a)*rr,cy+(float)Math.sin(a)*rr*.6f,depth-13.4f,.35f*zoom,(float)(.3+.3*Math.sin(t*.7+i)),STAR_GOLD);
        }
        // The world keeps falling away: a slow receding rim.
        Fx.shockRing(c,0,cy,depth-14,radius*1.5f*(float)(.5+k),1.4f,LIMB,.22*(1-k),0,q);
    }

    // ------------------------------------------------------------------ stage 4a: planet
    private void planet(Canvas c,StrikeView v,ClientConfig.Quality q,float depth,float zoom,double t,double k) {
        float radius=19f*zoom;
        world(c,v,q,0,0,depth-10,radius,t,.85f,zoom);
        // Debris belt: a tilted ellipse of fragment stars, the clearest read of how small a world is.
        int belt=q.level==2?110:q.level==1?72:36;
        double tilt=.52,roll=v.hash(9)*Math.PI*2+.06*t*.004;
        for(int i=0;i<belt;i++) {
            double h=v.hash(400+i),a=h*Math.PI*2+t*.0007;
            float rr=radius*(1.42f+.16f*(float)v.hash(420+i));
            float ex=(float)(Math.cos(a)*rr),ez=(float)(Math.sin(a)*rr);
            float bx=(float)(ex*Math.cos(roll)-ez*Math.sin(roll)),bz=(float)(ex*Math.sin(roll)+ez*Math.cos(roll));
            star(c,bx,(float)(bz*Math.sin(tilt)),depth-10+(float)(bz*Math.cos(tilt)),.12f+(float)v.hash(440+i)*.08f,.5f,STAR_WHITE);
        }
        // Moon: a small lit body that gives the planet an instantly readable size.
        float mx=radius*2.1f,my=radius*.85f;
        c.sphere(Layer.COSMIC_HULL,mx,my,depth-16,radius*.22f,argb(1,0x8f8a80),0,0,q.level>=1);
        c.sphere(Layer.COSMIC_SHELL,mx,my,depth-16,radius*.235f,argb(.4,LIMB),0,0,false);
        // A close pass of orbital debris between the camera and the world, for parallax.
        int debris=q.level==2?26:q.level==1?16:8;
        for(int i=0;i<debris;i++) {
            double h=v.hash(460+i);
            float a=(float)(h*Math.PI*2+k*1.7),rr=radius*(.9f+.35f*(float)v.hash(480+i));
            m.translation((float)Math.cos(a)*rr,(float)Math.sin(a)*rr*.5f-.5f,depth+4+(float)Math.sin(a)*rr*.4f)
                    .rotateXYZ((float)(t*.09+i),(float)(t*.07),0).scale(.28f*(1+(float)v.hash(500+i)));
            c.mesh(Layer.COSMIC_HULL,segment,m,argb(1,0x6a6156),0,0);
        }
    }

    // ------------------------------------------------------------------ stage 4b: solar system
    private void solarSystem(Canvas c,StrikeView v,ClientConfig.Quality q,float depth,float zoom,double t,double k) {
        float sunR=1.5f*zoom;
        // The star at the centre of everything.
        c.sphere(Layer.COSMIC_PLASMA,0,0,depth-14,sunR,argb(1,SUN_CORE),(float)(t*.02),(float)(t*.03),q.level>=1);
        c.sphere(Layer.COSMIC_PLASMA,0,0,depth-14,sunR*1.8f,argb(.7,SUN),(float)(-t*.02),0,q.level>=1);
        c.sphere(Layer.COSMIC_SHELL,0,0,depth-14,sunR*3.1f,argb(.35,SUN),(float)(t*.01),0,false);
        glow(c,0,0,depth-14,sunR*11,.5,SUN);
        glow(c,0,0,depth-14,sunR*3.4f,.85,SUN_CORE);
        // Orbital paths: thin tilted annuli that precess slowly and give the system its structure.
        int planets=q.level==2?8:q.level==1?7:5;
        double tilt0=.42,roll0=v.hash(21)*Math.PI*2;
        for(int i=0;i<planets;i++) {
            double h=v.hash(600+i);
            float rr=(4.4f+i*3.1f)*zoom;
            double roll=roll0+(i%2==0?.14:-.11)*i;
            float nx=(float)(Math.sin(tilt0)*Math.cos(roll)),ny=(float)Math.cos(tilt0),nz=(float)(Math.sin(tilt0)*Math.sin(roll));
            c.annulus(Layer.COSMIC_SHELL,0,0,depth-15,nx,ny,nz,Math.max(.02f,rr-.06f),rr,Fx.segments(q,40),
                    argb(.1,0x9ec8ff),argb(.16,0x9ec8ff),(float)i,.4f,.5f,1);
            // The planet itself: a small body on its orbit, with its own tint and a tiny moon for the big ones.
            double a=h*Math.PI*2+t*.0016*(1+.35*i);
            double ex=Math.cos(a)*rr,ez=Math.sin(a)*rr;
            float px=(float)(ex*Math.cos(roll)-ez*Math.sin(roll)),pz=(float)(ex*Math.sin(roll)+ez*Math.cos(roll));
            float py=(float)(pz*Math.sin(tilt0));pz=(float)(pz*Math.cos(tilt0));
            float pr=(.5f+(float)v.hash(620+i)*1.5f)*zoom;
            int tint=i%3==0?0x8fb4c8:i%3==1?0xb08a5e:0x5f7fa8;
            c.sphere(Layer.COSMIC_HULL,px,py,depth-15+pz,pr,argb(1,tint),0,0,q.level>=1);
            c.sphere(Layer.COSMIC_SHELL,px,py,depth-15+pz,pr*1.12f,argb(.34,LIMB),0,0,false);
            glow(c,px,py,depth-15+pz,pr*3.2f,.35,SUN);
            if(i==2&&q.level>=1)star(c,px+pr*3.4f,py+pr*.8f,depth-15+pz,.28f,.7f,STAR_BLUE);
        }
        // The world we came from, now a single mote on the third orbit.
        star(c,(float)(zoom*10.6),(float)(zoom*-3.4),depth-13.4f,.34f*.9f,.95f,STAR_BLUE);
    }

    // ------------------------------------------------------------------ stage 4c: galaxy
    private void galaxy(Canvas c,StrikeView v,ClientConfig.Quality q,float depth,float zoom,double t,double k) {
        int arms=q.level==2?5:q.level==1?4:3;
        int per=(int)((q.level==2?96:q.level==1?62:30)*zoom);
        double spin=t*.0025+v.hash(31)*Math.PI*2;
        // Disk: a wide flattened dust field that gives the galaxy its plane before the arms are laid in.
        c.sceneBillboard(Layer.COSMIC_GAS,0,0,depth-30,34f*zoom,13f*zoom,(float)(spin*.4),argb(.16,NEB_VIOLET));
        c.sceneBillboard(Layer.COSMIC_GAS,0,0,depth-28,24f*zoom,7f*zoom,0,argb(.2,0xffe9c8));
        for(int arm=0;arm<arms;arm++) {
            double base=spin+arm*Math.PI*2/arms;
            for(int i=0;i<per;i++) {
                double s=(i+1d)/per;
                double angle=base+s*3.5;
                float rr=(float)((1.2f+26f*s)*zoom);
                double h=v.hash(800+arm*211+i),scatter=1+(h-.5)*.16;
                float x=(float)Math.cos(angle)*rr*(float)scatter,y=(float)(Math.sin(angle)*rr*(float)scatter*.36);
                float size=(.03f+(float)(h*h)*.16f)*zoom;
                float warm=(float)Math.max(0,1-s*1.1);
                int rgb=Canvas.lerpRgb(0x9fb8ff,0xfff0c8,warm);
                star(c,x,y,depth-30+(float)((h-.5)*4),size,.35f+.5f*(float)Math.sin(t*.6+i+arm),rgb);
            }
            // Dust lanes: dark clouds laid along each arm so the arms read as raised structure.
            if(q.level>=1)for(int j=1;j<=2;j++) {
                double s=.32+j*.3,angle=base+s*3.5;
                float rr=(float)((1.2f+26f*s)*zoom);
                c.sceneBillboard(Layer.COSMIC_DARK,(float)Math.cos(angle)*rr,(float)(Math.sin(angle)*rr*.36),depth-24,
                        (4.5f-j*.6f)*zoom*(1+j*.2f),2.1f*zoom,0,argb(.3-j*.08,0x180d24));
            }
        }
        // Core: a dense bulge with a bright nucleus and a pair of polar jets.
        int bulge=(int)((q.level==2?130:q.level==1?84:40)*zoom);
        for(int i=0;i<bulge;i++) {
            double h1=v.hash(900+i),h2=v.hash(920+i),h3=v.hash(940+i);
            double a=h1*Math.PI*2;float r=(float)(Math.sqrt(h3)*4.6*zoom);
            float x=(float)Math.cos(a)*r,y=(float)(Math.sin(a)*r*.62),d=depth-28+(float)((h2-.5)*3);
            star(c,x,y,d,(.05f+(float)h2*.14f)*zoom,.4f+.5f*(float)Math.sin(t*1.1+i),Canvas.lerpRgb(0xfff6dc,0xffd9a0,h2));
        }
        glow(c,0,0,depth-28,6.4f*zoom,.55,0xfff0c8);
        c.sceneBillboard(Layer.COSMIC_PLASMA,0,0,depth-28,2.1f*zoom,2.1f*zoom,0,argb(.8,0xffecc0));
        if(q.level>=1)for(int s=-1;s<=1;s+=2)
            c.tube(Layer.COSMIC_GLOW,0,0,depth-28,0,s*15f*zoom,depth-28,.35f*zoom,.05f,Fx.segments(q,6),
                    argb(.18,0xdfe8ff),argb(0,0x9fb8ff),1,0,(float)(t*.01),(float)(t*.01)+2);
        // Our own system, still visible as an ordinary mote on the second arm.
        star(c,(float)(13.4*zoom),(float)(4.6*zoom),depth-26,.4f*zoom,.9f,STAR_GOLD);
    }

    // ------------------------------------------------------------------ stage 4d: observable universe
    private void universe(Canvas c,StrikeView v,ClientConfig.Quality q,float depth,float zoom,double t,double k) {
        // Cosmic web: filament nodes on a shell, joined by filaments where the shell's geometry really connects them.
        int nodes=(int)((q.level==2?150:q.level==1?100:52)*zoom);
        float[] px=new float[nodes],py=new float[nodes],pd=new float[nodes];
        for(int i=0;i<nodes;i++) {
            double h1=v.hash(1000+i),h2=v.hash(1100+i),h3=v.hash(1200+i),h4=v.hash(1300+i);
            double a=h1*Math.PI*2,e=Math.acos(1-2*h2*h2);
            float r=(20f+16f*(float)h3)*zoom;
            px[i]=(float)(Math.sin(e)*Math.cos(a)*r);
            py[i]=(float)(Math.cos(e)*r*.6);
            pd[i]=depth-34-(float)(h4*10);
            star(c,px[i],py[i],pd[i],(.05f+(float)h4*.16f)*zoom,.35f+.5f*(float)Math.sin(t*.5+i),i%7==0?STAR_GOLD:STAR_WHITE);
        }
        int links=nodes*(q.level==2?2:1)/2;
        for(int i=0;i<links;i++) {
            int a=(int)(v.hash(1400+i)*nodes),b=(int)(v.hash(1500+i)*nodes);
            if(a==b)continue;
            float dx=px[b]-px[a],dy=py[b]-py[a],dz=pd[b]-pd[a];
            if(dx*dx+dy*dy+dz*dz>360f*zoom*zoom)continue;
            c.streak(Layer.COSMIC_GAS,px[a],py[a],pd[a],px[b],py[b],pd[b],.035f,.035f,
                    argb(.2,0x7f9fd8),argb(0,0x4a5f9a),0,.3f,1,.7f);
        }
        // The boundary itself: a slowly turning shell with a redshifted edge, and the emptiness beyond it.
        int shells=q.level==2?3:q.level==1?2:1;
        for(int s=0;s<shells;s++) {
            float r=(32f+s*9f)*zoom;
            double a=t*.0025*(s%2==0?1:-1)+v.hash(50+s)*6.28;
            float nx=(float)(Math.sin(a)*.35),ny=(float)Math.cos(.35),nz=(float)Math.cos(a)*.35f;
            c.annulus(Layer.COSMIC_SHELL,0,0,depth-42+s*4,nx,ny,nz,Math.max(.05f,r-.12f),r,Fx.segments(q,52),
                    argb(.1-s*.02,0xff9a6a),argb(.24-s*.05,0xffc89a),(float)(1+s),0,0,.7f);
        }
        for(int s=0;s<2;s++) {
            int rim=(int)((q.level==2?70:q.level==1?44:22)*zoom);
            for(int i=0;i<rim;i++) {
                double h=v.hash(1600+s*97+i),a=h*Math.PI*2+t*.001*(s%2==0?1:-1);
                float r=(44f+s*10f)*zoom;
                star(c,(float)Math.cos(a)*r,(float)Math.sin(a)*r*.55f,depth-44-s*4,.12f*zoom,.3f+.3f*(float)Math.sin(t+i),0xd8a0ff);
            }
        }
        // Everything we crossed is one mote now, and the mothership's star hangs beyond the boundary.
        star(c,0,0,depth-40,.7f*zoom,.55f,0xfff0d0);
        star(c,(float)(9*zoom),(float)(-6*zoom),depth-40,1.1f*zoom,.5f+.5f*(float)Math.sin(t*.9),HARVEST);
    }

    // ------------------------------------------------------------------ stages 5-8: the star mothership
    private void mothership(Canvas c,StrikeView v,ClientConfig.Quality q,float depth,float zoom,double t) {
        double deploy=deploy(v),charge=charge(v),shoot=lance(v);
        float s=(1.5f+1.1f*(float)deploy)*zoom;
        float cy=2.5f,d=depth-6;
        int nodes=PresentationScale.nodes(v);
        int hull=argb(1,Canvas.lerpRgb(HULL_DARK,GOLD_TRIM,(float)(charge*.22)));
        float spin=(float)(t*.06);
        // --- core keel: stacked reactor corridors along the machine's axis.
        sub.identity().translate(0,cy,d).rotateY((float)(v.hash(11)*6.283)).scale(s,s,s);
        c.mesh(Layer.COSMIC_HULL,plinth,sub,hull,0,(float)(t*.012));
        // --- armoured collar rings, counter-rotating, extending outward as the machine deploys.
        float[] radii={1f,1.5f,2.1f};
        Mesh[] rings={ringA,ringB,ringC};
        for(int i=0;i<rings.length;i++) {
            float extend=(float)((1-deploy)*.34);
            m.set(sub).rotateY(spin*(i%2==0?1:-1.25f)+i*.7f).translate(0,2.4f-i*2.6f,0).scale(1-extend,1,1-extend);
            c.mesh(Layer.COSMIC_HULL,rings[i],m,argb(Math.min(1,.5+charge*.5),Canvas.lerpRgb(HULL_DARK,GOLD_TRIM,charge*.3f)),i*.4f,0);
        }
        // --- the focusing rings of light between the armour: the charge visibly circulating the hull.
        for(int i=0;i<3;i++) {
            float r=(7.4f+i*4.2f)*s,rr=r*(1+(float)charge*.25f);
            c.tube(Layer.COSMIC_GLOW,(float)Math.cos(spin+i)*rr,cy+2.4f-i*2.6f,(float)Math.sin(spin+i)*rr,
                    (float)Math.cos(spin+i+.35f)*rr,cy+2.4f-i*2.6f,(float)Math.sin(spin+i+.35f)*rr,
                    .22f*s,.22f*s,Fx.segments(q,8),argb(.4*charge,GOLD_TRIM),argb(.1*charge,GOLD_TRIM),2,(float)(t*.1),0,.6f);
        }
        // --- armoured segments around the mid hull.
        int segs=q.level==2?12:q.level==1?9:6;
        for(int i=0;i<segs;i++) {
            float a=(float)(i*Math.PI*2/segs+spin*.3+v.hash(20+i)*.2);
            float r=6.2f*s;
            m.set(sub).translate((float)Math.cos(a)*r,0,(float)Math.sin(a)*r).rotateY(-a).rotateZ((float)((deploy-1)*.4));
            c.mesh(Layer.COSMIC_HULL,segment,m,hull,i*.3f,0);
        }
        // --- star-harvesting vanes on the upper hull: enormous panels swinging open and drinking the starfield.
        int vanes=nodes>8?8:6;
        for(int i=0;i<vanes;i++) {
            float a=(float)(i*Math.PI*2/vanes+spin*.15);
            float open=(float)(-1.25+1.25*deploy);
            m.set(sub).rotateY(a).translate(0,4.2f,2.6f).rotateX(open).rotateZ((float)(Math.sin(i*1.7))) ;
            c.mesh(Layer.COSMIC_HULL,vane,m,argb(Math.min(1,.6+charge*.4),Canvas.lerpRgb(0x514d42,GOLD_TRIM,(float)charge*.35)),i*.2f,0);
            if(q.level>=1) {
                float hx=(float)Math.cos(a)*(20f*s),hz=(float)Math.sin(a)*(20f*s);
                c.tube(Layer.COSMIC_FLOW,hx,(float)(cy+4.2f*s),d+hz,hx*.42f,cy+2.4f*s,d+hz*.42f,.3f*s,.14f*s,Fx.segments(q,6),
                        argb(.55*charge,HARVEST),argb(.15*charge,GOLD_TRIM),2,(float)(t*.14),0,.7f);
            }
        }
        // --- harvest shells: curved collectors ringing the hull, lit by the core.
        for(int i=0;i<6;i++) {
            float a=(float)(i*Math.PI/3-spin*.22);
            m.set(sub).rotateY(a).translate(0,6.4f,0).rotateX((float)(.5-.5*deploy));
            c.mesh(Layer.COSMIC_HULL,harvester,m,argb(1,Canvas.lerpRgb(0x6a5a3c,GOLD_TRIM,(float)charge*.6)),i*.3f,0);
        }
        // --- targeting pylons: long booms that swing down and lock onto the world below.
        int pylons=q.level==2?8:q.level==1?6:4;
        for(int i=0;i<pylons;i++) {
            float a=(float)(i*Math.PI*2/pylons+spin*.08+Math.PI/pylons);
            float aim=(float)((1-deploy)*1.1);
            m.set(sub).rotateY(a).translate(8.4f,0,0).rotateZ(aim);
            c.mesh(Layer.COSMIC_HULL,pylon,m,hull,i*.37f,0);
            if(charge>.2) {
                float tip=(float)(cy-13.5*s);
                star(c,(float)(Math.cos(a)*8.4f*s*Math.cos(aim)),tip,(float)(d+Math.sin(a)*8.4f*s),(.3f+.5f*(float)charge)*s,(float)(charge*.8),0xff6a3a);
            }
        }
        // --- aperture: the iris blades and the contained star they hold.
        int blades=q.level==2?10:q.level==1?8:5;
        for(int i=0;i<blades;i++) {
            float a=(float)(i*Math.PI*2/blades);
            float open=(float)((1-charge)*2.2);
            m.set(sub).translate(0,-9.6f,0).rotateY(a).translate(open*s,0,0).rotateY((float)(-.35+charge*.35));
            c.mesh(Layer.COSMIC_HULL,iris,m,hull,0,0);
        }
        double pulse=.85+.15*Math.sin(t*2.1);
        float coreR=(1.1f+2.6f*(float)charge)*s;
        c.sphere(Layer.COSMIC_PLASMA,0,cy-9.6f*s,d,coreR*.5f,argb(1,APERTURE),(float)(t*.02),(float)(t*.03),q.level>=1);
        c.sphere(Layer.COSMIC_PLASMA,0,cy-9.6f*s,d,coreR,argb(.9*pulse,HARVEST),-(float)(t*.02),(float)(t*.03),q.level>=1);
        if(q.level>=1)c.sphere(Layer.COSMIC_SHELL,0,cy-9.6f*s,d,coreR*1.5f,argb(.45*charge,HARVEST),0,0,false);
        glow(c,0,cy-9.6f*s,d,coreR*5f,.35+.4*charge,HARVEST);
        // --- the lance: an enormous layered beam from the aperture down to the mote of a world below.
        if(shoot>0) {
            float top=cy-9.6f*s,bottom=-21f*zoom;
            float width=(2.2f+3.4f*(float)shoot)*s;
            int seg=Fx.segments(q,26);
            c.tube(Layer.COSMIC_LANCE,0,top,d,0,bottom,d,width*.42f,width*.52f,seg,argb(shoot,APERTURE),argb(shoot*.85,APERTURE),2,(float)(t*.13),(float)(t*.13),(float)(t*.13)+4);
            c.tube(Layer.COSMIC_LANCE,0,top,d,0,bottom,d,width*.95f,width*1.1f,seg,argb(shoot*.7,SUN),argb(shoot*.5,HARVEST),3,(float)(-t*.08),0,2);
            c.tube(Layer.COSMIC_LANCE_RIM,0,top,d,0,bottom,d,width*1.5f,width*1.75f,seg,argb(shoot*.4,HARVEST),argb(0,HARVEST),2,(float)(t*.06),0,1.4f);
            // Unstable edges: fast streaks blown outward along the column.
            int streaks=Fx.scaled(q,44);
            for(int i=0;i<streaks;i++) {
                double h=v.hash(1900+i),p=Fx.frac(t*(.05+.05*v.hash(2000+i))+h);
                double a=h*Math.PI*13;float rr=(float)(width*(1.1f+2.2f*v.hash(2100+i)));
                float y=top+(bottom-top)*(float)p;
                c.streak(Layer.COSMIC_GLOW,(float)Math.cos(a)*rr,y,d+(float)Math.sin(a)*rr,
                        (float)Math.cos(a)*rr*1.04f,y-9f,d+(float)Math.sin(a)*rr*1.04f,
                        .16f*s,.4f*s,argb((1-p)*shoot*.75,0xffe6b8),argb(0,HARVEST),0,.5f,1,.5f);
            }
            Fx.flash(c,0,top,d,width*4f,APERTURE,shoot*.9);
        }
        // --- the world below: a single luminous mote, and the scale reference for everything above it.
        float pr=1.05f*zoom;
        c.sphere(Layer.COSMIC_HULL,0,-21f*zoom,d-pr*1.6f,pr,argb(1,OCEAN),0,0,q.level>=1);
        c.sphere(Layer.COSMIC_SHELL,0,-21f*zoom,d-pr*1.6f,pr*1.2f,argb(.55,LIMB),0,0,false);
        glow(c,0,-21f*zoom,d-pr*1.6f,pr*4f,.3,LIMB);
        if(shoot>.2) {
            // Contact: the lance's own impact blooming on the mote's horizon.
            double s2=Math.min(1,(shoot-.2)/.8);
            glow(c,0,-21f*zoom,d-pr*1.4f,(float)(pr*(4+16*s2)),.55*shoot*(1-s2*.4),0xfff4d0);
            Fx.shockRing(c,0,-21f*zoom,d-pr*1.4f,(float)(pr*(2+9*s2)),.3f,APERTURE,.7*shoot*Math.max(0,1-s2*.7),0,q);
            Fx.shockRing(c,0,-21f*zoom,d-pr*1.4f,(float)(pr*(1.4+6*s2)),.2f,LIMB,.5*shoot*Math.max(0,1-s2),1.1f,q);
        }
    }
}
