package com.nullstar.client.camera;

import com.nullstar.client.ClientStrikeState;
import net.minecraft.world.phys.Vec3;

@FunctionalInterface
public interface CameraPreset {
    record Context(ClientStrikeState.Strike strike,double tick,Vec3 target,double progress) {}
    record Frame(Vec3 position,Vec3 lookAt,float fov) {}
    Frame sample(Context context);
}
