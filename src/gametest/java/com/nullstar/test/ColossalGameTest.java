package com.nullstar.test;

import com.nullstar.Nullstar;
import com.nullstar.config.*;
import com.nullstar.core.*;
import com.nullstar.server.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import java.util.UUID;

public final class ColossalGameTest {
    private static void restore(ServerConfig destination,ServerConfig previous) {
        try{for(var field:ServerConfig.class.getFields())field.set(destination,field.get(previous));}
        catch(ReflectiveOperationException exception){throw new RuntimeException(exception);}
    }
    @GameTest(maxTicks=30)
    public void colossalAdmissionConfirmationCapacityAndInertPreview(GameTestHelper helper) {
        var cfg=Nullstar.config();var previous=ConfigFiles.JSON.fromJson(ConfigFiles.JSON.toJson(cfg),ServerConfig.class);
        var engine=new WeaponEngine();var server=helper.getLevel().getServer();var owner=helper.makeMockServerPlayerInLevel();var other=helper.makeMockServerPlayerInLevel();
        var target=helper.absolutePos(new BlockPos(3,30,3));helper.setBlock(3,30,3,Blocks.STONE);
        owner.setPos(net.minecraft.world.phys.Vec3.atCenterOf(target).add(2,2,2));other.setPos(owner.position());
        try {
            cfg.operatorsOnly=false;cfg.publicServerMode=false;cfg.apocalypseEnabled=false;cfg.requireColossalConfirmation=true;
            helper.assertFalse(engine.activate(owner,"helios",target,false,ScaleTier.APOCALYPSE),"Disabled apocalypse admitted destructive work");
            cfg.apocalypseEnabled=true;
            helper.assertFalse(engine.activate(owner,"helios",target,false,ScaleTier.APOCALYPSE),"Confirmation was bypassed");
            helper.assertTrue(engine.active().isEmpty()&&engine.destruction().queued()==0,"Pending confirmation consumed destructive capacity");
            helper.assertTrue(engine.confirm(owner),"Bound confirmation rejected");
            var strike=engine.active().iterator().next();helper.assertTrue(strike.radius==500&&strike.depth==96&&strike.definition.duration()>=1800,"Mega-crater or long timeline not admitted");
            helper.assertFalse(engine.confirm(owner),"Confirmation replay admitted work");
            helper.assertTrue(engine.abort(strike.id,server),"Abort failed");
            cfg.maximumJobs=2;cfg.requireColossalConfirmation=false;
            helper.assertFalse(engine.activate(other,"glassfall",target,false,ScaleTier.APOCALYPSE),"Barrage overcommitted reservations");
            cfg.maximumJobs=32;cfg.maximumSimultaneousColossalJobs=1;
            helper.assertTrue(engine.activate(other,"helios",target,true,ScaleTier.APOCALYPSE),"Cinematic-only colossal preview rejected");
            for(int i=0;i<3610;i++)engine.tick(server);
            helper.assertTrue(engine.active().isEmpty()&&engine.destruction().queued()==0&&helper.getLevel().getBlockState(target).is(Blocks.STONE),"Colossal preview changed terrain or did not finish");
            cfg.publicServerMode=true;cfg.publicMaximumRadius=2;cfg.maximumDepth=2;
            helper.assertTrue(engine.activate(owner,"glassfall",target,false,ScaleTier.APOCALYPSE),"Public-clipped colossal barrage rejected");
            for(int i=0;i<2200;i++)engine.tick(server);
            helper.assertTrue(engine.active().isEmpty()&&engine.destruction().queued()==0&&helper.getLevel().getBlockState(target).isAir(),"Public-clipped barrage did not complete its real center crater");
            Nullstar.LOGGER.info("PART25 runtime: radius-500 admission, confirmation/replay, queue rejection, complete 3600-tick inert preview passed");
        } finally {engine.stop();restore(cfg,previous);server.getPlayerList().remove(owner);server.getPlayerList().remove(other);}
        helper.succeed();
    }
    @GameTest(maxTicks=30)
    public void stagedWorldJobsResumeTheirCursorAndRespectChangedCap(GameTestHelper helper) {
        var cfg=new ServerConfig();cfg.blockOperationsPerTick=16;cfg.publicServerMode=false;cfg.operatorsOnly=false;
        var manager=new DestructionManager();var id=UUID.randomUUID();var level=helper.getLevel();var owner=helper.makeMockServerPlayerInLevel();
        var target=helper.absolutePos(new BlockPos(3,8,3));helper.setBlock(3,8,3,Blocks.STONE);helper.setBlock(4,8,3,Blocks.STONE);
        manager.reserve(id,2,2);manager.enqueue(id,owner.getUUID(),level.dimension(),target,"helios",true,new CraterGeometry("basin",2,2,0,1),0);
        manager.enqueue(id,owner.getUUID(),level.dimension(),target,"helios",true,new CraterGeometry("ring",4,2,0,1,2.1),1);
        manager.tick(level.getServer(),cfg);
        helper.assertTrue(manager.jobs().stream().filter(j->j.wave()==1).allMatch(j->j.scanned()==0),"Secondary wave ran before primary completion");
        var file=level.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve("data/part25-fixture.json");
        try {
            manager.save(file);var restored=new DestructionManager();restored.load(file,cfg);
            helper.assertTrue(restored.jobs().getFirst().scanned()==manager.jobs().getFirst().scanned(),"Cursor did not persist");
            cfg.operatorsOnly=true;long beforePaused=restored.diagnostics(id).scanned();restored.tick(level.getServer(),cfg);
            helper.assertTrue(restored.diagnostics(id).scanned()==beforePaused,"Recovered unauthorized owner mutated terrain");cfg.operatorsOnly=false;
            for(int i=0;i<250&&restored.queued()>0;i++){restored.tick(level.getServer(),cfg);helper.assertTrue(restored.lastTickOperations()<=16,"Budget exceeded after recovery");}
            helper.assertTrue(restored.queued()==0&&restored.diagnostics(id).scanned()==restored.diagnostics(id).total()
                    &&!restored.strikeIds().contains(id),"Recovered waves failed to complete or retained admission capacity");
            helper.assertTrue(level.getBlockState(target).isAir(),"Recovered job did not mutate real terrain");
            helper.setBlock(3,8,3,Blocks.STONE);helper.setBlock(4,8,3,Blocks.STONE);
            var capped=new DestructionManager();var cappedId=UUID.randomUUID();cfg.maximumChangedBlocks=1;
            capped.reserve(cappedId,1,1);capped.enqueue(cappedId,owner.getUUID(),level.dimension(),target,"helios",false,new CraterGeometry("basin",3,2,0,1));
            for(int i=0;i<150&&capped.queued()>0;i++)capped.tick(level.getServer(),cfg);
            helper.assertTrue(capped.queued()==0&&capped.diagnostics(cappedId).changed()==1&&capped.diagnostics(cappedId).cause().equals("changed-block cap"),"Changed-block cap failed");
            Nullstar.LOGGER.info("PART25 runtime: staged real mutations, graceful file recovery, exact global budgets and changed-block cap passed");
        } catch(java.io.IOException exception){throw new RuntimeException(exception);}
        finally {try{java.nio.file.Files.deleteIfExists(file);}catch(java.io.IOException exception){throw new RuntimeException(exception);}level.getServer().getPlayerList().remove(owner);}
        helper.succeed();
    }
    @GameTest(maxTicks=30)
    public void hugeLoadedOnlyScanAndCancellationRemainBounded(GameTestHelper helper) {
        var manager=new DestructionManager();var config=new ServerConfig();config.publicServerMode=false;config.blockOperationsPerTick=64;
        var level=helper.getLevel();var target=helper.absolutePos(new BlockPos(3,8,3));var id=UUID.randomUUID();
        var corner=target.offset(-500,0,-500);boolean before=level.getChunkSource().hasChunk(corner.getX()>>4,corner.getZ()>>4);
        manager.reserve(id,1,1);manager.enqueue(id,UUID.randomUUID(),level.dimension(),target,"helios",true,new CraterGeometry("basin",500,96,.04,123));
        var second=UUID.randomUUID();helper.assertTrue(manager.reserve(second,1,2),"Concurrent huge job admission rejected");
        manager.enqueue(second,UUID.randomUUID(),level.dimension(),target.offset(1,0,1),"helios",true,new CraterGeometry("basin",500,96,.04,456));
        for(int i=0;i<10;i++){manager.tick(level.getServer(),config);helper.assertTrue(manager.lastTickOperations()<=64,"Huge footprint exceeded tick budget");}
        helper.assertTrue(manager.diagnostics(id).scanned()>0&&manager.progress(id).fraction()<1,"Huge progress invalid");
        helper.assertTrue(manager.diagnostics(second).scanned()>0,"Round-robin starved a concurrent huge job");
        helper.assertTrue(level.getChunkSource().hasChunk(corner.getX()>>4,corner.getZ()>>4)==before,"Huge job force-loaded a corner chunk");
        manager.cancel(id,"fixture cancellation");helper.assertTrue(manager.queued()==1,"Cancellation affected another huge job");manager.cancel(second);
        Nullstar.LOGGER.info("PART25 runtime: radius-500 bounded scan and no corner chunk loading passed; full footprint NOT completed");helper.succeed();
    }
}
