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
    private final ActivationGate gate=new ActivationGate();
    private final DestructionManager destruction=new DestructionManager();
    private long tick;
    public DestructionManager destruction(){return destruction;}
    public Collection<ActiveStrike> active(){return Collections.unmodifiableCollection(active.values());}
    public long tickCount(){return tick;}
    public String selected(UUID owner){return selections.getOrDefault(owner,"helios");}
    public boolean authorized(ServerPlayer player) {
        return !Nullstar.config().operatorsOnly || player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
    }
    public void request(ServerPlayer player,RequestPayload payload) {
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
        ServerConfig config=Nullstar.config(); UUID owner=player.getUUID();
        if(!WeaponCatalog.IDS.contains(weapon)){status(player,"Unknown weapon.");return false;}
        var definition=Nullstar.catalog().get(weapon).scaled(config.cinematicDurationMultiplier);
        if(!preview && !authorized(player)){status(player,"An operator is required to activate destructive weapons.");return false;}
        if(!TargetingService.valid(player,target,definition.targeting().range(),definition.targeting().groundRequired())
                || !ProtectionHooks.allows((net.minecraft.server.level.ServerLevel)player.level(),owner,target)) {
            status(player,"Target is out of range, protected, or unloaded.");return false;
        }
        if(!gate.ready(owner,tick)){status(player,"Weapon cooldown: "+(gate.remaining(owner,tick)+19)/20+"s.");return false;}
        if(active.values().stream().anyMatch(s->s.owner.equals(owner)) || active.size()>=config.maximumActiveStrikes) {
            status(player,"Strike already active or server capacity reached.");return false;
        }
        UUID id=UUID.randomUUID();int impacts=(int)definition.events().stream().filter(c->c.effect().equals("impact")).count();
        boolean cinematic=preview || !config.terrainDestruction;
        if(!destruction.reserve(id,cinematic?0:impacts,config.maximumJobs)){status(player,"Destruction queue is full.");return false;}
        var strike=new ActiveStrike(id,owner,player.level().dimension(),target,definition,tick,
                player.level().getRandom().nextLong(),cinematic,Math.min(config.maximumRadius,definition.impact().radius()),
                Math.min(config.maximumDepth,definition.impact().depth()));
        active.put(id,strike);targets.remove(owner);selections.put(owner,weapon);
        gate.activate(owner,tick,(int)Math.round(definition.cooldownTicks()*config.cooldownMultiplier));
        status(player,cinematic?"Preview launched: no damage or terrain changes.":"Strike authorized. Move clear of target.");
        broadcast(((net.minecraft.server.level.ServerLevel)player.level()).getServer(),snapshot(strike,strike.timeline.at(0),false));
        return true;
    }
    public void tick(MinecraftServer server) {
        tick++;gate.prune(tick);targets.entrySet().removeIf(e->e.getValue().expires<tick);
        Iterator<ActiveStrike> iterator=active.values().iterator();
        while(iterator.hasNext()) {
            ActiveStrike strike=iterator.next();var level=server.getLevel(strike.dimension);var owner=server.getPlayerList().getPlayer(strike.owner);
            if(level==null || owner==null || !owner.isAlive() || !owner.level().dimension().equals(strike.dimension)
                    || !level.getChunkSource().hasChunk(strike.target.getX()>>4,strike.target.getZ()>>4)) {
                finish(server,strike,true);iterator.remove();continue;
            }
            int age=(int)(tick-strike.startTick);var advance=strike.timeline.advance(age);var state=advance.state();
            if(advance.changed()) {
                strike.definition.phases().stream().filter(s->s.phase()==state.phase()).findFirst().ifPresent(stage->
                        SoundScheduler.phase(level,strike.target,stage,strike.definition,Nullstar.config()));
            }
            if(!strike.preview && strike.definition.behavior().equals("singularity") && state.phase()==Phase.FIRING && age%5==0)
                EntityEffects.attract(level,strike.owner,strike.target,strike.radius,Nullstar.config());
            for(var cue:advance.cues()) {
                BlockPos position=TargetingService.surfaceNear(level,strike.target.offset((int)Math.round(cue.x()),0,(int)Math.round(cue.z())));
                int radius=Math.max(1,(int)Math.round(strike.radius*cue.radiusScale()));
                int cueIndex=strike.cueIndex++;
                if(position==null){if(cue.effect().equals("impact")&&!strike.preview) releaseUnusedImpact(strike);continue;}
                broadcast(server,new VisualPayload(strike.id,cueIndex,cue.effect(),position,radius,
                        cue.effect().equals("projectile")?Math.max(1,(int)Math.round(30*Nullstar.config().cinematicDurationMultiplier)):40));
                if(cue.effect().equals("impact")&&!strike.preview) {
                    var impact=strike.definition.impact();
                    destruction.enqueue(strike.id,strike.owner,strike.dimension,position,strike.definition.behavior(),impact.scorch(),
                            new CraterGeometry(impact.shape(),radius,Math.max(1,(int)Math.round(strike.depth*cue.radiusScale())),impact.irregularity(),strike.seed+cueIndex));
                    EntityEffects.impact(level,strike.owner,position,radius,impact.damage()*(float)cue.radiusScale(),impact.knockback(),Nullstar.config());
                }
            }
            boolean done=state.phase()==Phase.COMPLETE && destruction.progress(strike.id).active()==0;
            if(done){finish(server,strike,false);iterator.remove();}
            else if(advance.changed() || tick%20==0)broadcast(server,snapshot(strike,state,false));
        }
        destruction.tick(server,Nullstar.config());
        if(tick%20==0)for(var player:server.getPlayerList().getPlayers()) {
            if(selections.containsKey(player.getUUID())) status(player,"");
            // Periodic resync handles late entry into a dimension and view range without huge replay streams.
        }
    }
    private void releaseUnusedImpact(ActiveStrike strike) {destruction.releaseReservation(strike.id);}
    private void finish(MinecraftServer server,ActiveStrike strike,boolean cancelled) {
        if(cancelled)strike.timeline.cancel();
        broadcast(server,snapshot(strike,new Timeline.State(cancelled?Phase.CANCELLED:Phase.COMPLETE,0,1,(int)Math.max(0,tick-strike.startTick)),true));
        var progress=destruction.progress(strike.id);
        Nullstar.LOGGER.info("Strike {} {}: {} changed blocks",strike.id,cancelled?"cancelled":"complete",progress.changed());
        destruction.cancel(strike.id);
    }
    private StatePayload snapshot(ActiveStrike s,Timeline.State state,boolean terminal) {
        String camera=s.definition.phases().stream().filter(p->p.phase()==state.phase()).map(WeaponDefinition.Stage::camera).findFirst().orElse("none");
        return new StatePayload(s.id,s.owner,s.dimension.identifier().toString(),s.definition.id(),state.phase(),state.age(),state.phaseAge(),state.phaseDuration(),
                tick,s.target,s.radius,s.depth,s.seed,s.preview,destruction.progress(s.id).fraction(),s.definition.presentation().color(),
                s.definition.presentation().orbitalHeight(),camera==null?"none":camera,terminal);
    }
    private void broadcast(MinecraftServer server,net.minecraft.network.protocol.common.custom.CustomPacketPayload payload) {
        if(server==null)return;
        for(var player:server.getPlayerList().getPlayers()) {
            ActiveStrike strike=payload instanceof StatePayload state?active.get(state.id()):payload instanceof VisualPayload visual?active.get(visual.strike()):null;
            if(strike!=null && !player.getUUID().equals(strike.owner)
                    && (!player.level().dimension().equals(strike.dimension) || player.blockPosition().distSqr(strike.target)>256*256))continue;
            if(ServerPlayNetworking.canSend(player,payload.type()))ServerPlayNetworking.send(player,payload);
        }
    }
    public void status(ServerPlayer player,String message) {
        var target=targets.get(player.getUUID());boolean has=target!=null && target.dimension.equals(player.level().dimension());
        if(ServerPlayNetworking.canSend(player,StatusPayload.TYPE))ServerPlayNetworking.send(player,new StatusPayload(selected(player.getUUID()),message,has,
                has?target.position:BlockPos.ZERO,(int)gate.remaining(player.getUUID(),tick)));
        if(!message.isEmpty())player.sendOverlayMessage(Component.literal("NULLSTAR: "+message));
    }
    public void join(ServerPlayer player) {
        status(player,"Controller ready: aim and use to designate a target.");
        for(var strike:active.values())if(strike.dimension.equals(player.level().dimension())&&ServerPlayNetworking.canSend(player,StatePayload.TYPE))
            ServerPlayNetworking.send(player,snapshot(strike,strike.timeline.at((int)(tick-strike.startTick)),false));
    }
    public void disconnect(UUID owner) {gate.disconnect(owner);selections.remove(owner);targets.remove(owner);}
    public int cancelOwner(UUID owner,MinecraftServer server) {
        int count=0;var iterator=active.values().iterator();
        while(iterator.hasNext()){var s=iterator.next();if(s.owner.equals(owner)){finish(server,s,true);iterator.remove();count++;}}
        return count;
    }
    public void stop(){active.clear();selections.clear();targets.clear();gate.clear();destruction.clear();tick=0;}
}
