package com.nullstar.client.mixin;

import com.nullstar.client.NullstarClient;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 26.3 camera seam after vanilla alignment/FOV, before both culling and projection are built. */
@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow private Vec3 position;
    @Shadow private float yRot;
    @Shadow private float xRot;
    @Shadow private float fov;
    @Shadow private boolean detached;
    @Shadow protected abstract void setPosition(Vec3 position);
    @Shadow protected abstract void setRotation(float yaw,float pitch);
    @Inject(method="update",at=@At(value="INVOKE",target="Lnet/minecraft/client/Camera;getViewRotationMatrix(Lorg/joml/Matrix4f;)Lorg/joml/Matrix4f;"))
    private void nullstar$apply(DeltaTracker tracker,CallbackInfo ci) {
        var pose=NullstarClient.CAMERA.sample(position,yRot,xRot,fov,NullstarClient.tickCount()+tracker.getGameTimeDeltaPartialTick(true));
        if(pose!=null){setPosition(pose.position());setRotation(pose.yaw(),pose.pitch());fov=pose.fov();detached=true;}
    }
    @Inject(method="extractRenderState",at=@At("RETURN"))
    private void nullstar$detachedState(CameraRenderState state,DeltaTracker tracker,CallbackInfo ci) {
        if(NullstarClient.CAMERA.applied()){state.isFirstPerson=false;state.smartCull=false;}
    }
}
