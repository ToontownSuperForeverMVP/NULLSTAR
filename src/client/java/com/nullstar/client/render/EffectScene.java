package com.nullstar.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.nullstar.client.ClientConfig;
import com.nullstar.client.ClientStrikeState;
import com.nullstar.client.NullstarClient;
import com.nullstar.client.presentation.PresentationLayers;
import com.nullstar.client.render.scene.GlassfallScene;
import com.nullstar.client.render.scene.HeliosScene;
import com.nullstar.client.render.scene.SingularityScene;
import com.nullstar.client.render.scene.TargetingScene;
import com.nullstar.client.render.scene.WeaponScene;
import com.nullstar.core.Phase;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Client effects engine. Ticks deterministic per-strike presentation from read-only snapshots and authoritative
 * cues, then extracts bounded custom geometry into the 26.3 submit pipeline. It is a {@link PresentationLayers.Layer}
 * so its lifecycle follows the existing presentation reset contract.
 */
public final class EffectScene implements PresentationLayers.Layer {
    private final Canvas canvas=new Canvas();
    private final CosmeticParticles particles=new CosmeticParticles();
    private final Map<UUID,List<StrikeView.Event>> events=new HashMap<>();
    private final Map<UUID,Integer> seenCue=new HashMap<>();
    private final Map<UUID,Phase> lastPhase=new HashMap<>();
    private final Map<String,WeaponScene> scenes=Map.of("helios",new HeliosScene(),"singularity",new SingularityScene(),"glassfall",new GlassfallScene());
    private final TargetingScene targeting=new TargetingScene();
    private final com.nullstar.client.render.scene.CosmicMontageScene montage=new com.nullstar.client.render.scene.CosmicMontageScene();
    private final PoseStack identity=new PoseStack();
    private int lastVertices,lastDropped,lastStrikes;
    private double frameMillis;
    private boolean failed;

    public CosmeticParticles particles(){return particles;}
    public List<StrikeView.Event> events(UUID strike){return events.getOrDefault(strike,List.of());}
    /** Diagnostics for performance evidence and the debug overlay line. */
    public String diagnostics() {
        return String.format("strikes=%d vertices=%d dropped=%d particles=%d peak=%d collect=%.3fms custom=%s",
                lastStrikes,lastVertices,lastDropped,particles.active(),particles.peak(),frameMillis,NullstarMaterials.verified());
    }
    public int lastVertices(){return lastVertices;}
    public double frameMillis(){return frameMillis;}

    public StrikeView view(ClientStrikeState.Strike strike,double now,UUID player) {
        var state=strike.state();
        Vec3 target=Vec3.atBottomCenterOf(state.target()).add(0,1,0);
        return new StrikeView(strike,state,state.phase(),strike.phaseProgress(now),strike.age(now),target,state.radius(),state.depth(),
                (float)state.orbitalHeight(),state.color(),state.seed(),state.owner().equals(player),events(state.id()),now);
    }

    /** Called once per client presentation tick with the whole read-only strike cache. */
    @Override public void tick(Minecraft minecraft,ClientStrikeState state,long tick) {
        if(minecraft.level==null)return;
        ClientConfig config=NullstarClient.config();
        UUID player=minecraft.player==null?null:minecraft.player.getUUID();
        for(var cue:state.cues()) {
            UUID id=cue.payload().strike();
            if(cue.payload().index()<=seenCue.getOrDefault(id,-1))continue;
            seenCue.put(id,cue.payload().index());
            var event=new StrikeView.Event(cue.payload(),cue.received());
            events.computeIfAbsent(id,key->new ArrayList<>()).add(event);
            var strike=state.strikes().get(id);var scene=strike==null?null:scenes.get(strike.state().weapon());
            if(scene!=null)scene.event(view(strike,tick,player),event,particles,minecraft.level,config.quality);
        }
        for(var entry:new ArrayList<>(lastPhase.entrySet())) {
            if(state.strikes().containsKey(entry.getKey()))continue;
            // A strike that vanished before its impact was cancelled: drop its pending cosmetic work immediately.
            if(entry.getValue().ordinal()<Phase.IMPACT.ordinal())particles.removeOwner(entry.getKey().hashCode());
            lastPhase.remove(entry.getKey());events.remove(entry.getKey());seenCue.remove(entry.getKey());
        }
        for(var strike:state.strikes().values()) {
            lastPhase.put(strike.state().id(),strike.state().phase());
            var scene=scenes.get(strike.state().weapon());
            if(scene!=null)scene.tick(view(strike,tick,player),particles,minecraft.level,config.quality);
        }
        particles.setCap(config.cosmeticParticles());
        particles.tick(minecraft.level);
    }
    @Override public void reset() {
        particles.clear();events.clear();seenCue.clear();lastPhase.clear();
        NullstarMaterials.invalidate();
        // A new level gets a fresh attempt; a persistent fault simply re-latches the native fallback.
        failed=false;
    }

    public void collect(LevelRenderContext context) {
        Minecraft mc=Minecraft.getInstance();
        if(mc.level==null||mc.player==null||failed)return;
        long start=System.nanoTime();
        try {
            var camera=context.levelState().cameraRenderState;
            ClientConfig config=NullstarClient.config();
            float partial=mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
            double now=NullstarClient.tickCount()+partial;
            canvas.begin(camera.pos,camera.orientation,config.glowIntensity);
            UUID player=mc.player.getUUID();
            int strikes=0;
            for(var strike:NullstarClient.STATE.strikes().values()) {
                var scene=scenes.get(strike.state().weapon());
                if(scene==null)continue;
                StrikeView view=view(strike,now,player);
                if(view.owned()&&mc.gui.screen()==null&&NullstarClient.config().cinematicCamera&&!NullstarClient.config().reducedMotion&&NullstarClient.CAMERA.detached(strike)) {
                    var forward=camera.orientation.transform(new org.joml.Vector3f(0,0,-1));
                    canvas.origin(camera.pos.add(forward.x*32,forward.y*32,forward.z*32));
                    montage.render(view,canvas,config.quality);strikes++;continue;
                }
                double reach=Math.max(view.radius()*3.5,48);
                AABB bounds=new AABB(view.target().x-reach,view.target().y-view.depth()-8,view.target().z-reach,
                        view.target().x+reach,view.target().y+view.height()+48,view.target().z+reach);
                if(!camera.cullFrustum.isVisible(bounds))continue;
                canvas.origin(view.target());
                scene.render(view,canvas,config.quality);
                strikes++;
            }
            boolean striking=NullstarClient.STATE.strikes().values().stream().anyMatch(s->s.state().owner().equals(player));
            if(NullstarClient.holdingController(mc)&&!striking)targeting.render(canvas,now,config.quality);
            particles.render(canvas,camera.pos,partial);
            for(Layer layer:Layer.values()) {
                VertexSink sink=canvas.sink(layer);
                if(!sink.isEmpty())context.submitNodeCollector().submitCustomGeometry(identity,layer.type.get(),(pose,consumer)->sink.flush(consumer));
            }
            lastStrikes=strikes;lastVertices=canvas.totalVertices();lastDropped=canvas.droppedVertices();
        } catch(RuntimeException exception) {
            // Presentation must never take the client down: disable the custom layer and keep the native fallback.
            failed=true;
            com.nullstar.Nullstar.LOGGER.error("NULLSTAR effects renderer failed; falling back to native particles",exception);
        }
        frameMillis=frameMillis*.9+(System.nanoTime()-start)/1e6*.1;
    }
    /** True when the custom renderer is unusable and the native particle fallback should carry the presentation. */
    public boolean nativeFallback() {
        return failed||NullstarClient.config().quality==ClientConfig.Quality.LOW||NullstarMaterials.unavailable();
    }
}
