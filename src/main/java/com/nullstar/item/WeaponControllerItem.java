// Copyright (C) 2026 ToontownSuperForeverMVP and NULLSTAR contributors
// SPDX-License-Identifier: GPL-3.0-only

package com.nullstar.item;

import com.nullstar.Nullstar;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

public final class WeaponControllerItem extends Item {
    public WeaponControllerItem(Properties properties){super(properties);}
    @Override public InteractionResult use(Level level,Player player,InteractionHand hand) {
        if(player instanceof ServerPlayer serverPlayer)Nullstar.engine().useController(serverPlayer);
        return InteractionResult.SUCCESS;
    }
}
