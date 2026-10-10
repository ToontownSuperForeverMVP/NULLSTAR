// Copyright (C) 2026 ToontownSuperForeverMVP and NULLSTAR contributors
// SPDX-License-Identifier: GPL-3.0-only

package com.nullstar.core;

public final class TargetRules {
    private TargetRules() {}
    public static boolean withinRange(double eyeX, double eyeY, double eyeZ, double x, double y, double z, int range) {
        if (!Double.isFinite(eyeX) || !Double.isFinite(eyeY) || !Double.isFinite(eyeZ)
                || !Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z) || range <= 0) return false;
        double dx = eyeX - x, dy = eyeY - y, dz = eyeZ - z;
        return dx * dx + dy * dy + dz * dz <= (double) range * range;
    }
}
