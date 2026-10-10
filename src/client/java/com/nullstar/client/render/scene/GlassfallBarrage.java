// Copyright (C) 2026 ToontownSuperForeverMVP and NULLSTAR contributors
// SPDX-License-Identifier: GPL-3.0-only

package com.nullstar.client.render.scene;

import com.nullstar.client.ClientConfig;
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

/**
 * Part 3 colossal Glassfall impacts, finale and aftermath.
 *
 * <p>Every authoritative impact cue (sixteen peripheral strikes, the final centre strike and the ring passes) gets a
 * crystalline eruption at its exact cue position: a short contact flash, a cluster of glassy shards with an
 * additive rim, a cyan ground front and a rising frost plume. The finale raises a faceted crystalline pressure dome
 * that swells, fractures into panels and sheds them outward, with a central monolith cluster and a pillar of light.
 * The aftermath leaves a glittering shard field, low frost haze and the linked crater outlines; the floor glow sits
 * at the true basin depth so it shows only where terrain has really been excavated.
 *
 * <p>Glassy surfaces use the fog-free alpha-blended {@link Layer#CRYSTAL_GLASS} so they keep their body in full
 * daylight instead of saturating the frame white like stacked additive crystal did.
 */
final class GlassfallBarrage {
    static final int CYAN=0x56e6ff,ICE=0xd8fbff,DEEP=0x1a7fb0,FROST=0xb8d4e0,GLASS=0x8fdcf0;
    private static final Mesh SHARD=new Mesh.Builder().uvScale(.35f).crystal(.55f,.25f,2.6f,6,.2f).build();
    private static final Mesh MONOLITH=new Mesh.Builder().uvScale(.25f).crystal(1f,.4f,4.2f,6,.15f)
            .push().translate(.9f,0,.3f).rotateZ(-.35f).crystal(.6f,.3f,3.1f,5,.2f).pop()
            .push().translate(-.8f,0,-.5f).rotateX(.3f).crystal(.55f,.3f,2.6f,5,.25f).pop().build();
    private static final Mesh PANEL=new Mesh.Builder().uvScale(.5f).box(-.5f,-.03f,-.5f,.5f,.03f,.5f).build();
    private final Matrix4f m=new Matrix4f();

    void render(StrikeView v,Canvas c,ClientConfig.Quality q,double t) {
        float secondary=Math.max(v.radius(),PresentationScale.secondaryReach(v));
        boolean reduced=Fx.reducedFlashing();
        int index=0;
        for(var e:v.events()) {
            if(!e.impact())continue;
            int i=index++;
            double s=e.age(v.now());
            if(s>420)continue;
            Vec3 at=e.position().subtract(v.target());
            float x=(float)at.x,y=(float)at.y,z=(float)at.z,radius=e.payload().radius();
            boolean fin=radius>=v.radius()*.8;
            if(fin&&s<300)finale(v,c,q,t,s,x,y,z,radius,secondary,reduced);
            else if(!fin)eruption(v,c,q,t,s,i,x,y,z,radius,reduced);
        }
        aftermath(v,c,q,t,secondary);
    }

    /** One peripheral strike: flash, shard cluster, ground front and a frost plume, dissolving over ~12 s. */
    private void eruption(StrikeView v,Canvas c,ClientConfig.Quality q,double t,double s,int i,float x,float y,float z,float radius,boolean reduced) {
        if(s<6)c.fxSprite(Layer.WORLD_FX,Canvas.FX_FLARE,0,x,y+3,z,radius*.6f,radius*.6f,0,argb((reduced?.3:.9)*(1-s/6),ICE));
        float front=(float)(radius*2.2*Fx.easeOut(range(s,0,40)));
        double fa=1-range(s,25,60);
        if(fa>0&&front>1)c.annulus(Layer.WORLD_FX,x,y+.8f,z,0,1,0,front*.8f,front,Fx.segments(q,48),argb(.75*fa,0x9ff0ff),argb(.75*fa,0x9ff0ff),1,
                Canvas.fxU(.4+.03*(i%8)),Canvas.fxV(Canvas.FX_SHOCK),Canvas.fxV(Canvas.FX_SHOCK)+1);
        double grow=Fx.easeOut(range(s,0,8)),fade=1-smooth(range(s,150,240));
        int shards=q.level==2?9:q.level==1?6:3;
        float height=Math.min(26,radius*.28f);
        if(fade>0)for(int k=0;k<shards;k++) {
            double a=v.hash(15000+i*37+k)*Math.PI*2,rr=radius*(.12+.45*v.hash(15100+i*37+k));
            float h=(float)(height*(.5+.7*v.hash(15200+i*37+k))*grow);
            m.translation(x+(float)(Math.cos(a)*rr),y-1,z+(float)(Math.sin(a)*rr)).rotateY((float)a).rotateZ((float)(.2+.5*v.hash(15300+i*37+k))).rotateX((float)Math.PI).scale(h*.5f,h,h*.5f);
            c.mesh(Layer.CRYSTAL_GLASS,SHARD,m,argb(.62*fade,GLASS),0,0);
            if(q.level>=1)c.mesh(Layer.RIM_CRYSTAL,SHARD,m,argb(.35*fade,ICE),0,(float)(t*.01));
        }
        double plume=range(s,1,6)*(1-range(s,60,140));
        if(plume>0) {
            int puffs=q.level==2?6:q.level==1?4:2;
            for(int k=0;k<puffs;k++) {
                double f=(k+.5)/puffs;float w=(float)(radius*(.18+.22*f)*(.6+.6*range(s,0,50)));
                c.ash(x+(float)Math.sin(k*2.1+t*.002)*w*.2f,y+(float)(radius*.5*f*Math.min(1,s/30)),z+(float)Math.cos(k*1.7)*w*.2f,w,w,(float)(k+i+t*.002),
                        argb(.6*plume,Canvas.lerpRgb(FROST,0x6f8a98,(float)f)),(k+i)&3);
            }
        }
    }

    /** The final centre strike: a faceted crystalline pressure dome that swells, fractures and sheds its panels. */
    private void finale(StrikeView v,Canvas c,ClientConfig.Quality q,double t,double s,float x,float y,float z,float radius,float secondary,boolean reduced) {
        if(s<8) {
            c.fxSprite(Layer.WORLD_FX,Canvas.FX_FLARE,0,x,y+6,z,radius*.9f,radius*.9f,0,argb((reduced?.35:1)*(1-s/8),ICE));
            c.fxSprite(Layer.WORLD_FX,Canvas.FX_CORONA,1,x,y+6,z,radius*.5f,radius*.5f,(float)(t*.02),argb(.8*(1-s/8),CYAN));
        }
        // Pillar of light from the strike.
        double pillar=1-range(s,10,50);
        if(pillar>0) {
            float v0=Canvas.fxV(Canvas.FX_BEAM),w=(float)(radius*.12*pillar+2);
            c.tube(Layer.WORLD_FX,x,y,z,x,y+700,z,w,w*.3f,Fx.segments(q,16),argb(.9*pillar,ICE),argb(.2*pillar,CYAN),1,Canvas.fxU(.4),v0,v0+16);
        }
        // Ground front to the authoritative outer ring.
        float front=(float)(secondary*Fx.easeOut(range(s,0,70)));
        double fa=1-range(s,50,110);
        if(fa>0&&front>2)c.annulus(Layer.WORLD_FX,x,y+1,z,0,1,0,front*.86f,front,Fx.segments(q,96),argb(.85*fa,0xb8f6ff),argb(.85*fa,0xb8f6ff),1,
                Canvas.fxU(.9),Canvas.fxV(Canvas.FX_SHOCK),Canvas.fxV(Canvas.FX_SHOCK)+1);
        // The dome: faceted glass panels on a hemisphere. It swells, holds, then fractures outward and fades.
        float dome=(float)(Math.min(150,radius*1.1)*Fx.easeOut(range(s,0,26)));
        double crack=range(s,40,110),alive=1-range(s,90,170);
        if(alive>0&&dome>2) {
            int rings=q.level==2?7:q.level==1?5:3;
            for(int r=0;r<rings;r++) {
                double el=(r+.5)/rings*Math.PI*.5;
                int around=Math.max(6,(int)Math.round(Math.cos(el)*(q.level==2?22:q.level==1?15:9)));
                for(int k=0;k<around;k++) {
                    double az=(k+(r%2)*.5)/around*Math.PI*2+t*.0006;
                    double h=v.hash(16000+r*41+k);
                    float shed=(float)(crack*crack*(30+60*h)),fall=(float)(crack*crack*crack*40*h);
                    float rad=dome+shed;
                    float px=x+(float)(Math.cos(el)*Math.cos(az)*rad),py=y+(float)(Math.sin(el)*rad)-fall,pz=z+(float)(Math.cos(el)*Math.sin(az)*rad);
                    float size=(float)(dome*Math.PI*.5/rings*1.05);
                    m.translation(px,py,pz).rotateY((float)(-az)).rotateZ((float)(el-Math.PI/2)).rotateX((float)(crack*h*3))
                            .scale(size*(float)(1-.3*crack),1,(float)(size*Math.max(.35,Math.cos(el))*(1-.3*crack)));
                    double alpha=alive*(.3-.1*crack)*(.75+.25*h);
                    c.mesh(Layer.CRYSTAL_GLASS,PANEL,m,argb(alpha,GLASS),0,0);
                }
            }
            m.identity().translate(x,y,z).scale(dome,dome*.98f,dome);
            c.mesh(Layer.RIM_CRYSTAL,Mesh.UNIT_SPHERE,m,argb(.45*alive*(1-crack*.6),CYAN),0,(float)(t*.01));
            c.fxSprite(Layer.WORLD_FX,Canvas.FX_CORONA,.7,x,y+dome*.3f,z,dome*.9f,dome*.9f,(float)(t*.004),argb(.35*alive,CYAN));
        }
        // Central monolith cluster growing out of the strike.
        double grow=Fx.easeOut(range(s,2,30)),fade=1-smooth(range(s,220,300));
        if(fade>0) {
            float h=(float)(Math.min(70,radius*.6)*grow);
            for(int k=0;k<3;k++) {
                double a=k*2.1+v.hash(16900);
                m.translation(x+(float)Math.cos(a)*h*.25f*k,y-2,z+(float)Math.sin(a)*h*.25f*k).rotateY((float)a).rotateZ(k*.18f).rotateX((float)Math.PI)
                        .scale(h*(.5f-.1f*k)*.5f,h*(1-.2f*k)*.6f,h*(.5f-.1f*k)*.5f);
                c.mesh(Layer.CRYSTAL_GLASS,MONOLITH,m,argb(.7*fade,GLASS),0,0);
                if(q.level>=1)c.mesh(Layer.RIM_CRYSTAL,MONOLITH,m,argb(.4*fade,ICE),0,(float)(t*.008));
            }
        }
        // A frost cloud rolling up off the shattered dome.
        double cloud=range(s,30,70)*(1-range(s,180,300));
        if(cloud>0) {
            int puffs=q.level==2?20:q.level==1?13:7;
            for(int k=0;k<puffs;k++) {
                double a=k*Math.PI*2/puffs+t*.0008;float rr=dome*(.7f+.4f*(float)v.hash(16500+k));
                float w=radius*.28f;
                c.ash(x+(float)Math.cos(a)*rr,y+w*.5f+(float)(v.hash(16600+k)*dome*.4),z+(float)Math.sin(a)*rr,w,w*.7f,(float)(k+t*.002),
                        argb(.5*cloud,Canvas.lerpRgb(FROST,0x7f98a6,(float)v.hash(16700+k))),k&3);
            }
        }
    }

    /** Long aftermath: a glittering shard field, low frost haze and the true-depth basin glow. */
    private void aftermath(StrikeView v,Canvas c,ClientConfig.Quality q,double t,float secondary) {
        if(!v.atOrAfter(Phase.AFTERMATH)||v.atOrAfter(Phase.COMPLETE))return;
        double a=v.through(Phase.AFTERMATH);
        float radius=v.radius(),depth=v.depth();
        int glints=q.level==2?60:q.level==1?36:16;
        for(int i=0;i<glints;i++) {
            double h=v.hash(17000+i),tw=.5+.5*Math.sin(t*(.08+.1*h)+i);
            double ang=h*Math.PI*2;float rr=(float)(radius*Math.sqrt(v.hash(17100+i)));
            c.fxSprite(Layer.WORLD_FX,Canvas.FX_FLARE,0,(float)Math.cos(ang)*rr,2+(float)(v.hash(17200+i)*6),(float)Math.sin(ang)*rr,3+4*(float)tw,3+4*(float)tw,0,argb(.55*tw*(1-a*.7),ICE));
        }
        int haze=q.level==2?18:q.level==1?12:6;
        for(int i=0;i<haze;i++) {
            double ang=i*Math.PI*2/haze+t*.0004;float rr=(float)(radius*(.3+.7*v.hash(17300+i)));
            float w=radius*.18f;
            c.ash((float)Math.cos(ang)*rr,w*.15f,(float)Math.sin(ang)*rr,w,w*.25f,(float)(i+t*.001),argb(.12*(1-a*.8),FROST),i&3);
        }
        float y=-depth*.9f;int col=argb(.5*(1-.7*a),0x3ab8e8);
        c.quad(Layer.GLOW,-radius,y,-radius,radius,y,-radius,radius,y,radius,-radius,y,radius,0,0,1,1,col,col,col,col,0,1,0);
        int shown=0;
        for(var e:v.events()) {
            if(!e.impact()||shown>=8)continue;
            shown++;
            Vec3 at=e.position().subtract(v.target());float r=e.payload().radius()*(1.4f+(float)a*.4f);
            c.annulus(Layer.WORLD_FX,(float)at.x,(float)at.y+.4f,(float)at.z,0,1,0,r*.95f,r,Fx.segments(q,48),argb(.3*(1-a),CYAN),argb(.3*(1-a),CYAN),1,
                    Canvas.fxU(.2),Canvas.fxV(Canvas.FX_SHOCK),Canvas.fxV(Canvas.FX_SHOCK)+1);
        }
    }
}
