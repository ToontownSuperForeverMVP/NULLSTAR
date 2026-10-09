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
    private static final class Coverage {long last=Long.MIN_VALUE;}
    private record Entry(UUID strike, UUID owner, ResourceKey<Level> dimension, BlockPos target,
                         String weapon, boolean scorch, DestructionJob job, int wave, boolean recovered,Coverage coverage,com.nullstar.core.ScaleTier tier) {}
    private static final class Statistics { long total,scanned,changed,nanos,chunks,loadedChunks; int active; boolean planned; String cause="running"; }
    public record Progress(double fraction, long changed, int active) {}
    public record Diagnostics(UUID strike,long scanned,long total,long changed,int active,long chunks,long loadedChunks,
                              double milliseconds,int reserved,String cause) {}
    public record JobStatus(UUID strike,int wave,long scanned,long total,boolean recovered) {}
    public java.util.List<JobStatus> jobs(){return jobs.stream().map(e->new JobStatus(e.strike,e.wave,e.job.scanned(),e.job.total(),e.recovered)).toList();}
    private final ArrayDeque<Entry> jobs=new ArrayDeque<>();
    private final Map<UUID,Integer> reservations=new HashMap<>();
    private final Map<UUID,Statistics> statistics=new HashMap<>();
    private int lastTickOperations;
    private final Map<UUID,Diagnostics> history=new java.util.LinkedHashMap<>();
    private final java.util.Set<UUID> failures=new java.util.HashSet<>();
    public boolean reserve(UUID strike,int count,int limit) {
        if (count<0 || limit<0 || reservations.containsKey(strike) || statistics.containsKey(strike)
                || (long)jobs.size()+reservations.values().stream().mapToLong(Integer::longValue).sum()+count>limit) return false;
        reservations.put(strike,count);return true;
    }
    public void enqueue(UUID strike, UUID owner, ResourceKey<Level> dimension, BlockPos target,
                        String weapon, boolean scorch, CraterGeometry geometry) {
        enqueue(strike,owner,dimension,target,weapon,scorch,geometry,0);
    }
    public void plan(UUID strike,long candidates) {
        if(!reservations.containsKey(strike)||candidates<0)throw new IllegalArgumentException("Invalid destruction plan");
        var stats=statistics.computeIfAbsent(strike,key->new Statistics());stats.total=candidates;stats.planned=true;
    }
    public void enqueue(UUID strike, UUID owner, ResourceKey<Level> dimension, BlockPos target,
                        String weapon, boolean scorch, CraterGeometry geometry,int wave) {
        enqueue(strike,owner,dimension,target,weapon,scorch,geometry,wave,com.nullstar.core.ScaleTier.STANDARD);
    }
    public void enqueue(UUID strike, UUID owner, ResourceKey<Level> dimension, BlockPos target,
                        String weapon, boolean scorch, CraterGeometry geometry,int wave,com.nullstar.core.ScaleTier tier) {
        int reserved=reservations.getOrDefault(strike,0);
        if (reserved<=0) throw new IllegalStateException("Unreserved destruction work");
        if(wave<0||wave>7)throw new IllegalArgumentException("Invalid wave");
        var job=new DestructionJob(geometry,target.getX(),target.getZ());
        reservations.put(strike,reserved-1);
        jobs.add(new Entry(strike,owner,dimension,target.immutable(),weapon,scorch,job,wave,false,new Coverage(),tier));
        var stats=statistics.computeIfAbsent(strike,key->new Statistics());if(!stats.planned)stats.total+=job.total();stats.active++;
    }
    public void releaseReservation(UUID strike) {reservations.computeIfPresent(strike,(key,count)->Math.max(0,count-1));}
    public void releaseReservation(UUID strike,long skipped) {releaseReservation(strike);var stats=statistics.get(strike);if(stats!=null&&stats.planned)stats.scanned+=skipped;}
    public void tick(MinecraftServer server,ServerConfig config) {
        if(!config.terrainDestruction){lastTickOperations=0;return;}
        int left=config.blockOperationsPerTick;long deadline=System.nanoTime()+4_000_000L;int stalled=0;
        while (left>0 && !jobs.isEmpty() && System.nanoTime()<deadline) {
            var entry=jobs.removeFirst();var level=server.getLevel(entry.dimension);
            var stats=statistics.get(entry.strike);
            if(stats.changed>=config.maximumChangedBlocks) {stats.scanned+=entry.job.scanned();stats.active--;cancel(entry.strike,"changed-block cap");failures.add(entry.strike);stalled=0;continue;}
            if(level==null){stats.scanned+=entry.job.scanned();stats.active--;cancel(entry.strike,"missing level");failures.add(entry.strike);stalled=0;continue;}
            if(jobs.stream().anyMatch(e->e.strike.equals(entry.strike)&&e.wave<entry.wave)
                    || entry.recovered && (server.getPlayerList().getPlayer(entry.owner)==null
                    || !server.getPlayerList().getPlayer(entry.owner).isAlive()
                    || !server.getPlayerList().getPlayer(entry.owner).level().dimension().equals(entry.dimension)
                    || config.operatorsOnly&&!server.getPlayerList().getPlayer(entry.owner).permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER))) {
                jobs.addLast(entry);if(++stalled>=jobs.size())break;continue;
            }
            stalled=0;long start=System.nanoTime();
            int used=0;
            long before=entry.job.scanned();
            final long[] checkedChunk={Long.MIN_VALUE};final boolean[] available={false};
            try {
                if (level==null) entry.job.cancel();
                else used=entry.job.step(Math.min(left,64),(x,y,z,remove,scorch)-> {
                    BlockPos pos=entry.target.offset(x,y,z);
                    if (pos.getY()<level.getMinY() || pos.getY()>level.getMaxY() || !level.getWorldBorder().isWithinBounds(pos)
                            || !level.getChunkSource().hasChunk(pos.getX()>>4,pos.getZ()>>4)) return false;
                    if(stats.changed>=config.maximumChangedBlocks)return false;
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
                    boolean changed=level.setBlock(pos,replacement,Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE);
                    if(changed)stats.changed++;
                    return changed;
                },(x,z)-> {
                    int cx=(entry.target.getX()+x)>>4,cz=(entry.target.getZ()+z)>>4;
                    long key=((long)cx<<32)^(cz&0xffffffffL);
                    if(key!=checkedChunk[0]){checkedChunk[0]=key;available[0]=level.getChunkSource().hasChunk(cx,cz);}
                    boolean loaded=available[0];
                    if(key!=entry.coverage.last){entry.coverage.last=key;stats.chunks++;if(loaded)stats.loadedChunks++;}
                    return loaded;
                });
            } catch(RuntimeException exception) {
                used=(int)(entry.job.scanned()-before);
                Nullstar.LOGGER.error("Destruction failed for strike {}; cancelling its work",entry.strike,exception);
                stats.scanned+=entry.job.scanned();stats.active--;
                cancel(entry.strike,"terrain exception");failures.add(entry.strike);
                left-=used;continue;
            }
            left-=used;
            stats.nanos+=System.nanoTime()-start;
            if (entry.job.done()) {
                stats.scanned+=entry.job.scanned();stats.active--;
                if(entry.recovered&&stats.active==0)cancel(entry.strike,"recovered work complete");
            } else jobs.addLast(entry);
        }
        lastTickOperations=config.blockOperationsPerTick-left;
    }
    public Progress progress(UUID strike) {
        var stats=statistics.get(strike);if (stats==null)return new Progress(0,0,0);
        long scanned=stats.scanned;
        for (var entry:jobs) if(entry.strike.equals(strike)) scanned+=entry.job.scanned();
        return new Progress(stats.total==0?0:Math.min(1,scanned/(double)stats.total),stats.changed,stats.active);
    }
    public void cancel(UUID strike) {
        cancel(strike,"cancelled");
    }
    public void cancel(UUID strike,String cause) {
        var stats=statistics.get(strike);
        if(stats!=null) {
            stats.cause=cause;var current=diagnostics(strike);
            history.put(strike,new Diagnostics(current.strike,current.scanned,current.total,current.changed,0,current.chunks,current.loadedChunks,current.milliseconds,0,cause));
            while(history.size()>64)history.remove(history.keySet().iterator().next());
        }
        jobs.removeIf(entry->{if(!entry.strike.equals(strike))return false;entry.job.cancel();return true;});
        reservations.remove(strike);statistics.remove(strike);
    }
    public Diagnostics diagnostics(UUID strike) {
        var stats=statistics.get(strike);if(stats==null)return history.get(strike);
        long scanned=stats.scanned;for(var entry:jobs)if(entry.strike.equals(strike))scanned+=entry.job.scanned();
        return new Diagnostics(strike,scanned,stats.total,stats.changed,stats.active,stats.chunks,stats.loadedChunks,
                stats.nanos/1e6,reservations.getOrDefault(strike,0),stats.cause);
    }
    public java.util.Set<UUID> strikeIds(){return java.util.Set.copyOf(statistics.keySet());}
    public java.util.Set<UUID> ownerStrikes(UUID owner){return jobs.stream().filter(e->e.owner.equals(owner)).map(Entry::strike).collect(java.util.stream.Collectors.toUnmodifiableSet());}
    public java.util.Collection<Diagnostics> diagnostics(){var ids=new java.util.LinkedHashSet<>(history.keySet());ids.addAll(statistics.keySet());return ids.stream().map(this::diagnostics).toList();}
    private record SavedJob(UUID strike,UUID owner,String dimension,int x,int y,int z,String weapon,boolean scorch,int wave,com.nullstar.core.ScaleTier tier,DestructionJob.Checkpoint cursor) {}
    private record SavedStats(UUID strike,long scanned,long changed,long nanos,long chunks,long loadedChunks) {}
    private record Saved(int version,java.util.List<SavedJob> jobs,java.util.List<SavedStats> statistics) {}
    /** Atomic graceful-save checkpoint, not a transaction journal synchronized with chunk storage. */
    public void save(java.nio.file.Path path) throws java.io.IOException {
        var savedJobs=jobs.stream().map(e->new SavedJob(e.strike,e.owner,e.dimension.identifier().toString(),e.target.getX(),e.target.getY(),e.target.getZ(),e.weapon,e.scorch,e.wave,e.tier,e.job.checkpoint())).toList();
        var savedStats=statistics.entrySet().stream().filter(e->jobs.stream().anyMatch(j->j.strike.equals(e.getKey())))
                .map(e->new SavedStats(e.getKey(),e.getValue().scanned,e.getValue().changed,e.getValue().nanos,e.getValue().chunks,e.getValue().loadedChunks)).toList();
        java.nio.file.Files.createDirectories(path.getParent());var temporary=path.resolveSibling(path.getFileName()+".tmp");
        java.nio.file.Files.writeString(temporary,com.nullstar.config.ConfigFiles.JSON.toJson(new Saved(1,savedJobs,savedStats)));
        try{java.nio.file.Files.move(temporary,path,java.nio.file.StandardCopyOption.REPLACE_EXISTING,java.nio.file.StandardCopyOption.ATOMIC_MOVE);}
        catch(java.nio.file.AtomicMoveNotSupportedException exception){java.nio.file.Files.move(temporary,path,java.nio.file.StandardCopyOption.REPLACE_EXISTING);}
    }
    public void load(java.nio.file.Path path,ServerConfig config) throws java.io.IOException {
        if(!java.nio.file.Files.exists(path))return;
        if(java.nio.file.Files.size(path)>1_048_576)throw new java.io.IOException("Checkpoint exceeds size limit");
        Saved data=com.nullstar.config.ConfigFiles.JSON.fromJson(java.nio.file.Files.readString(path),Saved.class);
        if(data==null||data.version!=1||data.jobs==null||data.statistics==null||data.jobs.size()>config.maximumJobs||data.statistics.size()>32)
            throw new java.io.IOException("Invalid checkpoint schema or capacity");
        var restored=new DestructionManager();
        for(var saved:data.statistics) {
            if(saved.strike==null||saved.scanned<0||saved.scanned>1_000_000_000_000L||saved.changed<0||saved.changed>config.maximumChangedBlocks||saved.nanos<0||saved.chunks<0||saved.loadedChunks<0
                    ||restored.statistics.containsKey(saved.strike))
                throw new java.io.IOException("Invalid checkpoint statistics");
            var stats=new Statistics();stats.scanned=saved.scanned;stats.changed=saved.changed;stats.nanos=saved.nanos;stats.chunks=saved.chunks;stats.loadedChunks=saved.loadedChunks;
            stats.total=saved.scanned;restored.statistics.put(saved.strike,stats);
        }
        for(var saved:data.jobs) {
            if(saved.strike==null||saved.owner==null||!com.nullstar.core.WeaponCatalog.IDS.contains(saved.weapon)||saved.wave<0||saved.wave>=config.maximumDevastationWaves
                    ||saved.tier==null||saved.tier==com.nullstar.core.ScaleTier.APOCALYPSE&&!config.apocalypseEnabled
                    ||saved.tier!=com.nullstar.core.ScaleTier.STANDARD&&config.colossalCinematicOnly
                    ||Math.abs((long)saved.x)>30_000_000||Math.abs((long)saved.z)>30_000_000||Math.abs((long)saved.y)>2048
                    ||saved.cursor==null||saved.cursor.geometry().radius()>config.radiusCap(saved.tier)
                    ||saved.cursor.geometry().floor()>config.depthCap(saved.tier)
                    ||saved.cursor.originX()!=saved.x||saved.cursor.originZ()!=saved.z)throw new java.io.IOException("Checkpoint exceeds current safety policy");
            var job=DestructionJob.restore(saved.cursor);var stats=restored.statistics.get(saved.strike);
            if(stats==null)throw new java.io.IOException("Missing checkpoint statistics");
            var dimension=ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,net.minecraft.resources.Identifier.parse(saved.dimension));
            restored.jobs.add(new Entry(saved.strike,saved.owner,dimension,new BlockPos(saved.x,saved.y,saved.z),saved.weapon,saved.scorch,job,saved.wave,true,new Coverage(),saved.tier));
            stats.total+=job.total();stats.active++;restored.reservations.put(saved.strike,0);
        }
        long colossalCount=restored.jobs.stream().filter(e->e.tier!=com.nullstar.core.ScaleTier.STANDARD).map(Entry::strike).distinct().count();
        if(colossalCount>config.maximumSimultaneousColossalJobs)throw new java.io.IOException("Checkpoint exceeds colossal concurrency policy");
        for(var stat:restored.statistics.entrySet()) {
            long consumed=stat.getValue().scanned+restored.jobs.stream().filter(e->e.strike.equals(stat.getKey())).mapToLong(e->e.job.scanned()).sum();
            if(stat.getValue().changed>consumed||stat.getValue().active==0)throw new java.io.IOException("Invalid checkpoint progress accounting");
        }
        clear();jobs.addAll(restored.jobs);statistics.putAll(restored.statistics);reservations.putAll(restored.reservations);
    }
    public java.util.Set<UUID> drainFailures() {var result=java.util.Set.copyOf(failures);failures.clear();return result;}
    public void clear() {jobs.forEach(e->e.job.cancel());jobs.clear();reservations.clear();statistics.clear();history.clear();failures.clear();lastTickOperations=0;}
    public int queued() {return jobs.size();}
    public int lastTickOperations() {return lastTickOperations;}
}
