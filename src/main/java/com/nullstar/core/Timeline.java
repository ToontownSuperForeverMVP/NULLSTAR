package com.nullstar.core;

import java.util.ArrayList;
import java.util.List;

/** Monotonic absolute ticks. A cue is delivered exactly once, even across delayed ticks. */
public final class Timeline {
    public record State(Phase phase, int phaseAge, int phaseDuration, int age) {}
    public record Advance(State state, boolean changed, List<WeaponDefinition.Cue> cues) {}
    private final WeaponDefinition definition;
    private int lastAge = -1;
    private Phase lastPhase = Phase.IDLE;
    private boolean cancelled;
    public Timeline(WeaponDefinition definition) { this.definition = definition; }
    public State at(int age) {
        if (cancelled) return new State(Phase.CANCELLED, 0, 1, Math.max(0, age));
        int start = 0;
        for (var stage : definition.phases()) {
            if (age < start + stage.ticks()) return new State(stage.phase(), Math.max(0, age - start), stage.ticks(), Math.max(0, age));
            start += stage.ticks();
        }
        return new State(Phase.COMPLETE, 0, 1, age);
    }
    public Advance advance(int age) {
        if (age < lastAge) throw new IllegalArgumentException("Timeline cannot rewind");
        List<WeaponDefinition.Cue> cues = new ArrayList<>();
        if (!cancelled) for (var cue : definition.events()) if (cue.tick() > lastAge && cue.tick() <= age) cues.add(cue);
        State state = at(age);
        boolean changed = state.phase() != lastPhase;
        lastAge = age; lastPhase = state.phase();
        return new Advance(state, changed, List.copyOf(cues));
    }
    public void cancel() { cancelled = true; }
}
