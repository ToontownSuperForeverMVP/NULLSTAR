package com.nullstar.test;

import com.nullstar.Nullstar;
import com.nullstar.client.NullstarClient;
import com.nullstar.core.Phase;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Blocks;
import java.nio.file.Path;

/** Real client/server loop in a disposable flat world; never uses the Prism user's saves. */
public final class StrikeClientGameTest implements FabricClientGameTest {
    private static final BlockPos TARGET=new BlockPos(0,80,0);
    @Override public void runTest(ClientGameTestContext context) {
        try(TestSingleplayerContext world=context.worldBuilder().create()) {
            world.getConnection().waitForChunksDownload();world.getConnection().waitForChunksRender();
            world.getServer().runOnServer(server->{
                check(BuiltInRegistries.ITEM.getOptional(Nullstar.id("controller")).orElse(null)==Nullstar.CONTROLLER,"Controller not registered");
                Nullstar.config().cooldownMultiplier=.1;
            });
            world.getServer().runCommand("tp @p 52 82 52");
            world.getServer().runCommand("give @p nullstar:controller");
            context.waitTicks(30);world.getConnection().waitForChunksDownload();
            for(String weapon:new String[]{"helios","singularity","glassfall"}) {
                platform(world);world.getConnection().waitForClientboundPackets();context.waitTicks(10);
                if(weapon.equals("helios")) {
                    context.runOnClient(mc->{
                        double dx=.5-mc.player.getX(),dz=.5-mc.player.getZ(),dy=80.5-mc.player.getEyePosition().y;
                        mc.player.setYRot((float)Math.toDegrees(Math.atan2(-dx,dz)));
                        mc.player.setXRot((float)-Math.toDegrees(Math.atan2(dy,Math.hypot(dx,dz))));
                    });context.waitTicks(10);
                    context.getInput().pressKey(options->options.keyUse);
                    context.waitFor(mc->NullstarClient.status().hasTarget(),100);
                    check(context.computeOnClient(mc->NullstarClient.status().target().distSqr(TARGET))<9,"Designation did not hit intended ground");
                    screenshot(context,"helios-designation");context.getInput().pressKey(InputConstants.KEY_G);
                } else world.getServer().runCommand("execute as @p at @s run nullstar fire "+weapon+" 0 80 0");
                context.waitFor(mc->!NullstarClient.STATE.strikes().isEmpty(),100);
                context.waitFor(mc->NullstarClient.STATE.strikes().values().stream().anyMatch(s->s.state().phase()==Phase.CHARGING),120);
                context.waitTicks(12);screenshot(context,weapon+"-charge");
                context.waitFor(mc->NullstarClient.STATE.strikes().values().stream().anyMatch(s->s.state().phase()==Phase.FIRING),240);
                context.waitTicks(10);screenshot(context,weapon+"-firing");
                check(context.computeOnClient(mc->NullstarClient.CAMERA.applied()),"Cinematic camera not applied");
                if(weapon.equals("helios")) {
                    context.getInput().pressKey(InputConstants.KEY_K);context.waitTicks(20);
                    check(!context.computeOnClient(mc->NullstarClient.CAMERA.applied()),"Skip did not restore normal camera");
                }
                context.waitFor(mc->NullstarClient.STATE.strikes().values().stream().anyMatch(s->s.state().phase()==Phase.AFTERMATH),260);
                screenshot(context,weapon+"-aftermath");
                world.getServer().waitFor(server->Nullstar.engine().active().isEmpty(),500);
                world.getConnection().waitForClientboundPackets();context.waitTicks(20);
                world.getServer().runOnServer(server->{
                    check(world.getConnection().getServerLevel().getBlockState(TARGET).isAir(),weapon+" did not create persistent crater");
                    check(Nullstar.engine().destruction().queued()==0,"Completed strike retained work");
                });
                check(context.computeOnClient(mc->NullstarClient.STATE.strikes().isEmpty()&&!NullstarClient.CAMERA.applied()),"Completion did not restore presentation");
                screenshot(context,weapon+"-crater");
            }
            platform(world);world.getServer().runCommand("execute as @p at @s run nullstar preview helios 0 80 0");
            world.getServer().waitFor(server->!Nullstar.engine().active().isEmpty(),100);
            world.getServer().waitFor(server->Nullstar.engine().active().isEmpty(),500);
            world.getServer().runOnServer(server->check(world.getConnection().getServerLevel().getBlockState(TARGET).is(Blocks.STONE),"Preview changed terrain"));
            world.getServer().runCommand("execute as @p at @s run nullstar fire helios 0 80 0");
            world.getServer().waitFor(server->!Nullstar.engine().active().isEmpty(),100);
            world.getServer().runCommand("execute as @p at @s run nullstar cancel");
            context.waitTicks(20);
            check(context.computeOnClient(mc->NullstarClient.STATE.strikes().isEmpty()&&!NullstarClient.CAMERA.applied()),"Cancellation did not restore camera");
            world.getServer().runOnServer(server->Nullstar.config().cooldownMultiplier=1);
        }
    }
    private static void platform(TestSingleplayerContext world) {
        world.getServer().runCommand("fill -24 60 -24 24 70 24 minecraft:stone");
        world.getServer().runCommand("fill -24 71 -24 24 80 24 minecraft:stone");
    }
    private static void screenshot(ClientGameTestContext context,String name) {
        context.takeScreenshot(TestScreenshotOptions.of(name).withDestinationDir(Path.of("build/evidence")));
    }
    private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
}
