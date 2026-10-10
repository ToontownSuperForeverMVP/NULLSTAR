// Copyright (C) 2026 ToontownSuperForeverMVP and NULLSTAR contributors
// SPDX-License-Identifier: GPL-3.0-only

package com.nullstar.core;

/** Clips both child crater radius and center displacement into an admitted physical footprint. */
public final class ImpactFootprint {
    public record Offset(int x,int z) {}
    private ImpactFootprint() {}
    public static Offset offset(double x,double z,int configuredRadius,int effectiveRadius,int childRadius,int maximumRadius) {
        if(!Double.isFinite(x)||!Double.isFinite(z)||configuredRadius<1||effectiveRadius<1||childRadius<1||maximumRadius<childRadius)
            throw new IllegalArgumentException("Invalid impact footprint");
        double ratio=effectiveRadius/(double)configuredRadius,dx=x*ratio,dz=z*ratio;
        int limit=maximumRadius-childRadius;double distance=Math.hypot(dx,dz);
        if(distance>limit&&distance>0){dx*=limit/distance;dz*=limit/distance;}
        int ix=(int)Math.round(dx),iz=(int)Math.round(dz);
        // Integer rounding must not put a child outside the radial safety envelope.
        while((long)ix*ix+(long)iz*iz>(long)limit*limit) {
            if(Math.abs(ix)>=Math.abs(iz))ix-=Integer.signum(ix);else iz-=Integer.signum(iz);
        }
        return new Offset(ix,iz);
    }
}
