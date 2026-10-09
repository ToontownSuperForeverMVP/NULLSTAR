package com.nullstar.client;

import com.nullstar.Nullstar;
import com.nullstar.client.camera.CinematicCamera;
import com.nullstar.client.presentation.EffectRenderer;
import com.nullstar.client.presentation.PresentationLayers;
import com.nullstar.config.ConfigFiles;
import com.nullstar.core.WeaponCatalog;
import com.nullstar.network.*;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;

public final class NullstarClient implements ClientModInitializer {
    public static final ClientStrikeState STATE=new ClientStrikeState();
    public static final CinematicCamera CAMERA=new CinematicCamera();
    private static final EffectRenderer EFFECTS=new EffectRenderer();
    private static ClientConfig config;
    private static long tick,sequence,statusReceived;
    private static StatusPayload status=new StatusPayload("helios","",false,BlockPos.ZERO,0);
    private static BlockPos preview;
    private static net.minecraft.client.multiplayer.ClientLevel observedLevel;
    private static KeyMapping activate,select,skip,camera;
    public static ClientConfig config(){return config;}
    public static long tickCount(){return tick;}
    public static BlockPos targetPreview(){return status.hasTarget()?status.target():preview;}
    public static StatusPayload status(){return status;}
    public static int cooldown(){return Math.max(0,status.cooldown()-(int)(tick-statusReceived));}
    @Override public void onInitializeClient() {
        try{config=ConfigFiles.load(FabricLoader.getInstance().getConfigDir().resolve("nullstar/client.json"),ClientConfig.class,ClientConfig::new);config.validate();}
        catch(Exception exception){Nullstar.LOGGER.error("Invalid client settings; using defaults",exception);config=new ClientConfig();}
        var category=KeyMapping.Category.register(Nullstar.id("controls"));
        activate=KeyMappingHelper.registerKeyMapping(new KeyMapping("key.nullstar.activate",InputConstants.KEY_G,category));
        select=KeyMappingHelper.registerKeyMapping(new KeyMapping("key.nullstar.select",InputConstants.KEY_V,category));
        skip=KeyMappingHelper.registerKeyMapping(new KeyMapping("key.nullstar.skip",InputConstants.KEY_K,category));
        camera=KeyMappingHelper.registerKeyMapping(new KeyMapping("key.nullstar.camera",InputConstants.KEY_C,category));
        ClientPlayNetworking.registerGlobalReceiver(StatePayload.TYPE,(payload,ctx)->STATE.snapshot(payload,tick));
        ClientPlayNetworking.registerGlobalReceiver(VisualPayload.TYPE,(payload,ctx)->STATE.cue(payload,tick));
        ClientPlayNetworking.registerGlobalReceiver(StatusPayload.TYPE,(payload,ctx)->{status=payload;statusReceived=tick;});
        ClientPlayConnectionEvents.DISCONNECT.register((handler,client)->{reset();sequence=0;});
        ClientPlayConnectionEvents.JOIN.register((handler,sender,client)->sequence=0);
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
        HudElementRegistry.addLast(Nullstar.id("controller_hud"),NullstarHud::draw);
    }
    private void tick(Minecraft mc) {
        if(mc.level==null||mc.player==null){reset();return;}
        if(observedLevel!=mc.level){STATE.clear();CAMERA.reset();EFFECTS.clear();PresentationLayers.reset();observedLevel=mc.level;
            status=new StatusPayload(status.weapon(),"",false,BlockPos.ZERO,status.cooldown());}
        if(mc.isPaused())return;
        tick++;STATE.tick(tick,mc.level.dimension().identifier().toString());
        while(skip.consumeClick()){CAMERA.skip();request(RequestPayload.SKIP);}
        while(camera.consumeClick()) {
            config.cinematicCamera=!config.cinematicCamera;CAMERA.skip();
            try{java.nio.file.Files.writeString(FabricLoader.getInstance().getConfigDir().resolve("nullstar/client.json"),ConfigFiles.JSON.toJson(config));}
            catch(Exception exception){Nullstar.LOGGER.error("Could not save camera preference",exception);}
        }
        if(mc.gui.screen()==null) {
            while(activate.consumeClick())request(status.hasTarget()?RequestPayload.ACTIVATE:RequestPayload.ACQUIRE);
            while(select.consumeClick()) {
                String next=WeaponCatalog.IDS.get((Math.max(0,WeaponCatalog.IDS.indexOf(status.weapon()))+1)%WeaponCatalog.IDS.size());
                if(ClientPlayNetworking.canSend(RequestPayload.TYPE))ClientPlayNetworking.send(new RequestPayload(1,++sequence,RequestPayload.SELECT,next,false));
            }
        }
        preview=null;
        if(mc.player.getMainHandItem().is(Nullstar.CONTROLLER)) {
            var eye=mc.player.getEyePosition();var end=eye.add(mc.player.getViewVector(1).scale(128));
            var hit=mc.level.clip(new ClipContext(eye,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,mc.player));
            if(hit.getType()==HitResult.Type.BLOCK)preview=hit.getBlockPos();
        }
        EFFECTS.tick(mc,tick);PresentationLayers.tick(mc,STATE,tick);
    }
    private static void request(int action) {
        if(ClientPlayNetworking.canSend(RequestPayload.TYPE))ClientPlayNetworking.send(new RequestPayload(1,++sequence,action,status.weapon(),false));
    }
    public static void reset(){STATE.clear();CAMERA.reset();EFFECTS.clear();PresentationLayers.reset();preview=null;observedLevel=null;tick=0;statusReceived=0;status=new StatusPayload("helios","",false,BlockPos.ZERO,0);}
}
