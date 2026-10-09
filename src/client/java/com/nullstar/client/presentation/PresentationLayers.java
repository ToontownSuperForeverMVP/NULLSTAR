package com.nullstar.client.presentation;

import com.nullstar.client.ClientStrikeState;
import net.minecraft.client.Minecraft;
import java.util.ArrayList;
import java.util.List;

/** Extension seam for render-state extraction, asset animation, audio and shader systems in Part 2. */
public final class PresentationLayers {
    public interface Layer {
        void tick(Minecraft minecraft,ClientStrikeState state,long tick);
        default void reset() {}
    }
    private static final List<Layer> LAYERS=new ArrayList<>();
    private PresentationLayers() {}
    public static void register(Layer layer){LAYERS.add(layer);}
    public static void tick(Minecraft mc,ClientStrikeState state,long tick){for(var layer:LAYERS)layer.tick(mc,state,tick);}
    public static void reset(){for(var layer:LAYERS)layer.reset();}
}
