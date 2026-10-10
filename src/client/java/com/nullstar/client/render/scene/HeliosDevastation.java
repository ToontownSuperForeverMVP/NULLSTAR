package com.nullstar.client.render.scene;

import com.nullstar.client.ClientConfig;
import com.nullstar.client.NullstarClient;
import com.nullstar.client.render.Canvas;
import com.nullstar.client.render.Layer;
import com.nullstar.client.render.Mesh;
import com.nullstar.client.render.PresentationScale;
import com.nullstar.client.render.StrikeView;
import com.nullstar.core.Phase;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import static com.nullstar.client.render.Canvas.argb;
import static com.nullstar.client.render.StrikeView.range;
import static com.nullstar.client.render.StrikeView.smooth;
import static com.nullstar.client.render.StrikeView.smoother;

/**
 * Part 3 world-scale devastation for TITANIC/APOCALYPSE Helios: the stellar lance arriving from space, contact,
 * the fireball, the regional shock and pressure fronts, the two annular devastation passes, the ash column and its
 * spreading cap, and the clearing aftermath over the real crater.
 *
 * <p>Timing is phase-relative ({@link #devastation}), so the choreography stretches with the operator's profile and a
 * late observer reconstructs exactly the same moment. All magnitudes derive from the authoritative radius, ring
 * scales and beam descriptor. STANDARD strikes never reach this class.
 *
 * <p>Honesty about terrain: the molten crater floor is drawn at the true basin depth with a normal depth test, so it
 * only shows where the server has actually excavated; still-standing terrain hides it. Ash and dust use the fog-free
 * {@link Layer#ASH} material so plumes far beyond render distance read as dark silhouettes on the horizon instead of
 * washing the frame to the sky colour.
 */
final class HeliosDevastation {
    static final int HOT=0xfff6e6,WHITE_GOLD=0xfff0c8,GOLD=0xffa62a,EMBER=0xff4a12,ASH_DARK=0x2e2824,ASH=0x5a524c,DUST=0x8a7a68,CLOUD=0xe8eef4;
    private final Matrix4f m=new Matrix4f();

    /** Normalized devastation time: IMPACT 0-.1, wave 1 .1-.3, wave 2 .3-.5, aftermath .5-1; -1 before impact. */
    static double devastation(StrikeView v) {
        double p=v.progress();
        return switch(v.phase()) {
            case IMPACT -> .1*p;
            case DEVASTATION_WAVE_1 -> .1+.2*p;
            case DEVASTATION_WAVE_2 -> .3+.2*p;
            case AFTERMATH -> .5+.5*p;
            case COOLDOWN -> 1;
            // COMPLETE can linger while deferred terrain regions wait to load; the presentation is over by then.
            default -> -1;
        };
    }

    // ------------------------------------------------------------------ the lance in the world
    /**
     * The lance enters from space straight above the target, is caught and refocused by the collector, and drives
     * down to the descending head. The relay segment above the collector keeps spatial continuity with the cosmic
     * act, where the camera rode the lance down onto the planet.
     */
    void lance(StrikeView v,Canvas c,ClientConfig.Quality q,double t,float top,float head,float width,double intensity) {
        if(intensity<=0)return;
        int seg=Fx.segments(q,22);
        float v0=Canvas.fxV(Canvas.FX_BEAM);
        float sky=top+Math.max(600,top*1.6f);
        // Incoming relay beam from orbit down to the collector crown.
        c.tube(Layer.WORLD_FX,0,sky,0,0,top+6,0,width*.7f,width*.55f,seg,argb(.75*intensity,0xffe2b0),argb(.9*intensity,0xfff0d0),
                1,Canvas.fxU(.2),v0,v0+12);
        if(top-head<1)return;
        float span=(top-head)/40f;
        c.tube(Layer.WORLD_FX,0,top,0,0,head,0,width*.42f,width*.42f,seg,argb(intensity,0xfff6e2),argb(intensity,0xffe8c0),1,Canvas.fxU(0),v0,v0+Math.min(60,span));
        c.tube(Layer.WORLD_FX,0,top,0,0,head,0,width,width*1.1f,seg,argb(.75*intensity,0xffa040),argb(.6*intensity,0xff8a30),1,Canvas.fxU(.6),v0,v0+Math.min(60,span*.6f));
        c.tube(Layer.WORLD_FX,0,top,0,0,head,0,width*2.2f,width*2.6f,seg,argb(.32*intensity,0xff6a20),argb(.2*intensity,0xff5020),1,Canvas.fxU(1),v0,v0+Math.min(60,span*.4f));
        // Ejected streaks racing down the column.
        int streaks=Fx.scaled(q,54);
        for(int i=0;i<streaks;i++) {
            double h=v.hash(9500+i),f=Fx.frac(t*(.02+.02*v.hash(9600+i))+h);
            float y=top+(head-top)*(float)f;
            double a=h*Math.PI*13+t*.05;float rr=(float)(width*(1.4+2.6*v.hash(9700+i)));
            c.streak(Layer.GLOW,(float)Math.cos(a)*rr,y,(float)Math.sin(a)*rr,(float)Math.cos(a)*rr*1.06f,y-width*1.6f,(float)Math.sin(a)*rr*1.06f,
                    width*.05f,width*.12f,argb(.75*(1-f*.4)*intensity,0xffe6b8),argb(0,GOLD),0,0,1,1);
        }
        // The descending head: a compressed plasma sheath and a bow flare.
        c.fxSprite(Layer.WORLD_FX,Canvas.FX_CORONA,.9,0,head,0,width*5,width*5,(float)(t*.01),argb(.7*intensity,0xffc070));
        c.fxSprite(Layer.WORLD_FX,Canvas.FX_FLARE,0,0,head,0,width*5,width*5,0,argb((Fx.reducedFlashing()?.25:.55)*intensity,0xfff2d8));
    }

    /** The cloud deck tearing open around the lance as it passes, at a real overworld cloud altitude. */
    void cloudPunch(StrikeView v,Canvas c,ClientConfig.Quality q,double t,float head,float width) {
        float deck=(float)Math.max(60,190-v.target().y);
        if(head>deck+40)return;
        double open=smooth(range(deck-head,-40,deck*.6));
        int puffs=q.level==2?28:q.level==1?18:10;
        for(int i=0;i<puffs;i++) {
            double a=i*Math.PI*2/puffs+v.hash(9800+i)*.3;
            float r=(float)(width*3+open*(width*10+90)*(.8+.4*v.hash(9900+i)));
            c.ash((float)Math.cos(a)*r,deck+(float)((v.hash(9950+i)-.5)*12),(float)Math.sin(a)*r,34,16,(float)(t*.002+i),argb(.55*(1-open*.4),CLOUD),i&3);
        }
    }

    // ------------------------------------------------------------------ the impact and its aftermath
    void impact(StrikeView v,Canvas c,ClientConfig.Quality q,double t) {
        double d=devastation(v);
        if(d<0)return;
        float radius=v.radius(),depth=v.depth();
        float outer=Math.max(radius,PresentationScale.secondaryReach(v));
        boolean reduced=Fx.reducedFlashing();
        Vec3 eye=c.cameraLocal();
        // 1. Contact: a hard, short flash and the lance compressing into the ground.
        if(d<.04) {
            double k=1-d/.04;
            c.fxSprite(Layer.WORLD_FX,Canvas.FX_FLARE,0,0,4,0,radius*.55f,radius*.55f,0,argb((reduced?.3:.95)*k,HOT));
            c.fxSprite(Layer.WORLD_FX,Canvas.FX_CORONA,1,0,4,0,radius*.35f*(float)(1.2-k*.4),radius*.35f*(float)(1.2-k*.4),(float)(t*.02),argb(.8*k,WHITE_GOLD));
        }
        // 2. Fireball: a churning photosphere swelling out of the ground, rising and cooling into the ash cap.
        double fb=smooth(range(d,0,.1));
        // Sized so a camera inside the loaded area (never more than ~240 blocks out) frames it instead of being
        // swallowed by it: about 225 blocks across at its largest, rising toward the cap.
        float rf=(float)(radius*(.035+.13*Fx.easeOut(range(d,0,.12))+.06*smooth(range(d,.1,.3))));
        float fy=(float)(rf*.4+radius*.45*smoother(range(d,.12,.5)));
        double bright=fb*(1-smooth(range(d,.22,.62)));
        // The body: an alpha-blended shell of churning fire and soot gives the fireball mass in full daylight; the
        // additive photosphere over it only supplies the incandescence, so it never collapses into a white disc.
        double body=fb*(1-smooth(range(d,.48,.7)));
        if(body>.01) {
            int shell=q.level==2?34:q.level==1?22:12;
            double soot=range(d,.06,.4);
            for(int i=0;i<shell;i++) {
                double fy0=1-2*(i+.5)/shell,ring=Math.sqrt(1-fy0*fy0),az=i*2.39996+t*.0015*(i%2==0?1:-1);
                float bx=(float)(Math.cos(az)*ring*rf*.78),by=(float)(fy0*rf*.7),bz=(float)(Math.sin(az)*ring*rf*.78);
                double heat=Math.max(0,1-soot*1.3-(fy0>0?fy0*.35:0));
                int tint=Canvas.lerpRgb(0x2e1c14,Canvas.lerpRgb(0xd8641e,0xffc070,(float)heat),(float)Math.min(1,heat*1.4+.15));
                c.ash(bx,fy+by,bz,rf*.5f,rf*.5f,(float)(t*.003+i),argb(.82*body,tint),i&3);
            }
        }
        if(bright>.01) {
            double inside=range(Math.sqrt(eye.x*eye.x+(eye.y-fy)*(eye.y-fy)+eye.z*eye.z),rf*.95,rf*1.4);
            int col=Canvas.lerpRgb(Canvas.lerpRgb(HOT,GOLD,range(d,.02,.1)),EMBER,range(d,.12,.4));
            m.identity().translate(0,fy,0).rotateY((float)(t*.004)).scale(rf*.92f,rf*.85f,rf*.92f);
            c.mesh(Layer.WORLD_FX,Mesh.UNIT_SPHERE,m,argb(Math.min(1,bright*.5),col),Canvas.fxU(.95),Canvas.fxV(Canvas.FX_STAR));
            c.fxSprite(Layer.WORLD_FX,Canvas.FX_CORONA,1,0,fy,0,rf*2.1f,rf*2.1f,(float)(t*.003),argb(.4*bright*inside,col));
        }
        // 3. Primary shock: a razor ground front racing to the outermost devastation ring, with a condensation dome.
        float r1=(float)(outer*Fx.easeOut(range(d,0,.24)));
        double a1=1-range(d,.16,.3);
        if(a1>0&&r1>2) {
            c.annulus(Layer.WORLD_FX,0,1.5f,0,0,1,0,r1*.8f,r1,Fx.segments(q,96),argb(.85*a1,0xffd8a0),argb(.85*a1,0xffd8a0),1,Canvas.fxU(.1),
                    Canvas.fxV(Canvas.FX_SHOCK),Canvas.fxV(Canvas.FX_SHOCK)+1);
            m.identity().translate(0,0,0).scale(r1*.92f,r1*.5f,r1*.92f);
            c.mesh(Layer.RIM_FLOW,Mesh.UNIT_SPHERE,m,argb(.1*a1*(1-range(d,.02,.2)),0xfff0e0),0,(float)(t*.01));
        }
        // 4. The ground-hugging pressure wall: a dark rolling front of pulverised terrain trailing the shock.
        double aw=smooth(range(d,.02,.06))*(1-range(d,.26,.4));
        if(aw>0) {
            float rw=r1*.9f;
            int n=q.level==2?64:q.level==1?44:24;
            float h=(float)(radius*(.06+.08*(1-range(d,.1,.35))));
            float w=(float)(Math.PI*2*rw/n*.75);
            for(int i=0;i<n;i++) {
                double a=i*Math.PI*2/n+v.hash(10000+i)*.05;
                float x=(float)Math.cos(a)*rw,z=(float)Math.sin(a)*rw;
                c.ash(x,h*.7f,z,Math.max(w,h*.8f),h,(float)(v.hash(10100+i)*6),argb(.85*aw,Canvas.lerpRgb(ASH_DARK,DUST,(float)v.hash(10200+i)*.6f)),i&3);
                c.ash(x,h*1.45f,z,Math.max(w,h*.8f)*.8f,h*.6f,(float)(v.hash(10300+i)*6),argb(.55*aw,Canvas.lerpRgb(DUST,0xc89a70,.4f)),(i+1)&3);
            }
        }
        // 5. Devastation passes: rings of eruptions walking out to the authoritative annular radii.
        // The outer pass ends at the authoritative secondary reach; the inner pass sits halfway out to it.
        eruptions(v,c,q,t,d,.1,.42,radius*.55f,radius*.95f,q.level==2?18:q.level==1?12:6,11000);
        float ringB=outer,ringA=radius+(outer-radius)*.5f;
        eruptions(v,c,q,t,d,.28,.56,radius,ringA,q.level==2?20:q.level==1?13:7,11400);
        eruptions(v,c,q,t,d,.34,.62,ringA,ringB,q.level==2?22:q.level==1?14:7,11800);
        for(int i=0;i<2;i++) {
            double start=i==0?.3:.36,pd=range(d,start,start+.12);
            if(pd<=0||pd>=1)continue;
            float r=(float)(radius+((i==0?ringA:ringB)-radius)*Fx.easeOut(pd));
            c.annulus(Layer.WORLD_FX,0,1.2f,0,0,1,0,r*.9f,r,Fx.segments(q,96),argb(.6*(1-pd),0xffb070),argb(.6*(1-pd),0xffb070),1,Canvas.fxU(.5+.2*i),
                    Canvas.fxV(Canvas.FX_SHOCK),Canvas.fxV(Canvas.FX_SHOCK)+1);
        }
        // 5b. A low, dark dust horizon where the outer passes ground out, so the eruptions stand on something even
        //     where the real terrain is beyond render distance.
        double band=smooth(range(d,.3,.42))*(1-.55*smooth(range(d,.6,1)));
        if(band>0) {
            int n=q.level==2?56:q.level==1?36:20;
            float rr=ringB*.94f,h=radius*.05f,w=(float)(Math.PI*2*rr/n*.7);
            for(int i=0;i<n;i++) {
                double a=i*Math.PI*2/n+v.hash(12800+i)*.04;
                c.ash((float)Math.cos(a)*rr,h*.6f,(float)Math.sin(a)*rr,Math.max(w,h*1.4f),h,(float)(v.hash(12900+i)*6),argb(.6*band,Canvas.lerpRgb(ASH_DARK,DUST,.3f)),i&3);
            }
        }
        // 6. The ash column and its spreading cap, underlit by the cooling fireball, clearing at the very end.
        double col=smooth(range(d,.16,.34));
        if(col>0) {
            double clear=1-.75*smooth(range(d,.78,1));
            float capY=(float)(radius*(.3+.55*smooth(range(d,.2,.62))));
            float capR=(float)(radius*(.18+.62*smooth(range(d,.3,.95))));
            int slices=q.level==2?10:q.level==1?7:4;
            for(int i=0;i<slices;i++) {
                double f=(i+.5)/slices;
                float y=(float)(capY*f*col);
                float w=(float)(radius*(.11+.05*f+.03*Math.sin(t*.01+i)));
                int tint=Canvas.lerpRgb(Canvas.lerpRgb(0x8a4a20,ASH,(float)range(d,.3,.7)),ASH_DARK,(float)f*.6f);
                c.ash((float)(Math.sin(t*.006+i)*w*.08),y,(float)(Math.cos(t*.005+i)*w*.08),w,w*1.15f,(float)(t*.002*(i%2==0?1:-1)+i),argb(.82*col*clear,tint),i&3);
            }
            int ring=q.level==2?22:q.level==1?15:8;
            for(int i=0;i<ring;i++) {
                double a=i*Math.PI*2/ring+t*.0006;
                float rr=(float)(capR*(.55+.45*v.hash(12000+i)));
                float w=(float)(capR*.34+radius*.06);
                int tint=Canvas.lerpRgb(ASH,ASH_DARK,(float)v.hash(12100+i));
                c.ash((float)Math.cos(a)*rr,capY+(float)((v.hash(12200+i)-.5)*radius*.06),(float)Math.sin(a)*rr,w,w*.42f,(float)(v.hash(12300+i)*6),argb(.8*col*clear,tint),i&3);
            }
            int crown=q.level==2?7:q.level==1?5:3;
            for(int i=0;i<crown;i++) {
                double a=i*Math.PI*2/crown+t*.0004;
                float rr=capR*.28f,w=capR*.3f;
                c.ash((float)Math.cos(a)*rr,capY+radius*.05f,(float)Math.sin(a)*rr,w,w*.5f,(float)(i*1.7+t*.001),argb(.7*col*clear,ASH_DARK),(i+2)&3);
            }
            // Underlight from the dying fireball and lightning crawling through the cap.
            double glowBase=(1-range(d,.45,.8));
            if(glowBase>0)c.fxSprite(Layer.WORLD_FX,Canvas.FX_CORONA,.6,0,capY*.25f,0,radius*.4f,radius*.4f,(float)(t*.002),argb(.45*glowBase*col,EMBER));
            if(d>.3&&d<.92&&q.level>=1) {
                int bolts=reduced?1:3;
                for(int i=0;i<bolts;i++) {
                    double cyc=Fx.frac(t*.031+i*.37);
                    if(cyc>.12)continue;
                    double a=v.hash((int)(t*.031+i*.37)*13+i)*Math.PI*2;
                    float rr=capR*.6f;
                    c.arc(Layer.GLOW,(float)Math.cos(a)*rr*.2f,capY*.85f,(float)Math.sin(a)*rr*.2f,(float)Math.cos(a)*rr,capY*1.02f,(float)Math.sin(a)*rr,
                            radius*.03f,8,radius*.006f,argb((reduced?.35:.9)*(1-cyc/.12),0xe8dcff),(long)(t*.031+i*.37)*31+i);
                }
            }
        }
        // 7. The basin floor: molten light at the true crater depth, visible only where terrain is really gone.
        double molten=smooth(range(d,.06,.2))*(1-.7*smooth(range(d,.6,1)));
        if(molten>0) {
            float y=-depth*.92f;
            c.quad(Layer.GLOW,-radius,y,-radius,radius,y,-radius,radius,y,radius,-radius,y,radius,0,0,1,1,
                    argb(.7*molten,0xff7a28),argb(.7*molten,0xff7a28),argb(.7*molten,0xff7a28),argb(.7*molten,0xff7a28),0,1,0);
            c.quad(Layer.GLOW,-radius*.5f,y+.5f,-radius*.5f,radius*.5f,y+.5f,-radius*.5f,radius*.5f,y+.5f,radius*.5f,-radius*.5f,y+.5f,radius*.5f,0,0,1,1,
                    argb(.6*molten,0xffc070),argb(.6*molten,0xffc070),argb(.6*molten,0xffc070),argb(.6*molten,0xffc070),0,1,0);
        }
        // 8. Residual stellar energy guttering over ground zero through the aftermath.
        double residue=smooth(range(d,.5,.6))*(1-range(d,.85,1));
        if(residue>0) {
            c.fxSprite(Layer.WORLD_FX,Canvas.FX_CORONA,.7,0,radius*.05f,0,radius*.22f,radius*.22f,(float)(t*.004),argb(.35*residue,GOLD));
            int sparks=q.level==2?6:q.level==1?4:2;
            for(int i=0;i<sparks;i++) {
                double cyc=Fx.frac(t*.02+i*.21);
                double a=v.hash(12500+i)*Math.PI*2+t*.003;
                float rr=radius*(.1f+.4f*(float)v.hash(12600+i));
                c.arc(Layer.GLOW,0,1,0,(float)Math.cos(a)*rr,1+radius*.02f,(float)Math.sin(a)*rr,radius*.02f,7,radius*.004f,
                        argb(.6*residue*(1-cyc),0xffd08a),(long)(t*.02+i*.21)*17+i);
            }
        }
    }

    /** One devastation pass: plumes of ejecta bursting up along an annulus, each with a fiery root, rising and dispersing. */
    private void eruptions(StrikeView v,Canvas c,ClientConfig.Quality q,double t,double d,double start,double end,float r0,float r1,int count,int salt) {
        if(d<start)return;
        for(int i=0;i<count;i++) {
            double h=v.hash(salt+i);
            double s=start+(end-start)*.45*h;
            double life=range(d,s,s+(end-start)*.7);
            if(life<=0)continue;
            double a=i*Math.PI*2/count+h*.4;
            float rr=r0+(r1-r0)*(float)v.hash(salt+100+i);
            float x=(float)Math.cos(a)*rr,z=(float)Math.sin(a)*rr;
            float hgt=(float)(r1*(.08+.16*v.hash(salt+200+i))*Fx.easeOut(life*1.4));
            double fade=1-range(life,.55,1)*.7;
            int stack=q.level==2?5:q.level==1?4:2;
            for(int k=0;k<stack;k++) {
                double f=(k+.5)/stack;
                float w=(float)(hgt*(.22+.3*f));
                c.ash(x,hgt*(float)f,z,w,w*1.1f,(float)(h*6+k+t*.002),argb(.8*fade,Canvas.lerpRgb(ASH_DARK,DUST,(float)(f*.7))),(i+k)&3);
            }
            if(life<.35)c.fxSprite(Layer.WORLD_FX,Canvas.FX_FLARE,0,x,hgt*.1f,z,hgt*.5f,hgt*.5f,0,argb(.7*(1-life/.35),0xffa050));
        }
    }

    static boolean active(StrikeView v){return PresentationScale.colossal(v)>0;}
    static boolean flashing(){return !NullstarClient.config().reducedFlashing;}
}
