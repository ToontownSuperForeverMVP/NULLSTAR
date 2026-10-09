package com.nullstar.client;

import com.nullstar.config.ServerConfig;

public final class ClientConfig {
    public boolean cinematicCamera=true;
    public boolean reducedMotion=false;
    public boolean reducedFlashing=true;
    public int particleBudget=192;
    public double cameraShakeIntensity=.35;
    public void validate() {
        particleBudget=ServerConfig.clamp(particleBudget,0,512);
        cameraShakeIntensity=ServerConfig.finite(cameraShakeIntensity,0,1,.35);
    }
}
