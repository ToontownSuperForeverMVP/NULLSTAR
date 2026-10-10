// Copyright (C) 2026 ToontownSuperForeverMVP and NULLSTAR contributors
// SPDX-License-Identifier: GPL-3.0-only

package com.nullstar.config;

/** Hard clamps are deliberate: a malformed configuration must never remove safety limits. */
public final class ServerConfig {
    public boolean terrainDestruction = true;
    public boolean operatorsOnly = true;
    public int maximumRadius = 24;
    public int maximumDepth = 24;
    public int blockOperationsPerTick = 1536;
    public int maximumActiveStrikes = 4;
    public int maximumJobs = 16;
    public double cinematicDurationMultiplier = 1;
    public double cooldownMultiplier = 1;
    public double soundVolumeMultiplier = 1;
    public int entityBudget = 64;
    public com.nullstar.core.ScaleTier defaultScale = com.nullstar.core.ScaleTier.STANDARD;
    public boolean apocalypseEnabled = false;
    public boolean publicServerMode = true;
    public boolean colossalCinematicOnly = false;
    public boolean requireColossalConfirmation = true;
    public boolean persistDestructionJobs = false;
    public int maximumColossalDiameter = 1536;
    public int maximumColossalDepth = 192;
    public int publicMaximumRadius = 48;
    public int maximumDevastationWaves = 8;
    public int maximumSimultaneousColossalJobs = 2;
    public long maximumChangedBlocks = 100_000_000;
    public int maximumEntityRadius = 128;
    public int colossalObserverRange = 2048;
    /**
     * Part 3: terrain tiles that are unloaded when a crater job reaches them are remembered and excavated later when
     * normal play loads them, instead of being skipped for good. Nothing is ever force-loaded.
     */
    public boolean deferUnloadedRegions = true;
    /** How long (ticks) finished jobs keep waiting for deferred regions before abandoning them; 6000 = 5 minutes. */
    public int deferredRegionTicks = 6000;
    public void validate() {
        maximumRadius = clamp(maximumRadius, 1, 48); maximumDepth = clamp(maximumDepth, 1, 48);
        blockOperationsPerTick = clamp(blockOperationsPerTick, 16, 8192);
        maximumActiveStrikes = clamp(maximumActiveStrikes, 1, 8); maximumJobs = clamp(maximumJobs, 1, 32);
        entityBudget = clamp(entityBudget, 1, 128);
        if (defaultScale == null) defaultScale = com.nullstar.core.ScaleTier.STANDARD;
        maximumColossalDiameter=clamp(maximumColossalDiameter,2,2048);
        maximumColossalDepth=clamp(maximumColossalDepth,1,384);
        publicMaximumRadius=clamp(publicMaximumRadius,1,48);
        maximumDevastationWaves=clamp(maximumDevastationWaves,1,8);
        maximumSimultaneousColossalJobs=clamp(maximumSimultaneousColossalJobs,1,8);
        maximumChangedBlocks=Math.max(1,Math.min(1_000_000_000L,maximumChangedBlocks));
        maximumEntityRadius=clamp(maximumEntityRadius,1,512);
        colossalObserverRange=clamp(colossalObserverRange,256,4096);
        deferredRegionTicks=clamp(deferredRegionTicks,0,72000);
        cinematicDurationMultiplier = finite(cinematicDurationMultiplier, .25, 4, 1);
        cooldownMultiplier = finite(cooldownMultiplier, .1, 10, 1);
        soundVolumeMultiplier = finite(soundVolumeMultiplier, 0, 2, 1);
    }
    public int radiusCap(com.nullstar.core.ScaleTier tier) {
        if(tier==com.nullstar.core.ScaleTier.STANDARD)return maximumRadius;
        return publicServerMode?Math.min(publicMaximumRadius,maximumColossalDiameter/2):maximumColossalDiameter/2;
    }
    public int depthCap(com.nullstar.core.ScaleTier tier) {
        return tier==com.nullstar.core.ScaleTier.STANDARD?maximumDepth:publicServerMode?Math.min(maximumDepth,maximumColossalDepth):maximumColossalDepth;
    }
    public static int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }
    public static double finite(double value, double min, double max, double fallback) {
        return Double.isFinite(value) ? Math.max(min, Math.min(max, value)) : fallback;
    }
}
