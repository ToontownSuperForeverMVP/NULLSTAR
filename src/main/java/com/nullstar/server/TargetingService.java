package com.nullstar.server;

import com.nullstar.core.TargetRules;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class TargetingService {
    private TargetingService() {}
    public static BlockPos acquire(ServerPlayer player, int range, boolean groundRequired) {
        ServerLevel level = (ServerLevel)player.level();
        Vec3 eye = player.getEyePosition(), end = eye.add(player.getViewVector(1).scale(range));
        // Stop before the first unloaded column. Vanilla clip must not request arbitrary chunks.
        for (int i=0; i<=range; i++) {
            Vec3 sample=eye.lerp(end,i/(double)range);
            if (!level.getChunkSource().hasChunk((int)Math.floor(sample.x)>>4,(int)Math.floor(sample.z)>>4)) {
                end=eye.lerp(end,Math.max(0,i-1)/(double)range); break;
            }
        }
        BlockHitResult hit=new LoadedBlockView(level).clip(new ClipContext(eye,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,player));
        if (hit.getType()!= HitResult.Type.BLOCK) return null;
        BlockPos target=hit.getBlockPos();
        return valid(player,target,range,groundRequired) ? target.immutable() : null;
    }
    public static boolean valid(ServerPlayer player, BlockPos pos, int range, boolean groundRequired) {
        ServerLevel level=(ServerLevel)player.level(); Vec3 eye=player.getEyePosition();
        return player.isAlive() && !player.isSpectator() && pos.getY()>=level.getMinY() && pos.getY()<=level.getMaxY()
                && level.getWorldBorder().isWithinBounds(pos) && level.getChunkSource().hasChunk(pos.getX()>>4,pos.getZ()>>4)
                && TargetRules.withinRange(eye.x,eye.y,eye.z,pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,range)
                && (!groundRequired || !level.getBlockState(pos).isAir());
    }
    public static BlockPos surfaceNear(ServerLevel level, BlockPos origin) {
        if (!level.getChunkSource().hasChunk(origin.getX()>>4,origin.getZ()>>4)) return null;
        // A bounded local ground probe preserves cave targets and never teleports a strike to the sky.
        for (int offset=8; offset>=-24; offset--) {
            BlockPos pos=origin.offset(0,offset,0);
            if (pos.getY()>=level.getMinY() && pos.getY()<=level.getMaxY() && level.getWorldBorder().isWithinBounds(pos)
                    && !level.getBlockState(pos).isAir() && level.getBlockState(pos.above()).isAir()) return pos;
        }
        return origin;
    }
}
