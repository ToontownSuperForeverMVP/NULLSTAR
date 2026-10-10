// Copyright (C) 2026 ToontownSuperForeverMVP and NULLSTAR contributors
// SPDX-License-Identifier: GPL-3.0-only

package com.nullstar.network;

import com.nullstar.Nullstar;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record StatusPayload(String weapon, String displayName, String message, boolean hasTarget, BlockPos target, int cooldown) implements CustomPacketPayload {
    public static final Type<StatusPayload> TYPE=new Type<>(Nullstar.id("status_v1"));
    public static final StreamCodec<RegistryFriendlyByteBuf,StatusPayload> CODEC=StreamCodec.of((b,p)-> {
        b.writeUtf(p.weapon,32);b.writeUtf(p.displayName,96);b.writeUtf(p.message,256);b.writeBoolean(p.hasTarget);b.writeBlockPos(p.target);b.writeVarInt(p.cooldown);
    },b->new StatusPayload(b.readUtf(32),b.readUtf(96),b.readUtf(256),b.readBoolean(),b.readBlockPos(),b.readVarInt()));
    public StatusPayload {
        if (weapon==null || weapon.length()>32 || displayName==null || displayName.length()>96 || message==null || message.length()>256 || target==null || cooldown<0 || cooldown>720000)
            throw new IllegalArgumentException("Invalid status");
    }
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
