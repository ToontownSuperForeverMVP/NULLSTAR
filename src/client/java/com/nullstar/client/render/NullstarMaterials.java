package com.nullstar.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.CompareOp;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.nullstar.Nullstar;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.oit.OitPipelineSet;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * NULLSTAR's original GPU materials for the 26.3 RenderPearl pipeline.
 *
 * <p>Every pipeline is registered as optional, so a driver/shader failure only logs a warning during resource
 * reload. The first use verifies compilation; unavailable materials fall back to vanilla render types that accept
 * the same vertex stream (unused attributes are ignored by the buffer builder).
 */
public final class NullstarMaterials {
    public enum Energy { CORE, RIM, FLAT }
    public static final Identifier PLASMA=Nullstar.id("textures/effect/plasma.png");
    public static final Identifier FLOW=Nullstar.id("textures/effect/flow.png");
    public static final Identifier GLOW=Nullstar.id("textures/effect/glow.png");
    public static final Identifier SHOCK=Nullstar.id("textures/effect/shock.png");
    public static final Identifier SMOKE=Nullstar.id("textures/effect/smoke.png");
    public static final Identifier CRYSTAL=Nullstar.id("textures/effect/crystal.png");
    public static final Identifier RUBBLE=Nullstar.id("textures/effect/rubble.png");
    public static final Identifier VOID=Nullstar.id("textures/effect/void.png");
    public static final Identifier HALO=Nullstar.id("textures/effect/halo.png");
    /** Part 2.5B cosmic materials: point stars, nebula gas and the colossal stellar-lance fabric. */
    public static final Identifier STAR=Nullstar.id("textures/effect/star.png");
    public static final Identifier GAS=Nullstar.id("textures/effect/gas.png");
    public static final Identifier LANCE=Nullstar.id("textures/effect/lance.png");
    public static final Identifier COSMIC_HULL=Nullstar.id("textures/machine/cosmic_hull.png");
    /** Part 3: equirectangular world surface and cloud deck for the planetary act. */
    public static final Identifier PLANET=Nullstar.id("textures/effect/planet.png");
    public static final Identifier CLOUDS=Nullstar.id("textures/effect/clouds.png");
    public static final Identifier HELIOS_HULL=Nullstar.id("textures/machine/helios_hull.png");
    public static final Identifier GLASSFALL_HULL=Nullstar.id("textures/machine/glassfall_hull.png");
    public static final Identifier SINGULARITY_HULL=Nullstar.id("textures/machine/singularity_hull.png");

    private static final List<RenderPipeline> ALL=new ArrayList<>();
    private static final Map<String,RenderType> TYPES=new HashMap<>();
    private static RenderPipeline.Snippet energySnippet,smokeSnippet,hullSnippet;
    private static final Map<Energy,RenderPipeline> ENERGY=new HashMap<>();
    private static final Map<Energy,OitPipelineSet> ENERGY_OIT=new HashMap<>();
    private static RenderPipeline smoke,hull,debris;
    private static OitPipelineSet smokeOit;
    /** Part 3 cosmic pipelines: the opaque procedural sky dome, the procedural additive energy materials and baked hulls. */
    private static RenderPipeline cosmosSky,cosmosFx,bakedHull;
    private static OitPipelineSet cosmosFxOit;
    private static RenderType skyType,fxType;
    private static RenderPipeline ash;
    private static OitPipelineSet ashOit;
    private static Boolean available;
    private static boolean initialized;

    private NullstarMaterials() {}

    /** Declares pipelines before the first resource reload so they are precompiled with vanilla shaders. */
    public static void initialize() {
        if(initialized)return;
        initialized=true;
        energySnippet=RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
                .withVertexShader(Nullstar.id("core/energy")).withFragmentShader(Nullstar.id("core/energy"))
                .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
                .withVertexBinding(0,DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL)
                .withPrimitiveTopology(PrimitiveTopology.QUADS)
                .withCull(false)
                .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL,false))
                .buildSnippet();
        for(Energy mode:Energy.values()) {
            String define="MODE_"+mode.name(),name="energy_"+mode.name().toLowerCase();
            ENERGY.put(mode,optional(RenderPipeline.builder(energySnippet).withLocation(Nullstar.id("pipeline/"+name))
                    .withShaderDefine(define).withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING)).build()));
            var oit=OitPipelineSet.builder("nullstar_"+name,RenderPipeline.builder(energySnippet).withShaderDefine(define).withShaderDefine("OIT_ADDITIVE")).build();
            optional(oit.depthBoundsPipeline());optional(oit.transmittancePipeline());optional(oit.accumulatePipeline());
            ENERGY_OIT.put(mode,oit);
        }
        smokeSnippet=RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
                .withVertexShader(Nullstar.id("core/smoke")).withFragmentShader(Nullstar.id("core/smoke"))
                .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
                .withVertexBinding(0,DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL)
                .withPrimitiveTopology(PrimitiveTopology.QUADS)
                .withCull(false)
                .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL,false))
                .buildSnippet();
        smoke=optional(RenderPipeline.builder(smokeSnippet).withLocation(Nullstar.id("pipeline/smoke"))
                .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT)).build());
        smokeOit=OitPipelineSet.builder("nullstar_smoke",RenderPipeline.builder(smokeSnippet)).build();
        optional(smokeOit.depthBoundsPipeline());optional(smokeOit.transmittancePipeline());optional(smokeOit.accumulatePipeline());
        ash=optional(RenderPipeline.builder(smokeSnippet).withLocation(Nullstar.id("pipeline/ash")).withShaderDefine("NO_FOG")
                .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT)).build());
        ashOit=OitPipelineSet.builder("nullstar_ash",RenderPipeline.builder(smokeSnippet).withShaderDefine("NO_FOG")).build();
        optional(ashOit.depthBoundsPipeline());optional(ashOit.transmittancePipeline());optional(ashOit.accumulatePipeline());
        hullSnippet=RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
                .withVertexShader(Nullstar.id("core/hull")).withFragmentShader(Nullstar.id("core/hull"))
                .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
                .withVertexBinding(0,DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL)
                .withPrimitiveTopology(PrimitiveTopology.QUADS)
                .withCull(false)
                .withDepthStencilState(DepthStencilState.DEFAULT)
                .buildSnippet();
        hull=optional(RenderPipeline.builder(hullSnippet).withLocation(Nullstar.id("pipeline/hull")).withColorTargetState(ColorTargetState.DEFAULT).build());
        debris=optional(RenderPipeline.builder(hullSnippet).withLocation(Nullstar.id("pipeline/debris")).withShaderDefine("DEBRIS")
                .withColorTargetState(ColorTargetState.DEFAULT).build());
        bakedHull=optional(RenderPipeline.builder(hullSnippet).withLocation(Nullstar.id("pipeline/baked_hull")).withShaderDefine("BAKED")
                .withColorTargetState(ColorTargetState.DEFAULT).build());
        // The sky dome sits a fraction of a block from the eye and writes that depth, so it replaces every piece of the
        // Minecraft world behind it (all terrain beyond ~0.6 blocks) while the compressed cosmic scene, nearer still,
        // keeps winning the ordinary depth test whatever order the batches are drawn in.
        cosmosSky=optional(RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
                .withLocation(Nullstar.id("pipeline/cosmos_sky"))
                .withVertexShader(Nullstar.id("core/cosmos_sky")).withFragmentShader(Nullstar.id("core/cosmos_sky"))
                .withVertexBinding(0,DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL)
                .withPrimitiveTopology(PrimitiveTopology.QUADS)
                .withCull(false)
                .withDepthStencilState(DepthStencilState.DEFAULT)
                .withColorTargetState(ColorTargetState.DEFAULT).build());
        var fxSnippet=RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
                .withVertexShader(Nullstar.id("core/cosmos_fx")).withFragmentShader(Nullstar.id("core/cosmos_fx"))
                .withVertexBinding(0,DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL)
                .withPrimitiveTopology(PrimitiveTopology.QUADS)
                .withCull(false)
                .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL,false))
                .buildSnippet();
        cosmosFx=optional(RenderPipeline.builder(fxSnippet).withLocation(Nullstar.id("pipeline/cosmos_fx"))
                .withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING)).build());
        cosmosFxOit=OitPipelineSet.builder("nullstar_cosmos_fx",RenderPipeline.builder(fxSnippet).withShaderDefine("OIT_ADDITIVE")).build();
        optional(cosmosFxOit.depthBoundsPipeline());optional(cosmosFxOit.transmittancePipeline());optional(cosmosFxOit.accumulatePipeline());
    }
    private static RenderPipeline optional(RenderPipeline pipeline){ALL.add(pipeline);return RenderPipelines.registerOptional(pipeline);}

    /** Compiles (or finds) every custom pipeline once. Must be called on the render thread. */
    public static boolean customAvailable() {
        if(available==null) {
            boolean ok=initialized;
            for(var pipeline:ALL) {
                try { if(RenderSystem.getCompiledPipelineNullable(pipeline)==null){ok=false;break;} }
                catch(RuntimeException exception){ok=false;break;}
            }
            available=ok;
            if(ok)Nullstar.LOGGER.info("NULLSTAR custom render pipelines ready ({} programs)",ALL.size());
            else Nullstar.LOGGER.warn("NULLSTAR custom render pipelines unavailable; using vanilla render-type fallback");
        }
        return available;
    }
    /** Resource reloads recompile shaders; verify them again before the next use. */
    public static void invalidate(){available=null;}
    public static boolean verified(){return available!=null&&available;}
    public static boolean unavailable(){return available!=null&&!available;}

    private static java.util.function.Supplier<com.mojang.renderpearl.api.textures.GpuSampler> linearRepeat() {
        return ()->RenderSystem.getSamplerCache().getRepeat(FilterMode.LINEAR);
    }
    public static RenderType energy(Energy mode,Identifier texture) {
        if(!customAvailable())return RenderTypes.lightning();
        return TYPES.computeIfAbsent("energy/"+mode+"/"+texture,key->RenderType.create("nullstar_energy_"+mode.name().toLowerCase(),
                RenderSetup.builder(ENERGY.get(mode)).setOitPipelines(ENERGY_OIT.get(mode))
                        .withTexture("Sampler0",texture,linearRepeat()).sortOnUpload().createRenderSetup()));
    }
    public static RenderType smoke(Identifier texture) {
        if(!customAvailable())return RenderTypes.entityTranslucent(texture);
        return TYPES.computeIfAbsent("smoke/"+texture,key->RenderType.create("nullstar_smoke",
                RenderSetup.builder(smoke).setOitPipelines(smokeOit).withTexture("Sampler0",texture,linearRepeat()).sortOnUpload().createRenderSetup()));
    }
    public static RenderType hull(Identifier texture) {
        if(!customAvailable())return RenderTypes.entitySolid(texture);
        return TYPES.computeIfAbsent("hull/"+texture,key->RenderType.create("nullstar_hull",
                RenderSetup.builder(hull).withTexture("Sampler0",texture).createRenderSetup()));
    }
    /** Fog-free smoke for colossal ash columns, plumes and pressure walls. */
    public static RenderType ash(Identifier texture) {
        if(!customAvailable())return RenderTypes.entityTranslucent(texture);
        return TYPES.computeIfAbsent("ash/"+texture,key->RenderType.create("nullstar_ash",
                RenderSetup.builder(ash).setOitPipelines(ashOit).withTexture("Sampler0",texture,linearRepeat()).sortOnUpload().createRenderSetup()));
    }
    /** Procedural cosmos dome. Without custom shaders the dome falls back to a dark textured shell. */
    public static RenderType cosmosSky() {
        if(!customAvailable())return RenderTypes.entitySolid(GAS);
        if(skyType==null)skyType=RenderType.create("nullstar_cosmos_sky",RenderSetup.builder(cosmosSky).createRenderSetup());
        return skyType;
    }
    /** Procedural additive energy (galaxy, photosphere, corona, beam, nebula, atmosphere, shock, flare). */
    public static RenderType cosmosFx() {
        if(!customAvailable())return RenderTypes.lightning();
        if(fxType==null)fxType=RenderType.create("nullstar_cosmos_fx",RenderSetup.builder(cosmosFx).setOitPipelines(cosmosFxOit).sortOnUpload().createRenderSetup());
        return fxType;
    }
    /** Hull plating lit by the virtual scene's own light rig (baked into vertex colour) instead of the world sun. */
    public static RenderType bakedHull(Identifier texture) {
        if(!customAvailable())return RenderTypes.entitySolid(texture);
        return TYPES.computeIfAbsent("baked/"+texture,key->RenderType.create("nullstar_baked_hull",
                RenderSetup.builder(bakedHull).withTexture("Sampler0",texture).createRenderSetup()));
    }
    public static RenderType debris(Identifier texture) {
        if(!customAvailable())return RenderTypes.entitySolid(texture);
        return TYPES.computeIfAbsent("debris/"+texture,key->RenderType.create("nullstar_debris",
                RenderSetup.builder(debris).withTexture("Sampler0",texture).createRenderSetup()));
    }
}
