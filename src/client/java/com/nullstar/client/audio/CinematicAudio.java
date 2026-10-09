package com.nullstar.client.audio;

import com.nullstar.NullstarSounds;
import com.nullstar.client.ClientStrikeState;
import com.nullstar.client.NullstarClient;
import com.nullstar.client.presentation.PresentationLayers;
import com.nullstar.client.render.StrikeView;
import com.nullstar.core.Phase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Client-side accent layers synchronized to authoritative phases and cues. The server still sends one phase sound
 * per transition; these layers add mechanical detail, per-projectile audio and distance-delayed aftershocks.
 * Triggers fire on phase-progress crossings and once per cue index, so state resyncs never replay them.
 */
public final class CinematicAudio implements PresentationLayers.Layer {
    /** One accent: plays when the strike crosses {@code at} (0..1) inside {@code phase}. */
    private record Accent(Phase phase,double at,String sound,float volume,float pitch,boolean orbital) {}
    private static final Map<String,List<Accent>> ACCENTS=Map.of(
            "helios",List.of(
                    new Accent(Phase.ORBITAL_ALIGNMENT,.35,"helios.ring_lock",3,1f,true),
                    new Accent(Phase.ORBITAL_ALIGNMENT,.48,"helios.ring_lock",2.4f,1.25f,true),
                    new Accent(Phase.ORBITAL_ALIGNMENT,.62,"helios.ring_lock",2.4f,.85f,true),
                    new Accent(Phase.ORBITAL_ALIGNMENT,.8,"helios.pulse",3,1f,true),
                    new Accent(Phase.ORBITAL_ALIGNMENT,.92,"helios.pulse",3.5f,1.15f,true)),
            "singularity",List.of(
                    new Accent(Phase.FIRING,.62,"singularity.collapse",3,.8f,false),
                    new Accent(Phase.FIRING,.8,"singularity.collapse",3.5f,.6f,false)),
            "glassfall",List.of(
                    new Accent(Phase.ORBITAL_ALIGNMENT,.95,"ui.lock",2,.7f,true)));
    private static final int MAX_INSTANCES=10;
    private final Map<UUID,Phase> lastPhase=new HashMap<>();
    private final Map<UUID,Double> lastProgress=new HashMap<>();
    private final Map<UUID,Integer> lastCue=new HashMap<>();
    private final Set<UUID> known=new HashSet<>();
    private final List<SoundInstance> playing=new ArrayList<>();
    private final RandomSource random=RandomSource.create();
    private boolean hadTarget;
    private String lastWeapon;
    private int played,skipped;

    public String diagnostics(){return "audioLayers played="+played+" capped="+skipped+" active="+playing.size();}

    @Override public void tick(Minecraft mc,ClientStrikeState state,long tick) {
        if(mc.level==null||mc.player==null)return;
        playing.removeIf(sound->!mc.getSoundManager().isActive(sound));
        double volume=NullstarClient.config().cinematicAudio;
        interfaceSounds(mc,volume);
        known.retainAll(state.strikes().keySet());
        lastPhase.keySet().retainAll(state.strikes().keySet());lastProgress.keySet().retainAll(state.strikes().keySet());lastCue.keySet().retainAll(state.strikes().keySet());
        for(var strike:state.strikes().values()) {
            UUID id=strike.state().id();
            StrikeView view=NullstarClient.SCENE.view(strike,tick,mc.player.getUUID());
            boolean first=known.add(id);
            Phase previous=lastPhase.put(id,view.phase());
            double before=previous==view.phase()?lastProgress.getOrDefault(id,0.0):0;
            lastProgress.put(id,view.progress());
            // A strike first seen mid-phase (late join) must not replay the accents it already passed.
            if(first&&view.progress()>0)before=view.progress();
            if(volume>0)for(var accent:ACCENTS.getOrDefault(view.weapon(),List.of())) {
                if(accent.phase!=view.phase()||accent.at<=before||accent.at>view.progress())continue;
                Vec3 at=view.target().add(0,accent.orbital?view.height():2,0);
                play(mc,accent.sound,at,(float)(accent.volume*volume),accent.pitch,0);
            }
            var events=NullstarClient.SCENE.events(id);
            Integer seen=lastCue.get(id);
            if(seen==null) {
                // First observation: everything already logged is history, not a fresh cue.
                seen=-1;
                for(var event:events)seen=Math.max(seen,event.payload().index());
            } else for(var event:events) {
                int index=event.payload().index();
                if(index<=seen)continue;
                seen=index;
                if(volume>0&&tick-event.received()<20)cue(mc,view,event,volume);
            }
            lastCue.put(id,seen);
        }
    }
    private void cue(Minecraft mc,StrikeView view,StrikeView.Event event,double volume) {
        Vec3 at=event.position();
        boolean small=event.payload().radius()<view.radius()*.8;
        switch(view.weapon()) {
            case "glassfall" -> {
                if(!event.impact()) {
                    play(mc,"glassfall.launch",view.target().add(0,view.height(),0),3.5f*(float)volume,small?1.05f:.8f,0);
                    play(mc,"glassfall.whistle",at.add(0,8,0),3*(float)volume,small?1:.75f,0);
                } else if(small) play(mc,"glassfall.strike",at,4*(float)volume,.92f+.16f*(float)view.hash(event.payload().index()),delay(mc,at));
                else play(mc,"helios.aftershock",at,3.5f*(float)volume,1.25f,delay(mc,at));
            }
            case "helios" -> {if(event.impact())play(mc,"helios.aftershock",at,4*(float)volume,1,delay(mc,at)+4);}
            case "singularity" -> {if(event.impact())play(mc,"helios.aftershock",at,4*(float)volume,.7f,delay(mc,at)+6);}
            default -> {}
        }
    }
    /** Sound travels about 17 blocks per tick; distant observers hear the low layer after the flash. */
    private static int delay(Minecraft mc,Vec3 source) {
        Vec3 listener=mc.gameRenderer.mainCamera().position();
        return (int)Math.min(30,listener.distanceTo(source)/17.15);
    }
    private void play(Minecraft mc,String id,Vec3 at,float volume,float pitch,int delay) {
        var event=NullstarSounds.get(id);
        if(event==null||volume<=0)return;
        if(playing.size()>=MAX_INSTANCES){skipped++;return;}
        var sound=new SimpleSoundInstance(event,SoundSource.PLAYERS,volume,pitch,random,at.x,at.y,at.z);
        if(delay>0)mc.getSoundManager().playDelayed(sound,delay);else mc.getSoundManager().play(sound);
        playing.add(sound);played++;
    }
    private void interfaceSounds(Minecraft mc,double volume) {
        var status=NullstarClient.status();
        boolean has=status.hasTarget();
        if(volume>0&&has&&!hadTarget)uiSound(mc,"ui.lock",1,volume);
        hadTarget=has;
        if(lastWeapon!=null&&!lastWeapon.equals(status.weapon())&&volume>0)uiSound(mc,"ui.select",1,volume);
        lastWeapon=status.weapon();
    }
    private void uiSound(Minecraft mc,String id,float pitch,double volume) {
        var event=NullstarSounds.get(id);
        if(event!=null)mc.getSoundManager().play(SimpleSoundInstance.forUI(event,pitch,(float)(.6*volume)));
    }
    @Override public void reset(){lastPhase.clear();lastProgress.clear();lastCue.clear();known.clear();playing.clear();hadTarget=false;}
}
