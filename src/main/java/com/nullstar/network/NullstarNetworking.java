// Copyright (C) 2026 ToontownSuperForeverMVP and NULLSTAR contributors
// SPDX-License-Identifier: GPL-3.0-only

package com.nullstar.network;

import com.nullstar.Nullstar;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public final class NullstarNetworking {
    private NullstarNetworking() {}
    public static void initialize() {
        PayloadTypeRegistry.serverboundPlay().register(RequestPayload.TYPE,RequestPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(StatePayload.TYPE,StatePayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(VisualPayload.TYPE,VisualPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(StatusPayload.TYPE,StatusPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(RequestPayload.TYPE,(payload,context)->
                Nullstar.engine().request(context.player(),payload));
        ServerPlayConnectionEvents.JOIN.register((handler,sender,server)->Nullstar.engine().join(handler.player));
        ServerPlayConnectionEvents.DISCONNECT.register((handler,server)->Nullstar.engine().disconnect(handler.player.getUUID()));
    }
}
