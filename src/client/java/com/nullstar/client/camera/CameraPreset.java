package com.nullstar.client.camera;

import com.nullstar.client.ClientStrikeState;
import net.minecraft.world.phys.Vec3;

/** Generic shot sampler. Presets may return null to decline a frame (for example, a target with no sky access). */
@FunctionalInterface
public interface CameraPreset {
    record Context(ClientStrikeState.Strike strike,double tick,Vec3 target,double progress) {}
    /**
     * @param roll   camera roll in degrees
     * @param rumble continuous shake request, 0..1, scaled by the user's shake preference
     */
    record Frame(Vec3 position,Vec3 lookAt,float fov,float roll,float rumble) {
        public Frame(Vec3 position,Vec3 lookAt,float fov){this(position,lookAt,fov,0,0);}
    }
    Frame sample(Context context);
}
