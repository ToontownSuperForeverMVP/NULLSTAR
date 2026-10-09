package com.nullstar.network;

import com.nullstar.Nullstar;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import java.util.UUID;

/** Transient cues are not replayed for late joins. Index allows client deduplication. */
public record VisualPayload(UUID strike, int index, String effect, BlockPos position, int radius, int duration) implements CustomPacketPayload {
    public static final Type<VisualPayload> TYPE = new Type<>(Nullstar.id("visual_v1"));
    public static final StreamCodec<RegistryFriendlyByteBuf,VisualPayload> CODEC=StreamCodec.of((b,p)-> {
        b.writeUUID(p.strike);b.writeVarInt(p.index);b.writeUtf(p.effect,32);b.writeBlockPos(p.position);b.writeVarInt(p.radius);b.writeVarInt(p.duration);
    },b->new VisualPayload(b.readUUID(),b.readVarInt(),b.readUtf(32),b.readBlockPos(),b.readVarInt(),b.readVarInt()));
    public VisualPayload {
        if (strike==null || index<0 || index>64 || !java.util.List.of("impact","projectile").contains(effect)
                || position==null || radius<1 || radius>48 || duration<1 || duration>200)
            throw new IllegalArgumentException("Invalid visual cue");
    }
    @Override public Type<? extends CustomPacketPayload> type() {return TYPE;}
}
