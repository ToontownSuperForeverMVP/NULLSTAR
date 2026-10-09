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
    HULL_HELIOS(16384,()->hull(HELIOS_HULL)),
    HULL_GLASSFALL(16384,()->hull(GLASSFALL_HULL)),
    HULL_SINGULARITY(4096,()->hull(SINGULARITY_HULL)),
    DEBRIS(8192,()->debris(RUBBLE)),
    /** Monumental machinery: the star-harvesting mothership, its rings and the orbital bombardment platform. */
    COSMIC_HULL(32768,()->hull(NullstarMaterials.COSMIC_HULL)),
    VOID(1024,()->smoke(NullstarMaterials.VOID)),
    SMOKE(8192,()->smoke(NullstarMaterials.SMOKE)),
    /** Alpha-blended dark backdrop that pushes the local world out of the cosmic frame. */
    COSMIC_DARK(8192,()->smoke(GAS)),
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
    COSMIC_SHELL(12288,()->energy(Energy.RIM,GAS));

    /** Hard vertex cap per frame for this material across all active strikes. */
    public final int budget;
    final Supplier<RenderType> type;
    Layer(int budget,Supplier<RenderType> type){this.budget=budget;this.type=type;}
    public boolean opaque(){return this==HULL_HELIOS||this==HULL_GLASSFALL||this==HULL_SINGULARITY||this==DEBRIS||this==COSMIC_HULL;}
}
