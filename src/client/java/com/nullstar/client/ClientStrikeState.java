package com.nullstar.client;

import com.nullstar.network.StatePayload;
import com.nullstar.network.VisualPayload;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Client presentation cache. Snapshot state never drives server damage or terrain changes. */
public final class ClientStrikeState {
    public record Strike(StatePayload state,long received) {
        public double phaseProgress(double tick){return Math.min(1,(state.phaseAge()+Math.max(0,tick-received))/(double)state.phaseDuration());}
        public double age(double tick){return state.age()+Math.max(0,tick-received);}
    }
    public record Cue(VisualPayload payload,long received) {}
    private final Map<UUID,Strike> strikes=new LinkedHashMap<>();
    private final Map<String,Cue> cues=new LinkedHashMap<>();
    private final Map<UUID,Integer> lastCue=new HashMap<>();
    public void snapshot(StatePayload payload,long tick) {
        if(payload.terminal()){strikes.remove(payload.id());lastCue.remove(payload.id());cues.values().removeIf(c->c.payload.strike().equals(payload.id()));return;}
        var prior=strikes.get(payload.id());
        if(prior!=null && prior.state.serverTick()>payload.serverTick())return;
        if(!strikes.containsKey(payload.id())&&strikes.size()>=8)return;
        strikes.put(payload.id(),new Strike(payload,tick));
    }
    public void cue(VisualPayload payload,long tick) {
        if(!strikes.containsKey(payload.strike()) || payload.index()<=lastCue.getOrDefault(payload.strike(),-1))return;
        lastCue.put(payload.strike(),payload.index());
        cues.put(payload.strike()+":"+payload.index(),new Cue(payload,tick));
    }
    public void tick(long tick,String dimension) {
        strikes.values().removeIf(s->tick-s.received>100 || !s.state.dimension().equals(dimension));
        cues.values().removeIf(c->tick-c.received>=c.payload.duration() || !strikes.containsKey(c.payload.strike()));
        lastCue.keySet().retainAll(strikes.keySet());
    }
    public Map<UUID,Strike> strikes(){return java.util.Collections.unmodifiableMap(strikes);}
    public java.util.Collection<Cue> cues(){return java.util.Collections.unmodifiableCollection(cues.values());}
    public void clear(){strikes.clear();cues.clear();lastCue.clear();}
}
