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
}
