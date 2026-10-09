package com.nullstar.network;

import com.nullstar.core.Phase;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class SerializationTest {
    private <T> void roundTrip(StreamCodec<RegistryFriendlyByteBuf,T> codec,T payload) {
        var buf=new RegistryFriendlyByteBuf(Unpooled.buffer(),RegistryAccess.EMPTY);
        try{codec.encode(buf,payload);assertTrue(buf.readableBytes()<512);assertEquals(payload,codec.decode(buf));assertEquals(0,buf.readableBytes());}
        finally{buf.release();}
    }
    @Test void requestRoundTrip() {roundTrip(RequestPayload.CODEC,new RequestPayload(1,7,RequestPayload.ACTIVATE,"helios",false));}
    @Test void stateRoundTrip() {roundTrip(StatePayload.CODEC,new StatePayload(UUID.randomUUID(),UUID.randomUUID(),"minecraft:overworld","helios",
            Phase.IMPACT,170,0,10,1234,new BlockPos(-77,64,129),20,13,42,true,.75,0xffaa00,72,"impact",false));}
    @Test void visualRoundTrip(){roundTrip(VisualPayload.CODEC,new VisualPayload(UUID.randomUUID(),2,"projectile",new BlockPos(3,-12,4),16,30));}
    @Test void statusRoundTrip(){roundTrip(StatusPayload.CODEC,new StatusPayload("singularity","Target ready",true,new BlockPos(4,5,6),600));}
    @Test void invalidRequestsAndStatesRejected() {
        assertThrows(IllegalArgumentException.class,()->new RequestPayload(2,1,0,"helios",false));
        assertThrows(IllegalArgumentException.class,()->new RequestPayload(1,1,999,"helios",false));
        assertThrows(IllegalArgumentException.class,()->new VisualPayload(UUID.randomUUID(),1,"impact",BlockPos.ZERO,999,20));
        var buf=new RegistryFriendlyByteBuf(Unpooled.buffer(),RegistryAccess.EMPTY);
        try{buf.writeVarInt(1);buf.writeLong(1);buf.writeVarInt(0);buf.writeUtf("x".repeat(33));buf.writeBoolean(false);
            assertThrows(io.netty.handler.codec.DecoderException.class,()->RequestPayload.CODEC.decode(buf));}
        finally{buf.release();}
    }
}
