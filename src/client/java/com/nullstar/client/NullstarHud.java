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
        int x=12,y=graphics.guiHeight()-78,w=244;
        graphics.fill(x-4,y-4,x+w,y+44,0xbd10151e);
        String title=NullstarClient.status().weapon().toUpperCase(java.util.Locale.ROOT);
        String detail=NullstarClient.cooldown()>0?"Cooldown "+(NullstarClient.cooldown()+19)/20+"s":"Ready";
        if(own!=null) {
            title=own.state().weapon().toUpperCase(java.util.Locale.ROOT)+(own.state().preview()?" [PREVIEW]":"");
            detail=own.state().phase().name().replace('_',' ');
            double progress=own.phaseProgress(NullstarClient.tickCount()+tracker.getGameTimeDeltaPartialTick(true));
            graphics.fill(x,y+25,x+w-8,y+29,0xff27313f);
            graphics.fill(x,y+25,x+(int)((w-8)*progress),y+29,0xff000000|own.state().color());
            if(own.state().progress()>0)detail+=" | terrain "+Math.round(own.state().progress()*100)+"%";
        }
        graphics.text(mc.font,title,x,y,0xffa7e6ff);
        graphics.text(mc.font,detail,x,y+12,0xffeeeeee);
        graphics.text(mc.font,own!=null?"K: skip camera  C: camera toggle":"Use: designate / fire  Shift-use: select",x,y+33,0xffaebbc9);
        var target=NullstarClient.targetPreview();
        if(target!=null)graphics.text(mc.font,(NullstarClient.status().hasTarget()?"LOCKED ":"AIM ")+target.getX()+" "+target.getY()+" "+target.getZ(),x,y-16,0xff8fe5ff);
        if(!NullstarClient.config().reducedFlashing)for(var cue:NullstarClient.STATE.cues())if(cue.payload().effect().equals("impact")) {
            double age=NullstarClient.tickCount()-cue.received();
            if(age<5){int alpha=(int)(32*(1-age/5));graphics.fill(0,0,graphics.guiWidth(),graphics.guiHeight(),(alpha<<24)|0xffe4b3);}
        }
    }
}
