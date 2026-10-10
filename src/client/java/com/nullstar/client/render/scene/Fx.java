// Copyright (C) 2026 ToontownSuperForeverMVP and NULLSTAR contributors
// SPDX-License-Identifier: GPL-3.0-only

package com.nullstar.client.render.scene;

import com.nullstar.client.ClientConfig;
import com.nullstar.client.NullstarClient;
import com.nullstar.client.render.Canvas;
import com.nullstar.client.render.CosmeticParticles;
import com.nullstar.client.render.Layer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import static com.nullstar.client.render.Canvas.argb;

/** Reusable effect building blocks shared by the weapon scenes. All coordinates are canvas-origin relative. */
final class Fx {
    private Fx() {}
    static int segments(ClientConfig.Quality quality,int ultra){return Math.max(6,(int)Math.round(ultra*(quality.level==2?1:quality.level==1?.67:.42)));}
    static float easeOut(double t){t=Math.max(0,Math.min(1,t));return (float)(1-Math.pow(1-t,3));}
    static float easeIn(double t){t=Math.max(0,Math.min(1,t));return (float)(t*t*t);}
    static float frac(double v){return (float)(v-Math.floor(v));}
    static boolean reducedFlashing(){return NullstarClient.config().reducedFlashing;}

    /**
     * Layered energy column from (x,top,z) down to (x,bottom,z): white-hot core, plasma body, turbulent envelope,
     * helical traces, radial arcs, shock diamonds and a soft camera-facing halo.
     */
    static void beam(Canvas c,float x,float top,float bottom,float z,float radius,double time,int core,int body,int envelope,
                     double intensity,ClientConfig.Quality q,long seed) {
        if(top-bottom<.1f||intensity<=0||radius<=0)return;
        int seg=segments(q,24);float len=top-bottom;float vRep=len/18f;
        float scroll=(float)(time*.09);
        c.tube(Layer.CORE_PLASMA,x,top,z,x,bottom,z,radius*.32f,radius*.36f,seg,argb(intensity,core),argb(intensity,core),1,0,scroll*3,scroll*3+vRep*2);
        c.tube(Layer.CORE_FLOW,x,top,z,x,bottom,z,radius*.72f,radius*.8f,seg,argb(intensity*.75,body),argb(intensity*.75,body),2,scroll,scroll*2.2f,scroll*2.2f+vRep);
        // Envelope breathes along its length so outer layers visibly churn around the stable core.
        int pieces=q.level==0?1:Math.max(2,(int)(len/10));
        for(int i=0;i<pieces;i++) {
            float y0=top-len*i/pieces,y1=top-len*(i+1)/pieces;
            float w0=radius*(1.2f+.18f*(float)Math.sin(y0*.35+time*.55)),w1=radius*(1.2f+.18f*(float)Math.sin(y1*.35+time*.55));
            c.tube(Layer.RIM_FLOW,x,y0,z,x,y1,z,w0,w1,seg,argb(intensity*.65,envelope),argb(intensity*.65,envelope),1,-scroll*.6f,
                    (float)i/pieces*vRep+scroll,(float)(i+1)/pieces*vRep+scroll);
        }
        float halo=radius*(q.level==2?5.5f:4.2f);
        c.streak(Layer.GLOW,x,top,z,x,bottom,z,halo,halo,argb(intensity*.16,envelope),argb(intensity*.2,envelope),0,.5f,1,.5f);
        if(q.level>=1) {
            int helices=q.level==2?3:2;
            float[] pts=new float[3*40];
            for(int h=0;h<helices;h++) {
                int n=Math.min(40,Math.max(8,(int)(len/2.5f)));
                for(int i=0;i<n;i++) {
                    float t=(float)i/(n-1),y=top-len*t;
                    double a=time*.45*(h%2==0?1:-1)+y*.32+h*Math.PI*2/helices;
                    float r=radius*(1.05f+.1f*h);
                    pts[i*3]=x+(float)Math.cos(a)*r;pts[i*3+1]=y;pts[i*3+2]=z+(float)Math.sin(a)*r;
                }
                c.ribbon(Layer.GLOW,pts,n,radius*.12f,radius*.08f,argb(intensity*.8,0xfff2c0),argb(intensity*.5,body),0);
            }
            int nodes=Math.max(2,(int)(len/9));
            for(int i=0;i<nodes;i++) {
                float y=top-len*frac(i/(float)nodes+time*.035);
                c.billboard(Layer.GLOW,x,y,z,radius*1.6f,radius*1.6f,(float)(time*.1+i),argb(intensity*.55,0xfff6d8),0,0,1,1);
            }
            int arcs=q.level==2?5:2;
            long step=(long)(time/2);
            for(int i=0;i<arcs;i++) {
                java.util.Random r=new java.util.Random(seed^(step*31+i));
                float y=bottom+len*r.nextFloat();double a=r.nextDouble()*Math.PI*2;float reach=radius*(2+r.nextFloat()*2.5f);
                c.arc(Layer.GLOW,x+(float)Math.cos(a)*radius*.8f,y,z+(float)Math.sin(a)*radius*.8f,
                        x+(float)Math.cos(a)*reach,y+(r.nextFloat()-.3f)*reach,z+(float)Math.sin(a)*reach,radius*.6f,6,radius*.1f,argb(intensity*.9,0xfff0c8),r.nextLong());
            }
        }
    }
    /** Expanding horizontal shock ring. The shock texture's leading edge sits on the outer radius. */
    static void shockRing(Canvas c,float x,float y,float z,float radius,float width,int rgb,double alpha,float spin,ClientConfig.Quality q) {
        if(alpha<=0||radius<=0)return;
        c.annulus(Layer.SHOCK,x,y,z,0,1,0,Math.max(0,radius-width),radius,segments(q,64),argb(alpha,rgb),argb(alpha,rgb),3,spin,0,1);
    }
    /** Tilted angular shock ring for crystalline impacts. */
    static void shockRing(Canvas c,float x,float y,float z,float nx,float ny,float nz,float radius,float width,int rgb,double alpha,int sides,float spin) {
        if(alpha<=0||radius<=0)return;
        c.annulus(Layer.SHOCK,x,y,z,nx,ny,nz,Math.max(0,radius-width),radius,sides,argb(alpha,rgb),argb(alpha,rgb),sides/2f,spin,0,1);
    }
    /** Vertical pressure wall that fades toward its top. */
    static void shockWall(Canvas c,float x,float y,float z,float radius,float height,int rgb,double alpha,ClientConfig.Quality q,float scroll) {
        if(alpha<=0||radius<=0||height<=0)return;
        c.tube(Layer.RIM_FLOW,x,y,z,x,y+height,z,radius,radius*.96f,segments(q,48),argb(alpha,rgb),argb(0,rgb),4,scroll,0,.6f);
    }
    /** Soft additive glow lying on the ground plane. */
    static void groundGlow(Canvas c,float x,float y,float z,float radius,int rgb,double alpha) {
        if(alpha<=0)return;
        int col=argb(alpha,rgb);
        c.quad(Layer.GLOW,x-radius,y,z-radius,x+radius,y,z-radius,x+radius,y,z+radius,x-radius,y,z+radius,0,0,1,1,col,col,col,col,0,0,0);
    }
    static void flash(Canvas c,float x,float y,float z,float size,int rgb,double alpha) {
        if(alpha<=0)return;
        double cap=reducedFlashing()?.42:1;
        c.billboard(Layer.GLOW,x,y,z,size*(reducedFlashing()?.75f:1),0,argb(Math.min(alpha,cap),rgb));
    }
    /** Terrain map colour at the impact, so dust and debris read as the struck material. */
    static int groundColor(ClientLevel level,Vec3 world) {
        if(level==null)return 0x8a8178;
        BlockPos pos=BlockPos.containing(world.x,world.y-1.2,world.z);
        for(int i=0;i<6;i++,pos=pos.below()) {
            var state=level.getBlockState(pos);
            if(!state.isAir()) {
                int col=state.getMapColor(level,pos).col;
                return col==0?0x8a8178:col;
            }
        }
        return 0x8a8178;
    }
    static int owner(com.nullstar.client.render.StrikeView view){return view.state().id().hashCode();}
    static int scaled(ClientConfig.Quality q,int ultra){return (int)Math.round(ultra*q.detail*Math.min(2,NullstarClient.config().particleIntensity));}

    /** Hot, tumbling terrain fragments thrown radially from an impact. */
    static void debrisBurst(CosmeticParticles p,int owner,Vec3 at,int count,float speed,float lift,int tint,float minSize,float maxSize,float heat) {
        for(int i=0;i<count;i++) {
            double a=p.random()*Math.PI*2,s=speed*(.35+p.random()*.9);
            p.spawn(CosmeticParticles.DEBRIS,owner,at.x+Math.cos(a)*p.random()*2,at.y+.5,at.z+Math.sin(a)*p.random()*2,
                    Math.cos(a)*s,lift*(.5+p.random()),Math.sin(a)*s,60+p.random()*60,minSize+p.random()*(maxSize-minSize),0xff000000|tint);
            p.tune(heat*(.4f+p.random()*.6f),0);
        }
    }
    static void dustRing(CosmeticParticles p,int owner,Vec3 at,int count,float radius,float speed,int tint,float size) {
        for(int i=0;i<count;i++) {
            double a=p.random()*Math.PI*2,r=radius*(.2+.8*p.random());
            p.spawn(CosmeticParticles.DUST,owner,at.x+Math.cos(a)*r,at.y+.3+p.random(),at.z+Math.sin(a)*r,
                    Math.cos(a)*speed*(.6+p.random()),.03+p.random()*.05,Math.sin(a)*speed*(.6+p.random()),90+p.random()*80,size*(.6f+p.random()*.8f),0xd0000000|tint);
            p.tune(0,.04f+p.random()*.05f);
        }
    }
    static void column(CosmeticParticles p,int owner,Vec3 at,int count,float radius,float rise,int tint,float size) {
        for(int i=0;i<count;i++) {
            double a=p.random()*Math.PI*2,r=radius*p.random();
            p.spawn(CosmeticParticles.DUST,owner,at.x+Math.cos(a)*r,at.y+p.random()*3,at.z+Math.sin(a)*r,
                    Math.cos(a)*.05,rise*(.4+p.random()),Math.sin(a)*.05,110+p.random()*70,size*(.7f+p.random()*.6f),0xc8000000|tint);
            p.tune(0,.05f+p.random()*.04f);
        }
    }
    static void embers(CosmeticParticles p,int owner,Vec3 at,int count,float radius,float speed,int rgb) {
        for(int i=0;i<count;i++) {
            double a=p.random()*Math.PI*2,r=radius*p.random();
            p.spawn(CosmeticParticles.EMBER,owner,at.x+Math.cos(a)*r,at.y+.4,at.z+Math.sin(a)*r,
                    Math.cos(a)*speed*p.random(),speed*(.4+p.random()),Math.sin(a)*speed*p.random(),50+p.random()*70,.18f+p.random()*.3f,0xff000000|rgb);
        }
    }
    // ------------------------------------------------------------------ colossal aftermath helpers
    /**
     * Layered expanding shock system for a colossal impact: a white-hot leading edge, progressively slower and
     * redder trailing rings, a ground-hugging pressure wall and an upward pressure dome. All radii come from the
     * authoritative shockwave reach, never from a hardcoded world size.
     */
    static void colossalShock(Canvas c,float x,float y,float z,float reach,double age,int rgb,double alpha,int layers,float spin,ClientConfig.Quality q) {
        if(alpha<=0||reach<=0)return;
        int rings=Math.max(1,Math.min(layers,q.level==2?6:q.level==1?4:2));
        for(int i=0;i<rings;i++) {
            double t=Math.max(0,age-i*2.2),span=14+i*7;
            if(t>span)continue;
            float r=reach*(.18f+.82f*easeOut(t/span));
            double a=alpha*(1-t/span)*(1-i*.11);
            int col=Canvas.lerpRgb(0xfff4e0,rgb,i/(double)rings);
            shockRing(c,x,y+.4f+i*.7f,z,r,2.2f+i*1.5f,col,a,spin*(i%2==0?1:-1),q);
        }
        double wall=(1-age/18);
        if(wall>0)shockWall(c,x,y,z,reach*.55f*easeOut(age/16),reach*.02f*(float)wall,rgb,alpha*.7*wall,q,spin*.05f);
        double dome=(1-age/24);
        if(dome>0&&q.level>=1) {
            float r=reach*.7f*easeOut(age/22);
            c.sphere(Layer.RIM_PLASMA,x,y,z,r*.55f,argb(alpha*.16*dome,rgb),(float)(age*.03),0,q.level==2);
        }
    }

    /** Towering debris/dust column: a tapering trunk of ash with a spreading frontal head. */
    static void ashColumn(Canvas c,float x,float y,float z,float radius,float height,int rgb,double alpha,float scroll,int slices,ClientConfig.Quality q) {
        if(alpha<=0||height<=0||radius<=0)return;
        int pieces=Math.max(1,Math.min(slices,q.level==2?6:q.level==1?4:2));
        for(int i=0;i<pieces;i++) {
            float y0=y+height*i/pieces,y1=y+height*(i+1)/pieces;
            float w0=radius*(1+.55f*i/pieces),w1=radius*(1+.55f*(i+1)/pieces);
            double a=alpha*(1-(double)i/pieces);
            c.tube(Layer.SMOKE,x,y0,z,x,y1,z,w0,w1,Math.max(4,segments(q,20)),argb(a,rgb),argb(a*.45,rgb),2,scroll+i*.4f,(float)i/pieces,(float)(i+1)/pieces);
        }
        c.tube(Layer.RIM_FLOW,x,y,z,x,y+height,z,radius*.35f,radius*.9f,segments(q,16),argb(alpha*.5,rgb),argb(0,rgb),1,scroll,0,1);
    }

    /** Molten residue pooling in a new basin: uneven additive patches of hot material. */
    static void moltenPool(Canvas c,float x,float y,float z,float radius,int rgb,double alpha,float scroll,int blobs) {
        if(alpha<=0||radius<=0)return;
        groundGlow(c,x,y,z,radius,rgb,alpha*.45);
        for(int i=0;i<blobs;i++) {
            double a=scroll*1.7+i*2.399;float r=(float)(radius*(.2+.75*frac(i*.618+scroll*.11)));
            float px=x+(float)Math.cos(a)*r,pz=z+(float)Math.sin(a)*r;
            groundGlow(c,px,y,pz,radius*(.14f+.12f*frac(i*.37)),Canvas.lerpRgb(rgb,0xfff0c0,frac(i*.71)),alpha*(.25+.3*frac(i*.53))*Math.max(0,1-r/radius));
        }
    }

    /** Ash dome hanging over a devastated region; shelled billboards read as a settled smoke ceiling. */
    static void dustDome(Canvas c,float x,float y,float z,float radius,float height,int rgb,double alpha,int shells,ClientConfig.Quality q) {
        if(alpha<=0||radius<=0||height<=0)return;
        int rings=Math.max(1,Math.min(shells,q.level==2?3:q.level==1?2:1));
        for(int s=0;s<rings;s++) {
            int n=Math.max(4,segments(q,10));
            double band=(s+.5)/rings;float rr=(float)(radius*(.35+.65*band));
            float hh=(float)(height*(1-band*.75));
            float size=(float)(radius*(.35f+.3f*band));
            for(int i=0;i<n;i++) {
                double a=i*Math.PI*2/n+s*.4;
                c.smoke(x+(float)Math.cos(a)*rr,y+hh,z+(float)Math.sin(a)*rr,size,(float)(a*.5),argb(alpha*(1-band*.6),rgb),s&3);
            }
            c.tube(Layer.SMOKE,x,y+(float)(height*(1-band)*.3),z,x,y+hh,z,rr*1.02f,rr,Math.max(4,segments(q,12)),argb(alpha*.2*(1-band),rgb),argb(0,rgb),3,(float)band*3,0,.7f);
        }
    }

    static void sparks(CosmeticParticles p,int owner,Vec3 at,int count,float speed,int rgb) {
        for(int i=0;i<count;i++) {
            double a=p.random()*Math.PI*2,e=p.random()*1.2;
            p.spawn(CosmeticParticles.SPARK,owner,at.x,at.y+.5,at.z,Math.cos(a)*Math.cos(e)*speed,Math.sin(e)*speed+.2,Math.sin(a)*Math.cos(e)*speed,
                    14+p.random()*16,.6f+p.random()*.6f,0xff000000|rgb);
        }
    }
}
