package com.nullstar.client.render;

import com.nullstar.core.ScaleTier;

/**
 * Maps the authoritative colossal descriptors onto world-space presentation magnitudes.
 *
 * <p>The server owns the damage; these values only decide how large the machinery, the beam and the aftermath
 * <em>look</em>. Astronomical descriptors ({@code structureScale} up to 10^9) are compressed through a logarithm so
 * they can never be multiplied into raw world vertices, exactly as the colossal engineering contract requires.
 * STANDARD resolves to the unchanged Part 2 magnitudes.
 */
public final class PresentationScale {
    private PresentationScale() {}

    /** 0 for STANDARD, ~0.56 for TITANIC, 1 for APOCALYPSE. Derived from the authoritative cinematic descriptor. */
    public static double colossal(StrikeView v) {
        double scale = v.scale().cinematicScale();
        if (!Double.isFinite(scale) || scale <= 1) return 0;
        return Math.max(0, Math.min(1, Math.log10(scale) / 9));
    }

    public static boolean colossal(StrikeView v, ScaleTier tier) {
        return v.scale().tier() == tier;
    }

    /** Linear machinery multiplier: 1.0 STANDARD, ~2.9 TITANIC, ~4.4 APOCALYPSE. */
    public static float structure(StrikeView v) {
        return (float) (1 + 3.4 * colossal(v));
    }

    /** Emitter altitude in blocks: the machine climbs as the event grows so the ground stays a scale reference. */
    public static float altitude(StrikeView v) {
        double base = Math.max(8, v.height());
        return (float) Math.min(900, base * (1 + 3.6 * colossal(v)));
    }

    /** Cosmetic beam radius in blocks. STANDARD keeps the validated 1.7-block lance. */
    public static float beamRadius(StrikeView v) {
        double width = Math.max(2, v.scale().beamWidth());
        return (float) (1.7 + (width - 2) * 0.55);
    }

    /** How far the pressure wave and the residual devastation read out from the impact centre. */
    public static float shockReach(StrikeView v) {
        double reach = v.scale().shockwaveRadius();
        return (float) Math.max(v.radius() * 3, Double.isFinite(reach) ? Math.min(2400, reach) : v.radius() * 3);
    }

    /** Radius of the secondary devastation annuli, bounded by the permitted footprint. */
    public static float secondaryReach(StrikeView v) {
        return Math.max(v.radius(), v.scale().secondaryRadius());
    }

    /** Number of orbiting/deployed machine structures the profile asks for (1..64). */
    public static int nodes(StrikeView v) {
        return Math.max(1, Math.min(64, v.scale().alignmentNodes()));
    }
}
