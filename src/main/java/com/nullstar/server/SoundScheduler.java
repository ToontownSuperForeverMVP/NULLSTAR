package com.nullstar.server;

import com.nullstar.config.ServerConfig;
import com.nullstar.core.WeaponDefinition;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;

public final class SoundScheduler {
    private SoundScheduler() {}
    public static void phase(ServerLevel level,BlockPos position,WeaponDefinition.Stage stage,WeaponDefinition def,ServerConfig config) {
        if(stage.sound()==null || stage.sound().isBlank() || config.soundVolumeMultiplier==0)return;
        BuiltInRegistries.SOUND_EVENT.getOptional(Identifier.withDefaultNamespace(stage.sound())).ifPresent(sound->
                level.playSound(null,position,sound,SoundSource.PLAYERS,(float)(2*config.soundVolumeMultiplier),def.presentation().soundPitch()));
    }
}
