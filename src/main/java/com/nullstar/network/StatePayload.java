package com.nullstar.network;

import com.nullstar.Nullstar;
import com.nullstar.core.Phase;
import com.nullstar.core.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import java.util.UUID;

/** Bounded snapshot. Phase age is interpolated locally; no per-frame packets. */
public record StatePayload(UUID id, UUID owner, String dimension, String weapon, String displayName, Phase phase,
        int age, int phaseAge, int phaseDuration, long serverTick, BlockPos target,
        int radius, int depth, long seed, boolean preview, double progress,
        int color, double orbitalHeight, double particleScale, String camera, boolean terminal,
        ColossalMetadata scale) implements CustomPacketPayload {
    public StatePayload(UUID id,UUID owner,String dimension,String weapon,String displayName,Phase phase,int age,int phaseAge,int phaseDuration,
                        long serverTick,BlockPos target,int radius,int depth,long seed,boolean preview,double progress,int color,
                        double orbitalHeight,double particleScale,String camera,boolean terminal) {
        this(id,owner,dimension,weapon,displayName,phase,age,phaseAge,phaseDuration,serverTick,target,radius,depth,seed,preview,progress,
                color,orbitalHeight,particleScale,camera,terminal,ColossalMetadata.standard(radius));
    }
    public static final Type<StatePayload> TYPE = new Type<>(Nullstar.id("state_v2"));
    public static final StreamCodec<RegistryFriendlyByteBuf, StatePayload> CODEC = StreamCodec.of((b,p)-> {
        b.writeUUID(p.id);b.writeUUID(p.owner);b.writeUtf(p.dimension,128);b.writeUtf(p.weapon,32);b.writeUtf(p.displayName,96);b.writeEnum(p.phase);
        b.writeVarInt(p.age);b.writeVarInt(p.phaseAge);b.writeVarInt(p.phaseDuration);b.writeLong(p.serverTick);b.writeBlockPos(p.target);
        b.writeVarInt(p.radius);b.writeVarInt(p.depth);b.writeLong(p.seed);b.writeBoolean(p.preview);b.writeDouble(p.progress);
        b.writeInt(p.color);b.writeDouble(p.orbitalHeight);b.writeDouble(p.particleScale);b.writeUtf(p.camera,32);b.writeBoolean(p.terminal);
        var s=p.scale;b.writeEnum(s.tier());b.writeEnum(s.scene());b.writeDouble(s.cinematicScale());b.writeDouble(s.structureScale());
        b.writeDouble(s.beamWidth());b.writeDouble(s.shockwaveRadius());b.writeVarInt(s.secondaryRadius());b.writeVarInt(s.blastRadius());b.writeVarInt(s.entityRadius());
        b.writeUtf(s.script(),32);b.writeUtf(s.anchor(),16);b.writeVarInt(s.alignmentNodes());b.writeVarInt(s.subimpacts());b.writeVarInt(s.footprintRadius());b.writeVarInt(s.waves());b.writeVarInt(s.checkpoint());b.writeVarInt(s.duration());b.writeBoolean(s.committed());
    },b->new StatePayload(b.readUUID(),b.readUUID(),b.readUtf(128),b.readUtf(32),b.readUtf(96),b.readEnum(Phase.class),b.readVarInt(),
            b.readVarInt(),b.readVarInt(),b.readLong(),b.readBlockPos(),b.readVarInt(),b.readVarInt(),b.readLong(),b.readBoolean(),
            b.readDouble(),b.readInt(),b.readDouble(),b.readDouble(),b.readUtf(32),b.readBoolean(),
            new ColossalMetadata(b.readEnum(ScaleTier.class),b.readEnum(CinematicScene.class),b.readDouble(),b.readDouble(),b.readDouble(),b.readDouble(),
                    b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readUtf(32),b.readUtf(16),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readBoolean())));
    public StatePayload {
        if (id==null || owner==null || dimension==null || dimension.length()>128 || weapon==null || weapon.length()>32
                || displayName==null || displayName.length()>96 || phase==null || age<0 || phaseAge<0 || phaseDuration<1 || phaseDuration>20000
                || target==null || radius<1 || radius>1024 || depth<1 || depth>384 || !Double.isFinite(progress) || scale==null
                || progress<0 || progress>1 || !Double.isFinite(orbitalHeight) || orbitalHeight<8 || orbitalHeight>128
                || !Double.isFinite(particleScale) || particleScale<0 || particleScale>2
                || camera==null || camera.length()>32) throw new IllegalArgumentException("Invalid strike snapshot");
    }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
