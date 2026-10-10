// Copyright (C) 2026 ToontownSuperForeverMVP and NULLSTAR contributors
// SPDX-License-Identifier: GPL-3.0-only

package com.nullstar.client.render.scene;

import com.nullstar.Nullstar;
import com.nullstar.client.ClientConfig;
import com.nullstar.client.NullstarClient;
import com.nullstar.client.render.Canvas;
import com.nullstar.client.render.Layer;
import net.minecraft.world.phys.Vec3;
import static com.nullstar.client.render.Canvas.argb;

/**
 * Holographic ground reticle for the controller: concentric rotating rings, angular calibration markers, a scanning
 * sweep and the selected weapon's effective radius. Locking contracts the reticle and raises a beacon line.
 * The displayed radius is the bundled definition; the server remains authoritative over the actual crater.
 */
public final class TargetingScene {
    private double lockedSince=-1;
    private boolean wasLocked;

    public void render(Canvas c,double now,ClientConfig.Quality q) {
        var pos=NullstarClient.targetPreview();
        if(pos==null){wasLocked=false;return;}
        boolean locked=NullstarClient.status().hasTarget();
        if(locked&&!wasLocked)lockedSince=now;
        wasLocked=locked;
        String weapon=NullstarClient.status().weapon();
        var definition=Nullstar.catalog()==null?null:Nullstar.catalog().get(weapon);
        int rgb=definition==null?0x55ddff:definition.presentation().color();
        float radius=definition==null?16:Math.min(48,definition.impact().radius());
        c.origin(Vec3.atBottomCenterOf(pos).add(0,1.06,0));
        double lockAge=locked&&lockedSince>=0?now-lockedSince:99;
        double snap=locked?Math.min(1,lockAge/6):0;
        float pulse=(float)(.85+.15*Math.sin(now*.35));
        int segs=Fx.segments(q,64);
        int accent=locked?rgb:0x7fe8ff;
        // Effective radius ring and dashed outer calibration ring.
        c.annulus(Layer.SHOCK,0,0,0,0,1,0,radius-.6f,radius,segs,argb(.65*pulse,accent),argb(.65*pulse,accent),6,(float)(now*.004),0,1);
        for(int i=0;i<24;i++) {
            double a=i*Math.PI/12+now*.012;
            float r0=radius+1.2f,r1=radius+(i%6==0?3.2f:2f);
            c.streak(Layer.GLOW,(float)Math.cos(a)*r0,0,(float)Math.sin(a)*r0,(float)Math.cos(a)*r1,0,(float)Math.sin(a)*r1,
                    .12f,.12f,argb(.7,accent),argb(.25,accent),0,.5f,1,.5f);
        }
        // Inner rotating rings contract to a tight lock.
        float inner=(float)(2.6-1.2*snap);
        c.annulus(Layer.GLOW,0,0,0,0,1,0,inner,inner+.3f,segs/2,argb(.8,0xffffff),argb(.2,accent),1,0,.45f,.55f);
        for(int k=0;k<3;k++) {
            double a=now*(k%2==0?.06:-.045)+k*2.1;float r=inner+1.4f+k*1.6f;
            for(int j=0;j<3;j++) {
                double a0=a+j*Math.PI*2/3,a1=a0+Math.PI/4.5;
                int n=8;float[] pts=new float[3*n];
                for(int i=0;i<n;i++){double t=a0+(a1-a0)*i/(n-1);pts[i*3]=(float)Math.cos(t)*r;pts[i*3+1]=0;pts[i*3+2]=(float)Math.sin(t)*r;}
                c.ribbon(Layer.GLOW,pts,n,.16f,.16f,argb(.75,accent),argb(.75,accent),0);
            }
        }
        // Scanning sweep line.
        double sweep=now*.08;
        c.streak(Layer.GLOW,0,0,0,(float)Math.cos(sweep)*radius,0,(float)Math.sin(sweep)*radius,.05f,.25f,argb(.1,accent),argb(.6,accent),0,.5f,1,.5f);
        c.billboard(Layer.GLOW,0,.2f,0,1.2f+(float)(1-snap)*.8f,0,argb(.9,0xffffff));
        if(locked) {
            float beacon=(float)(40*Math.min(1,lockAge/10));
            c.streak(Layer.GLOW,0,0,0,0,beacon,0,.3f,.05f,argb(.8,rgb),argb(0,rgb),0,.5f,1,.5f);
            if(lockAge<12)Fx.shockRing(c,0,.05f,0,(float)(radius*(.4+lockAge/12*.9)),1.5f,0xffffff,(1-lockAge/12)*.8,0,q);
        }
    }
}
