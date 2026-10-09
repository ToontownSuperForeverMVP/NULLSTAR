package com.nullstar.core;

/** Scene coordinates are normalized presentation space, never world destinations. */
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
}
