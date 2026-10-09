package com.nullstar.test;

import com.nullstar.Nullstar;
import com.nullstar.NullstarSounds;
import com.nullstar.client.NullstarClient;
import com.nullstar.core.Phase;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import java.util.Properties;

/**
 * Real dedicated server over a network connection: registry-synced original sounds, authoritative strike delivery,
 * cinematic camera, crater persistence, owner disconnect cancellation and clean presentation after reconnecting.
 */
public final class DedicatedServerClientGameTest implements FabricClientGameTest {
    private static final BlockPos TARGET=new BlockPos(0,80,0);
    @Override public void runTest(ClientGameTestContext context) {
        if(!VisualCaptureClientGameTest.selected("dedicated"))return;
        Properties properties=new Properties();
        properties.setProperty("allow-flight","true");
        try(TestDedicatedServerContext server=context.worldBuilder().createServer(properties)) {
            try(var connection=server.connect()) {
                connection.waitForChunksDownload();
                String name=context.computeOnClient(mc->mc.player.getGameProfile().name());
                server.runCommand("op "+name);
                server.runCommand("gamemode creative "+name);
                // Vanilla caps one fill at 32,768 blocks; build the platform in two volumes.
                server.runCommand("fill -24 60 -24 24 70 24 minecraft:stone");
                server.runCommand("fill -24 71 -24 24 80 24 minecraft:stone");
                server.runCommand("tp "+name+" 40 84 40");
                server.runOnServer(s->{var p=s.getPlayerList().getPlayerByName(name);p.getAbilities().flying=true;p.onUpdateAbilities();Nullstar.config().cooldownMultiplier=.1;});
                context.waitTicks(20);connection.waitForClientboundPackets();
                // Original sound events must resolve on the client from the synced registry and bundled sounds.json.
                check(context.computeOnClient(mc->NullstarSounds.get("helios.impact")!=null
                        &&mc.getSoundManager().getSoundEvent(Identifier.fromNamespaceAndPath("nullstar","helios.impact"))!=null),"Original sound event missing on client");
                server.runCommand("execute as "+name+" at @s run nullstar fire helios 0 80 0");
                context.waitFor(mc->NullstarClient.STATE.strikes().values().stream().anyMatch(s->s.state().phase()==Phase.ORBITAL_ALIGNMENT),300);
                context.waitTicks(5);
                check(context.computeOnClient(mc->NullstarClient.CAMERA.applied()),"Dedicated-server strike did not drive the owner's cinematic camera");
                check(context.computeOnClient(mc->NullstarClient.SCENE.lastVertices()>0),"Effects engine extracted no geometry for a networked strike");
                server.waitFor(s->Nullstar.engine().active().isEmpty(),700);
                context.waitTicks(30);
                server.runOnServer(s->check(s.overworld().getBlockState(TARGET).isAir(),"Dedicated-server Helios did not create a crater"));
                check(context.computeOnClient(mc->NullstarClient.STATE.strikes().isEmpty()&&!NullstarClient.CAMERA.applied()),"Presentation not restored after networked completion");
                context.waitTicks(80);
                // Creative players do not fly by default on a dedicated server; keep the owner within range.
                server.runCommand("tp "+name+" 40 84 40");
                server.runOnServer(s->{var p=s.getPlayerList().getPlayerByName(name);p.getAbilities().flying=true;p.onUpdateAbilities();});
                context.waitTicks(5);
                String reason=server.computeOnServer(s->{
                    var player=s.getPlayerList().getPlayerByName(name);var pos=new BlockPos(22,80,-22);
                    String detail="block="+s.overworld().getBlockState(pos)+" valid="+com.nullstar.server.TargetingService.valid(player,pos,112,true)
                            +" eye="+player.getEyePosition()+" active="+Nullstar.engine().active().size();
                    return Nullstar.engine().activate(player,"singularity",pos,false)?"ok":detail;
                });
                check(reason.equals("ok"),"Second networked strike rejected: "+reason);
                context.waitTicks(40);connection.waitForClientboundPackets();
                check(context.computeOnClient(mc->!NullstarClient.STATE.strikes().isEmpty()),"Second networked strike rejected: "+context.computeOnClient(mc->NullstarClient.status().message()));
                context.waitFor(mc->NullstarClient.STATE.strikes().values().stream().anyMatch(s->s.state().phase()==Phase.CHARGING),200);
            }
            // The owner disconnected mid-charge: the server must cancel the strike and release its work.
            server.waitFor(s->Nullstar.engine().active().isEmpty()&&Nullstar.engine().destruction().queued()==0,100);
            try(var connection=server.connect()) {
                connection.waitForChunksDownload();context.waitTicks(20);
                check(context.computeOnClient(mc->NullstarClient.STATE.strikes().isEmpty()&&!NullstarClient.CAMERA.applied()
                        &&mc.getCameraEntity()==mc.player),"Reconnect retained stale strike presentation or camera");
                server.runOnServer(s->check(s.overworld().getBlockState(new BlockPos(22,80,-22)).is(net.minecraft.world.level.block.Blocks.STONE),
                        "Cancelled strike changed terrain"));
            }
            server.runOnServer(s->Nullstar.config().cooldownMultiplier=1);
        }
    }
    private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
}
