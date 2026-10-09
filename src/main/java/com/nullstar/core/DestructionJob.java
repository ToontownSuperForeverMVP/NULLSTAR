package com.nullstar.core;

/** Resumable scan; each candidate consumes budget, including unloaded/protected/air cells. */
public final class DestructionJob {
    @FunctionalInterface public interface Visitor { boolean visit(int x, int y, int z, boolean remove, boolean scorch); }
    private final CraterGeometry geometry;
    private final long total;
    private long cursor, changed;
    private boolean cancelled;
    public DestructionJob(CraterGeometry geometry) {
        this.geometry = geometry;
        total = (long) (geometry.radius() * 2 + 1) * (geometry.radius() * 2 + 1) * (geometry.floor() + geometry.ceiling() + 2);
    }
    public int step(int budget, Visitor visitor) {
        if (budget < 0) throw new IllegalArgumentException("Negative budget");
        int used = 0, width = geometry.radius() * 2 + 1, height = geometry.floor() + geometry.ceiling() + 2;
        while (used < budget && !done()) {
            long index = cursor++;
            int y = (int) (index % height) - geometry.floor() - 1;
            int x = (int) ((index / height) % width) - geometry.radius();
            int z = (int) (index / height / width) - geometry.radius();
            boolean remove = geometry.removes(x, y, z), scorch = geometry.shell(x, y, z);
            if ((remove || scorch) && visitor.visit(x, y, z, remove, scorch)) changed++;
            used++;
        }
        return used;
    }
    public void cancel() { cancelled = true; }
    public boolean cancelled() { return cancelled; }
    public boolean done() { return cancelled || cursor >= total; }
    public double progress() { return cursor / (double) total; }
    public long scanned() { return cursor; }
    public long changed() { return changed; }
    public long total() { return total; }
}
