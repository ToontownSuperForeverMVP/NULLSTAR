// Copyright (C) 2026 ToontownSuperForeverMVP and NULLSTAR contributors
// SPDX-License-Identifier: GPL-3.0-only

package com.nullstar.server;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.tags.BlockTags;

/** Conservative aftermath rules. Entity containers are excluded by DestructionManager. */
public final class BlockTransformations {
    private BlockTransformations() {}
    public static BlockState scorch(String weapon,BlockState state) {
        if (weapon.equals("glassfall") && (state.is(Blocks.SAND)||state.is(Blocks.RED_SAND))) return Blocks.GLASS.defaultBlockState();
        if (state.is(Blocks.STONE)||state.is(Blocks.DEEPSLATE)||state.is(Blocks.COBBLESTONE))
            return (weapon.equals("singularity")?Blocks.OBSIDIAN:Blocks.SMOOTH_BASALT).defaultBlockState();
        if (state.is(Blocks.DIRT)||state.is(Blocks.GRASS_BLOCK)||state.is(Blocks.SAND)||state.is(Blocks.GRAVEL))
            return Blocks.BLACKSTONE.defaultBlockState();
        if(state.is(BlockTags.LOGS))return Blocks.COAL_BLOCK.defaultBlockState();
        return state;
    }
}
