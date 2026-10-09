package com.nullstar.server;

import com.nullstar.Nullstar;
import com.nullstar.config.ServerConfig;
import com.nullstar.core.CraterGeometry;
import com.nullstar.core.DestructionJob;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Global round-robin budget across dimensions. Stores keys/positions, never retained chunk references. */
public final class DestructionManager {
    public static final TagKey<Block> PROTECTED=TagKey.create(Registries.BLOCK,Nullstar.id("protected_blocks"));
    private record Entry(UUID strike, UUID owner, ResourceKey<Level> dimension, BlockPos target,
                         String weapon, boolean scorch, DestructionJob job) {}
    private static final class Statistics { long total,scanned,changed; int active; }
    public record Progress(double fraction, long changed, int active) {}
    private final ArrayDeque<Entry> jobs=new ArrayDeque<>();
    private final Map<UUID,Integer> reservations=new HashMap<>();
    private final Map<UUID,Statistics> statistics=new HashMap<>();
    private int lastTickOperations;
    public boolean reserve(UUID strike,int count,int limit) {
        if (count<0 || jobs.size()+reservations.values().stream().mapToInt(Integer::intValue).sum()+count>limit) return false;
        reservations.put(strike,count);return true;
    }
    public void enqueue(UUID strike, UUID owner, ResourceKey<Level> dimension, BlockPos target,
                        String weapon, boolean scorch, CraterGeometry geometry) {
        int reserved=reservations.getOrDefault(strike,0);
        if (reserved<=0) throw new IllegalStateException("Unreserved destruction work");
        reservations.put(strike,reserved-1);
        var job=new DestructionJob(geometry);
        jobs.add(new Entry(strike,owner,dimension,target.immutable(),weapon,scorch,job));
        var stats=statistics.computeIfAbsent(strike,key->new Statistics());stats.total+=job.total();stats.active++;
    }
    public void releaseReservation(UUID strike) {reservations.computeIfPresent(strike,(key,count)->Math.max(0,count-1));}
    public void tick(MinecraftServer server,ServerConfig config) {
        int left=config.blockOperationsPerTick;long deadline=System.nanoTime()+4_000_000L;
        while (left>0 && !jobs.isEmpty() && System.nanoTime()<deadline) {
            var entry=jobs.removeFirst();var level=server.getLevel(entry.dimension);
            int used=0;
            if (level==null) entry.job.cancel();
            else used=entry.job.step(Math.min(left,64),(x,y,z,remove,scorch)-> {
                BlockPos pos=entry.target.offset(x,y,z);
                if (y < -config.maximumDepth || x*x+z*z > config.maximumRadius*config.maximumRadius) return false;
                if (pos.getY()<level.getMinY() || pos.getY()>level.getMaxY() || !level.getWorldBorder().isWithinBounds(pos)
                        || !level.getChunkSource().hasChunk(pos.getX()>>4,pos.getZ()>>4)) return false;
                var state=level.getBlockState(pos);
                var actor=server.getPlayerList().getPlayer(entry.owner);
                if (state.isAir() || state.getDestroySpeed(level,pos)<0 || state.is(PROTECTED) || state.hasBlockEntity()
                        || (actor!=null && !actor.mayInteract(level,pos))
                        || !ProtectionHooks.allows(level,entry.owner,pos)) return false;
                var replacement=Blocks.AIR.defaultBlockState();
                if (!remove) {
                    if (!scorch || !entry.scorch) return false;
                    replacement=BlockTransformations.scorch(entry.weapon,state);
                    if (replacement==state) return false;
                }
                return level.setBlock(pos,replacement,Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE);
            });
            left-=used;
            if (entry.job.done()) {
                var stats=statistics.get(entry.strike);stats.scanned+=entry.job.scanned();stats.changed+=entry.job.changed();stats.active--;
            } else jobs.addLast(entry);
        }
        lastTickOperations=config.blockOperationsPerTick-left;
    }
    public Progress progress(UUID strike) {
        var stats=statistics.get(strike);if (stats==null)return new Progress(0,0,0);
        long scanned=stats.scanned,changed=stats.changed;
        for (var entry:jobs) if(entry.strike.equals(strike)) {scanned+=entry.job.scanned();changed+=entry.job.changed();}
        return new Progress(stats.total==0?0:scanned/(double)stats.total,changed,stats.active);
    }
    public void cancel(UUID strike) {
        jobs.removeIf(entry->{if(!entry.strike.equals(strike))return false;entry.job.cancel();return true;});
        reservations.remove(strike);statistics.remove(strike);
    }
    public void clear() {jobs.forEach(e->e.job.cancel());jobs.clear();reservations.clear();statistics.clear();}
    public int queued() {return jobs.size();}
    public int lastTickOperations() {return lastTickOperations;}
}
