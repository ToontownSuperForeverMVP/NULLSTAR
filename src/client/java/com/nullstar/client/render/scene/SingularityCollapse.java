// Copyright (C) 2026 ToontownSuperForeverMVP and NULLSTAR contributors
// SPDX-License-Identifier: GPL-3.0-only

package com.nullstar.client.render.scene;

import com.nullstar.client.ClientConfig;
import com.nullstar.client.render.Canvas;
import com.nullstar.client.render.Layer;
import com.nullstar.client.render.Mesh;
import com.nullstar.client.render.PresentationScale;
import com.nullstar.client.render.StrikeView;
import org.joml.Matrix4f;
import static com.nullstar.client.render.Canvas.argb;
import static com.nullstar.client.render.StrikeView.range;
import static com.nullstar.client.render.StrikeView.smooth;

/**
 * Part 3 colossal Singularity collapse and aftermath: the gravitational inverse of a Helios blast.
 *
 * <p>The anomaly crushes to a point, holds, and releases twin jets and a violet ground shock; then the region keeps
 * falling <em>inward</em>: fracture rings contract toward the sink, torn-up terrain rains back into the basin along
 * spiralling paths, violet arcs crawl around its rim, a dark dust vortex turns over the sink, fragments stay
 * suspended by residual gravity and a micro-anomaly flickers out above the floor. The floor glow sits at the true
 * basin depth with a normal depth test, so it shows only where the server has really excavated. Timing is
 * phase-relative ({@link HeliosDevastation#devastation}) so late observers reconstruct the same moment.
 */
final class SingularityCollapse {
    static final int VIOLET=0xa060ff,PALE=0xe8d8ff,DEEP=0x3a1066,HOT=0xfff4ff,DUST=0x2c2434,DUST_LIT=0x5a4a6a;
    private static final Mesh SLAB=new Mesh.Builder().uvScale(.4f).box(-.6f,-.16f,-.9f,.6f,.16f,.9f).build();
    private final Matrix4f m=new Matrix4f();

    void render(StrikeView v,Canvas c,ClientConfig.Quality q,double t,int groundTint) {
        double d=HeliosDevastation.devastation(v);
        if(d<0)return;
        float radius=v.radius(),depth=v.depth(),cy=SingularityScene.coreY(v);
        float reach=Math.max(radius,PresentationScale.secondaryReach(v));
        boolean reduced=Fx.reducedFlashing();
        // 1. The crush and the release: a pinpoint, then an expanding shell of violet light and darkness.
        if(d<.08) {
            double crush=1-range(d,0,.018),burst=range(d,.012,.08);
            if(crush>0)c.fxSprite(Layer.WORLD_FX,Canvas.FX_FLARE,0,0,cy,0,60+90*(float)(1-crush),60+90*(float)(1-crush),0,argb((reduced?.35:.95)*crush,HOT));
            if(burst>0) {
                float shell=(float)(radius*.9*Fx.easeOut(burst));
                m.identity().translate(0,cy,0).scale(shell);
                c.mesh(Layer.RIM_PLASMA,Mesh.UNIT_SPHERE,m,argb(.35*(1-burst),VIOLET),(float)(t*.03),0);
                c.fxSprite(Layer.WORLD_FX,Canvas.FX_CORONA,1,0,cy,0,shell*1.4f,shell*1.4f,(float)(t*.01),argb(.6*(1-burst),VIOLET));
                c.billboard(Layer.VOID_DEEP,0,cy,0,shell*1.1f,0,argb(.5*(1-burst),0x06000c));
            }
        }
        // 2. Twin release jets along the collapse axis, narrowing as the energy is spent.
        double jets=range(d,.01,.04)*(1-range(d,.06,.16));
        if(jets>0) {
            int seg=Fx.segments(q,16);float w=(float)(14*jets);float v0=Canvas.fxV(Canvas.FX_BEAM);
            c.tube(Layer.WORLD_FX,0,cy,0,0,cy+700,0,w,w*.25f,seg,argb(jets,PALE),argb(.3*jets,VIOLET),1,Canvas.fxU(.3),v0,v0+18);
            c.tube(Layer.WORLD_FX,0,cy,0,0,-depth,0,w*1.2f,w*.6f,seg,argb(jets,PALE),argb(.6*jets,VIOLET),1,Canvas.fxU(.3),v0,v0+6);
        }
        // 3. The gravitational shock: a violet ground front to the authoritative secondary reach, then a dust skirt.
        float r1=(float)(reach*Fx.easeOut(range(d,.015,.2)));
        double a1=1-range(d,.14,.28);
        if(a1>0&&r1>2) {
            c.annulus(Layer.WORLD_FX,0,1.2f,0,0,1,0,r1*.82f,r1,Fx.segments(q,96),argb(.8*a1,0xc8a0ff),argb(.8*a1,0xc8a0ff),1,Canvas.fxU(.7),
                    Canvas.fxV(Canvas.FX_SHOCK),Canvas.fxV(Canvas.FX_SHOCK)+1);
            int n=q.level==2?48:q.level==1?32:18;
            float h=radius*.06f,w=(float)(Math.PI*2*r1*.9/n*.8);
            for(int i=0;i<n;i++) {
                double a=i*Math.PI*2/n;
                c.ash((float)Math.cos(a)*r1*.9f,h*.6f,(float)Math.sin(a)*r1*.9f,Math.max(w,h),h,(float)(i*1.3),argb(.7*a1,DUST),i&3);
            }
        }
        // 4. The region keeps falling inward: fracture rings contracting toward the sink, wave after wave.
        double inward=smooth(range(d,.08,.14))*(1-range(d,.6,.85));
        if(inward>0) {
            int rings=q.level==2?5:q.level==1?4:2;
            for(int k=0;k<rings;k++) {
                double cyc=Fx.frac(t*.006+k/(double)rings);
                float r=(float)(reach*(1-Math.pow(cyc,.7))*.95+radius*.12);
                c.annulus(Layer.WORLD_FX,0,.8f,0,0,1,0,r*.93f,r,Fx.segments(q,96),argb(.55*inward*(1-cyc*.4),k%2==0?VIOLET:PALE),
                        argb(.55*inward*(1-cyc*.4),k%2==0?VIOLET:PALE),1,Canvas.fxU(.2+.15*k),Canvas.fxV(Canvas.FX_SHOCK),Canvas.fxV(Canvas.FX_SHOCK)+1);
            }
        }
        // 5. Terrain that was torn up during the attraction rains back into the basin along spiralling paths.
        double rain=smooth(range(d,.05,.1))*(1-range(d,.55,.75));
        if(rain>0) {
            int pieces=Fx.scaled(q,40);
            for(int i=0;i<pieces;i++) {
                double h=v.hash(13000+i),cyc=Fx.frac(t*(.004+.004*v.hash(13100+i))+h);
                double a=h*Math.PI*2+cyc*2.2;
                float rr=(float)(radius*(.15+.75*v.hash(13200+i))*(1-cyc*.3));
                float y=(float)(cy*(1-cyc)-depth*.5*cyc);
                float size=(float)(2+6*v.hash(13300+i));
                float x=(float)Math.cos(a)*rr,z=(float)Math.sin(a)*rr;
                m.translation(x,y,z).rotateXYZ((float)(t*.05+i),(float)(t*.03),0).scale(size*1.6f,size*.4f,size);
                c.mesh(Layer.DEBRIS,SLAB,m,argb(0,groundTint),0,0);
                if(q.level>=1)c.streak(Layer.GLOW,x,y,z,x,y+size*4,z,size*.2f,size*.05f,argb(.4*rain,VIOLET),argb(0,DEEP),0,0,1,1);
            }
        }
        // 6. Violet arcs crawling around the rim.
        double crawl=smooth(range(d,.1,.2))*(1-range(d,.75,1));
        if(crawl>0&&q.level>=1) {
            int arcs=reduced?2:q.level==2?6:4;long step=(long)(t/4);
            for(int i=0;i<arcs;i++) {
                java.util.Random r=new java.util.Random(v.seed()^(step*517+i));
                double a0=r.nextDouble()*Math.PI*2,e1=a0+.25+r.nextDouble()*.5;
                float r0=radius*(.7f+.4f*r.nextFloat());
                c.arc(Layer.GLOW,(float)Math.cos(a0)*r0,2,(float)Math.sin(a0)*r0,(float)Math.cos(e1)*r0,2,(float)Math.sin(e1)*r0,
                        radius*.05f,9,radius*.008f,argb((reduced?.4:.85)*crawl,PALE),r.nextLong());
            }
        }
        // 7. A dark dust vortex turning over the sink, settling and thinning through the aftermath.
        double vortex=smooth(range(d,.12,.3))*(1-.65*smooth(range(d,.75,1)));
        if(vortex>0) {
            int puffs=q.level==2?40:q.level==1?26:14;
            for(int i=0;i<puffs;i++) {
                double f=(i+.5)/puffs,a=f*Math.PI*6+t*.004*(1.5-f);
                float rr=(float)(radius*(.15+.8*f)),y=(float)(-depth*.25+radius*.18*(1-f)+Math.sin(t*.01+i)*4);
                float w=(float)(radius*(.08+.1*f));
                c.ash((float)Math.cos(a)*rr,y,(float)Math.sin(a)*rr,w,w*.55f,(float)(a+t*.002),argb(.6*vortex,Canvas.lerpRgb(DUST,DUST_LIT,(float)f)),i&3);
            }
        }
        // 8. Fragments held aloft by residual gravity, bobbing in slow orbits over the basin.
        double hover=smooth(range(d,.35,.5))*(1-range(d,.92,1));
        if(hover>0) {
            int pieces=Fx.scaled(q,26);
            for(int i=0;i<pieces;i++) {
                double h=v.hash(14000+i),a=h*Math.PI*2+t*.0015*(1+v.hash(14100+i));
                float rr=(float)(radius*(.1+.6*v.hash(14200+i))),y=(float)(18+50*v.hash(14300+i)+Math.sin(t*.02+i)*3);
                float size=(float)(1.5+4.5*v.hash(14400+i));
                float x=(float)Math.cos(a)*rr,z=(float)Math.sin(a)*rr;
                m.translation(x,y,z).rotateXYZ((float)(t*.01+i),(float)(t*.007),0).scale(size*1.4f,size*.5f,size);
                c.mesh(Layer.DEBRIS,SLAB,m,argb(0,groundTint),0,0);
                c.streak(Layer.GLOW,x,y-size*.5f,z,x,y-size*.5f-8,z,size*.3f,size*.05f,argb(.35*hover,VIOLET),argb(0,DEEP),0,0,1,1);
            }
        }
        // 9. The residual micro-anomaly flickering out above the floor.
        double residue=smooth(range(d,.45,.55))*(1-range(d,.8,.95));
        if(residue>0) {
            float r=(float)(3.5*(.75+.25*Math.sin(t*.7)))*(float)residue;
            c.sphere(Layer.DEBRIS,0,40,0,r,0x00000000,0,0,false);
            c.billboard(Layer.HALO,0,40,0,r*1.7f,(float)(t*.1),argb(.85*residue,PALE));
            c.fxSprite(Layer.WORLD_FX,Canvas.FX_CORONA,.8,0,40,0,r*6,r*6,(float)(t*.02),argb(.45*residue,VIOLET));
        }
        // 10. The basin floor at the true depth, glowing violet only where the terrain really is gone.
        double floor=smooth(range(d,.06,.2))*(1-.6*smooth(range(d,.6,1)));
        if(floor>0) {
            float y=-depth*.9f;int col=argb(.6*floor,0x8a3ad8);
            c.quad(Layer.GLOW,-radius,y,-radius,radius,y,-radius,radius,y,radius,-radius,y,radius,0,0,1,1,col,col,col,col,0,1,0);
        }
    }
}
