package com.nullstar.test;

import com.nullstar.Nullstar;
import com.nullstar.client.ClientConfig;
import com.nullstar.client.NullstarClient;
import com.nullstar.core.CinematicScene;
import com.nullstar.core.ColossalMetadata;
import com.nullstar.core.Phase;
import com.nullstar.core.ScaleCatalog;
import com.nullstar.core.ScaleProfile;
import com.nullstar.core.ScaleTier;
import com.nullstar.network.StatePayload;
import net.minecraft.core.BlockPos;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Real-client visual evidence: builds a disposable diorama (plateau, ridge, forest, pond), fires each weapon at
 * ULTRA quality and captures frames at authored story beats. Also records renderer diagnostics per frame.
 * Never touches the user's saves; worlds live under build/run/clientGameTest.
 */
public final class VisualCaptureClientGameTest implements FabricClientGameTest {
    static final int GROUND=-40;
    private static final Path OUT=Path.of("build/evidence/part2");
    /** Part 2.5B colossal frames and their measurements are kept apart from the Part 2 capture set. */
    private static final Path COLOSSAL_OUT=Path.of("build/evidence/part25b");
    private final List<String> diagnostics=new ArrayList<>();
    private TestSingleplayerContext current;

    /** -Ptests=regression,dedicated,capture selects client suites (default: all). */
    static boolean selected(String suite){return System.getProperty("nullstar.tests","regression,dedicated,capture").contains(suite);}
    @Override public void runTest(ClientGameTestContext context) {
        if(!selected("capture"))return;
        String selection=System.getProperty("nullstar.capture","helios,singularity,glassfall");
        // Each scene gets a fresh, identical diorama so earlier craters never invalidate later targets.
        for(String scene:new String[]{"helios","singularity","glassfall","colossal","modes","stress"}) {
            if(!selection.contains(scene))continue;
            try(TestSingleplayerContext world=context.worldBuilder().adjustSettings(settings->settings.setAllowCommands(true)).create()) {
                current=world;
                prepare(context,world);
                switch(scene) {
                    case "helios" -> {shot(context,"00_diorama");helios(context,world);}
                    case "singularity" -> singularity(context,world);
                    case "glassfall" -> glassfall(context,world);
                    case "colossal" -> colossal(context,world);
                    case "stress" -> stress(context,world);
                    default -> modes(context,world);
                }
                world.getServer().runOnServer(server->Nullstar.config().cooldownMultiplier=1);
            }
        }
        try{Files.createDirectories(OUT);Files.createDirectories(COLOSSAL_OUT);var recorded=List.copyOf(diagnostics);
            Files.write(OUT.resolve("renderer-diagnostics.txt"),recorded);
            Files.write(COLOSSAL_OUT.resolve("renderer-diagnostics.txt"),recorded);}catch(Exception exception){throw new RuntimeException(exception);}
    }
    private static void prepare(ClientGameTestContext context,TestSingleplayerContext world) {
        world.getConnection().waitForChunksDownload();
        world.getServer().runCommand("gamemode creative @p");
        // Fixed noon lighting so frames are comparable regardless of camera heading (no sunrise glow).
        world.getServer().runCommand("time set noon");
        diorama(world);
        world.getServer().runCommand("tp @p 46 "+(GROUND+14)+" 58 -140 18");
        world.getServer().runOnServer(server->{var p=world.getConnection().getServerPlayer();p.getAbilities().flying=true;p.onUpdateAbilities();Nullstar.config().cooldownMultiplier=.1;});
        world.getServer().runCommand("give @p nullstar:controller");
        context.runOnClient(mc->{var c=NullstarClient.config();c.quality=ClientConfig.Quality.ULTRA;c.particleIntensity=1;c.cinematicCamera=true;c.reducedMotion=false;});
        context.waitTicks(40);world.getConnection().waitForChunksRender();
    }

    private static void fill(TestSingleplayerContext world,int x0,int y0,int z0,int x1,int y1,int z1,String block) {
        for(int y=y0;y<=y1;y++)world.getServer().runCommand("fill "+x0+" "+y+" "+z0+" "+x1+" "+y+" "+z1+" "+block);
    }
    /** Deep plateau so craters never reach the flat world's bedrock, with slopes, trees and water for scale. */
    static void diorama(TestSingleplayerContext world) {
        fill(world,-56,-60,-56,56,GROUND-4,56,"minecraft:stone");
        fill(world,-56,GROUND-3,-56,56,GROUND-1,56,"minecraft:dirt");
        fill(world,-56,GROUND,-56,56,GROUND,56,"minecraft:grass_block");
        for(int k=0;k<12;k++) {
            int x=26+k*2;
            fill(world,x,GROUND+k,-56,56,GROUND+k,56,"minecraft:stone");
            fill(world,x,GROUND+k+1,-56,56,GROUND+k+1,56,"minecraft:grass_block");
        }
        fill(world,-38,GROUND-3,18,-24,GROUND,32,"minecraft:water");
        fill(world,-40,GROUND-3,32,-40,GROUND,32,"minecraft:stone");
        int[][] trees={{-14,-22},{-22,-10},{-30,-26},{-8,-34},{12,-30},{18,-18},{-26,4},{-34,-4},{-18,26},{-6,30},{8,34},{16,24},
                {22,8},{-44,-40},{-46,10},{-10,44},{40,-30},{46,-12},{44,20},{30,40},{-40,40},{4,-46},{-20,-46},{34,-44}};
        for(int[] tree:trees) {
            int x=tree[0],z=tree[1];
            int y=x>=26?GROUND+1+Math.min(11,(x-26)/2+1):GROUND+1;
            world.getServer().runCommand("place feature minecraft:"+((x+z)%3==0?"birch":"fancy_oak")+" "+x+" "+y+" "+z);
        }
        world.getServer().runCommand("kill @e[type=item]");
        world.getConnection().waitForChunksRender();
    }

    private void helios(ClientGameTestContext context,TestSingleplayerContext world) {
        world.getServer().runCommand("execute as @p at @s run nullstar fire helios 0 "+GROUND+" 0");
        waitStrike(context);
        int[] ages={8,34,52,74,92,106,118,126,134,150,162,171,174,180,190,215,260};
        String[] names={"acquire","charge_warning","reveal","descended","align_rings","align_vanes","core_open","discharge","beam_mid","entry","entry_low",
                "impact_flash","impact_core","shockwaves","pressure_wave","aftermath","aftermath_late"};
        captureAges(context,"helios",ages,names);
        finish(context,world,"helios");
    }
    private void singularity(ClientGameTestContext context,TestSingleplayerContext world) {
        world.getServer().runCommand("execute as @p at @s run nullstar fire singularity 0 "+GROUND+" 0");
        waitStrike(context);
        int[] ages={30,70,100,130,170,200,214,219,222,226,236,260,300};
        String[] names={"formation","accretion","charged","attraction","attraction_late","destabilize","contraction","silence","implosion","shockwave","release","aftermath","aftermath_late"};
        captureAges(context,"singularity",ages,names);
        finish(context,world,"singularity");
    }
    private void glassfall(ClientGameTestContext context,TestSingleplayerContext world) {
        world.getServer().runCommand("execute as @p at @s run nullstar fire glassfall 0 "+GROUND+" 0");
        waitStrike(context);
        int[] ages={40,64,84,96,112,121,140,158,188,208,236,252,261,264,270,290,330};
        String[] names={"lattice_deploy","lattice_lock","rails_aim","first_launch","first_descent","first_impact","second_impact","mid_barrage","third_impact",
                "fourth_descent","final_launch","final_descent","final_impact","energy_dome","angular_shock","aftermath","aftermath_late"};
        captureAges(context,"glassfall",ages,names);
        finish(context,world,"glassfall");
    }
    /** Accessibility and quality modes: observer view with cinematics disabled, reduced motion, LOW preset. */
    private void modes(ClientGameTestContext context,TestSingleplayerContext world) {
        // Interface evidence: aiming reticle/HUD from the player's view, then the visual settings screen.
        world.getServer().runCommand("tp @p 30 "+(GROUND+10)+" 30 135 25");
        context.waitTicks(20);
        shot(context,"modes_ui_aim_reticle");
        context.setScreen(()->new com.nullstar.client.NullstarSettingsScreen(null));
        context.waitTicks(5);
        shot(context,"modes_ui_settings_screen");
        context.setScreen(()->null);
        context.waitTicks(5);
        context.runOnClient(mc->{var c=NullstarClient.config();c.reducedMotion=true;c.quality=ClientConfig.Quality.MEDIUM;});
        world.getServer().runCommand("tp @p 30 "+(GROUND+10)+" 30 135 25");
        world.getServer().runCommand("execute as @p at @s run nullstar fire helios -28 "+GROUND+" -28");
        waitStrike(context);
        captureAges(context,"modes_reduced_motion_medium",new int[]{110,130,175},new String[]{"core","beam","impact"});
        finish(context,world,"modes_reduced_motion");
        context.runOnClient(mc->{var c=NullstarClient.config();c.quality=ClientConfig.Quality.LOW;});
        world.getServer().runCommand("execute as @p at @s run nullstar fire helios 22 "+GROUND+" -30");
        waitStrike(context);
        captureAges(context,"modes_low",new int[]{110,130,175},new String[]{"core","beam","impact"});
        finish(context,world,"modes_low");
        context.runOnClient(mc->{var c=NullstarClient.config();c.quality=ClientConfig.Quality.ULTRA;c.reducedMotion=false;mc.options.improvedTransparency().set(true);});
        world.getServer().runCommand("execute as @p at @s run nullstar fire helios 0 "+GROUND+" 30");
        waitStrike(context);
        captureAges(context,"modes_improved_transparency",new int[]{110,130,175},new String[]{"core","beam","impact"});
        finish(context,world,"modes_oit");
        context.runOnClient(mc->mc.options.improvedTransparency().set(false));
    }

    /**
     * Presentation stress: four concurrent synthetic strikes (the default active-strike cap) at heavy phases, injected
     * into the client cache only. Measures extraction cost, vertex/particle bounds and frame time; never touches the server.
     */
    private void stress(ClientGameTestContext context,TestSingleplayerContext world) {
        world.getServer().runCommand("tp @p 0 "+(GROUND+40)+" 70 180 30");
        context.waitTicks(20);world.getConnection().waitForChunksRender();
        String[][] plan={{"helios","ATMOSPHERIC_ENTRY","-26","-26"},{"helios","AFTERMATH","26","-26"},{"singularity","FIRING","-26","26"},{"glassfall","FIRING","26","26"}};
        java.util.UUID[] ids=new java.util.UUID[plan.length];
        for(int i=0;i<ids.length;i++)ids[i]=java.util.UUID.randomUUID();
        for(int step=0;step<6;step++) {
            final int s=step;
            context.runOnClient(mc->{
                long tick=NullstarClient.tickCount();
                for(int i=0;i<plan.length;i++) {
                    var def=Nullstar.catalog().get(plan[i][0]);
                    Phase phase=Phase.valueOf(plan[i][1]);
                    int duration=def.phases().stream().filter(p->p.phase()==phase).findFirst().orElseThrow().ticks();
                    int phaseAge=Math.min(duration-1,(s*20)%duration);
                    var pos=new net.minecraft.core.BlockPos(Integer.parseInt(plan[i][2]),GROUND,Integer.parseInt(plan[i][3]));
                    NullstarClient.STATE.snapshot(new com.nullstar.network.StatePayload(ids[i],java.util.UUID.randomUUID(),mc.level.dimension().identifier().toString(),def.id(),def.name(),
                            phase,200+s*20,phaseAge,duration,tick+s,pos,Math.min(24,def.impact().radius()),Math.min(24,def.impact().depth()),i*31L,true,0,
                            def.presentation().color(),def.presentation().orbitalHeight(),def.presentation().particleScale(),"none",false),tick);
                    if(s%2==0)NullstarClient.STATE.cue(new com.nullstar.network.VisualPayload(ids[i],s,s%4==0?"impact":"projectile",pos.offset(s%3*3,0,0),
                            plan[i][0].equals("glassfall")?6:16,plan[i][0].equals("glassfall")&&s%4!=0?30:40),tick);
                }
            });
            context.waitTicks(10);
            shot(context,"stress_"+step+"_four_concurrent");
            context.waitTicks(10);
        }
        context.runOnClient(mc->NullstarClient.STATE.clear());
    }

    /** One authored beat of the colossal measurement pass: the phase, the normalized scene it stages and its shot. */
    private record Beat(Phase phase,CinematicScene scene,double progress,String name,String camera) {}

    /**
     * Part 2.5B colossal presentation measurement. One synthetic APOCALYPSE Helios strike is injected into the client
     * presentation cache at every magnitude boundary the client derives from the authoritative operator profile, so
     * the cosmic act and the colossal world devastation are captured and their vertex/particle/extraction/frame cost
     * is recorded instead of estimated. Client-only, exactly like the existing stress scene: no server strike is
     * created, no terrain changes and no client value becomes gameplay input.
     */
    private void colossal(ClientGameTestContext context,TestSingleplayerContext world) {
        try{Files.createDirectories(COLOSSAL_OUT);}catch(Exception exception){throw new RuntimeException(exception);}
        world.getServer().runCommand("tp @p 150 -20 150 225 22");
        world.getServer().runCommand("time set noon");
        context.runOnClient(mc->{var c=NullstarClient.config();c.quality=ClientConfig.Quality.ULTRA;
            c.particleIntensity=1;c.cinematicCamera=true;c.reducedMotion=false;});
        context.waitTicks(30);world.getConnection().waitForChunksRender();
        ScaleProfile profile=ScaleCatalog.defaults().stream()
                .filter(p->p.weapon().equals("helios")&&p.tier()==ScaleTier.APOCALYPSE).findFirst().orElseThrow();
        // The strike is owned by the local player: ownership decides camera detachment and the detached cosmic act.
        java.util.UUID id=java.util.UUID.randomUUID();
        String dimension=world.getServer().computeOnServer(server->
                world.getConnection().getServerLevel().dimension().identifier().toString());
        Beat[] beats={
                new Beat(Phase.ASCENT,CinematicScene.ASCENT,.55,"00_ascent","cosmic_montage"),
                new Beat(Phase.PLANETARY_VIEW,CinematicScene.PLANET,.50,"01_planet","cosmic_montage"),
                new Beat(Phase.SOLAR_SYSTEM_VIEW,CinematicScene.SOLAR_SYSTEM,.50,"02_solar_system","cosmic_montage"),
                new Beat(Phase.GALACTIC_VIEW,CinematicScene.GALAXY,.50,"03_galaxy","cosmic_montage"),
                new Beat(Phase.COSMIC_BOUNDARY_VIEW,CinematicScene.COSMIC_BOUNDARY,.50,"04_universe","cosmic_montage"),
                new Beat(Phase.MOTHERSHIP_REVEAL,CinematicScene.STELLAR_MOTHERSHIP,.45,"05_mothership_reveal","cosmic_montage"),
                new Beat(Phase.DEPLOYMENT,CinematicScene.STELLAR_MOTHERSHIP,.90,"06_deployment","cosmic_montage"),
                new Beat(Phase.CHARGING,CinematicScene.STELLAR_MOTHERSHIP,.70,"07_charging","cosmic_montage"),
                new Beat(Phase.FIRING,CinematicScene.STELLAR_MOTHERSHIP,.60,"08_stellar_lance","cosmic_montage"),
                new Beat(Phase.ATMOSPHERIC_ENTRY,CinematicScene.WORLD,.50,"09_descent","helios_pursuit"),
                new Beat(Phase.IMPACT,CinematicScene.WORLD,.35,"10_impact","helios_annihilation"),
                new Beat(Phase.DEVASTATION_WAVE_1,CinematicScene.WORLD,.60,"11_pressure_wall","helios_pressure"),
                new Beat(Phase.AFTERMATH,CinematicScene.WORLD,.50,"12_aftermath","helios_sweep")};
        for(int i=0;i<beats.length;i++) {
            final Beat beat=beats[i];final int index=i;final int duration=profile.durationTicks();
            context.runOnClient(mc->{
                long tick=NullstarClient.tickCount();
                java.util.UUID owner=mc.player.getUUID();
                var metadata=new ColossalMetadata(profile.tier(),beat.scene(),profile.cinematicScale(),profile.structureScale(),
                        profile.beamWidth(),profile.shockwaveRadius(),profile.secondaryRadius(),profile.blastRadius(),profile.entityRadius(),
                        profile.script(),profile.anchor(),profile.alignmentNodes(),profile.subimpacts(),profile.radius(),profile.waves(),0,
                        duration,false);
                long stamp=tick+index*20L;
                NullstarClient.STATE.snapshot(new StatePayload(id,owner,dimension,"helios","HELIOS ANNIHILATOR",beat.phase(),
                        200+index*20,(int)Math.min(duration-1,duration*beat.progress()),duration,stamp,new BlockPos(0,GROUND,0),
                        profile.radius(),profile.depth(),7L,false,beat.progress(),0xffaa00,72,1,beat.camera(),false,metadata),tick);
            });
            context.waitTicks(12);
            shot(context,"colossal_"+beat.name(),COLOSSAL_OUT);
        }
        context.runOnClient(mc->NullstarClient.STATE.clear());
    }

    private static void waitStrike(ClientGameTestContext context) {
        context.waitFor(mc->!NullstarClient.STATE.strikes().isEmpty(),200);
    }
    private static double age(ClientGameTestContext context) {
        return context.computeOnClient(mc->NullstarClient.STATE.strikes().values().stream().findFirst()
                .map(s->s.age(NullstarClient.tickCount())).orElse(9999.0));
    }
    private void captureAges(ClientGameTestContext context,String weapon,int[] ages,String[] names) {
        for(int i=0;i<ages.length;i++) {
            int wanted=ages[i];
            double now=age(context);
            if(now>=9999)break;
            if(wanted>now)context.waitTicks((int)Math.round(wanted-now));
            shot(context,weapon+"_"+String.format("%02d",i)+"_"+names[i]);
        }
    }
    private void finish(ClientGameTestContext context,TestSingleplayerContext world,String label) {
        world.getServer().waitFor(server->Nullstar.engine().active().isEmpty(),900);
        world.getConnection().waitForClientboundPackets();context.waitTicks(30);
        context.waitFor(mc->!NullstarClient.CAMERA.applied(),100);
        shot(context,label+"_zz_crater_player_view");
        context.waitTicks(80);
    }
    private void shot(ClientGameTestContext context,String name) {shot(context,name,OUT);}
    /**
     * Captures one frame and records the renderer diagnostics beside it: phase, age, FPS, measured frame time,
     * extraction cost, submitted/dropped vertices and the live cosmetic particle count.
     */
    private void shot(ClientGameTestContext context,String name,Path destination) {
        context.runOnClient(mc->mc.gui.hud.getChat().clearMessages(true));
        context.waitTick();
        String info=context.computeOnClient(mc->{
            var strike=NullstarClient.STATE.strikes().values().stream().findFirst();
            String phase=strike.map(s->s.state().phase().name()+" age="+Math.round(s.age(NullstarClient.tickCount()))).orElse("idle");
            return name+" | "+phase+" | fps="+mc.getFps()+String.format(" frame=%.2fms",mc.getFrameTimeNs()/1e6)+" | "+NullstarClient.SCENE.diagnostics();
        });
        if(current!=null){double mspt=current.getServer().computeOnServer(server->server.getAverageTickTimeNanos()/1e6);info+=String.format(" | serverTick=%.2fms",mspt);}
        diagnostics.add(info);
        context.takeScreenshot(TestScreenshotOptions.of(name).withDestinationDir(destination));
    }
}
