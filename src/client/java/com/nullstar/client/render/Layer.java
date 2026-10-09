package com.nullstar.client.render;

import net.minecraft.client.renderer.rendertype.RenderType;
import java.util.function.Supplier;
import static com.nullstar.client.render.NullstarMaterials.*;

/** Material slots used by every weapon scene. Each slot owns one bounded {@link VertexSink}. */
public enum Layer {
    HULL_HELIOS(16384,()->hull(HELIOS_HULL)),
    HULL_GLASSFALL(16384,()->hull(GLASSFALL_HULL)),
    HULL_SINGULARITY(4096,()->hull(SINGULARITY_HULL)),
    DEBRIS(8192,()->debris(RUBBLE)),
    VOID(1024,()->smoke(NullstarMaterials.VOID)),
    SMOKE(8192,()->smoke(NullstarMaterials.SMOKE)),
    CORE_PLASMA(16384,()->energy(Energy.CORE,PLASMA)),
    CORE_FLOW(16384,()->energy(Energy.CORE,FLOW)),
    RIM_PLASMA(8192,()->energy(Energy.RIM,PLASMA)),
    RIM_FLOW(8192,()->energy(Energy.RIM,FLOW)),
    CORE_CRYSTAL(8192,()->energy(Energy.CORE,CRYSTAL)),
    RIM_CRYSTAL(8192,()->energy(Energy.RIM,CRYSTAL)),
    GLOW(16384,()->energy(Energy.FLAT,NullstarMaterials.GLOW)),
    SHOCK(8192,()->energy(Energy.FLAT,NullstarMaterials.SHOCK)),
    HALO(2048,()->energy(Energy.FLAT,NullstarMaterials.HALO));

    /** Hard vertex cap per frame for this material across all active strikes. */
    public final int budget;
    final Supplier<RenderType> type;
    Layer(int budget,Supplier<RenderType> type){this.budget=budget;this.type=type;}
    public boolean opaque(){return this==HULL_HELIOS||this==HULL_GLASSFALL||this==HULL_SINGULARITY||this==DEBRIS;}
}
