package com.nullstar.server;

import com.nullstar.config.ServerConfig;
import com.nullstar.core.WeaponDefinition;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.world.phys.Vec3;

public final class SoundScheduler {
    private SoundScheduler() {}
    public static void phase(ServerLevel level,BlockPos position,WeaponDefinition.Stage stage,WeaponDefinition def,ServerConfig config) {
        if(stage.sound()==null || stage.sound().isBlank() || config.soundVolumeMultiplier==0)return;
        BuiltInRegistries.SOUND_EVENT.getOptional(Identifier.withDefaultNamespace(stage.sound())).ifPresent(sound-> {
            boolean orbital=stage.phase()==com.nullstar.core.Phase.CHARGING || stage.phase()==com.nullstar.core.Phase.ORBITAL_ALIGNMENT
                    || stage.phase()==com.nullstar.core.Phase.FIRING && def.behavior().equals("helios");
            double height=orbital?(def.behavior().equals("singularity")?4:def.presentation().orbitalHeight()):0;
            Vec3 source=Vec3.atCenterOf(position).add(0,height,0);
            var packet=new ClientboundSoundPacket(Holder.direct(sound),SoundSource.PLAYERS,source.x,source.y,source.z,
                    (float)(8*config.soundVolumeMultiplier),def.presentation().soundPitch(),level.getRandom().nextLong());
            // Vanilla's player-position radius filter would drop cues while the visual camera is near the strike.
            for(var player:level.players())if(player.blockPosition().distSqr(position)<=256*256)player.connection.send(packet);
        });
    }
}
