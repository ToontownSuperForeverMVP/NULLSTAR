package com.nullstar.client;

import com.nullstar.Nullstar;
import net.minecraft.client.Minecraft;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class NullstarHud {
    private NullstarHud() {}
    public static void draw(GuiGraphicsExtractor graphics,DeltaTracker tracker) {
        Minecraft mc=Minecraft.getInstance();if(mc.player==null||mc.gui.hud.isHidden())return;
        var own=NullstarClient.STATE.strikes().values().stream().filter(s->s.state().owner().equals(mc.player.getUUID())).findFirst().orElse(null);
        if(own==null&&!mc.player.getMainHandItem().is(Nullstar.CONTROLLER))return;
        int x=8,y=20,w=Math.max(110,Math.min(244,graphics.guiWidth()/2-16));
        graphics.fill(x-4,y-4,x+w,y+59,0xbd10151e);
        String title=NullstarClient.status().displayName();
        String detail=NullstarClient.cooldown()>0?"Cooldown "+(NullstarClient.cooldown()+19)/20+"s":"Ready";
        if(own!=null) {
            title=(own.state().preview()?"PREVIEW ":"")+own.state().displayName();
            detail=own.state().phase().name().replace('_',' ');
            double progress=own.phaseProgress(NullstarClient.tickCount()+tracker.getGameTimeDeltaPartialTick(true));
            graphics.fill(x,y+25,x+w-8,y+29,0xff27313f);
            graphics.fill(x,y+25,x+(int)((w-8)*progress),y+29,0xff000000|own.state().color());
            if(own.state().progress()>0)graphics.text(mc.font,"Terrain "+Math.round(own.state().progress()*100)+"%",x,y+33,0xffaebbc9);
        }
        graphics.text(mc.font,mc.font.plainSubstrByWidth(title,w-8),x,y,0xffa7e6ff);
        graphics.text(mc.font,mc.font.plainSubstrByWidth(detail,w-8),x,y+12,0xffeeeeee);
        graphics.text(mc.font,mc.font.plainSubstrByWidth(own!=null?NullstarClient.cinematicHint():NullstarClient.activationHint(),w-8),x,y+45,0xffaebbc9);
        if(own==null)graphics.text(mc.font,mc.font.plainSubstrByWidth(NullstarClient.selectionHint(),w-8),x,y+33,0xffaebbc9);
        var target=NullstarClient.targetPreview();
        if(target!=null)graphics.text(mc.font,mc.font.plainSubstrByWidth((NullstarClient.status().hasTarget()?"LOCKED ":"AIM ")+target.getX()+" "+target.getY()+" "+target.getZ(),w-8),x,y-16,0xff8fe5ff);
        if(!NullstarClient.config().reducedFlashing)for(var cue:NullstarClient.STATE.cues())if(cue.payload().effect().equals("impact")) {
            double age=NullstarClient.tickCount()-cue.received();
            if(age<5){int alpha=(int)(32*(1-age/5));graphics.fill(0,0,graphics.guiWidth(),graphics.guiHeight(),(alpha<<24)|0xffe4b3);}
        }
    }
}
