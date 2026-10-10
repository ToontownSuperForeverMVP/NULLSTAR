package com.nullstar.test;

import com.nullstar.client.ClientConfig;
import com.nullstar.client.NullstarClient;
import com.nullstar.client.NullstarSettingsScreen;
import com.nullstar.config.ConfigFiles;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import java.nio.file.Path;

/** Verifies optional integration, persistence and navigation in a real client. */
public final class SettingsClientGameTest implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        if (!VisualCaptureClientGameTest.selected("settings")) return;
        boolean installed=FabricLoader.getInstance().isModLoaded("modmenu");
        context.runOnClient(mc->{
            try {
                Screen parent=installed?(Screen)Class.forName("com.terraformersmc.modmenu.api.ModMenuApi")
                        .getMethod("createModsScreen",Screen.class).invoke(null,new TitleScreen()):new TitleScreen();
                Screen screen=installed?(Screen)Class.forName("com.terraformersmc.modmenu.ModMenu")
                        .getMethod("getConfigScreen",String.class,Screen.class).invoke(null,"nullstar",parent):new NullstarSettingsScreen(parent);
                check(screen instanceof NullstarSettingsScreen,"Registered factory missing");
                mc.gui.setScreen(screen);
                check(screen.children().size()==13,"Client controls missing");
                for(var child:screen.children())if(child instanceof AbstractWidget w)
                    check(w.getY()>=0&&w.getY()+w.getHeight()<=screen.height,"Widget outside screen");
                boolean before=NullstarClient.config().cinematicCamera;
                ((CycleButton<?>)screen.children().get(2)).onPress(new KeyEvent(0,0,0));
                check(NullstarClient.config().cinematicCamera!=before,"Camera control failed");
                AbstractSliderButton budget=(AbstractSliderButton)screen.children().get(10);
                budget.onClick(new MouseButtonEvent(budget.getX()+budget.getWidth()-4,budget.getY()+10,new MouseButtonInfo(0,0)),false);
                check(NullstarClient.config().particleBudget==512,"Budget slider failed");
                screen.onClose();
                check(mc.gui.screen()==parent,"Parent navigation failed");
                ClientConfig saved=ConfigFiles.load(FabricLoader.getInstance().getConfigDir().resolve("nullstar/client.json"),ClientConfig.class,ClientConfig::new);
                check(saved.cinematicCamera!=before&&saved.particleBudget==512,"Save failed");
                mc.gui.setScreen(screen);
                ((Button)screen.children().get(11)).onPress(new KeyEvent(0,0,0));
                check(ConfigFiles.JSON.toJson(NullstarClient.config()).equals(ConfigFiles.JSON.toJson(new ClientConfig())),"Reset missed a preference");
            }catch(ReflectiveOperationException|java.io.IOException e){throw new AssertionError(e);}
        });
        context.waitTicks(3);
        context.takeScreenshot(TestScreenshotOptions.of(installed?"modmenu-settings":"standalone-settings").withDestinationDir(Path.of("build/evidence/modmenu")));
        context.runOnClient(mc->{
            Screen screen=mc.gui.screen();
            check(screen.keyPressed(new KeyEvent(InputConstants.KEY_ESCAPE,0,0)),"Escape did not close settings");
            check(mc.gui.screen()!=screen,"Escape did not restore parent");
            mc.gui.setScreen(null);
            com.nullstar.Nullstar.LOGGER.info("NULLSTAR settings: factory, controls, persistence, defaults and parent navigation passed (Mod Menu installed: {})",installed);});
    }
    private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
}
