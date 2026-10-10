// Copyright (C) 2026 ToontownSuperForeverMVP and NULLSTAR contributors
// SPDX-License-Identifier: GPL-3.0-only

package com.nullstar.server;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

/** A ray's collision-shape neighbor reads must also avoid chunk generation, including corner crossings. */
public final class LoadedBlockView implements BlockGetter {
    private final ServerLevel level;
    public LoadedBlockView(ServerLevel level){this.level=level;}
    private boolean available(BlockPos pos){return pos.getY()>=getMinY() && pos.getY()<=level.getMaxY()
            && level.getWorldBorder().isWithinBounds(pos) && level.getChunkSource().hasChunk(pos.getX()>>4,pos.getZ()>>4);}
    @Override public BlockEntity getBlockEntity(BlockPos pos){return available(pos)?level.getBlockEntity(pos):null;}
    @Override public BlockState getBlockState(BlockPos pos){return available(pos)?level.getBlockState(pos):Blocks.AIR.defaultBlockState();}
    @Override public FluidState getFluidState(BlockPos pos){return available(pos)?level.getFluidState(pos):Fluids.EMPTY.defaultFluidState();}
    @Override public int getHeight(){return level.getHeight();}
    @Override public int getMinY(){return level.getMinY();}
}
