package com.nullstar.test;

import com.nullstar.Nullstar;
import com.nullstar.config.ServerConfig;
import com.nullstar.core.CraterGeometry;
import com.nullstar.server.DestructionManager;
import com.nullstar.server.EntityEffects;
import com.nullstar.server.ProtectionHooks;
import com.nullstar.server.LoadedBlockView;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.block.Blocks;
import java.util.UUID;

public final class DestructionGameTest {
    @GameTest(maxTicks=30)
    public void authorityReplayAndConcurrentStrikeFailureIsolation(GameTestHelper helper) {
        var server=helper.getLevel().getServer();var first=helper.makeMockServerPlayerInLevel();var second=helper.makeMockServerPlayerInLevel();
        var engine=new com.nullstar.server.WeaponEngine();var cfg=Nullstar.config();
        boolean operators=cfg.operatorsOnly,terrain=cfg.terrainDestruction;int radius=cfg.maximumRadius,depth=cfg.maximumDepth;
        var target=helper.absolutePos(new BlockPos(3,30,3));helper.setBlock(3,30,3,Blocks.STONE);
        first.setPos(net.minecraft.world.phys.Vec3.atCenterOf(target).add(2,2,2));second.setPos(first.position());
        try {
            cfg.operatorsOnly=true;
            helper.assertFalse(engine.authorized(first),"Mock player unexpectedly has operator permission");
            helper.assertFalse(engine.activate(first,"helios",target,true),"Unauthorized preview was admitted");
            helper.assertFalse(engine.activate(first,"helios",target,false),"Unauthorized destructive strike was admitted");
            helper.assertTrue(engine.active().isEmpty(),"Rejected request consumed a strike slot");
            cfg.operatorsOnly=false;cfg.maximumRadius=2;cfg.maximumDepth=2;
            first.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.item.ItemStack(Nullstar.CONTROLLER));
            engine.request(first,new com.nullstar.network.RequestPayload(1,1,2,"glassfall",false));
            engine.request(first,new com.nullstar.network.RequestPayload(1,1,2,"helios",false));
            helper.assertTrue(engine.selected(first.getUUID()).equals("glassfall"),"Replayed selection was accepted");
            helper.assertTrue(engine.activate(first,"helios",target,false),"First strike rejected");
            helper.assertFalse(engine.activate(first,"helios",target,false),"Duplicate activation was admitted");
            helper.assertTrue(engine.activate(second,"glassfall",target,false),"Concurrent strike rejected");
            var broken=engine.active().stream().filter(s->s.owner.equals(first.getUUID())).findFirst().orElseThrow();
            broken.timeline.advance(1000); // Inject an invariant failure into one event, preserving the other.
            engine.tick(server);
            helper.assertTrue(engine.active().size()==1&&engine.active().iterator().next().owner.equals(second.getUUID()),"Failure did not isolate the broken strike");
            for(int i=0;i<450;i++)engine.tick(server);
            helper.assertTrue(engine.active().isEmpty()&&engine.destruction().queued()==0,"Concurrent survivor did not complete");
            helper.assertTrue(helper.getLevel().getBlockState(target).isAir(),"Surviving barrage did not mutate terrain");
            helper.setBlock(3,30,3,Blocks.STONE);
            helper.assertFalse(engine.activate(second,"glassfall",target,false),"Cooldown bypassed after completion");
            for(int i=0;i<160;i++)engine.tick(server);
            helper.assertTrue(engine.activate(first,"helios",target,true),"Authorized preview rejected");
            for(int i=0;i<400;i++)engine.tick(server);
            helper.assertTrue(engine.active().isEmpty()&&helper.getLevel().getBlockState(target).is(Blocks.STONE),"Preview mutated terrain or failed to finish");
            cfg.terrainDestruction=false;
            helper.assertTrue(engine.activate(second,"glassfall",target,false),"Disabled-terrain activation rejected");
            for(int i=0;i<450;i++)engine.tick(server);
            helper.assertTrue(engine.active().isEmpty()&&helper.getLevel().getBlockState(target).is(Blocks.STONE),"Disabled destruction changed terrain");
        } finally {
            cfg.operatorsOnly=operators;cfg.maximumRadius=radius;cfg.maximumDepth=depth;cfg.terrainDestruction=terrain;
            engine.stop();server.getPlayerList().remove(first);server.getPlayerList().remove(second);
        }
        helper.succeed();
    }
    @GameTest(maxTicks=20)
    public void targetingDoesNotGenerateUnloadedChunks(GameTestHelper helper) {
        var level=helper.getLevel();var distant=helper.absolutePos(new BlockPos(4000,4,4000));
        helper.assertFalse(level.getChunkSource().hasChunk(distant.getX()>>4,distant.getZ()>>4),"Fixture chunk already loaded");
        var view=new LoadedBlockView(level);
        helper.assertTrue(view.getBlockState(distant).isAir(),"Unloaded block view is not empty");
        helper.assertTrue(view.getFluidState(distant).isEmpty(),"Unloaded fluid view is not empty");
        helper.assertFalse(level.getChunkSource().hasChunk(distant.getX()>>4,distant.getZ()>>4),"Targeting generated a chunk");helper.succeed();
    }
    @GameTest(maxTicks=100)
    public void craterRespectsBudgetAndProtectedBlocks(GameTestHelper helper) {
        var manager=new DestructionManager();var config=new ServerConfig();config.blockOperationsPerTick=16;
        var id=UUID.randomUUID();var owner=UUID.randomUUID();var level=helper.getLevel();
        BlockPos target=helper.absolutePos(new BlockPos(3,4,3));
        helper.setBlock(3,4,3,Blocks.STONE);helper.setBlock(3,3,3,Blocks.BEDROCK);helper.setBlock(4,4,3,Blocks.CHEST);
        helper.setBlock(2,4,3,Blocks.DIAMOND_BLOCK);
        AutoCloseable protection=ProtectionHooks.register((world,user,pos)->!pos.equals(target.offset(-1,0,0)));
        helper.assertTrue(manager.reserve(id,1,1),"Queue reservation failed");
        helper.assertFalse(manager.reserve(UUID.randomUUID(),1,1),"Queue overcommitted");
        manager.enqueue(id,owner,level.dimension(),target,"helios",true,new CraterGeometry("crater",2,2,0,1));
        helper.onEachTick(()->{
            manager.tick(level.getServer(),config);helper.assertTrue(manager.lastTickOperations()<=16,"Global budget exceeded");
        });
        helper.succeedWhen(()->{
            helper.assertTrue(manager.queued()==0,"Job still processing");
            helper.assertBlockPresent(Blocks.AIR,3,4,3);helper.assertBlockPresent(Blocks.BEDROCK,3,3,3);
            helper.assertBlockPresent(Blocks.CHEST,4,4,3);helper.assertBlockPresent(Blocks.DIAMOND_BLOCK,2,4,3);
            helper.assertTrue(manager.progress(id).changed()>0,"No terrain changed");
            helper.assertTrue(manager.progress(id).active()==0,"Completed jobs retained active accounting");
            try{protection.close();}catch(Exception exception){throw new RuntimeException(exception);}
        });
    }
    @GameTest(maxTicks=50)
    public void cancellationStopsWorldMutations(GameTestHelper helper) {
        var manager=new DestructionManager();var config=new ServerConfig();config.blockOperationsPerTick=16;
        var id=UUID.randomUUID();var level=helper.getLevel();BlockPos target=helper.absolutePos(new BlockPos(3,4,3));
        helper.setBlock(3,4,3,Blocks.STONE);manager.reserve(id,1,1);
        manager.enqueue(id,UUID.randomUUID(),level.dimension(),target,"helios",true,new CraterGeometry("crater",2,2,0,1));
        manager.tick(level.getServer(),config);manager.cancel(id);
        helper.runAtTickTime(10,()->{
            manager.tick(level.getServer(),config);helper.assertBlockPresent(Blocks.STONE,3,4,3);
            helper.assertTrue(manager.queued()==0,"Cancelled job retained");helper.succeed();
        });
    }
    @GameTest(maxTicks=30)
    public void impactAndGravityAffectLivingEntities(GameTestHelper helper) {
        var level=helper.getLevel();var mob=helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE,5,3,3);
        var center=helper.absolutePos(new BlockPos(3,3,3));var config=new ServerConfig();float health=mob.getHealth();
        EntityEffects.attract(level,UUID.randomUUID(),center,5,config);
        helper.assertTrue(mob.getDeltaMovement().lengthSqr()>0,"Attraction did not move the entity");
        EntityEffects.impact(level,UUID.randomUUID(),center,5,10,1,config);
        helper.assertTrue(mob.getHealth()<health,"Impact did not damage the entity");helper.succeed();
    }
    @GameTest(maxTicks=30)
    public void playerEffectsRespectPvpAndDamageHasAnOwner(GameTestHelper helper) {
        var level=helper.getLevel();var rules=level.getGameRules();
        boolean previous=rules.get(net.minecraft.world.level.gamerules.GameRules.PVP);
        var actor=helper.makeMockServerPlayerInLevel();
        var victim=helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var center=helper.absolutePos(new BlockPos(3,3,3));
        victim.setPos(net.minecraft.world.phys.Vec3.atCenterOf(center).add(2,0,0));level.addFreshEntity(victim);
        var mob=helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE,4,3,3);var config=new ServerConfig();
        try {
            rules.set(net.minecraft.world.level.gamerules.GameRules.PVP,false,level.getServer());
            float health=victim.getHealth();var velocity=victim.getDeltaMovement();
            EntityEffects.impact(level,actor.getUUID(),center,5,10,1,config);
            EntityEffects.attract(level,actor.getUUID(),center,5,config);
            helper.assertTrue(victim.getHealth()==health,"PvP-disabled player was damaged");
            helper.assertTrue(victim.getDeltaMovement().equals(velocity),"PvP-disabled player was pushed or attracted");
            helper.assertTrue(mob.getLastHurtByMob()==actor,"Blast damage lost owner attribution");
            rules.set(net.minecraft.world.level.gamerules.GameRules.PVP,true,level.getServer());
            EntityEffects.impact(level,actor.getUUID(),center,5,10,1,config);
            helper.assertTrue(victim.getHealth()<health,"PvP-enabled player did not take damage");
            helper.assertTrue(victim.getDeltaMovement().lengthSqr()>0,"PvP-enabled knockback was missing");
        } finally {
            rules.set(net.minecraft.world.level.gamerules.GameRules.PVP,previous,level.getServer());
            victim.discard();level.getServer().getPlayerList().remove(actor);
        }
        helper.succeed();
    }
}
