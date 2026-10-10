// Copyright (C) 2026 ToontownSuperForeverMVP and NULLSTAR contributors
// SPDX-License-Identifier: GPL-3.0-only

package com.nullstar.client.render;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Bounded pool of client-only cosmetic particles and debris. Struct-of-arrays storage is allocated once at the
 * absolute cap; the active cap follows the quality preset. Nothing here touches blocks, entities or the server.
 */
public final class CosmeticParticles {
    public static final int DEBRIS=0,EMBER=1,DUST=2,SPARK=3,SHARD=4,MOTE=5;
    public static final int ABSOLUTE_CAP=1024;
    private static final Mesh CHUNK=new Mesh.Builder().uvScale(.5f).box(-.5f,-.5f,-.5f,.5f,.5f,.5f).build();
    private static final Mesh SLIVER=new Mesh.Builder().crystal(.35f,1.1f,.9f,5,.3f).build();
    private final float[] x=new float[ABSOLUTE_CAP],y=new float[ABSOLUTE_CAP],z=new float[ABSOLUTE_CAP];
    private final float[] px=new float[ABSOLUTE_CAP],py=new float[ABSOLUTE_CAP],pz=new float[ABSOLUTE_CAP];
    private final float[] vx=new float[ABSOLUTE_CAP],vy=new float[ABSOLUTE_CAP],vz=new float[ABSOLUTE_CAP];
    private final float[] age=new float[ABSOLUTE_CAP],life=new float[ABSOLUTE_CAP],size=new float[ABSOLUTE_CAP],grow=new float[ABSOLUTE_CAP];
    private final float[] spin=new float[ABSOLUTE_CAP],spinSpeed=new float[ABSOLUTE_CAP],heat=new float[ABSOLUTE_CAP],floor=new float[ABSOLUTE_CAP];
    private final int[] type=new int[ABSOLUTE_CAP],color=new int[ABSOLUTE_CAP],owner=new int[ABSOLUTE_CAP];
    private final boolean[] alive=new boolean[ABSOLUTE_CAP];
    private final Vec3 base=Vec3.ZERO;
    private int count,cursor,cap=384,peak;
    private final Matrix4f matrix=new Matrix4f();
    private long seed=0x5EED;

    public void setCap(int value){cap=Math.max(0,Math.min(ABSOLUTE_CAP,value));}
    public int active(){return count;}
    public int peak(){return peak;}
    public float random(){seed=seed*6364136223846793005L+1442695040888963407L;return (seed>>>40)/(float)(1<<24);}
    public float signed(){return random()*2-1;}

    /** Returns false when the pool is at its cap; callers simply skip the cosmetic detail. */
    public boolean spawn(int kind,int ownerKey,double wx,double wy,double wz,double svx,double svy,double svz,float lifetime,float scale,int argb) {
        if(count>=cap||lifetime<=0)return false;
        for(int n=0;n<ABSOLUTE_CAP;n++) {
            int i=cursor;cursor=(cursor+1)%ABSOLUTE_CAP;
            if(alive[i])continue;
            alive[i]=true;count++;peak=Math.max(peak,count);
            x[i]=px[i]=(float)wx;y[i]=py[i]=(float)wy;z[i]=pz[i]=(float)wz;
            vx[i]=(float)svx;vy[i]=(float)svy;vz[i]=(float)svz;
            age[i]=0;life[i]=lifetime;size[i]=scale;grow[i]=0;type[i]=kind;color[i]=argb;owner[i]=ownerKey;
            spin[i]=random()*6.28f;spinSpeed[i]=signed()*.35f;heat[i]=kind==DEBRIS?1:0;floor[i]=Float.NaN;
            return true;
        }
        return false;
    }
    /** Debris heat (0..1) and dust growth per tick can be adjusted right after a successful spawn. */
    public void tune(float initialHeat,float growth) {
        int i=(cursor+ABSOLUTE_CAP-1)%ABSOLUTE_CAP;
        if(alive[i]){heat[i]=initialHeat;grow[i]=growth;}
    }
    public void removeOwner(int ownerKey) {
        for(int i=0;i<ABSOLUTE_CAP;i++)if(alive[i]&&owner[i]==ownerKey){alive[i]=false;count--;}
    }
    public void clear(){java.util.Arrays.fill(alive,false);count=0;}

    public void tick(ClientLevel level) {
        for(int i=0;i<ABSOLUTE_CAP;i++) {
            if(!alive[i])continue;
            if(++age[i]>=life[i]){alive[i]=false;count--;continue;}
            px[i]=x[i];py[i]=y[i];pz[i]=z[i];
            switch(type[i]) {
                case DEBRIS,SHARD -> {
                    vy[i]-=.075f;vx[i]*=.985f;vz[i]*=.985f;heat[i]=Math.max(0,heat[i]-.012f);
                    if(level!=null&&(Float.isNaN(floor[i])||((int)age[i]&3)==0))
                        floor[i]=level.getHeight(Heightmap.Types.MOTION_BLOCKING,(int)Math.floor(x[i]),(int)Math.floor(z[i]));
                    float nextY=y[i]+vy[i];
                    if(!Float.isNaN(floor[i])&&nextY<floor[i]+size[i]*.5f&&vy[i]<0) {
                        nextY=floor[i]+size[i]*.5f;vy[i]=-vy[i]*.28f;vx[i]*=.55f;vz[i]*=.55f;spinSpeed[i]*=.5f;
                        if(Math.abs(vy[i])<.05f)vy[i]=0;
                    }
                    y[i]=nextY;x[i]+=vx[i];z[i]+=vz[i];spin[i]+=spinSpeed[i];
                }
                case DUST -> {vx[i]*=.92f;vz[i]*=.92f;vy[i]=vy[i]*.95f+.006f;x[i]+=vx[i];y[i]+=vy[i];z[i]+=vz[i];size[i]+=grow[i];spin[i]+=spinSpeed[i]*.1f;}
                case EMBER -> {vx[i]*=.96f;vz[i]*=.96f;vy[i]=vy[i]*.96f-.004f;x[i]+=vx[i];y[i]+=vy[i];z[i]+=vz[i];}
                case SPARK -> {vy[i]-=.05f;x[i]+=vx[i];y[i]+=vy[i];z[i]+=vz[i];}
                default -> {vx[i]*=.9f;vy[i]*=.9f;vz[i]*=.9f;x[i]+=vx[i];y[i]+=vy[i];z[i]+=vz[i];}
            }
        }
    }

    public void render(Canvas canvas,Vec3 camera,float partial) {
        if(count==0)return;
        canvas.origin(camera);
        float cx=(float)camera.x,cy=(float)camera.y,cz=(float)camera.z;
        for(int i=0;i<ABSOLUTE_CAP;i++) {
            if(!alive[i])continue;
            float lx=px[i]+(x[i]-px[i])*partial-cx,ly=py[i]+(y[i]-py[i])*partial-cy,lz=pz[i]+(z[i]-pz[i])*partial-cz;
            float t=(age[i]+partial)/life[i],fade=Math.min(1,(1-t)*4)*Math.min(1,t*10+.2f);
            int rgb=color[i]&0xffffff;float baseAlpha=(color[i]>>>24)/255f;
            switch(type[i]) {
                case DEBRIS -> {
                    float s=size[i]*Math.min(1,(1-t)*5);
                    matrix.translation(lx,ly,lz).rotateXYZ(spin[i],spin[i]*.7f,spin[i]*.3f).scale(s);
                    canvas.mesh(Layer.DEBRIS,CHUNK,matrix,Canvas.argb(heat[i],rgb),0,0);
                }
                case SHARD -> {
                    float s=size[i]*Math.min(1,(1-t)*5);
                    matrix.translation(lx,ly,lz).rotateXYZ(spin[i],spin[i]*1.3f,0).scale(s);
                    canvas.mesh(Layer.CORE_CRYSTAL,SLIVER,matrix,Canvas.argb(baseAlpha*fade,rgb),0,0);
                }
                case DUST -> canvas.smoke(lx,ly,lz,size[i],spin[i],Canvas.argb(baseAlpha*fade*(1-t*.6f),rgb),i&3);
                case SPARK -> {
                    float sx=vx[i]*2.5f,sy=vy[i]*2.5f,sz=vz[i]*2.5f;
                    canvas.streak(Layer.GLOW,lx-sx,ly-sy,lz-sz,lx,ly,lz,size[i]*.15f,size[i]*.5f,Canvas.argb(0,rgb),Canvas.argb(baseAlpha*fade,rgb),0,0,1,1);
                }
                default -> canvas.billboard(Layer.GLOW,lx,ly,lz,size[i]*(type[i]==EMBER?1-t*.5f:1),spin[i],Canvas.argb(baseAlpha*fade,rgb));
            }
        }
    }
}
