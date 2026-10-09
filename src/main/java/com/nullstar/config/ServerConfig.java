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
    public void validate() {
        maximumRadius = clamp(maximumRadius, 1, 48); maximumDepth = clamp(maximumDepth, 1, 48);
        blockOperationsPerTick = clamp(blockOperationsPerTick, 16, 8192);
        maximumActiveStrikes = clamp(maximumActiveStrikes, 1, 8); maximumJobs = clamp(maximumJobs, 1, 32);
        entityBudget = clamp(entityBudget, 1, 128);
        cinematicDurationMultiplier = finite(cinematicDurationMultiplier, .25, 4, 1);
        cooldownMultiplier = finite(cooldownMultiplier, .1, 10, 1);
        soundVolumeMultiplier = finite(soundVolumeMultiplier, 0, 2, 1);
    }
    public static int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }
    public static double finite(double value, double min, double max, double fallback) {
        return Double.isFinite(value) ? Math.max(min, Math.min(max, value)) : fallback;
    }
}
