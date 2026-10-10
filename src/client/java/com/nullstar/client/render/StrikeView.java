// Copyright (C) 2026 ToontownSuperForeverMVP and NULLSTAR contributors
// SPDX-License-Identifier: GPL-3.0-only

package com.nullstar.client.render;

import com.nullstar.client.ClientStrikeState;
import com.nullstar.core.Phase;
import com.nullstar.network.StatePayload;
import com.nullstar.network.VisualPayload;
import net.minecraft.world.phys.Vec3;
import java.util.List;

/**
 * Read-only presentation snapshot for one strike at one render instant. Derived solely from the server's
 * {@link StatePayload} and authoritative {@link VisualPayload} cues; nothing here feeds back into gameplay.
 *
 * @param age      strike age in ticks, including the partial tick
 * @param progress progress through the current phase, 0..1
 * @param events   authoritative cues received for this strike, in arrival order
 */
public record StrikeView(ClientStrikeState.Strike strike,StatePayload state,Phase phase,double progress,double age,
                         Vec3 target,float radius,float depth,float height,int color,long seed,boolean owned,
                         List<Event> events,double now) {
    /** One authoritative cue with its client arrival time. */
    public record Event(VisualPayload payload,double received) {
        public double age(double now){return now-received;}
        public double progress(double now){return Math.max(0,Math.min(1,(now-received)/payload.duration()));}
        public Vec3 position(){return Vec3.atBottomCenterOf(payload.position()).add(0,1,0);}
        public boolean impact(){return payload.effect().equals("impact");}
    }
    public String weapon(){return state.weapon();}
    public com.nullstar.core.ColossalMetadata scale(){return state.scale();}
    public com.nullstar.core.CinematicScene cinematicScene(){return state.scale().scene();}
    /** 0 before the phase, its progress during it, 1 once a later phase is active. Missing phases count as complete. */
    public double through(Phase stage) {
        if(phase==Phase.CANCELLED||phase==Phase.COMPLETE)return 1;
        int current=phase.ordinal(),wanted=stage.ordinal();
        return current>wanted?1:current<wanted?0:progress;
    }
    public boolean in(Phase stage){return phase==stage;}
    public boolean before(Phase stage){return phase.ordinal()<stage.ordinal();}
    public boolean atOrAfter(Phase stage){return phase.ordinal()>=stage.ordinal();}
    /** Ticks since the most recent impact cue, or -1. */
    public double sinceImpact() {
        for(int i=events.size()-1;i>=0;i--)if(events.get(i).impact())return events.get(i).age(now);
        return -1;
    }
    public Event lastImpact() {
        for(int i=events.size()-1;i>=0;i--)if(events.get(i).impact())return events.get(i);
        return null;
    }
    /** Deterministic per-strike pseudo random value in [0,1). */
    public double hash(int salt) {
        long x=seed^(salt*0x9E3779B97F4A7C15L);
        x=(x^(x>>>30))*0xBF58476D1CE4E5B9L;x=(x^(x>>>27))*0x94D049BB133111EBL;x^=x>>>31;
        return (x>>>11)*0x1.0p-53;
    }
    public static double smooth(double t){t=Math.max(0,Math.min(1,t));return t*t*(3-2*t);}
    public static double smoother(double t){t=Math.max(0,Math.min(1,t));return t*t*t*(t*(t*6-15)+10);}
    /** Maps [a,b] to [0,1] with clamping. */
    public static double range(double value,double a,double b){return Math.max(0,Math.min(1,(value-a)/(b-a)));}
}
