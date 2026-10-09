package com.nullstar.server;

import com.nullstar.core.Timeline;
import com.nullstar.core.WeaponDefinition;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import java.util.UUID;

/** Only WeaponEngine mutates active strike state, on the server thread. */
public final class ActiveStrike {
    public final UUID id,owner;
    public final ResourceKey<Level> dimension;
    public final BlockPos target;
    public final WeaponDefinition definition;
    public final Timeline timeline;
    public final long startTick,seed;
    public final boolean preview;
    public final int radius,depth;
    public int cueIndex;
    public ActiveStrike(UUID id,UUID owner,ResourceKey<Level> dimension,BlockPos target,WeaponDefinition definition,
                        long startTick,long seed,boolean preview,int radius,int depth) {
        this.id=id;this.owner=owner;this.dimension=dimension;this.target=target.immutable();this.definition=definition;
        this.timeline=new Timeline(definition);this.startTick=startTick;this.seed=seed;this.preview=preview;this.radius=radius;this.depth=depth;
    }
}
