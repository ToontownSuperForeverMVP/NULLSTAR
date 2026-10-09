package com.nullstar.network;

import com.nullstar.Nullstar;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** v1 carries intent only. Targets, dimensions and blast settings are never accepted from clients. */
public record RequestPayload(int version, long sequence, int action, String weapon, boolean preview) implements CustomPacketPayload {
    public static final int ACQUIRE=0, ACTIVATE=1, SELECT=2, CANCEL=3, SKIP=4;
    public static final Type<RequestPayload> TYPE = new Type<>(Nullstar.id("request_v1"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RequestPayload> CODEC = StreamCodec.of(
            (buf,p) -> {buf.writeVarInt(p.version);buf.writeLong(p.sequence);buf.writeVarInt(p.action);buf.writeUtf(p.weapon,32);buf.writeBoolean(p.preview);},
            buf -> new RequestPayload(buf.readVarInt(),buf.readLong(),buf.readVarInt(),buf.readUtf(32),buf.readBoolean()));
    public RequestPayload {
        if (version != 1 || sequence <= 0 || action < 0 || action > 4 || weapon == null || weapon.length() > 32)
            throw new IllegalArgumentException("Invalid NULLSTAR request");
    }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
