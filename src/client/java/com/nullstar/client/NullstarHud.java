package com.nullstar.client;

import com.nullstar.Nullstar;
import com.nullstar.core.Phase;
import com.nullstar.core.WeaponCatalog;
import com.nullstar.network.StatePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import java.util.UUID;

/** Holographic controller HUD and cinematic screen effects. Reads only client presentation state. */
public final class NullstarHud {
    private static final Identifier ICONS=Nullstar.id("textures/gui/weapons.png");
    private static long selectedAt=-100,lockedAt=-1,terminalAt=-200;
    private static String lastWeapon,terminalText="";
    private static int terminalColor;
    private static boolean wasLocked;
    private static double letterbox;
    private NullstarHud() {}

    /** Records terminal snapshots for the owner's banner before the cache discards the strike. */
    public static void onState(StatePayload payload,UUID player) {
        if(!payload.terminal()||player==null||!payload.owner().equals(player))return;
        boolean cancelled=payload.phase()==Phase.CANCELLED;
        terminalText=cancelled?"STRIKE ABORTED":"STRIKE COMPLETE";
        terminalColor=cancelled?0xffff5a5a:0xff7fffb0;
        terminalAt=NullstarClient.tickCount();
    }
    /** Disconnect resets the client tick clock; clear transient banners so they cannot reappear. */
    public static void reset(){terminalAt=-200;selectedAt=-100;lockedAt=-1;lastWeapon=null;wasLocked=false;letterbox=0;}
    static int iconIndex(String weapon){return Math.max(0,WeaponCatalog.IDS.indexOf(weapon));}
    private static int argb(double alpha,int rgb){return ((int)Math.round(Math.max(0,Math.min(1,alpha))*255)<<24)|(rgb&0xffffff);}

    public static void draw(GuiGraphicsExtractor graphics,DeltaTracker tracker) {
        Minecraft mc=Minecraft.getInstance();if(mc.player==null||mc.gui.hud.isHidden())return;
        double now=NullstarClient.tickCount()+tracker.getGameTimeDeltaPartialTick(true);
        var status=NullstarClient.status();
        if(lastWeapon!=null&&!lastWeapon.equals(status.weapon()))selectedAt=NullstarClient.tickCount();
        lastWeapon=status.weapon();
        if(status.hasTarget()&&!wasLocked)lockedAt=NullstarClient.tickCount();
        wasLocked=status.hasTarget();
        var own=NullstarClient.STATE.strikes().values().stream().filter(s->s.state().owner().equals(mc.player.getUUID())).findFirst().orElse(null);
        boolean holding=NullstarClient.holdingController(mc);
        if(now>=terminalAt&&now-terminalAt<70)banner(graphics,mc,now);
        if(own==null&&!holding)return;
        panel(graphics,mc,own,now);
        if(holding&&(own==null||own.state().phase()==Phase.ACQUIRING)&&!NullstarClient.CAMERA.applied())reticle(graphics,mc,now);
        if(now>=selectedAt&&now-selectedAt<60)selector(graphics,mc,now);
    }

    private static void panel(GuiGraphicsExtractor g,Minecraft mc,ClientStrikeState.Strike own,double now) {
        var status=NullstarClient.status();
        String weapon=own!=null?own.state().weapon():status.weapon();
        var definition=Nullstar.catalog()==null?null:Nullstar.catalog().get(weapon);
        int rgb=own!=null?own.state().color():definition==null?0x7fe8ff:definition.presentation().color();
        int x=8,y=8,w=Math.max(150,Math.min(236,g.guiWidth()/2-16)),h=own!=null?62:52;
        g.fill(x,y,x+w,y+h,0xb0080c14);
        g.fill(x,y,x+2,y+h,argb(1,rgb));
        corner(g,x,y,w,h,argb(.85,rgb));
        g.blit(RenderPipelines.GUI_TEXTURED,ICONS,x+6,y+6,iconIndex(weapon)*32,0,24,24,32,32,96,32);
        String title=(own!=null&&own.state().preview()?"PREVIEW · ":"")+(own!=null?own.state().displayName():status.displayName());
        g.text(mc.font,mc.font.plainSubstrByWidth(title,w-40),x+36,y+7,0xffe8f8ff);
        String detail;
        if(own!=null)detail=own.state().phase().name().replace('_',' ');
        else if(NullstarClient.cooldown()>0)detail="RECHARGING "+(NullstarClient.cooldown()+19)/20+"s";
        else detail=status.hasTarget()?"TARGET LOCKED":"READY";
        g.text(mc.font,detail,x+36,y+18,own!=null?argb(1,rgb):NullstarClient.cooldown()>0?0xffffb060:0xff9fffc8);
        int bx=x+6,bw=w-12,by=y+33;
        if(own!=null) {
            timeline(g,definition,own,bx,by,bw,rgb,now);
            if(own.state().progress()>0) {
                g.fill(bx,by+9,bx+bw,by+11,0xff1b2430);
                g.fill(bx,by+9,bx+(int)(bw*own.state().progress()),by+11,0xffd0d8e0);
                g.text(mc.font,"TERRAIN "+Math.round(own.state().progress()*100)+"%",bx+bw-60,by+13,0xffaebbc9);
            }
            g.text(mc.font,mc.font.plainSubstrByWidth(NullstarClient.cinematicHint(),bw-62),bx,by+13,0xff8a96a4);
        } else {
            int cooldown=NullstarClient.cooldown();
            var def=definition;
            if(cooldown>0&&def!=null) {
                double fraction=1-Math.min(1,cooldown/(double)Math.max(1,def.cooldownTicks()));
                g.fill(bx,by,bx+bw,by+3,0xff1b2430);g.fill(bx,by,bx+(int)(bw*fraction),by+3,0xffffb060);
            }
            g.text(mc.font,mc.font.plainSubstrByWidth(NullstarClient.activationHint()+"   "+NullstarClient.selectionHint(),bw),bx,by+6,0xff8a96a4);
        }
        var target=NullstarClient.targetPreview();
        if(target!=null&&own==null) {
            double dist=Math.sqrt(mc.player.blockPosition().distSqr(target));
            int range=definition==null?128:definition.targeting().range();
            String text=(status.hasTarget()?"LOCK ":"AIM ")+target.getX()+" "+target.getY()+" "+target.getZ()+"  "+Math.round(dist)+"m";
            g.text(mc.font,text,x+6,y+h+4,dist>range?0xffff6a6a:status.hasTarget()?argb(1,rgb):0xff8fe5ff);
        }
    }
    /** Segmented phase timeline proportional to the weapon's stage lengths. */
    private static void timeline(GuiGraphicsExtractor g,com.nullstar.core.WeaponDefinition def,ClientStrikeState.Strike own,int x,int y,int w,int rgb,double now) {
        g.fill(x,y,x+w,y+6,0xff141c26);
        Phase current=own.state().phase();double progress=own.phaseProgress(now);
        if(own.state().scale().tier()!=com.nullstar.core.ScaleTier.STANDARD) {
            double age=Math.min(1,own.age(now)/own.state().scale().duration());
            int fill=(int)(w*age);g.fill(x,y,x+fill,y+6,argb(1,rgb));
            for(int i=1;i<20;i++)g.fill(x+w*i/20,y,x+w*i/20+1,y+6,0xff05080c);
            return;
        }
        if(def==null){g.fill(x,y,x+(int)(w*progress),y+6,argb(1,rgb));return;}
        int total=def.duration(),cursor=x;
        for(var stage:def.phases()) {
            int width=Math.max(2,(int)Math.round(w*stage.ticks()/(double)total));
            boolean done=stage.phase().ordinal()<current.ordinal(),active=stage.phase()==current;
            int fill=done?width:active?(int)(width*progress):0;
            if(fill>0)g.fill(cursor,y,cursor+fill,y+6,argb(done?.55:1,rgb));
            if(active)g.fill(cursor+fill-1,y-1,cursor+fill+1,y+7,0xffffffff);
            g.fill(cursor+width-1,y,cursor+width,y+6,0xff05080c);
            cursor+=width;
        }
    }
    private static void corner(GuiGraphicsExtractor g,int x,int y,int w,int h,int color) {
        g.fill(x+w-10,y,x+w,y+1,color);g.fill(x+w-1,y,x+w,y+10,color);
        g.fill(x+w-10,y+h-1,x+w,y+h,color);g.fill(x+w-1,y+h-10,x+w,y+h,color);
    }

    /** Animated holographic reticle with lock contraction, range state and designation countdown. */
    private static void reticle(GuiGraphicsExtractor g,Minecraft mc,double now) {
        var status=NullstarClient.status();
        var definition=Nullstar.catalog()==null?null:Nullstar.catalog().get(status.weapon());
        int rgb=definition==null?0x7fe8ff:definition.presentation().color();
        var target=NullstarClient.targetPreview();
        boolean locked=status.hasTarget();
        boolean inRange=target!=null&&(definition==null||Math.sqrt(mc.player.blockPosition().distSqr(target))<=definition.targeting().range());
        int color=locked?argb(1,rgb):target==null?0x80ffffff:inRange?0xff8fe5ff:0xffff6a6a;
        int cx=g.guiWidth()/2,cy=g.guiHeight()/2;
        double snap=locked?Math.min(1,(now-lockedAt)/6):0;
        float spread=(float)(14-5*snap+(locked?0:Math.sin(now*.2)*1.5));
        var pose=g.pose();
        pose.pushMatrix();pose.translate(cx,cy);pose.rotate((float)(now*(locked?.01:.04)));
        for(int i=0;i<4;i++) {
            pose.pushMatrix();pose.rotate((float)(i*Math.PI/2));
            g.fill((int)spread,(int)spread-1,(int)spread+6,(int)spread,color);
            g.fill((int)spread+5,(int)spread-6,(int)spread+6,(int)spread,color);
            pose.popMatrix();
        }
        pose.popMatrix();
        pose.pushMatrix();pose.translate(cx,cy);pose.rotate((float)(-now*.025));
        for(int i=0;i<12;i++){pose.pushMatrix();pose.rotate((float)(i*Math.PI/6));g.fill(-1,-26,1,i%3==0?-21:-23,argb(locked?.9:.45,locked?rgb:0x8fe5ff));pose.popMatrix();}
        pose.popMatrix();
        g.fill(cx-1,cy-1,cx+1,cy+1,0xffffffff);
        if(locked) {
            // Designations expire after 200 server ticks; the countdown arc predicts the remaining window.
            double remaining=Math.max(0,1-(NullstarClient.tickCount()-lockedAt)/200.0);
            int segments=24,lit=(int)Math.round(segments*remaining);
            pose.pushMatrix();pose.translate(cx,cy);
            for(int i=0;i<lit;i++){pose.pushMatrix();pose.rotate((float)(-Math.PI/2+i*Math.PI*2/segments));g.fill(30,-1,33,1,argb(.9,rgb));pose.popMatrix();}
            pose.popMatrix();
            g.centeredText(mc.font,"LOCKED",cx,cy+36,argb(1,rgb));
        } else if(target!=null) {
            int dist=(int)Math.round(Math.sqrt(mc.player.blockPosition().distSqr(target)));
            g.text(mc.font,dist+"m",cx+20,cy+6,color);
            if(!inRange)g.centeredText(mc.font,"OUT OF RANGE",cx,cy+36,0xffff6a6a);
        }
    }
    private static void selector(GuiGraphicsExtractor g,Minecraft mc,double now) {
        double age=now-selectedAt,alpha=Math.min(1,Math.min(age/4,(60-age)/10));
        int count=WeaponCatalog.IDS.size(),size=26,gap=6,total=count*size+(count-1)*gap;
        int x0=g.guiWidth()/2-total/2,y=g.guiHeight()-78;
        int selected=iconIndex(NullstarClient.status().weapon());
        g.fill(x0-6,y-6,x0+total+6,y+size+16,argb(.7*alpha,0x080c14));
        for(int i=0;i<count;i++) {
            int x=x0+i*(size+gap);
            if(i==selected)g.fill(x-2,y-2,x+size+2,y+size+2,argb(alpha,0xe8f8ff));
            g.fill(x,y,x+size,y+size,argb(.9*alpha,0x101824));
            g.blit(RenderPipelines.GUI_TEXTURED,ICONS,x+1,y+1,i*32,0,24,24,32,32,96,32,argb(i==selected?alpha:alpha*.5,0xffffff));
        }
        g.centeredText(mc.font,NullstarClient.status().displayName(),g.guiWidth()/2,y+size+5,argb(alpha,0xe8f8ff));
    }
    private static void banner(GuiGraphicsExtractor g,Minecraft mc,double now) {
        double age=now-terminalAt,alpha=Math.min(1,Math.min(age/4,(70-age)/12));
        int w=mc.font.width(terminalText)+24,x=g.guiWidth()/2-w/2,y=g.guiHeight()/4;
        g.fill(x,y,x+w,y+16,argb(.75*alpha,0x080c14));
        g.fill(x,y+15,x+w,y+16,(terminalColor&0xffffff)|((int)(alpha*255)<<24));
        g.centeredText(mc.font,terminalText,g.guiWidth()/2,y+4,(terminalColor&0xffffff)|((int)(alpha*255)<<24));
    }

    /** Screen effects beneath the HUD: letterbox for cinematic shots, weapon-tinted vignette and impact wash. */
    public static void effects(GuiGraphicsExtractor g,DeltaTracker tracker) {
        Minecraft mc=Minecraft.getInstance();if(mc.player==null)return;
        var config=NullstarClient.config();
        double now=NullstarClient.tickCount()+tracker.getGameTimeDeltaPartialTick(true);
        double wanted=NullstarClient.CAMERA.applied()&&config.letterbox?1:0;
        letterbox+=(wanted-letterbox)*.12;
        int w=g.guiWidth(),h=g.guiHeight();
        if(letterbox>.01){int bar=(int)Math.round(h*.09*letterbox);g.fill(0,0,w,bar,0xff000000);g.fill(0,h-bar,w,h,0xff000000);}
        var own=NullstarClient.STATE.strikes().values().stream().filter(s->s.state().owner().equals(mc.player.getUUID())).findFirst().orElse(null);
        if(own==null)return;
        double strength=0;int tint=own.state().color();
        Phase phase=own.state().phase();double p=own.phaseProgress(now);
        switch(own.state().weapon()) {
            case "helios" -> strength=phase==Phase.CHARGING?.08*p:phase==Phase.ORBITAL_ALIGNMENT?.08+.06*p:phase==Phase.FIRING||phase==Phase.ATMOSPHERIC_ENTRY?.16:0;
            case "singularity" -> {tint=0x14001f;strength=phase==Phase.CHARGING?.12*p:phase==Phase.FIRING?.15+.35*Math.pow(p,2):0;}
            case "glassfall" -> strength=phase==Phase.CHARGING||phase==Phase.ORBITAL_ALIGNMENT?.07:phase==Phase.FIRING?.06:0;
            default -> {}
        }
        if(strength>0)vignette(g,w,h,tint,strength);
        for(var cue:NullstarClient.STATE.cues()) {
            if(!cue.payload().strike().equals(own.state().id())||!cue.payload().effect().equals("impact"))continue;
            double age=now-cue.received();
            if(age<0||age>14)continue;
            boolean small=cue.payload().radius()<own.state().radius()*.8;
            // Reduced flashing: a soft, low-alpha warm wash with a slow decay, never a white frame.
            double peak=config.reducedFlashing?.12:(small?.18:.4);
            double alpha=peak*Math.exp(-age/(config.reducedFlashing?5:3));
            int color=config.reducedFlashing?own.state().color():0xfffaf0;
            g.fill(0,0,w,h,argb(alpha*(small?.6:1),color));
        }
    }
    private static void vignette(GuiGraphicsExtractor g,int w,int h,int rgb,double strength) {
        int steps=8;
        for(int i=0;i<steps;i++) {
            int inset=(int)(Math.min(w,h)*.035*i);
            double a=strength*(1-i/(double)steps)*.35;
            int c=argb(a,rgb);
            g.fill(0,0,w,inset+4,c);g.fill(0,h-inset-4,w,h,c);g.fill(0,inset+4,inset+4,h-inset-4,c);g.fill(w-inset-4,inset+4,w,h-inset-4,c);
        }
    }
}
