// Copyright (C) 2026 ToontownSuperForeverMVP and NULLSTAR contributors
// SPDX-License-Identifier: GPL-3.0-only

package com.nullstar.core;

/** Bounded reconstruction contract for presentation. No coordinates in astronomical scene space. */
public record ColossalMetadata(ScaleTier tier,CinematicScene scene,double cinematicScale,double structureScale,
        double beamWidth,double shockwaveRadius,int secondaryRadius,int blastRadius,int entityRadius,
        String script,String anchor,int alignmentNodes,int subimpacts,int footprintRadius,int waves,int checkpoint,int duration,boolean committed) {
    public ColossalMetadata {
        if(tier==null||scene==null||!finite(cinematicScale,1,1e12)||!finite(structureScale,1,1e9)
                ||!finite(beamWidth,.1,2048)||!finite(shockwaveRadius,1,8192)||secondaryRadius<1||secondaryRadius>2048
                ||blastRadius<1||blastRadius>512||entityRadius<1||entityRadius>512||script==null||!script.matches("[a-z0-9_]{1,32}")
                ||anchor==null||!java.util.List.of("orbital","stellar","cosmic").contains(anchor)
                ||alignmentNodes<1||alignmentNodes>64||subimpacts<0||subimpacts>24||footprintRadius<1||footprintRadius>1024||waves<1||waves>8||checkpoint<0||checkpoint>32||duration<1||duration>76800)
            throw new IllegalArgumentException("Invalid colossal metadata");
    }
    private static boolean finite(double n,double min,double max){return Double.isFinite(n)&&n>=min&&n<=max;}
    public static ColossalMetadata standard(int radius) {
        return new ColossalMetadata(ScaleTier.STANDARD,CinematicScene.WORLD,1,1,2,Math.max(1,radius*3),radius,
                Math.min(512,radius),Math.min(512,radius*2),"standard","orbital",3,0,radius,1,0,1,false);
    }
}
