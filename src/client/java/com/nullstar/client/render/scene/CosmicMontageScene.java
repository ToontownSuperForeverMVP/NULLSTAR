package com.nullstar.client.render.scene;

import com.nullstar.client.ClientConfig;
import com.nullstar.client.NullstarClient;
import com.nullstar.client.render.Canvas;
import com.nullstar.client.render.Layer;
import com.nullstar.client.render.Mesh;
import com.nullstar.client.render.NullstarMaterials;
import com.nullstar.client.render.StrikeView;
import com.nullstar.core.CinematicScene;
import com.nullstar.core.Phase;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import static com.nullstar.client.render.Canvas.argb;
import static com.nullstar.client.render.StrikeView.range;
import static com.nullstar.client.render.StrikeView.smooth;
import static com.nullstar.client.render.StrikeView.smoother;

/**
 * The cosmic act of the colossal Helios annihilation, Part 3 reconstruction.
 *
 * <p>The act is one continuous outward journey filmed by an authored <em>virtual camera</em>: the real camera rises
 * out of the Minecraft world and punches through the cloud deck, then the virtual camera climbs over the planet's
 * limb, pulls back past its moon into the solar system, out of the Milky Way, backward through the cosmic web, turns
 * to face the observable boundary, passes through it, and finds the star-harvesting mothership on the far side. The
 * mothership deploys, charges and fires; the camera rides the stellar lance all the way back to the planet.
 *
 * <p>Rendering goes through {@link Canvas#beginVirtual}: an opaque procedural sky dome overwrites colour and depth of
 * the world drawn before it, and every cosmic body is compressed (direction-preserving) to within a block of the
 * eye, so no terrain can intrude into the frame. Each act hands its subject to the next at the centre of the frame,
 * so the journey reads as continuous rather than as a slideshow.
 *
 * <p>Everything is a pure function of the read-only {@link StrikeView}; nothing touches the world or gameplay.
 */
public final class CosmicMontageScene {
    static final int SUN=0xffe2a8,SUN_HOT=0xfff6e2,SKY_BLUE=0x5aa8ff,WARM=0xffb070,WHITE=0xffffff,COLD=0x9fc4ff,RED=0xff6a48;
    /** Acts of the journey, in order. */
    public enum Act { WORLD_RISE, ASCENT, PLANET, SOLAR, GALAXY, UNIVERSE, SHIP }

    private static Mesh planetSphere,lowSphere;
    private final StellarMothership ship=new StellarMothership();
    private final StellarMothership.Pose pose=new StellarMothership.Pose();
    private final Canvas.Light light=new Canvas.Light();
    private final Matrix4f m=new Matrix4f();
    // Virtual camera of the current frame.
    private float ex,ey,ez,tx,ty,tz,upX=0,upY=1,upZ=0,lens=1;
    private final float[] cat=new float[3];

    private static void build() {
        if(planetSphere!=null)return;
        planetSphere=new Mesh.Builder().sphere(1,48,24).build();
        lowSphere=new Mesh.Builder().sphere(1,24,12).build();
    }

    /** Act and its 0..1 progress from the authoritative phase. */
    public record Stage(Act act,double p) {}
    public static Stage stage(StrikeView v) {
        double p=v.progress();
        return switch(v.phase()) {
            case ASCENT -> rise(.6*p);
            case EXOATMOSPHERIC_TRANSITION -> rise(.6+.4*p);
            case PLANETARY_VIEW -> new Stage(Act.PLANET,p);
            case SOLAR_SYSTEM_VIEW -> new Stage(Act.SOLAR,p);
            case GALACTIC_VIEW -> new Stage(Act.GALAXY,p);
            case COSMIC_BOUNDARY_VIEW -> new Stage(Act.UNIVERSE,p);
            case MOTHERSHIP_REVEAL,DEPLOYMENT,CHARGING,ORBITAL_ALIGNMENT,FIRING -> new Stage(Act.SHIP,p);
            default -> switch(v.cinematicScene()) {
                case ASCENT -> rise(.5);case PLANET -> new Stage(Act.PLANET,.5);case SOLAR_SYSTEM -> new Stage(Act.SOLAR,.5);
                case GALAXY -> new Stage(Act.GALAXY,.5);case COSMIC_BOUNDARY -> new Stage(Act.UNIVERSE,.5);default -> new Stage(Act.SHIP,1);
            };
        };
    }
    /** The ascent first lifts the real camera out of the world (WORLD_RISE), then hands over to the virtual climb. */
    private static Stage rise(double a){return a<WORLD_RISE_END?new Stage(Act.WORLD_RISE,a/WORLD_RISE_END):new Stage(Act.ASCENT,(a-WORLD_RISE_END)/(1-WORLD_RISE_END));}
    static final double WORLD_RISE_END=.3;
    /** Real-camera lift (blocks) and downward pitch blend while the act still films the real world; -1 once virtual. */
    public static double worldRise(com.nullstar.core.Phase phase,double progress) {
        if(phase!=Phase.ASCENT)return -1;
        double a=.6*progress;
        return a<WORLD_RISE_END?a/WORLD_RISE_END:-1;
    }

    public void render(StrikeView view,Canvas canvas,ClientConfig.Quality quality,Vec3 camera) {
        if(view.cinematicScene()==CinematicScene.WORLD)return;
        build();
        Stage stage=stage(view);
        double t=view.age();
        if(stage.act()==Act.WORLD_RISE){worldRise(view,canvas,quality,camera,stage.p(),t);return;}
        boolean reduced=NullstarClient.config().reducedFlashing;
        try {
            switch(stage.act()) {
                case ASCENT -> ascent(view,canvas,quality,stage.p(),t,reduced);
                case PLANET -> planetAct(view,canvas,quality,stage.p(),t,reduced);
                case SOLAR -> solar(view,canvas,quality,stage.p(),t,reduced);
                case GALAXY -> galaxy(view,canvas,quality,stage.p(),t,reduced);
                case UNIVERSE -> universe(view,canvas,quality,stage.p(),t,reduced);
                default -> mothership(view,canvas,quality,stage.p(),t,reduced);
            }
        } finally {
            canvas.endVirtual();
        }
    }

    // ------------------------------------------------------------------ camera helpers
    private void eye(double x,double y,double z){ex=(float)x;ey=(float)y;ez=(float)z;}
    private void at(double x,double y,double z){tx=(float)x;ty=(float)y;tz=(float)z;}
    private void begin(Canvas c,StrikeView v,ClientConfig.Quality q,double journey,double flash,double nebula,double band) {
        c.beginVirtual(ex,ey,ez,tx-ex,ty-ey,tz-ez,upX,upY,upZ,lens);
        int controls=(c255(band)<<24)|(c255(.62)<<16)|(c255(flash)<<8)|c255(nebula);
        int seg=q.level==2?40:q.level==1?32:24;
        c.skyDome(seg,seg/2,(float)journey,(float)(v.age()/20),controls);
    }
    private static int c255(double v){return (int)Math.round(Math.max(0,Math.min(1,v))*255);}
    private static double lerp(double a,double b,double t){return a+(b-a)*t;}
    private static double expo(double a,double b,double t){return a*Math.pow(b/a,Math.max(0,Math.min(1,t)));}
    /** Catmull-Rom through key positions (x,y,z triples) at parameter t in [0,1]; result in {@link #cat}. */
    private float[] spline(float[] keys,double t) {
        int n=keys.length/3-1;
        double f=Math.max(0,Math.min(1,t))*n;
        int i=Math.min(n-1,(int)f);double u=f-i;
        for(int a=0;a<3;a++) {
            double p0=keys[Math.max(0,i-1)*3+a],p1=keys[i*3+a],p2=keys[(i+1)*3+a],p3=keys[Math.min(n,i+2)*3+a];
            cat[a]=(float)(.5*((2*p1)+(-p0+p2)*u+(2*p0-5*p1+4*p2-p3)*u*u+(-p0+3*p1-3*p2+p3)*u*u*u));
        }
        return cat;
    }
    /** Small deterministic camera tremor, honouring the player's shake preference. */
    private void tremor(double amount,double t) {
        double k=amount*NullstarClient.config().cameraShakeIntensity*(NullstarClient.config().shakeAllowed()?1:0);
        if(k<=0)return;
        ex+=(float)(Math.sin(t*1.7)*k+Math.sin(t*4.1)*k*.5);ey+=(float)(Math.sin(t*2.3+1)*k*.6);ez+=(float)(Math.sin(t*1.3+2)*k);
    }

    // ------------------------------------------------------------------ shared painters
    private void glow(Canvas c,float x,float y,float z,float size,double alpha,int rgb){c.billboard(Layer.COSMIC_GLOW,x,y,z,size,0,argb(alpha,rgb));}
    private void flare(Canvas c,float x,float y,float z,float size,double alpha,int rgb){c.fxSprite(Layer.COSMIC_FX,Canvas.FX_FLARE,0,x,y,z,size,size,0,argb(alpha,rgb));}
    private void corona(Canvas c,float x,float y,float z,float size,double control,double alpha,int rgb,float spin){c.fxSprite(Layer.COSMIC_FX,Canvas.FX_CORONA,control,x,y,z,size,size,spin,argb(alpha,rgb));}
    /** A star body: procedural photosphere, two coronae and a bloom. */
    private void star(Canvas c,float x,float y,float z,float r,double instability,double t,int rgb,double brightness) {
        m.identity().translate(x,y,z).rotateY((float)(t*.003)).rotateX(.4f).scale(r);
        c.mesh(Layer.COSMIC_FX,lowSphere,m,argb(Math.min(1,brightness),rgb),Canvas.fxU(instability),Canvas.fxV(Canvas.FX_STAR));
        corona(c,x,y,z,r*3.2f,instability,.7*brightness,rgb,(float)(t*.002));
        corona(c,x,y,z,r*8f,Math.min(1,instability+.25),.25*brightness,rgb,(float)(-t*.0014));
        glow(c,x,y,z,r*14f,.18*brightness,rgb);
    }
    /**
     * The home world: textured, sunlit body with night-side lights, an independent cloud deck and a limb-scattering
     * atmosphere, all lit by the same key light.
     */
    private void world(Canvas c,float x,float y,float z,float r,double spin,float sx,float sy,float sz,ClientConfig.Quality q,double detail) {
        light.key(sx,sy,sz,0xfff3e0,2.15f).point(0,0,0,0,0,1).ambient(0x1a2433,.16f).rim(0x6fb0ff,.14f);
        m.identity().translate(x,y,z).rotateX(-1.1f).rotateY((float)spin).scale(r);
        c.litMesh(Layer.COSMIC_PLANET,planetSphere,m,light,Canvas.Shading.PLANET,WHITE,.85,0,0);
        if(detail>0) {
            m.identity().translate(x,y,z).rotateX(-1.1f).rotateY((float)(spin*1.35+.7)).scale(r*1.012f);
            c.litMesh(Layer.COSMIC_CLOUD,q.level==0?lowSphere:planetSphere,m,light,Canvas.Shading.SCATTER,WHITE,.92*detail,0,0);
        }
        light.ambient(0x0a1424,.04f).rim(0,0);
        m.identity().translate(x,y,z).scale(r*1.04f);
        c.litMesh(Layer.COSMIC_FX,planetSphere,m,light,Canvas.Shading.SCATTER,SKY_BLUE,.95,Canvas.fxU(0),Canvas.fxV(Canvas.FX_ATMOSPHERE));
    }
    /** Streaks of near stars sliding past the lens, length tied to the virtual camera's speed: the sense of travel. */
    private void flow(Canvas c,StrikeView v,int count,float radius,float dx,float dy,float dz,double alpha,int salt) {
        float speed=(float)Math.sqrt(dx*dx+dy*dy+dz*dz);
        if(speed<1e-3f||alpha<=0)return;
        c.virtualOrigin(ex,ey,ez);
        for(int i=0;i<count;i++) {
            double h1=v.hash(salt+i*3),h2=v.hash(salt+i*3+1),h3=v.hash(salt+i*3+2);
            // Positions wrap through a cube around the camera so the field is endless in any direction of travel.
            float px=wrap((float)(h1*2-1)*radius-ex,radius),py=wrap((float)(h2*2-1)*radius-ey,radius),pz=wrap((float)(h3*2-1)*radius-ez,radius);
            float d=(float)Math.sqrt(px*px+py*py+pz*pz);
            if(d<radius*.08f)continue;
            float fade=(float)(alpha*Math.min(1,(1-d/(radius*1.7f))*2));
            if(fade<=0)continue;
            c.streak(Layer.COSMIC_STAR,px,py,pz,px+dx,py+dy,pz+dz,radius*.0016f,radius*.0008f,argb(fade*.6,WHITE),argb(0,COLD),0,0,1,1);
        }
    }
    private static float wrap(float value,float radius){float span=radius*2;value=value%span;if(value>radius)value-=span;if(value<-radius)value+=span;return value;}

    // ------------------------------------------------------------------ act 0: rising out of the real world
    /**
     * The real camera is lifted out of the world by {@code CinematicCamera}; this act only adds the closing cloud
     * deck and the white punch-through that hands over to the virtual climb.
     */
    private void worldRise(StrikeView v,Canvas c,ClientConfig.Quality q,Vec3 camera,double p,double t) {
        c.origin(camera);
        double veil=smooth(range(p,.45,1));
        int puffs=q.level==2?46:q.level==1?30:16;
        for(int i=0;i<puffs;i++) {
            double h=v.hash(6000+i),a=h*Math.PI*2,rr=8+v.hash(6100+i)*34,lift=(v.hash(6200+i)-.3)*22-(1-veil)*40;
            float x=(float)(Math.cos(a)*rr),z=(float)(Math.sin(a)*rr),y=(float)(lift+p*6);
            c.smoke(x,y,z,(float)(9+v.hash(6300+i)*14),(float)(t*.004+h*6),argb(.75*veil,0xf4f7fb),i&3);
        }
        // White-out: the camera enters the deck and the world is gone.
        double white=smooth(range(p,.82,1));
        if(white>0) {
            c.origin(camera);
            c.billboard(Layer.SMOKE,c.forwardX()*.6f,c.forwardY()*.6f,c.forwardZ()*.6f,2.4f,1.6f,0,argb(white,0xf6f9ff),0,0,.5f,.5f);
        }
    }

    // ------------------------------------------------------------------ act 1: the climb over the limb
    static final float RP=1000;
    /** Sun direction from the home world (normalised): low over the limb ahead of the climb. */
    static final float SUN_X=-.46f,SUN_Y=.42f,SUN_Z=-.78f;
    private void ascent(StrikeView v,Canvas c,ClientConfig.Quality q,double p,double t,boolean reduced) {
        double k=smoother(p);
        double h=expo(14,1400,smooth(p)*.9+p*.1);
        // Facing the sunrise over the limb (-Z), the pitch drops from just under the horizon to the planet below.
        double dip=Math.acos(RP/(RP+h));
        double pitch=-(dip+lerp(.06,.95,k));
        eye(0,h,0);
        at(0,h+Math.sin(pitch)*100,-Math.cos(pitch)*100);
        upX=0;upY=1;upZ=0;lens=(float)lerp(1.1,1,k);
        tremor(lerp(.04,.0,k)*h*.02,t);
        begin(c,v,q,lerp(0,1,smooth(range(p,.05,.9))),0,.6,.8);
        c.virtualOrigin(0,0,0);
        world(c,0,-RP,0,RP,t*.00025,SUN_X,SUN_Y,SUN_Z,q,1);
        sunInSky(c,t,reduced,1);
        // The last of the cloud deck, thinning as the camera clears it.
        double veil=1-smooth(range(p,0,.22));
        if(veil>0) {
            c.virtualOrigin(ex,ey,ez);
            c.billboard(Layer.SMOKE,c.forwardX()*24,c.forwardY()*24,c.forwardZ()*24,34,24,0,argb(veil,0xf6f9ff),0,0,.5f,.5f);
        }
    }
    /** The sun as seen from the home world's orbit: a real star with corona and flare. */
    private void sunInSky(Canvas c,double t,boolean reduced,double alpha) {
        float d=60000;
        c.virtualOrigin(0,-RP,0);
        star(c,SUN_X*d,SUN_Y*d+RP,SUN_Z*d,520,.2,t,SUN,alpha);
        flare(c,SUN_X*d,SUN_Y*d+RP,SUN_Z*d,9000,(reduced?.35:.7)*alpha,SUN_HOT);
    }

    // ------------------------------------------------------------------ act 2: the planet and its moon
    private void planetAct(StrikeView v,Canvas c,ClientConfig.Quality q,double p,double t,boolean reduced) {
        double k=smoother(p);
        // From above the dayside (where the climb ended) the camera swings around to put the sun behind the limb,
        // then keeps pulling back so the world becomes a small blue body beside its moon.
        double dist=expo(RP+1400,RP*9,smooth(p));
        // End on a half-lit world: the final direction sits about 100 degrees from the sun, so the terminator
        // crosses the disc and the moon and limb both catch the light.
        float px=-SUN_Y*SUN_X,py=1-SUN_Y*SUN_Y,pz=-SUN_Y*SUN_Z,pl=(float)Math.sqrt(px*px+py*py+pz*pz);
        float qx=-.17f*SUN_X+.98f*px/pl,qy=-.17f*SUN_Y+.98f*py/pl,qz=-.17f*SUN_Z+.98f*pz/pl;
        float[] path={0,1,0, qx*.4f,1-.4f*(1-qy),qz*.4f, qx*.8f,1-.8f*(1-qy),qz*.8f, qx,qy,qz};
        float[] dir=spline(path,k);
        double dl=Math.sqrt(dir[0]*dir[0]+dir[1]*dir[1]+dir[2]*dir[2]);
        double dx=dir[0]/dl,dy=dir[1]/dl,dz=dir[2]/dl;
        eye(dx*dist,-RP+dy*dist,dz*dist);
        // Frame the planet slightly off centre early, centred at the end for the hand-off to the solar system.
        double off=(1-k)*RP*.6;
        at(off*.3,-RP-off*.5,0);
        upX=0;upY=1;upZ=0;lens=1;
        begin(c,v,q,1,0,.7,1);
        c.virtualOrigin(0,0,0);
        world(c,0,-RP,0,RP,t*.00025,SUN_X,SUN_Y,SUN_Z,q,1);
        // The moon: a grey cratered body (hull albedo reads as regolith at this distance) lit by the same sun.
        float mx=RP*3.6f,my=-RP+RP*.55f,mz=RP*2.4f;
        light.key(SUN_X,SUN_Y,SUN_Z,0xfff3e0,1.6f).point(0,0,0,0,0,1).ambient(0x101418,.05f).rim(0,0);
        m.identity().translate(mx,my,mz).rotateY((float)(t*.0004)).scale(RP*.27f);
        c.litMesh(Layer.COSMIC_ROCK,planetSphere,m,light,Canvas.Shading.BAKED,0xa49e94,0,0,0);
        sunInSky(c,t,reduced,1);
        // A sparse ring of orbital stations, glinting, as a final reminder of a world below.
        int glints=q.level==2?18:q.level==1?12:6;
        c.virtualOrigin(0,-RP,0);
        for(int i=0;i<glints;i++) {
            double a=v.hash(6400+i)*Math.PI*2+t*.0009,rr=RP*(1.25+.06*v.hash(6500+i));
            flare(c,(float)(Math.cos(a)*rr),(float)(Math.sin(a)*rr*.18),(float)(Math.sin(a)*rr),RP*.012f,.6,COLD);
        }
    }

    // ------------------------------------------------------------------ act 3: the solar system
    static final float[] ORBITS={260,420,640,900,1400,2000,2700},SIZES={12,22,26,16,80,66,40};
    static final int[] TINTS={0xb39a7c,0xd8b98a,0x5a8fd8,0xb8603e,0xc9a678,0xd8c392,0x86b4d6};
    private void solar(StrikeView v,Canvas c,ClientConfig.Quality q,double p,double t,boolean reduced) {
        double k=smoother(p);
        double home=v.hash(31)*Math.PI*2+t*.00035;
        float hx=(float)(Math.cos(home)*ORBITS[2]),hz=(float)(Math.sin(home)*ORBITS[2]);
        // Pull back from the home world while climbing above the ecliptic and turning to centre the sun.
        double dist=expo(70,5200,smooth(p));
        double elev=lerp(.06,.62,k);
        double ax=hx/ORBITS[2],az=hz/ORBITS[2];
        double bx=lerp(hx,0,smooth(range(p,.15,.85))),bz=lerp(hz,0,smooth(range(p,.15,.85)));
        eye(bx+ax*dist*Math.cos(elev)-az*dist*.25,Math.sin(elev)*dist,bz+az*dist*Math.cos(elev)+ax*dist*.25);
        at(bx,0,bz);
        upX=0;upY=1;upZ=0;lens=1;
        begin(c,v,q,1,0,.6,1);
        c.virtualOrigin(0,0,0);
        star(c,0,0,0,60,.25,t,SUN,1);
        flare(c,0,0,0,520,reduced?.25:.45,SUN_HOT);
        light.key(0,1,0,0,0).point(-ex,-ey,-ez,SUN_HOT,2.6f,2400).ambient(0x0c1018,.06f).rim(0x8fb0ff,.08f);
        int planets=q.level==0?5:ORBITS.length;
        for(int i=0;i<planets;i++) {
            double a=i==2?home:v.hash(600+i)*Math.PI*2+t*.0006/(1+i*.6);
            float r=ORBITS[i],px=(float)(Math.cos(a)*r),pz=(float)(Math.sin(a)*r),size=SIZES[i];
            float camDist=(float)Math.sqrt((ex-px)*(ex-px)+ey*ey+(ez-pz)*(ez-pz));
            float line=Math.max(1.4f,(float)Math.sqrt(ex*ex+ey*ey+ez*ez)*.0011f);
            c.annulus(Layer.COSMIC_GLOW,0,0,0,0,1,0,r-line,r+line,Fx.segments(q,128),argb(i==2?.4:.2,COLD),argb(i==2?.4:.2,COLD),1,0,0,1);
            // A glint keeps each world findable once it is far smaller than a pixel.
            flare(c,px,0,pz,Math.max(size*3,camDist*.008f),i==2?.85:.5,i==2?0x9fd0ff:TINTS[i]);
            if(i==2) { world(c,px,0,pz,size,t*.003,-px,0,-pz,q,1); continue; }
            light.key(-px,0,-pz,SUN_HOT,1.7f).point(0,0,0,0,0,1);
            m.identity().translate(px,0,pz).rotateZ(.3f*i).rotateY((float)(t*.002)).scale(size);
            c.litMesh(Layer.COSMIC_ROCK,lowSphere,m,light,Canvas.Shading.BAKED,TINTS[i],0,(float)(.13*i),0);
            if(size>30) {
                m.identity().translate(px,0,pz).rotateZ(.3f*i).rotateY((float)(t*.0021+.5)).scale(size*1.01f);
                c.litMesh(Layer.COSMIC_CLOUD,lowSphere,m,light,Canvas.Shading.SCATTER,TINTS[i],.85,0,(float)(.2*i));
                c.annulus(Layer.COSMIC_GAS,px,0,pz,(float)Math.sin(.3*i),(float)Math.cos(.3*i),0,size*1.5f,size*2.4f,Fx.segments(q,48),
                        argb(.4,TINTS[i]),argb(.12,TINTS[i]),2,0,.2f,.8f);
            }
        }
        // Asteroid belt.
        int rocks=q.level==2?260:q.level==1?160:70;
        for(int i=0;i<rocks;i++) {
            double a=v.hash(6600+i)*Math.PI*2+t*.0004,r=1040+v.hash(6700+i)*240;
            c.billboard(Layer.COSMIC_STAR,(float)(Math.cos(a)*r),(float)((v.hash(6800+i)-.5)*30),(float)(Math.sin(a)*r),2.2f+(float)v.hash(6900+i)*2,0,argb(.55,0xd8cbb8));
        }
        // Heliopause: a faint bubble edge hinting at where the system ends.
        if(q.level>=1)c.annulus(Layer.COSMIC_SHELL,0,0,0,0,1,0,3300,3700,Fx.segments(q,96),argb(0,COLD),argb(.08*k,COLD),1,0,0,1);
    }

    // ------------------------------------------------------------------ act 4: the Milky Way
    static final float RG=40000;
    private void galaxy(StrikeView v,Canvas c,ClientConfig.Quality q,double p,double t,boolean reduced) {
        double k=smoother(p);
        double spin=t*.00012+.3;
        // Our sun sits on a spiral arm in the procedural disc's own arm phase.
        double th=Math.log(.59)*2.4+spin,rs=.55*RG;
        float sx=(float)(Math.cos(th)*rs),sz=(float)(Math.sin(th)*rs);
        double dist=expo(300,RG*2.0,smooth(p)*.85+p*.15);
        double elev=lerp(.3,1.0,k);
        double cx=lerp(sx,0,smooth(range(p,.1,.8))),cz=lerp(sz,0,smooth(range(p,.1,.8)));
        double dirx=sx/rs,dirz=sz/rs;
        eye(cx+dirx*dist*Math.cos(elev)*.7+dirz*dist*.5,Math.sin(elev)*dist,cz+dirz*dist*Math.cos(elev)*.7-dirx*dist*.5);
        at(cx,0,cz);
        upX=0;upY=1;upZ=0;lens=1;
        // Leaving the disc: the band of the Milky Way fades from the sky because we are now outside it.
        begin(c,v,q,lerp(1,1.85,smooth(range(p,.25,.9))),0,.5,1-smooth(range(p,.15,.6)));
        c.virtualOrigin(0,0,0);
        // The disc, rotated with the galaxy, plus a slightly larger, fainter outer disc for soft edges.
        float ca=(float)Math.cos(spin),sa=(float)Math.sin(spin);
        // Inside the disc the core would burn out the frame, so the disc brightens as the camera clears it.
        double clear=range(Math.sqrt(ex*ex+ey*ey+ez*ez),RG*.2,RG*1.2);
        c.fxSheet(Layer.COSMIC_FX,Canvas.FX_GALAXY,.5,0,0,0,ca*RG,0,sa*RG,-sa*RG,0,ca*RG,argb(.45+.5*clear,0xb8c8ff));
        // Volumetric bulge and halo.
        // The bulge glow only reads from outside the disc; inside it would wash the frame out.
        double outside=range(Math.sqrt(ex*ex+ey*ey+ez*ez),RG*.35,RG*1.3);
        corona(c,0,0,0,RG*.3f,.2,.5*outside,0xffd9a0,(float)(t*.0005));
        int clusters=q.level==2?40:q.level==1?26:12;
        for(int i=0;i<clusters;i++) {
            double a=v.hash(7000+i)*Math.PI*2,e=(v.hash(7100+i)-.5)*2.2,r=RG*(.25+.6*v.hash(7200+i));
            flare(c,(float)(Math.cos(a)*r*Math.cos(e)),(float)(Math.sin(e)*r*.6),(float)(Math.sin(a)*r*Math.cos(e)),RG*.012f,.5,0xfff0d0);
        }
        // Arm stars with real depth: near the start they stream past the lens, then settle into the arms.
        int armStars=q.level==2?420:q.level==1?260:120;
        for(int i=0;i<armStars;i++) {
            double h=v.hash(7300+i),s=.08+.9*v.hash(7400+i),arm=(i%2)*Math.PI;
            double ang=Math.log(s+.04)*2.4+arm+spin+(h-.5)*.35;
            float r=(float)(s*RG),y=(float)((v.hash(7500+i)-.5)*RG*.03*(1-s*.6));
            float x=(float)(Math.cos(ang)*r),z=(float)(Math.sin(ang)*r);
            float size=(float)(RG*(.0018+.004*v.hash(7600+i)*v.hash(7600+i)));
            c.billboard(Layer.COSMIC_STAR,x,y,z,size,0,argb(.55+.4*v.hash(7700+i),s<.3?0xffe2b8:0xd0dcff));
        }
        // Here: the sun, still bright at the start, then just another mote in an arm.
        flare(c,sx,0,sz,(float)lerp(60,RG*.02,k),lerp(1,.4,k),SUN_HOT);
        // Satellite galaxies give depth to the far field.
        for(int i=0;i<3;i++) {
            double a=v.hash(7800+i)*Math.PI*2;
            float gx=(float)(Math.cos(a)*RG*2.2),gz=(float)(Math.sin(a)*RG*2.2),gy=(float)((v.hash(7900+i)-.4)*RG);
            float r=RG*(.12f+.08f*(float)v.hash(7950+i));
            c.fxSheet(Layer.COSMIC_FX,Canvas.FX_GALAXY,.3,gx,gy,gz,r,r*.3f,0,0,r*.2f,r,argb(.55,0xd8d0ff));
        }
        // Speed flow near the start of the pull-back.
        double speed=1-smooth(range(p,0,.45));
        if(speed>0)flow(c,v,q.level==2?110:70,600,(float)(ex-cx)*.004f*(float)speed,(float)ey*.004f*(float)speed,(float)(ez-cz)*.004f*(float)speed,.8*speed,8000);
    }

    // ------------------------------------------------------------------ act 5: the cosmic web and the boundary
    static final float WEB=90000,WALL=260000;
    private void universe(StrikeView v,Canvas c,ClientConfig.Quality q,double p,double t,boolean reduced) {
        // Retreat backward from the Milky Way into the web, turn around to face away from home, then accelerate into
        // the observable boundary and through it.
        double retreat=smooth(range(p,0,.5)),turn=smoother(range(p,.42,.66)),charge=Math.pow(range(p,.62,1),2.2);
        double z=lerp(9000,WEB*1.2,retreat)+charge*(WALL-WEB*1.2+4000);
        eye(Math.sin(p*2.1)*4000,1800+retreat*6000,z);
        double yaw=Math.PI*turn;
        at(ex+Math.sin(yaw)*1000,ey-(1-turn)*120,ez-Math.cos(yaw)*1000);
        upX=(float)(Math.sin(turn*Math.PI)*.18);upY=1;upZ=0;lens=1;
        double through=smooth(range(p,.9,.97)),beyond=smooth(range(p,.93,1));
        double flash=reduced?through*.25:Math.sin(through*Math.PI)*.85;
        begin(c,v,q,Math.min(4,lerp(2,3,smooth(range(p,0,.4)))+beyond),flash,.6,0);
        c.virtualOrigin(0,0,0);
        // Home: the Milky Way at the origin, shrinking as we retreat.
        double homeFade=1-turn;
        if(homeFade>0)c.fxSheet(Layer.COSMIC_FX,Canvas.FX_GALAXY,.5,0,0,0,RG,0,0,0,RG*.8f,RG*.35f,argb(.95*homeFade,0xb8c8ff));
        // The web's galaxies: clustered along filaments between nodes, each a tiny procedural disc.
        int nodes=10;
        int perLink=q.level==2?14:q.level==1?9:5;
        for(int i=0;i<nodes;i++) {
            float ax=(float)((v.hash(8100+i)-.5)*WEB*1.6),ay=(float)((v.hash(8200+i)-.5)*WEB*.8),az=(float)(v.hash(8300+i)*WEB*1.9);
            int j=(i*7+3)%nodes;
            float bx=(float)((v.hash(8100+j)-.5)*WEB*1.6),by=(float)((v.hash(8200+j)-.5)*WEB*.8),bz=(float)(v.hash(8300+j)*WEB*1.9);
            corona(c,ax,ay,az,WEB*.05f,.3,.35,0xffd2a8,0);
            for(int s=0;s<perLink;s++) {
                double f=(s+v.hash(8400+i*31+s))/perLink;
                float gx=(float)lerp(ax,bx,f)+(float)((v.hash(8500+i*31+s)-.5)*WEB*.04);
                float gy=(float)lerp(ay,by,f)+(float)((v.hash(8600+i*31+s)-.5)*WEB*.04);
                float gz=(float)lerp(az,bz,f)+(float)((v.hash(8700+i*31+s)-.5)*WEB*.04);
                float r=WEB*(.012f+.016f*(float)v.hash(8800+i*31+s));
                double o=v.hash(8900+i*31+s)*Math.PI;
                c.fxSheet(Layer.COSMIC_FX,Canvas.FX_GALAXY,.4,gx,gy,gz,(float)Math.cos(o)*r,(float)Math.sin(o)*r*.5f,0,0,r*.4f,(float)Math.cos(o+1)*r,
                        argb(.6,s%3==0?0xffe0c0:0xc8d4ff));
            }
        }
        // The boundary: a vast luminous, redshifted wall the camera dives through.
        double wall=smooth(range(p,.55,.9))*(1-beyond);
        if(wall>0) {
            int sheets=q.level==2?6:q.level==1?4:2;
            for(int s=0;s<sheets;s++) {
                float wz=WALL+s*2600,w=WEB*(2.4f-.2f*s);
                c.fxSheet(Layer.COSMIC_FX,Canvas.FX_NEBULA,.2+.13*s,0,ey,wz,w,0,0,0,w*.7f,0,argb(.42*wall,s%2==0?RED:0xffb070));
            }
        }
        // Speed: once the camera turns and dives, the field streams by.
        double speed=charge;
        if(speed>0)flow(c,v,q.level==2?140:80,WEB*.12f,0,0,(float)(-WEB*.05*speed),.9*speed,8300);
    }

    // ------------------------------------------------------------------ acts 6-9: the stellar mothership
    static final float TARGET_Z=-62000,TARGET_R=1500;
    private void mothership(StrikeView v,Canvas c,ClientConfig.Quality q,double p,double t,boolean reduced) {
        StellarMothership.pose(v,pose);
        double fireBurst=0;
        switch(v.phase()) {
            case MOTHERSHIP_REVEAL -> {
                // Out of the boundary flash: the ship hangs against the foundry nebula, a dark bud around a star.
                float[] path={2500,640,4300, 2050,500,3250, 1650,370,2450};
                float[] e=spline(path,smooth(p));eye(e[0],e[1],e[2]);
                at(0,0,lerp(400,200,p));lens=(float)lerp(.95,1.15,smooth(p));
            }
            case DEPLOYMENT -> {
                // Orbit from the star side round to the flank, passing close over a blooming petal and a collector ring.
                float[] path={1700,380,2500, 1250,760,1500, 950,440,520, 1150,240,-520, 1600,320,-1250};
                float[] e=spline(path,p);eye(e[0],e[1],e[2]);
                float[] look={0,0,300, 0,200,700, 0,0,200, 0,0,-200, 0,0,-300};
                float[] l=spline(look,p);at(l[0],l[1],l[2]);lens=1;
            }
            case CHARGING -> {
                // In close to the crucible as the star is wrung out.
                float[] path={1500,520,1500, 900,300,1250, 640,180,1020};
                float[] e=spline(path,smooth(p));eye(e[0],e[1],e[2]);
                at(0,0,StellarMothership.STAR_Z);lens=(float)lerp(1,1.15,smooth(p));
                tremor(pose.instability*6,t);
            }
            case ORBITAL_ALIGNMENT -> {
                // Along the barrel to the bow; the iris opens, everything locks, then the held breath.
                float[] path={900,300,600, 760,260,-400, 640,240,-1500};
                float[] e=spline(path,smooth(range(p,0,.7)));eye(e[0],e[1],e[2]);
                at(0,0,lerp(-200,StellarMothership.APERTURE_Z,smooth(range(p,0,.7))));lens=1;
                if(pose.lock>0&&pose.lock<1)tremor(14,t*3);
            }
            default -> {
                // FIRING: hold, discharge, swing behind the lance, then ride it back to the world.
                double ride=smooth(range(p,.42,.96));
                if(p<.2) {
                    eye(640,240,-1500);at(0,0,StellarMothership.APERTURE_Z);
                    tremor(range(p,.1,.2)*28,t*3);
                } else if(p<.42) {
                    double s=smoother(range(p,.2,.42));
                    eye(lerp(640,1500,s),lerp(240,1100,s),lerp(-1500,-2600,s));
                    at(0,0,lerp(StellarMothership.APERTURE_Z,-9000,s));
                    tremor(10*(1-s),t*3);
                } else {
                    double f=Math.pow(ride,1.5);
                    eye(lerp(1500,TARGET_R*.9,f),lerp(1100,TARGET_R*.55,f),lerp(-2600,TARGET_Z+TARGET_R*2.6,f));
                    at(0,0,lerp(-9000,TARGET_Z,smooth(ride)));
                    tremor(6,t*2);
                }
                lens=1;
                fireBurst=range(p,.12,.13)*(1-range(p,.13,.2));
            }
        }
        upX=0;upY=1;upZ=0;
        double flash=(reduced?.15:.7)*fireBurst;
        boolean firing=v.in(Phase.FIRING);
        double entry=firing?smooth(range(p,.955,1)):0;
        begin(c,v,q,4,flash+(reduced?entry*.25:entry*.9),.8+.4*pose.charge,0);
        light.key(-.4f,.5f,.6f,0x8fb4d0,.9f).ambient(0x1c2030,.26f).rim(0xe09a60,1.0f);
        ship.render(c,pose,light,q,t,v,0,0,0);
        // The world, far ahead on the firing axis: a mote that grows into a planet as the camera rides the lance.
        c.virtualOrigin(0,0,0);
        world(c,0,0,TARGET_Z,TARGET_R,t*.0004,.3f,.6f,.7f,q,1);
        // The stellar lance.
        if(pose.fire>.12)lance(c,v,q,t,pose,reduced);
    }

    /** The colossal stellar lance from the aperture to the world, with turbulence, helices, streaks and contact. */
    private void lance(Canvas c,StrikeView v,ClientConfig.Quality q,double t,StellarMothership.Pose pose,boolean reduced) {
        double grow=smooth(range(pose.fire,.12,.3));
        float start=StellarMothership.APERTURE_Z-10,end=(float)lerp(start,TARGET_Z+TARGET_R*.98,smooth(range(pose.fire,.12,.42)));
        float w=(float)(40+50*grow);
        int seg=Fx.segments(q,24);
        c.virtualOrigin(0,0,0);
        float v0=Canvas.fxV(Canvas.FX_BEAM),len=(end-start)/-400f;
        c.tube(Layer.COSMIC_FX,0,0,start,0,0,end,w*.42f,w*.42f,seg,argb(1,0xfff4dc),argb(.9,0xffe0b0),1,Canvas.fxU(0),v0,v0+Math.min(60,len));
        c.tube(Layer.COSMIC_FX,0,0,start,0,0,end,w,w*1.1f,seg,argb(.8,0xffa040),argb(.6,0xff8030),1,Canvas.fxU(.6),v0,v0+Math.min(60,len*.6f));
        c.tube(Layer.COSMIC_FX,0,0,start,0,0,end,w*2.1f,w*2.4f,seg,argb(.35,0xff6a20),argb(.2,0xff5020),1,Canvas.fxU(1),v0,v0+Math.min(60,len*.4f));
        // Helical containment arcs and ejected streaks along the column.
        int streaks=Fx.scaled(q,60);
        for(int i=0;i<streaks;i++) {
            double h=v.hash(9100+i),f=Fx.frac(t*(.006+.006*v.hash(9200+i))+h);
            float z=(float)lerp(start,end,f);
            double a=h*Math.PI*13+t*.05;float rr=(float)(w*(1.3+2.8*v.hash(9300+i)));
            c.streak(Layer.COSMIC_GLOW,(float)Math.cos(a)*rr,(float)Math.sin(a)*rr,z,(float)Math.cos(a)*rr*1.1f,(float)Math.sin(a)*rr*1.1f,z+240,
                    w*.06f,w*.14f,argb(.7*(1-f*.5),0xffe6b8),argb(0,0xffa040),0,0,1,1);
        }
        corona(c,0,0,start,w*6,.8,.8,0xffb060,(float)(t*.01));
        flare(c,0,0,start,w*14,reduced?.4:.9,0xfff2d8);
        // Contact with the world's upper atmosphere.
        if(end<=TARGET_Z+TARGET_R*1.05f) {
            double hit=smooth(range(pose.fire,.42,.6));
            corona(c,0,0,TARGET_Z+TARGET_R,TARGET_R*(.25f+.4f*(float)hit),.9,.7*hit,0xffc890,(float)(t*.01));
            flare(c,0,0,TARGET_Z+TARGET_R,TARGET_R*1.4f,(reduced?.25:.6)*hit,0xfff4e0);
            c.annulus(Layer.COSMIC_FX,0,0,TARGET_Z+TARGET_R*.98f,0,0,1,TARGET_R*.05f,TARGET_R*(.2f+.7f*(float)hit),Fx.segments(q,48),
                    argb(.7*hit,0xffd0a0),argb(.7*hit,0xffd0a0),1,Canvas.fxU(.3),Canvas.fxV(Canvas.FX_SHOCK),Canvas.fxV(Canvas.FX_SHOCK)+1);
        }
    }

    /** The rise lifts the camera only when the owner actually watches the detached act. */
    static boolean customAvailable(){return NullstarMaterials.verified();}
}
