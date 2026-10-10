package com.nullstar.client.render;

import net.minecraft.client.renderer.rendertype.RenderType;
import java.util.function.Supplier;
import static com.nullstar.client.render.NullstarMaterials.*;

/**
 * Material slots used by every weapon scene. Each slot owns one bounded {@link VertexSink}.
 *
 * <p>Declaration order is draw order: opaque slots come first, then alpha-blended ones, then additive energy, so a
 * scene can build a solid body and lay its glow over it with the depth buffer doing the occlusion. The
 * {@code COSMIC_*} group serves the colossal normalized scenes (planet, solar system, galaxy, observable universe,
 * star mothership, stellar lance) and is deliberately separate so an immense cosmic composition can never starve
 * the world-space budgets of a concurrent strike.
 */
public enum Layer {
    /**
     * Part 3 procedural cosmos dome. First in draw order: it overwrites colour and depth of everything drawn before
     * it (the Minecraft world), and every cosmic layer after it depth-tests against its near depth.
     */
    COSMIC_SKY(4096,NullstarMaterials::cosmosSky),
    HULL_HELIOS(16384,()->hull(HELIOS_HULL)),
    HULL_GLASSFALL(16384,()->hull(GLASSFALL_HULL)),
    HULL_SINGULARITY(4096,()->hull(SINGULARITY_HULL)),
    DEBRIS(8192,()->debris(RUBBLE)),
    /** Monumental machinery: the star-harvesting mothership, its rings and the orbital bombardment platform. */
    COSMIC_HULL(32768,()->hull(NullstarMaterials.COSMIC_HULL)),
    /** Part 3 cosmic bodies lit by the virtual scene: the mothership, planets and moons. */
    COSMIC_BODY(65536,()->bakedHull(NullstarMaterials.COSMIC_HULL)),
    COSMIC_PLANET(8192,()->bakedHull(NullstarMaterials.PLANET)),
    /** Rocky moons and planets: regolith-like rubble plating under the scene's light rig. */
    COSMIC_ROCK(8192,()->bakedHull(RUBBLE)),
    VOID(1024,()->smoke(NullstarMaterials.VOID)),
    SMOKE(8192,()->smoke(NullstarMaterials.SMOKE)),
    /** Part 3 colossal ash: fog-free smoke so regional plumes and pressure walls read as silhouettes at any range. */
    ASH(16384,()->ash(NullstarMaterials.SMOKE)),
    /** Fog-free light-eating darkness around the colossal anomaly (alpha-blended void falloff). */
    VOID_DEEP(4096,()->ash(NullstarMaterials.VOID)),
    /** Fog-free alpha-blended crystal glass: Glassfall's colossal shards and pressure dome keep body in daylight. */
    CRYSTAL_GLASS(16384,()->ash(NullstarMaterials.CRYSTAL)),
    /** Alpha-blended dark backdrop that pushes the local world out of the cosmic frame. */
    COSMIC_DARK(8192,()->smoke(GAS)),
    /** Cloud deck over the planetary body, lit per vertex. */
    COSMIC_CLOUD(8192,()->smoke(NullstarMaterials.CLOUDS)),
    CORE_PLASMA(16384,()->energy(Energy.CORE,PLASMA)),
    CORE_FLOW(16384,()->energy(Energy.CORE,FLOW)),
    RIM_PLASMA(8192,()->energy(Energy.RIM,PLASMA)),
    RIM_FLOW(8192,()->energy(Energy.RIM,FLOW)),
    CORE_CRYSTAL(8192,()->energy(Energy.CORE,CRYSTAL)),
    RIM_CRYSTAL(8192,()->energy(Energy.RIM,CRYSTAL)),
    GLOW(16384,()->energy(Energy.FLAT,NullstarMaterials.GLOW)),
    SHOCK(8192,()->energy(Energy.FLAT,NullstarMaterials.SHOCK)),
    HALO(2048,()->energy(Energy.FLAT,NullstarMaterials.HALO)),
    /** Cosmic point stars: the additive backbone of every normalized scene. */
    COSMIC_STAR(24576,()->energy(Energy.FLAT,STAR)),
    /** Soft cosmic gas and dust clouds (alpha blended additive detail over the backdrop). */
    COSMIC_GAS(12288,()->energy(Energy.FLAT,GAS)),
    /** Additive cosmic glow: coronae, planet rims, engine blooms. */
    COSMIC_GLOW(20480,()->energy(Energy.FLAT,NullstarMaterials.GLOW)),
    /** Contained stellar plasma and accretion energy at cosmic scale. */
    COSMIC_PLASMA(16384,()->energy(Energy.CORE,PLASMA)),
    COSMIC_FLOW(12288,()->energy(Energy.CORE,FLOW)),
    /** Colossal beam interior and its unstable outer envelope. */
    COSMIC_LANCE(24576,()->energy(Energy.CORE,LANCE)),
    COSMIC_LANCE_RIM(16384,()->energy(Energy.RIM,LANCE)),
    /** Atmosphere and energy shells around planetary bodies. */
    COSMIC_SHELL(12288,()->energy(Energy.RIM,GAS)),
    /** Part 3 procedural energy: one program for galaxy discs, photospheres, coronae, beams, nebulae and shocks. */
    COSMIC_FX(65536,NullstarMaterials::cosmosFx),
    /** The same procedural energy materials used in world space by the colossal world scenes. */
    WORLD_FX(32768,NullstarMaterials::cosmosFx);

    /** Hard vertex cap per frame for this material across all active strikes. */
    public final int budget;
    final Supplier<RenderType> type;
    Layer(int budget,Supplier<RenderType> type){this.budget=budget;this.type=type;}
    public boolean opaque(){return this==COSMIC_SKY||this==HULL_HELIOS||this==HULL_GLASSFALL||this==HULL_SINGULARITY||this==DEBRIS
            ||this==COSMIC_HULL||this==COSMIC_BODY||this==COSMIC_PLANET||this==COSMIC_ROCK;}
}
