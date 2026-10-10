// Copyright (C) 2026 ToontownSuperForeverMVP and NULLSTAR contributors
// SPDX-License-Identifier: GPL-3.0-only

package com.nullstar.client;

import com.nullstar.config.ServerConfig;

/** Client presentation preferences. None of these values reach the server or alter authoritative strikes. */
public final class ClientConfig {
    public enum Quality {
        LOW(0,96,.35),MEDIUM(1,384,.75),ULTRA(2,1024,1);
        public final int level;
        /** Upper bound for pooled cosmetic particles/debris owned by the custom renderer. */
        public final int cosmeticBudget;
        /** Geometry detail factor used for segment counts, layer counts and trail lengths. */
        public final double detail;
        Quality(int level,int cosmeticBudget,double detail){this.level=level;this.cosmeticBudget=cosmeticBudget;this.detail=detail;}
        public Quality next(){return values()[(ordinal()+1)%values().length];}
    }
    public boolean cinematicCamera=true;
    public boolean reducedMotion=false;
    public boolean reducedFlashing=true;
    /** Vanilla-particle budget for the native fallback layer. */
    public int particleBudget=192;
    public double cameraShakeIntensity=.35;
    public Quality quality=Quality.MEDIUM;
    /** Multiplier for custom cosmetic particles and debris; zero disables them entirely. */
    public double particleIntensity=1;
    public boolean screenDistortion=true;
    public double glowIntensity=1;
    public double cinematicAudio=1;
    public boolean letterbox=true;
    public void validate() {
        particleBudget=ServerConfig.clamp(particleBudget,0,512);
        cameraShakeIntensity=ServerConfig.finite(cameraShakeIntensity,0,1,.35);
        if(quality==null)quality=Quality.MEDIUM;
        particleIntensity=ServerConfig.finite(particleIntensity,0,2,1);
        glowIntensity=ServerConfig.finite(glowIntensity,0,1.5,1);
        cinematicAudio=ServerConfig.finite(cinematicAudio,0,1,1);
    }
    /** Effective custom particle cap after quality and intensity preferences. */
    public int cosmeticParticles(){return (int)Math.round(quality.cosmeticBudget*Math.min(1,particleIntensity));}
    public boolean shakeAllowed(){return !reducedMotion&&cameraShakeIntensity>0;}
}
