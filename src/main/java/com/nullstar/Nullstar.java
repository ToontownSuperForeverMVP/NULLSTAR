package com.nullstar;

import com.nullstar.config.ConfigFiles;
import com.nullstar.config.ServerConfig;
import com.nullstar.core.WeaponCatalog;
import com.nullstar.item.WeaponControllerItem;
import com.nullstar.network.NullstarNetworking;
import com.nullstar.server.NullstarCommands;
import com.nullstar.server.WeaponEngine;
import com.nullstar.server.SoundScheduler;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.CreativeModeTabs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Nullstar implements ModInitializer {
    public static final Logger LOGGER=LoggerFactory.getLogger("nullstar");
    public static Item CONTROLLER;
    private static ServerConfig config;
    private static WeaponCatalog catalog;
    private static com.nullstar.core.ScaleCatalog scales;
    private static final WeaponEngine ENGINE=new WeaponEngine();
    public static Identifier id(String path){return Identifier.fromNamespaceAndPath("nullstar",path);}
    public static ServerConfig config(){return config;}
    public static WeaponCatalog catalog(){return catalog;}
    public static com.nullstar.core.ScaleCatalog scales(){return scales;}
    public static WeaponEngine engine(){return ENGINE;}
    public static void reload() {
        try {
            var directory=FabricLoader.getInstance().getConfigDir().resolve("nullstar");
            var next=ConfigFiles.load(directory.resolve("server.json"),ServerConfig.class,ServerConfig::new);next.validate();
            var weapons=new WeaponCatalog(directory.resolve("weapons"),next.cinematicDurationMultiplier);
            var nextScales=new com.nullstar.core.ScaleCatalog(directory.resolve("scales.json"),weapons);
            SoundScheduler.validate(weapons);
            config=next;catalog=weapons;scales=nextScales;
        } catch(Exception exception){throw new IllegalStateException("Invalid NULLSTAR configuration; fix config/nullstar before startup",exception);}
    }
    @Override public void onInitialize() {
        NullstarSounds.initialize();
        reload();
        var key=ResourceKey.create(Registries.ITEM,id("controller"));
        CONTROLLER=Registry.register(BuiltInRegistries.ITEM,key,new WeaponControllerItem(new Item.Properties().setId(key).stacksTo(1)));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(entries->entries.accept(CONTROLLER));
        NullstarNetworking.initialize();NullstarCommands.initialize();
        ServerTickEvents.END_SERVER_TICK.register(ENGINE::tick);
        ServerLifecycleEvents.SERVER_STARTED.register(ENGINE::start);
        ServerLifecycleEvents.SERVER_STOPPING.register(ENGINE::checkpoint);
        ServerLifecycleEvents.SERVER_STOPPED.register(server->ENGINE.stop());
        LOGGER.info("NULLSTAR ready: 3 weapons; {} candidate operations/tick, radius cap {}",config.blockOperationsPerTick,config.maximumRadius);
    }
}
