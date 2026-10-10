package com.nullstar.server;

import com.nullstar.core.Timeline;
import com.nullstar.core.WeaponDefinition;
import com.nullstar.core.ScaleProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import java.util.UUID;

/** Only WeaponEngine mutates active strike state, on the server thread. */
public final class ActiveStrike {
    public final UUID id,owner;
    /** Part 3: the owner has been told that unloaded regions are waiting to be excavated. */
    public boolean deferralNotified;
    public final ResourceKey<Level> dimension;
    public final BlockPos target;
    public final WeaponDefinition definition;
    public final Timeline timeline;
    public final long startTick,seed;
    public final boolean preview;
    public final int radius,depth;
    public int cueIndex;
    public boolean committed;
    public String cancellationCause="event cancelled";
    public ScaleProfile scale;
    public int radiusCap,entityRadius,blastRadius;
    public ActiveStrike(UUID id,UUID owner,ResourceKey<Level> dimension,BlockPos target,WeaponDefinition definition,
                        long startTick,long seed,boolean preview,int radius,int depth) {
        this.id=id;this.owner=owner;this.dimension=dimension;this.target=target.immutable();this.definition=definition;
        this.timeline=new Timeline(definition);this.startTick=startTick;this.seed=seed;this.preview=preview;this.radius=radius;this.depth=depth;
        this.scale=ScaleProfile.standard(definition);this.radiusCap=radius;this.entityRadius=radius*2;this.blastRadius=radius;
    }
}
