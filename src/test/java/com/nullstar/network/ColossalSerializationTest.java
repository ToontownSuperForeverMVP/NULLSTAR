// Copyright (C) 2026 ToontownSuperForeverMVP and NULLSTAR contributors
// SPDX-License-Identifier: GPL-3.0-only

package com.nullstar.network;

import com.nullstar.core.*;
import io.netty.buffer.Unpooled;
import net.minecraft.core.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class ColossalSerializationTest {
    @Test void compactColossalStateAndLargeCueRoundTrip() {
        var metadata=new ColossalMetadata(ScaleTier.APOCALYPSE,CinematicScene.STELLAR_MOTHERSHIP,1e9,1e6,48,2000,650,128,128,"helios_colossal","stellar",12,0,500,3,10,2000,true);
        var payload=new StatePayload(UUID.randomUUID(),UUID.randomUUID(),"minecraft:overworld","helios","HELIOS",Phase.MOTHERSHIP_REVEAL,
                1000,50,100,10000,new BlockPos(-31,80,1),500,96,42,false,.25,0xffaa00,72,1,"cosmic_montage",false,metadata);
        var buffer=new RegistryFriendlyByteBuf(Unpooled.buffer(),RegistryAccess.EMPTY);
        try {
            StatePayload.CODEC.encode(buffer,payload);assertTrue(buffer.readableBytes()<512);assertEquals(payload,StatePayload.CODEC.decode(buffer));
            var cue=new VisualPayload(payload.id(),127,"impact",payload.target(),650,40);VisualPayload.CODEC.encode(buffer,cue);assertEquals(cue,VisualPayload.CODEC.decode(buffer));assertEquals(0,buffer.readableBytes());
            assertEquals("nullstar:state_v2",StatePayload.TYPE.id().toString());assertEquals("nullstar:visual_v2",VisualPayload.TYPE.id().toString());
        } finally{buffer.release();}
    }
}
