package com.nullstar.core;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Session replay/rate gate plus per-owner cooldown. Invoke only on the server thread. */
public final class ActivationGate {
    private record Session(long sequence, long nextRequest) {}
    private final Map<UUID, Session> sessions = new HashMap<>();
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    public boolean request(UUID owner, long sequence, long tick) {
        var prior = sessions.get(owner);
        if (sequence <= 0 || (prior != null && (sequence <= prior.sequence || tick < prior.nextRequest))) return false;
        sessions.put(owner, new Session(sequence, tick + 4)); return true;
    }
    public boolean ready(UUID owner, long tick) { return remaining(owner, tick) == 0; }
    public long remaining(UUID owner, long tick) { return Math.max(0, cooldowns.getOrDefault(owner, 0L) - tick); }
    public void activate(UUID owner, long tick, int cooldown) { cooldowns.put(owner, tick + cooldown); }
    public void disconnect(UUID owner) { sessions.remove(owner); }
    public void clear() { sessions.clear(); cooldowns.clear(); }
    public void prune(long tick) { cooldowns.values().removeIf(until -> until <= tick); }
}
