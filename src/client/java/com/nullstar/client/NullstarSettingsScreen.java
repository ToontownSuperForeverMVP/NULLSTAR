package com.nullstar.client;

import com.nullstar.Nullstar;
import com.nullstar.config.ConfigFiles;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;

/** Client-only visual and accessibility preferences. Saved to config/nullstar/client.json; never sent to servers. */
public final class NullstarSettingsScreen extends Screen {
    private final Screen parent;
    public NullstarSettingsScreen(Screen parent){super(Component.translatable("nullstar.settings.title"));this.parent=parent;}

    @Override protected void init() {
        ClientConfig c=NullstarClient.config();
        int w=150,left=width/2-w-4,right=width/2+4,y=height/6,row=24;
        addRenderableWidget(CycleButton.<ClientConfig.Quality>builder(q->Component.literal(q.name()),c.quality)
                .withValues(ClientConfig.Quality.values()).create(left,y,w,20,Component.translatable("nullstar.settings.quality"),(b,v)->c.quality=v));
        addRenderableWidget(slider(right,y,w,"nullstar.settings.particles",c.particleIntensity/2,v->c.particleIntensity=v*2,v->Math.round(v*200)+"%"));
        addRenderableWidget(CycleButton.onOffBuilder(c.cinematicCamera).create(left,y+row,w,20,Component.translatable("nullstar.settings.camera"),(b,v)->c.cinematicCamera=v));
        addRenderableWidget(slider(right,y+row,w,"nullstar.settings.shake",c.cameraShakeIntensity,v->c.cameraShakeIntensity=v,v->Math.round(v*100)+"%"));
        addRenderableWidget(CycleButton.onOffBuilder(c.reducedMotion).create(left,y+row*2,w,20,Component.translatable("nullstar.settings.reduced_motion"),(b,v)->c.reducedMotion=v));
        addRenderableWidget(CycleButton.onOffBuilder(c.reducedFlashing).create(right,y+row*2,w,20,Component.translatable("nullstar.settings.reduced_flashing"),(b,v)->c.reducedFlashing=v));
        addRenderableWidget(CycleButton.onOffBuilder(c.screenDistortion).create(left,y+row*3,w,20,Component.translatable("nullstar.settings.distortion"),(b,v)->c.screenDistortion=v));
        addRenderableWidget(slider(right,y+row*3,w,"nullstar.settings.glow",c.glowIntensity/1.5,v->c.glowIntensity=v*1.5,v->Math.round(v*150)+"%"));
        addRenderableWidget(slider(left,y+row*4,w,"nullstar.settings.audio",c.cinematicAudio,v->c.cinematicAudio=v,v->Math.round(v*100)+"%"));
        addRenderableWidget(CycleButton.onOffBuilder(c.letterbox).create(right,y+row*4,w,20,Component.translatable("nullstar.settings.letterbox"),(b,v)->c.letterbox=v));
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE,b->onClose()).bounds(width/2-100,y+row*6,200,20).build());
    }
    private static AbstractSliderButton slider(int x,int y,int w,String key,double initial,DoubleConsumer apply,DoubleFunction<String> label) {
        return new AbstractSliderButton(x,y,w,20,Component.empty(),Math.max(0,Math.min(1,initial))) {
            {updateMessage();}
            @Override protected void updateMessage(){setMessage(Component.translatable(key).append(": "+label.apply(value)));}
            @Override protected void applyValue(){apply.accept(value);}
        };
    }
    @Override public void extractRenderState(GuiGraphicsExtractor graphics,int mouseX,int mouseY,float a) {
        super.extractRenderState(graphics,mouseX,mouseY,a);
        graphics.centeredText(font,title,width/2,height/6-18,0xffe8f8ff);
        graphics.centeredText(font,Component.literal("Client presentation only - server destruction limits are unaffected."),width/2,height/6+24*5+4,0xff8a96a4);
    }
    @Override public void onClose() {
        ClientConfig c=NullstarClient.config();c.validate();
        try{java.nio.file.Files.writeString(FabricLoader.getInstance().getConfigDir().resolve("nullstar/client.json"),ConfigFiles.JSON.toJson(c));}
        catch(Exception exception){Nullstar.LOGGER.error("Could not save NULLSTAR client settings",exception);}
        minecraft.gui.setScreen(parent);
    }
}
