package com.nullstar.test;

import com.nullstar.Nullstar;
import com.nullstar.client.NullstarClient;
import com.nullstar.config.*;
import com.nullstar.core.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.core.BlockPos;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/** Accelerated authoritative timeline; real rendering, input, terrain and actual integrated-server restart. */
public final class ColossalClientGameTest implements FabricClientGameTest {
    private static final BlockPos TARGET=new BlockPos(0,80,0);
    private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    private static void advance(TestSingleplayerContext world,int age) {
        world.getServer().runOnServer(server->{
            var engine=Nullstar.engine();var strike=engine.active().iterator().next();
            int delta=Math.max(0,age-(int)(engine.tickCount()-strike.startTick));
            for(int i=0;i<delta;i++)engine.tick(server);
        });world.getConnection().waitForClientboundPackets();
    }
    private static void platform(TestSingleplayerContext world) {
        world.getServer().runCommand("fill -24 68 -24 24 80 24 minecraft:stone");
    }
    @Override public void runTest(ClientGameTestContext context) {
        if(!VisualCaptureClientGameTest.selected("colossal"))return;
        var cfg=Nullstar.config();var previous=ConfigFiles.JSON.fromJson(ConfigFiles.JSON.toJson(cfg),ServerConfig.class);
        net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave saved;
        UUID checkpointId=UUID.randomUUID();AtomicLong checkpointCursor=new AtomicLong();
        try {
            try(TestSingleplayerContext world=context.worldBuilder().adjustSettings(s->s.setAllowCommands(true)).create()) {
                saved=world.getWorldSave();world.getConnection().waitForChunksDownload();
                world.getServer().runCommand("gamemode creative @p");world.getServer().runCommand("tp @p 52 82 52 -135 15");
                platform(world);world.getServer().runCommand("give @p nullstar:controller");
                world.getServer().runOnServer(server->{var player=world.getConnection().getServerPlayer();player.getAbilities().flying=true;player.onUpdateAbilities();cfg.publicServerMode=false;cfg.apocalypseEnabled=true;cfg.requireColossalConfirmation=false;cfg.maximumColossalDiameter=64;cfg.maximumColossalDepth=12;cfg.blockOperationsPerTick=512;cfg.cooldownMultiplier=.1;});
                context.runOnClient(mc->{NullstarClient.config().cinematicCamera=true;NullstarClient.config().reducedMotion=false;});
                context.waitTicks(30);world.getConnection().waitForChunksRender();
                world.getServer().runOnServer(server->{int cap=cfg.maximumColossalDiameter;cfg.maximumColossalDiameter=1536;
                    try{server.getCommands().performPrefixedCommand(world.getConnection().getServerPlayer().createCommandSourceStack(),"nullstar bounds helios apocalypse 0 80 0");
                        check(Nullstar.engine().active().isEmpty()&&Nullstar.engine().destruction().queued()==0,"Huge bounds preview scheduled work");
                        check(world.getConnection().getServerLevel().getBlockState(TARGET).is(net.minecraft.world.level.block.Blocks.STONE),"Bounds preview mutated terrain");
                    }finally{cfg.maximumColossalDiameter=cap;}
                });
                world.getServer().runCommand("execute as @p at @s run nullstar preview helios apocalypse 0 80 0");
                context.waitFor(mc->!NullstarClient.STATE.strikes().isEmpty(),100);
                for(var phase:new Phase[]{Phase.PLANETARY_VIEW,Phase.SOLAR_SYSTEM_VIEW,Phase.GALACTIC_VIEW,Phase.COSMIC_BOUNDARY_VIEW,Phase.MOTHERSHIP_REVEAL}) {
                    int middle=world.getServer().computeOnServer(server->{var def=Nullstar.engine().active().iterator().next().definition;int tick=0;for(var stage:def.phases()){if(stage.phase()==phase)return tick+stage.ticks()/3;tick+=stage.ticks();}throw new AssertionError("Missing cosmic phase");});
                    advance(world,middle);context.waitTicks(10);
                    check(context.computeOnClient(mc->NullstarClient.STATE.strikes().values().stream().anyMatch(s->s.state().phase()==phase&&s.state().scale().scene()!=CinematicScene.WORLD)),"Scene did not reconstruct: "+phase);
                    check(context.computeOnClient(mc->NullstarClient.CAMERA.applied()&&mc.gameRenderer.mainCamera().position().distanceTo(mc.player.position())<12),"Detached shot escaped player space: "+phase);
                    context.runOnClient(mc->mc.gui.hud.getChat().clearMessages(true));context.waitTick();
                    context.takeScreenshot(TestScreenshotOptions.of("part25_"+phase.name().toLowerCase(java.util.Locale.ROOT)).withDestinationDir(Path.of("build/evidence/part25")));
                }
                // Rebuild from one current authoritative snapshot, without replaying old transient cues.
                context.runOnClient(mc->NullstarClient.reset());world.getServer().runOnServer(server->Nullstar.engine().join(world.getConnection().getServerPlayer()));
                world.getConnection().waitForClientboundPackets();context.waitTicks(5);
                check(context.computeOnClient(mc->NullstarClient.STATE.strikes().values().stream().anyMatch(s->s.state().scale().scene()==CinematicScene.STELLAR_MOTHERSHIP)),"Late snapshot lost scene identity");
                context.getInput().pressKey(options->java.util.Arrays.stream(options.keyMappings).filter(k->k.getName().equals("key.nullstar.skip")).findFirst().orElseThrow());
                context.waitTicks(20);check(context.computeOnClient(mc->!NullstarClient.CAMERA.applied()),"Cosmic skip failed to restore gameplay");
                world.getServer().runOnServer(server->check(!Nullstar.engine().active().isEmpty()&&Nullstar.engine().destruction().queued()==0,"Skip cancelled server event or preview queued terrain"));
                advance(world,3650);context.waitTicks(20);
                world.getServer().runOnServer(server->check(Nullstar.engine().active().isEmpty()&&world.getConnection().getServerLevel().getBlockState(TARGET).is(net.minecraft.world.level.block.Blocks.STONE),"Full colossal preview changed terrain"));
                world.getServer().runCommand("execute as @p at @s run nullstar fire helios apocalypse 0 80 0");
                context.waitFor(mc->!NullstarClient.STATE.strikes().isEmpty(),100);advance(world,2600);
                check(context.computeOnClient(mc->NullstarClient.STATE.strikes().values().stream().anyMatch(s->s.state().scale().committed())),"Damage commitment missing from state");
                context.getInput().pressKey(options->java.util.Arrays.stream(options.keyMappings).filter(k->k.getName().equals("key.nullstar.skip")).findFirst().orElseThrow());
                context.waitTicks(20);world.getServer().runOnServer(server->check(!Nullstar.engine().active().isEmpty(),"Skip cancelled committed devastation"));
                advance(world,1200);
                world.getServer().runOnServer(server->{for(int i=0;i<1500&&!Nullstar.engine().active().isEmpty();i++)Nullstar.engine().tick(server);
                    check(Nullstar.engine().active().isEmpty()&&world.getConnection().getServerLevel().getBlockState(TARGET).isAir(),"Small controlled colossal crater did not finish");
                    Nullstar.LOGGER.info("PART25 graphical: cosmic scenes, late reconstruction, native skip and controlled diameter-64 real crater passed");
                });
                context.waitTicks(20);
                platform(world);
                world.getServer().runOnServer(server->{
                    cfg.persistDestructionJobs=true;cfg.blockOperationsPerTick=16;
                    var manager=Nullstar.engine().destruction();check(manager.reserve(checkpointId,1,32),"Checkpoint fixture admission failed");
                    manager.enqueue(checkpointId,world.getConnection().getServerPlayer().getUUID(),world.getConnection().getServerLevel().dimension(),TARGET,
                            "helios",true,new CraterGeometry("basin",32,12,.04,123),0,ScaleTier.TITANIC);
                    manager.tick(server,cfg);checkpointCursor.set(manager.diagnostics(checkpointId).scanned());Nullstar.engine().checkpoint(server);
                });
            }
            try(TestSingleplayerContext reopened=saved.open()) {
                reopened.getConnection().waitForChunksDownload();context.waitTicks(10);
                reopened.getServer().runOnServer(server->{
                    var manager=Nullstar.engine().destruction();var diagnostic=manager.diagnostics(checkpointId);
                    check(diagnostic!=null&&diagnostic.scanned()>=checkpointCursor.get()&&diagnostic.scanned()<diagnostic.total(),"Actual server restart lost or completed the fixture cursor");
                    check(manager.jobs().stream().anyMatch(j->j.strike().equals(checkpointId)&&j.recovered()),"Restart did not mark recovered jobs");
                    check(manager.lastTickOperations()<=16,"Recovered job exceeded global budget");
                    Nullstar.LOGGER.info("PART25 graphical: actual world close/reopen restored job {} at {}/{} candidates",checkpointId,diagnostic.scanned(),diagnostic.total());
                    check(Nullstar.engine().abort(checkpointId,server),"Recovered job abort failed");Nullstar.engine().checkpoint(server);
                });
            }
        } finally {
            try{for(var field:ServerConfig.class.getFields())field.set(cfg,field.get(previous));}
            catch(ReflectiveOperationException exception){throw new RuntimeException(exception);}
        }
    }
}
