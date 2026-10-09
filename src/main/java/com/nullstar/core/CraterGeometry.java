package com.nullstar.core;

/** Deterministic geometry with smooth angular noise and layered radial falloff; no world access. */
public record CraterGeometry(String shape, int radius, int depth, double irregularity, long seed) {
    public CraterGeometry {
        if (!java.util.List.of("crater", "sphere", "ellipsoid").contains(shape) || radius < 1 || radius > 48
                || depth < 1 || depth > 48 || !Double.isFinite(irregularity) || irregularity < 0 || irregularity > .2)
            throw new IllegalArgumentException("Geometry exceeds hard safety limits");
    }
    public int ceiling() { return shape.equals("crater") ? Math.max(3, radius / 2) : (shape.equals("sphere") ? radius : depth); }
    public int floor() { return shape.equals("sphere") ? radius : depth; }
    public double boundary(double x, double z) {
        double angle = Math.atan2(z, x), phase = (seed & 65535) * .001;
        double noise = .55 * Math.sin(angle * 5 + phase) + .3 * Math.sin(angle * 9 - phase) + .15 * Math.sin(angle * 13 + phase * 2);
        return radius * (1 - irregularity * (.5 + noise * .5));
    }
    public double intensity(double x, double y, double z) {
        double r = Math.hypot(x, z) / boundary(x, z);
        if (r >= 1) return 0;
        if (!shape.equals("crater")) {
            double vertical = y / (shape.equals("sphere") ? radius : (double) depth);
            return Math.max(0, 1 - r * r - vertical * vertical);
        }
        double bowl = depth * Math.pow(1 - r * r, .65);
        if (y < -bowl || y > ceiling()) return 0;
        return Math.max(.01, (1 - r) * (y < 0 ? Math.max(.01, 1 + y / bowl) : 1));
    }
    public boolean removes(int x, int y, int z) { return intensity(x, y, z) > 0; }
    public boolean shell(int x, int y, int z) { return !removes(x, y, z) && removes(x, y + 1, z); }
}
