package com.nullstar;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import java.util.LinkedHashMap;
import java.util.Map;

/** Original synthesized NULLSTAR sound events. Common registration lets weapon JSON phase sounds resolve on servers. */
public final class NullstarSounds {
    private static final Map<String,SoundEvent> EVENTS=new LinkedHashMap<>();
    public static final String[] IDS={
        "helios.activate","helios.charge","helios.align","helios.ring_lock","helios.pulse","helios.fire","helios.descent",
        "helios.impact","helios.aftershock","helios.aftermath","helios.shutdown",
        "singularity.activate","singularity.charge","singularity.pull","singularity.collapse","singularity.implode","singularity.aftermath",
        "glassfall.activate","glassfall.charge","glassfall.align","glassfall.barrage","glassfall.launch","glassfall.whistle",
        "glassfall.strike","glassfall.final","glassfall.aftermath",
        "ui.lock","ui.select"
    };
    private NullstarSounds() {}
    public static void initialize() {
        if(!EVENTS.isEmpty())return;
        for(String id:IDS)EVENTS.put(id,Registry.register(BuiltInRegistries.SOUND_EVENT,Nullstar.id(id),SoundEvent.createVariableRangeEvent(Nullstar.id(id))));
    }
    /** Returns the registered event, or null when the ID is not part of the bundled set. */
    public static SoundEvent get(String id){return EVENTS.get(id);}
}
