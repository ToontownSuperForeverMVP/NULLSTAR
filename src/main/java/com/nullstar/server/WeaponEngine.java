package com.nullstar.server;

import com.nullstar.Nullstar;
import com.nullstar.config.ServerConfig;
import com.nullstar.core.*;
import com.nullstar.network.*;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import java.util.*;

/** Server-authoritative gameplay service. Requests never supply a trusted target or blast radius. */
public final class WeaponEngine {
    private record Target(net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension,BlockPos position,long expires) {}
    private final Map<UUID,ActiveStrike> active=new LinkedHashMap<>();
    private final Map<UUID,String> selections=new HashMap<>();
    private final Map<UUID,Target> targets=new HashMap<>();
    private record Confirmation(String weapon,ScaleTier tier,Target target) {}
    private final Map<UUID,ScaleTier> scales=new HashMap<>();
    private final Map<UUID,Confirmation> confirmations=new HashMap<>();
    private final ActivationGate gate=new ActivationGate();
    private final DestructionManager destruction=new DestructionManager();
    private long tick;
    public DestructionManager destruction(){return destruction;}
    public Collection<ActiveStrike> active(){return Collections.unmodifiableCollection(active.values());}
    public long tickCount(){return tick;}
    public String selected(UUID owner){return selections.getOrDefault(owner,"helios");}
    public ScaleTier scale(UUID owner){return scales.getOrDefault(owner,Nullstar.config().defaultScale);}
    public void scale(ServerPlayer player,ScaleTier tier) {
        scales.put(player.getUUID(),tier);confirmations.remove(player.getUUID());targets.remove(player.getUUID());
        status(player,"Scale selected: "+tier+". Public safety caps "+(Nullstar.config().publicServerMode?"enabled.":"disabled."));
    }
    public boolean confirm(ServerPlayer player) {
        var pending=confirmations.remove(player.getUUID());
        if(pending==null||pending.target.expires<tick||!pending.target.dimension.equals(player.level().dimension())) {
            status(player,"No current colossal activation to confirm.");return false;
        }
        return activate(player,pending.weapon,pending.target.position,false,pending.tier,true);
    }
    public boolean authorized(ServerPlayer player) {
        return !Nullstar.config().operatorsOnly || player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
    }
    public void request(ServerPlayer player,RequestPayload payload) {
        try { requestUnchecked(player,payload); }
        catch(RuntimeException exception) {
            failRequest(player,exception,"Request failed safely; see server log.");
        }
    }
    private void requestUnchecked(ServerPlayer player,RequestPayload payload) {
        UUID owner=player.getUUID();
        if(!gate.request(owner,payload.sequence(),tick))return;
        if(payload.action()==RequestPayload.SKIP){status(player,"Cinematic skipped; strike continues.");return;}
        if(payload.action()==RequestPayload.CANCEL){cancelOwner(player.getUUID(),((net.minecraft.server.level.ServerLevel)player.level()).getServer());return;}
        if(!player.getMainHandItem().is(Nullstar.CONTROLLER) && !player.getOffhandItem().is(Nullstar.CONTROLLER))return;
        if(payload.action()==RequestPayload.SELECT) {
            if(WeaponCatalog.IDS.contains(payload.weapon())){selections.put(owner,payload.weapon());targets.remove(owner);status(player,"Weapon selected.");}
        } else if(payload.action()==RequestPayload.ACQUIRE) acquire(player);
        else if(payload.action()==RequestPayload.ACTIVATE) activateConfirmed(player,payload.preview());
    }
    public void useController(ServerPlayer player) {
        if(player.isShiftKeyDown()) {
            selections.put(player.getUUID(),Nullstar.catalog().next(selected(player.getUUID())).id());targets.remove(player.getUUID());
            status(player,"Weapon selected.");return;
        }
        if(targets.containsKey(player.getUUID())) activateConfirmed(player,false); else acquire(player);
    }
    public void acquire(ServerPlayer player) {
        var def=Nullstar.catalog().get(selected(player.getUUID()));
        BlockPos target=TargetingService.acquire(player,def.targeting().range(),def.targeting().groundRequired());
        if(target==null){targets.remove(player.getUUID());status(player,"Aim at a loaded solid surface within "+def.targeting().range()+" blocks.");return;}
        targets.put(player.getUUID(),new Target(player.level().dimension(),target,tick+200));status(player,"Target confirmed. Use again or press activation key.");
    }
    private void activateConfirmed(ServerPlayer player,boolean preview) {
        var target=targets.get(player.getUUID());
        if(target==null){acquire(player);return;}
        if(target.expires<tick || !target.dimension.equals(player.level().dimension())) {
            targets.remove(player.getUUID());status(player,"Target expired. Acquire again.");return;
        }
        activate(player,selected(player.getUUID()),target.position,preview);
    }
    /** Shared entry for commands/tests and item activation; repeats all server validation. */
    public boolean activate(ServerPlayer player,String weapon,BlockPos target,boolean preview) {
        return activate(player,weapon,target,preview,scale(player.getUUID()));
    }
    public boolean activate(ServerPlayer player,String weapon,BlockPos target,boolean preview,ScaleTier tier) {
        return activate(player,weapon,target,preview,tier,false);
    }
    private boolean activate(ServerPlayer player,String weapon,BlockPos target,boolean preview,ScaleTier tier,boolean confirmed) {
        try { return activateUnchecked(player,weapon,target,preview,tier,confirmed); }
        catch(RuntimeException exception) {
            failRequest(player,exception,"Activation failed safely; see server log.");return false;
        }
    }
    private void failRequest(ServerPlayer player,RuntimeException exception,String message) {
        Nullstar.LOGGER.error("NULLSTAR request/activation failed for {}",player.getUUID(),exception);
        cancelOwner(player.getUUID(),((net.minecraft.server.level.ServerLevel)player.level()).getServer());
        try { status(player,message); }
        catch(RuntimeException feedbackFailure) { Nullstar.LOGGER.error("Could not deliver failure status",feedbackFailure); }
    }
    private boolean activateUnchecked(ServerPlayer player,String weapon,BlockPos target,boolean preview,ScaleTier tier,boolean confirmed) {
        ServerConfig config=Nullstar.config(); UUID owner=player.getUUID();
        if(!WeaponCatalog.IDS.contains(weapon)){status(player,"Unknown weapon.");return false;}
        var base=Nullstar.catalog().get(weapon);var profile=Nullstar.scales().get(weapon,tier,base);
        var definition=profile.apply(base,config.cinematicDurationMultiplier);
        if(!authorized(player)){status(player,"An operator is required to activate weapons, including previews.");return false;}
        boolean cinematic=preview || !config.terrainDestruction || tier!=ScaleTier.STANDARD && config.colossalCinematicOnly;
        if(tier==ScaleTier.APOCALYPSE && !cinematic && !config.apocalypseEnabled) {status(player,"Apocalypse destruction is disabled; use preview or enable apocalypseEnabled.");return false;}
        if(!cinematic && profile.waves()>config.maximumDevastationWaves){status(player,"Profile exceeds the server devastation-wave limit.");return false;}
        if(!TargetingService.valid(player,target,definition.targeting().range(),definition.targeting().groundRequired())
                || !ProtectionHooks.allows((net.minecraft.server.level.ServerLevel)player.level(),owner,target)) {
            status(player,"Target is out of range, protected, or unloaded.");return false;
        }
        if(!gate.ready(owner,tick)){status(player,"Weapon cooldown: "+(gate.remaining(owner,tick)+19)/20+"s.");return false;}
        if(active.values().stream().anyMatch(s->s.owner.equals(owner)) || active.size()>=config.maximumActiveStrikes) {
            status(player,"Strike already active or server capacity reached.");return false;
        }
        if(tier!=ScaleTier.STANDARD && active.values().stream().filter(s->s.scale.tier()!=ScaleTier.STANDARD).count()
                +destruction.strikeIds().stream().filter(id->!active.containsKey(id)).count()>=config.maximumSimultaneousColossalJobs) {
            status(player,"Colossal event capacity reached.");return false;
        }
        if(!cinematic && tier!=ScaleTier.STANDARD && config.requireColossalConfirmation && !confirmed) {
            confirmations.put(owner,new Confirmation(weapon,tier,new Target(player.level().dimension(),target.immutable(),tick+200)));
            status(player,"Pending "+tier+" "+weapon+" at "+target.toShortString()+"; diameter "+Math.min(profile.radius(),config.radiusCap(tier))*2
                    +". Run /nullstar confirm within 10 seconds.");return false;
        }
        UUID id=UUID.randomUUID();int impacts=(int)definition.events().stream().filter(c->c.effect().equals("impact")).count();
        if(!destruction.reserve(id,cinematic?0:impacts,config.maximumJobs)){status(player,"Destruction queue is full.");return false;}
        int radius=Math.min(config.radiusCap(tier),definition.impact().radius());
        if(definition.impact().shape().equals("sphere"))radius=Math.min(radius,config.depthCap(tier));
        var strike=new ActiveStrike(id,owner,player.level().dimension(),target,definition,tick,
                player.level().getRandom().nextLong(),cinematic,radius,
                Math.min(config.depthCap(tier),definition.impact().depth()));
        strike.scale=profile;strike.radiusCap=config.radiusCap(tier);
        strike.entityRadius=Math.min(profile.entityRadius(),config.maximumEntityRadius);
        strike.blastRadius=Math.min(profile.blastRadius(),config.maximumEntityRadius);
        if(config.publicServerMode&&tier!=ScaleTier.STANDARD){strike.entityRadius=Math.min(strike.entityRadius,config.publicMaximumRadius);strike.blastRadius=Math.min(strike.blastRadius,config.publicMaximumRadius);}
        if(!cinematic)destruction.plan(id,definition.events().stream().filter(c->c.effect().equals("impact")).mapToLong(c->{
            var geometry=geometry(strike,c);return geometry==null?0:new DestructionJob(geometry).total();
        }).sum());
        active.put(id,strike);targets.remove(owner);confirmations.remove(owner);selections.put(owner,weapon);
        gate.activate(owner,tick,(int)Math.round(definition.cooldownTicks()*config.cooldownMultiplier));
        status(player,cinematic?"Preview launched: no damage or terrain changes.":"Strike authorized. Move clear of target.");
        broadcast(((net.minecraft.server.level.ServerLevel)player.level()).getServer(),snapshot(strike,strike.timeline.at(0),false));
        return true;
    }
    public void tick(MinecraftServer server) {
        tick++;gate.prune(tick);targets.entrySet().removeIf(e->e.getValue().expires<tick);
        confirmations.entrySet().removeIf(e->e.getValue().target.expires<tick);
        Iterator<ActiveStrike> iterator=active.values().iterator();
        while(iterator.hasNext()) {
            ActiveStrike strike=iterator.next();
            try {
                var level=server.getLevel(strike.dimension);var owner=server.getPlayerList().getPlayer(strike.owner);
                if(level==null || owner==null || !owner.isAlive() || !owner.level().dimension().equals(strike.dimension)
                        || !level.getChunkSource().hasChunk(strike.target.getX()>>4,strike.target.getZ()>>4)) {
                    strike.cancellationCause=level==null?"missing level":owner==null?"owner disconnected":!owner.isAlive()?"owner died"
                            :!owner.level().dimension().equals(strike.dimension)?"owner changed dimension":"target chunk unloaded";
                    finish(server,strike,true);iterator.remove();continue;
                }
                int age=(int)(tick-strike.startTick);var advance=strike.timeline.advance(age);var state=advance.state();
                if(advance.changed()) {
                    strike.definition.phases().stream().filter(s->s.phase()==state.phase()).findFirst().ifPresent(stage->
                            SoundScheduler.phase(level,strike.target,stage,strike.definition,Nullstar.config()));
                }
                if(!strike.preview && strike.definition.behavior().equals("singularity") && state.phase()==Phase.FIRING && age%5==0)
                    EntityEffects.attractRadius(level,strike.owner,strike.target,strike.entityRadius,Nullstar.config());
                for(var cue:advance.cues()) {
                    int radius=Math.max(1,Math.min(strike.radiusCap,(int)Math.round(strike.radius*cue.radiusScale())));
                    var offset=strike.scale.tier()==ScaleTier.STANDARD?new ImpactFootprint.Offset((int)Math.round(cue.x()),(int)Math.round(cue.z()))
                            :ImpactFootprint.offset(cue.x(),cue.z(),strike.scale.radius(),strike.radius,radius,strike.radiusCap);
                    BlockPos position=TargetingService.surfaceNear(level,strike.target.offset(offset.x(),0,offset.z()));
                    int cueIndex=strike.cueIndex++;
                    if(position==null){if(cue.effect().equals("impact")&&!strike.preview) releaseUnusedImpact(strike,cue);continue;}
                    broadcast(server,new VisualPayload(strike.id,cueIndex,cue.effect(),position,radius,
                            cue.effect().equals("projectile")?Math.max(1,(int)Math.round(30*Nullstar.config().cinematicDurationMultiplier)):40));
                    if(cue.effect().equals("impact")&&!strike.preview) {
                        var impact=strike.definition.impact();
                        var geometry=geometry(strike,cue);
                        if(geometry==null){releaseUnusedImpact(strike,cue);continue;}
                        strike.committed=true;
                        destruction.enqueue(strike.id,strike.owner,strike.dimension,position,strike.definition.behavior(),impact.scorch(),
                                geometry,cue.wave(),strike.scale.tier());
                        EntityEffects.impactRadius(level,strike.owner,position,Math.max(1,strike.blastRadius*Math.min(1,cue.radiusScale()))*(strike.scale.tier()==ScaleTier.STANDARD?1.5:1),
                                impact.damage()*(float)Math.min(1,cue.radiusScale()),impact.knockback(),Nullstar.config());
                    }
                }
                boolean done=state.phase()==Phase.COMPLETE && destruction.progress(strike.id).active()==0;
                if(done){finish(server,strike,false);iterator.remove();}
                else if(advance.changed() || tick%20==0)broadcast(server,snapshot(strike,state,false));
            } catch(RuntimeException exception) {
                Nullstar.LOGGER.error("Strike {} failed; cancelling only this strike",strike.id,exception);
                strike.cancellationCause="timeline exception";
                finish(server,strike,true);iterator.remove();
            }
        }
        destruction.tick(server,Nullstar.config());
        if(tick%200==0)checkpoint(server);
        for(UUID failed:destruction.drainFailures()) {
            var strike=active.get(failed);
            if(strike!=null){var diagnostic=destruction.diagnostics(failed);strike.cancellationCause=diagnostic==null?"terrain failure":diagnostic.cause();finish(server,strike,true);active.remove(failed);}
        }
        if(tick%20==0)for(var player:server.getPlayerList().getPlayers()) {
            if(selections.containsKey(player.getUUID())) status(player,"");
            // Periodic resync handles late entry into a dimension and view range without huge replay streams.
        }
    }
    private static CraterGeometry geometry(ActiveStrike strike,WeaponDefinition.Cue cue) {
        return CraterGeometry.forCue(strike.definition.impact(),strike.radius,strike.depth,strike.radiusCap,strike.seed,cue);
    }
    private void releaseUnusedImpact(ActiveStrike strike,WeaponDefinition.Cue cue) {
        var geometry=geometry(strike,cue);destruction.releaseReservation(strike.id,geometry==null?0:new DestructionJob(geometry).total());
    }
    private void finish(MinecraftServer server,ActiveStrike strike,boolean cancelled) {
        try {
            if(cancelled)strike.timeline.cancel();
            broadcast(server,snapshot(strike,new Timeline.State(cancelled?Phase.CANCELLED:Phase.COMPLETE,0,1,(int)Math.max(0,tick-strike.startTick)),true));
            var progress=destruction.progress(strike.id);
            Nullstar.LOGGER.info("Strike {} {}: {} changed blocks",strike.id,cancelled?"cancelled":"complete",progress.changed());
        } catch(RuntimeException exception) {
            Nullstar.LOGGER.error("Could not publish terminal state for strike {}",strike.id,exception);
        } finally { destruction.cancel(strike.id,cancelled?strike.cancellationCause:"complete"); }
    }
    private StatePayload snapshot(ActiveStrike s,Timeline.State state,boolean terminal) {
        String camera=s.definition.phases().stream().filter(p->p.phase()==state.phase()).map(WeaponDefinition.Stage::camera).findFirst().orElse("none");
        return new StatePayload(s.id,s.owner,s.dimension.identifier().toString(),s.definition.id(),s.definition.name(),state.phase(),state.age(),state.phaseAge(),state.phaseDuration(),
                tick,s.target,s.radius,s.depth,s.seed,s.preview,destruction.progress(s.id).fraction(),s.definition.presentation().color(),
                s.definition.presentation().orbitalHeight(),s.definition.presentation().particleScale(),camera==null?"none":camera,terminal,
                new ColossalMetadata(s.scale.tier(),CinematicScene.forPhase(state.phase(),s.definition.id(),s.scale.tier()),s.scale.cinematicScale(),s.scale.structureScale(),
                        s.scale.beamWidth(),s.scale.shockwaveRadius(),Math.min(s.radiusCap,(int)Math.ceil(s.radius*s.scale.secondaryRadius()/ (double)s.scale.radius())),
                        s.blastRadius,s.entityRadius,s.scale.script(),s.scale.anchor(),s.scale.alignmentNodes(),s.scale.subimpacts(),s.radius,s.scale.waves(),
                        Math.max(0,s.definition.phases().stream().map(WeaponDefinition.Stage::phase).toList().indexOf(state.phase())),s.definition.duration(),s.committed));
    }
    private int observerRange(ActiveStrike strike){return strike.scale.tier()==ScaleTier.STANDARD?256:Nullstar.config().colossalObserverRange;}
    private void broadcast(MinecraftServer server,net.minecraft.network.protocol.common.custom.CustomPacketPayload payload) {
        if(server==null)return;
        ActiveStrike strike=payload instanceof StatePayload state?active.get(state.id()):payload instanceof VisualPayload visual?active.get(visual.strike()):null;
        if(strike==null)return;
        for(var player:server.getPlayerList().getPlayers()) {
            if(!player.getUUID().equals(strike.owner)
                    && (!player.level().dimension().equals(strike.dimension) || player.blockPosition().distSqr(strike.target)>(long)observerRange(strike)*observerRange(strike)))continue;
            if(ServerPlayNetworking.canSend(player,payload.type()))ServerPlayNetworking.send(player,payload);
        }
    }
    public void status(ServerPlayer player,String message) {
        var target=targets.get(player.getUUID());boolean has=target!=null && target.dimension.equals(player.level().dimension());
        if(ServerPlayNetworking.canSend(player,StatusPayload.TYPE))ServerPlayNetworking.send(player,new StatusPayload(selected(player.getUUID()),Nullstar.catalog().get(selected(player.getUUID())).name(),message,has,
                has?target.position:BlockPos.ZERO,(int)gate.remaining(player.getUUID(),tick)));
        if(!message.isEmpty())player.sendOverlayMessage(Component.literal("NULLSTAR: "+message));
    }
    public void join(ServerPlayer player) {
        status(player,"Controller ready: aim and use to designate a target.");
        for(var strike:active.values())if(strike.dimension.equals(player.level().dimension())
                && (strike.owner.equals(player.getUUID()) || player.blockPosition().distSqr(strike.target)<=(long)observerRange(strike)*observerRange(strike))
                && ServerPlayNetworking.canSend(player,StatePayload.TYPE))
            ServerPlayNetworking.send(player,snapshot(strike,strike.timeline.at((int)(tick-strike.startTick)),false));
    }
    public void disconnect(UUID owner) {gate.disconnect(owner);selections.remove(owner);targets.remove(owner);scales.remove(owner);confirmations.remove(owner);}
    public int cancelOwner(UUID owner,MinecraftServer server) {
        confirmations.remove(owner);targets.remove(owner);
        int count=0;var iterator=active.values().iterator();
        while(iterator.hasNext()){var s=iterator.next();if(s.owner.equals(owner)){finish(server,s,true);iterator.remove();count++;}}
        for(UUID id:destruction.ownerStrikes(owner)){destruction.cancel(id,"owner cancelled recovered work");count++;}
        if(count>0)checkpoint(server);
        return count;
    }
    public boolean abort(UUID id,MinecraftServer server) {
        var strike=active.remove(id);
        if(strike!=null){active.put(id,strike);strike.cancellationCause="operator abort";finish(server,strike,true);active.remove(id);checkpoint(server);return true;}
        if(!destruction.strikeIds().contains(id))return false;destruction.cancel(id,"operator abort");checkpoint(server);return true;
    }
    public int emergencyAbort(MinecraftServer server) {
        var ids=new HashSet<>(destruction.strikeIds());ids.addAll(active.keySet());ids.forEach(id->abort(id,server));confirmations.clear();checkpoint(server);return ids.size();
    }
    private static java.nio.file.Path checkpointPath(MinecraftServer server){return server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve("data/nullstar-jobs-v1.json");}
    public void start(MinecraftServer server) {
        if(!Nullstar.config().persistDestructionJobs||!Nullstar.config().terrainDestruction)return;
        try{destruction.load(checkpointPath(server),Nullstar.config());Nullstar.LOGGER.info("Restored {} NULLSTAR terrain jobs; waiting for their owners",destruction.queued());}
        catch(Exception exception){Nullstar.LOGGER.error("Terrain checkpoint rejected; no work resumed",exception);}
    }
    public void checkpoint(MinecraftServer server) {
        if(!Nullstar.config().persistDestructionJobs)return;
        try{destruction.save(checkpointPath(server));}catch(Exception exception){Nullstar.LOGGER.error("Could not save NULLSTAR terrain checkpoints",exception);}
    }
    public void stop(){active.clear();selections.clear();targets.clear();scales.clear();confirmations.clear();gate.clear();destruction.clear();tick=0;}
}
