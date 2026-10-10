// Copyright (C) 2026 ToontownSuperForeverMVP and NULLSTAR contributors
// SPDX-License-Identifier: GPL-3.0-only

package com.nullstar.test;

import com.nullstar.Nullstar;
import com.nullstar.config.ConfigFiles;
import com.nullstar.config.ServerConfig;
import com.nullstar.core.CraterGeometry;
import com.nullstar.core.ScaleTier;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.Heightmap;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Part 3 real-terrain destruction measurements (opt-in suite {@code destruction}).
 *
 * <p>Scaled experiments first, then the full radius-500 Apocalypse through the real command path with physical
 * destruction explicitly enabled, all in a disposable seeded overworld under build/run. The server is advanced in
 * bounded batches as fast as it will run (accelerated, not real time); every result records the engine's own
 * diagnostics: candidates scanned/planned, blocks changed, chunks visited/loaded, ticks used, measured job time and
 * the worst server tick, plus the real-time duration those ticks represent at 20 TPS. Nothing here is synthetic:
 * the blocks really change.
 */
public final class DestructionScaleClientGameTest implements FabricClientGameTest {
    private static final Path OUT=Path.of("build/evidence/part3");
    private final List<Map<String,Object>> results=new ArrayList<>();

    @Override public void runTest(ClientGameTestContext context) {
        if(!VisualCaptureClientGameTest.selected("destruction"))return;
        this.context=context;
        var cfg=Nullstar.config();var previous=ConfigFiles.JSON.fromJson(ConfigFiles.JSON.toJson(cfg),ServerConfig.class);
        try(TestSingleplayerContext world=context.worldBuilder().setUseConsistentSettings(false)
                .adjustSettings(s->{s.setAllowCommands(true);s.setSeed("nullstar-part3");}).create()) {
            context.runOnClient(mc->mc.options.renderDistance().set(4));
            world.getConnection().waitForChunksDownload();
            context.runOnClient(mc->mc.options.renderDistance().set(12));
            world.getServer().runCommand("gamemode creative @p");
            context.waitTicks(200);
            world.getServer().runOnServer(server->{cfg.publicServerMode=false;cfg.apocalypseEnabled=true;cfg.requireColossalConfirmation=false;
                cfg.maximumColossalDiameter=1536;cfg.maximumColossalDepth=192;cfg.operatorsOnly=false;cfg.cooldownMultiplier=.01;});
            // Scaled experiments straight through the destruction manager at a real surface position near the player.
            direct(world,"basin_r64",64,24,40);
            direct(world,"basin_r160",160,48,-60);
            // The real thing: Apocalypse Helios through the command path, physical damage enabled.
            apocalypse(world);
            Files.createDirectories(OUT);
            Files.writeString(OUT.resolve("destruction-measurements.json"),ConfigFiles.JSON.toJson(results));
        } catch(Exception exception) {
            throw new RuntimeException(exception);
        } finally {
            try{for(var field:ServerConfig.class.getFields())field.set(cfg,field.get(previous));}
            catch(ReflectiveOperationException exception){throw new RuntimeException(exception);}
        }
    }

    private static BlockPos surface(TestSingleplayerContext world,int dx,int dz) {
        return world.getServer().computeOnServer(server->{
            var level=world.getConnection().getServerLevel();var player=world.getConnection().getServerPlayer();
            int x=(int)Math.floor(player.getX())+dx,z=(int)Math.floor(player.getZ())+dz;
            return new BlockPos(x,level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z),z);
        });
    }

    private void direct(TestSingleplayerContext world,String name,int radius,int depth,int offset) {
        BlockPos target=surface(world,offset,offset);
        UUID id=UUID.randomUUID();
        world.getServer().runOnServer(server->{
            var manager=Nullstar.engine().destruction();
            if(!manager.reserve(id,1,32))throw new AssertionError("reservation refused for "+name);
            manager.enqueue(id,world.getConnection().getServerPlayer().getUUID(),world.getConnection().getServerLevel().dimension(),target,
                    "helios",true,new CraterGeometry("basin",radius,depth,.04,7L),0,ScaleTier.APOCALYPSE);
        });
        run(world,name,id,target,radius,depth,false);
        // A direct job has no strike to retire it; move it to history so it cannot hold colossal capacity.
        world.getServer().runOnServer(server->Nullstar.engine().destruction().cancel(id,"experiment complete"));
    }

    private void apocalypse(TestSingleplayerContext world) {
        // Weapons target the surface block itself (non-air), not the air above it.
        BlockPos target=surface(world,100,0).below();
        // The same authorised activation path the command uses, called directly so a refusal is reported, not hidden.
        boolean admitted=world.getServer().computeOnServer(server->Nullstar.engine().activate(world.getConnection().getServerPlayer(),"helios",target,false,ScaleTier.APOCALYPSE));
        if(!admitted)throw new AssertionError("Apocalypse activation refused at "+target.toShortString()+"; active="+world.getServer().computeOnServer(server->Nullstar.engine().active().size())
                +" queued="+world.getServer().computeOnServer(server->Nullstar.engine().destruction().queued()));
        UUID id=world.getServer().computeOnServer(server->Nullstar.engine().active().iterator().next().id);
        // The strike reaches its impact cue through the real 3,600-tick timeline before any terrain is queued.
        world.getServer().runOnServer(server->{for(int i=0;i<2520;i++)Nullstar.engine().tick(server);});
        // Main pass over the whole 1,000-diameter footprint (unloaded tiles are deferred, not lost).
        run(world,"helios_apocalypse_r500_main_pass",id,target,500,96,false);
        // Natural loading: fly the player over the eastern rim so those regions load, then let the job catch up.
        int before=world.getServer().computeOnServer(server->Nullstar.engine().destruction().deferredTiles(id));
        long mainChanged=world.getServer().computeOnServer(server->Nullstar.engine().destruction().diagnostics(id).changed());
        world.getServer().runCommand("tp @p "+(target.getX()+380)+" "+(target.getY()+120)+" "+target.getZ());
        context.waitTicks(400);
        long[] ticks={0};
        for(int batch=0;batch<6;batch++)world.getServer().runOnServer(server->{for(int i=0;i<1000;i++){Nullstar.engine().tick(server);ticks[0]++;}});
        int after=world.getServer().computeOnServer(server->Nullstar.engine().destruction().deferredTiles(id));
        long abandoned=world.getServer().computeOnServer(server->Nullstar.engine().destruction().abandonedTiles(id));
        boolean alive=world.getServer().computeOnServer(server->Nullstar.engine().active().stream().anyMatch(s->s.id.equals(id)));
        var d=world.getServer().computeOnServer(server->Nullstar.engine().destruction().diagnostics(id));
        Map<String,Object> row=new LinkedHashMap<>();
        row.put("experiment","helios_apocalypse_r500_deferred_replay_after_natural_loading");
        row.put("strikeStillActive",alive);row.put("deferredTilesBefore",before);row.put("deferredTilesAfter",after);row.put("abandonedTiles",abandoned);
        row.put("tilesExcavatedFromDeferral",alive?before-after-abandoned:"n/a (strike ended)");
        row.put("blocksChangedBeforeReplay",mainChanged);
        row.put("replayedCells",world.getServer().computeOnServer(server->Nullstar.engine().destruction().replayedCells(id)));
        row.put("engineTicks",ticks[0]);if(d!=null)row.put("blocksChanged",d.changed());
        results.add(row);Nullstar.LOGGER.info("PART3 destruction: {}",row);
        world.getServer().runOnServer(server->Nullstar.engine().abort(id,server));
    }
    private ClientGameTestContext context;

    /** Advances the engine in bounded batches until the work completes or the wall-clock cap is reached. */
    private void run(TestSingleplayerContext world,String name,UUID id,BlockPos target,int radius,int depth,boolean strike) {
        long wallStart=System.nanoTime();
        long[] ticks={0},worst={0};
        boolean done=false;
        while(!done&&System.nanoTime()-wallStart<1_200_000_000_000L) {
            done=world.getServer().computeOnServer(server->{
                var engine=Nullstar.engine();
                for(int i=0;i<2000;i++) {
                    long start=System.nanoTime();
                    engine.tick(server);
                    worst[0]=Math.max(worst[0],System.nanoTime()-start);ticks[0]++;
                    // A finished main pass that only waits for unloaded regions counts as the measured end point.
                    boolean finished=strike?engine.active().isEmpty()&&engine.destruction().queued()==0
                            :engine.destruction().queued()==0||engine.destruction().jobs().stream().allMatch(j->j.scanned()>=j.total());
                    if(finished)return true;
                }
                return false;
            });
        }
        double wall=(System.nanoTime()-wallStart)/1e9;
        var d=world.getServer().computeOnServer(server->Nullstar.engine().destruction().diagnostics(id));
        int tickBudget=world.getServer().computeOnServer(server->Nullstar.config().blockOperationsPerTick);
        Map<String,Object> row=new LinkedHashMap<>();
        row.put("experiment",name);row.put("radius",radius);row.put("depth",depth);row.put("target",target.toShortString());
        row.put("completed",done);row.put("engineTicks",ticks[0]);row.put("realTimeAt20Tps_s",ticks[0]/20.0);row.put("wallClockAccelerated_s",wall);
        row.put("blockOperationsPerTick",tickBudget);row.put("worstEngineTick_ms",worst[0]/1e6);
        row.put("deferredTilesWaiting",world.getServer().computeOnServer(server->Nullstar.engine().destruction().deferredTiles(id)));
        row.put("replayedCells",world.getServer().computeOnServer(server->Nullstar.engine().destruction().replayedCells(id)));
        if(d!=null) {
            row.put("candidatesScanned",d.scanned());row.put("candidatesPlanned",d.total());row.put("blocksChanged",d.changed());
            row.put("chunkVisits",d.chunks());row.put("loadedChunkVisits",d.loadedChunks());
            row.put("loadedFraction",d.chunks()==0?0:d.loadedChunks()/(double)d.chunks());row.put("jobTime_ms",d.milliseconds());row.put("cause",d.cause());
        }
        Runtime rt=Runtime.getRuntime();row.put("heapUsed_MB",(rt.totalMemory()-rt.freeMemory())/1048576);
        results.add(row);
        Nullstar.LOGGER.info("PART3 destruction: {}",row);
    }
}
