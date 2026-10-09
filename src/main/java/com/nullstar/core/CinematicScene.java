package com.nullstar.core;

/**
 * Scene coordinates are normalized presentation space, never world destinations.
 *
 * <p>{@link #forPhase(Phase, String, ScaleTier)} is the colossal resolution used by the authoritative snapshot: a
 * colossal Helios holds the cosmic act through its charge, alignment and discharge so the machine fires in the
 * star-mothership framing, then cuts back to the world for the descent and the impact.
 */
public enum CinematicScene {
    WORLD, ASCENT, PLANET, SOLAR_SYSTEM, GALAXY, COSMIC_BOUNDARY, STELLAR_MOTHERSHIP;
    public static CinematicScene forPhase(Phase phase) {
        return switch (phase) {
            case ASCENT, EXOATMOSPHERIC_TRANSITION -> ASCENT;
            case PLANETARY_VIEW -> PLANET;
            case SOLAR_SYSTEM_VIEW -> SOLAR_SYSTEM;
            case GALACTIC_VIEW -> GALAXY;
            case COSMIC_BOUNDARY_VIEW -> COSMIC_BOUNDARY;
            case MOTHERSHIP_REVEAL, DEPLOYMENT -> STELLAR_MOTHERSHIP;
            default -> WORLD;
        };
    }
    /**
     * Colossal resolution. Only the Helios timeline has a cosmic charging act; every other weapon keeps its
     * validated scene mapping, and STANDARD is never affected.
     */
    public static CinematicScene forPhase(Phase phase, String weapon, ScaleTier tier) {
        if (tier != null && tier != ScaleTier.STANDARD && "helios".equals(weapon)) {
            return switch (phase) {
                case CHARGING, ORBITAL_ALIGNMENT, FIRING -> STELLAR_MOTHERSHIP;
                default -> forPhase(phase);
            };
        }
        return forPhase(phase);
    }
}
