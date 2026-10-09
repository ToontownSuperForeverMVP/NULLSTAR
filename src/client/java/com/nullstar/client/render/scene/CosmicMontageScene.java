package com.nullstar.client.render.scene;

import com.nullstar.client.ClientConfig;
import com.nullstar.client.render.Canvas;
import com.nullstar.client.render.Layer;
import com.nullstar.client.render.StrikeView;
import com.nullstar.core.CinematicScene;

/** Minimal normalized scene foundation. Part 2.5B replaces this composition without world traversal. */
public final class CosmicMontageScene {
    public void render(StrikeView view,Canvas canvas,ClientConfig.Quality quality) {
        var scene=view.state().scale().scene();
        canvas.sceneBillboard(Layer.HULL_HELIOS,0,0,0,40,24,0,Canvas.argb(1,0x02040d));
        int count=quality==ClientConfig.Quality.LOW?24:80;
        for(int i=0;i<count;i++) {
            float x=(float)((view.hash(i*3+1)-.5)*75),y=(float)((view.hash(i*3+2)-.5)*45);
            float size=.035f+(float)view.hash(i*3+3)*.09f;
            canvas.sceneBillboard(Layer.GLOW,x,y,1,size,size,0,Canvas.argb(.6,0xcbdfff));
        }
        float size=(float)(7*(1-view.progress()*.8));
        int color=scene==CinematicScene.PLANET||scene==CinematicScene.ASCENT?0x257daf:scene==CinematicScene.STELLAR_MOTHERSHIP?view.color():0x8a76c2;
        canvas.sceneBillboard(Layer.GLOW,0,0,2,size,size,0,Canvas.argb(.65,color));
        if(scene==CinematicScene.SOLAR_SYSTEM||scene==CinematicScene.GALAXY||scene==CinematicScene.COSMIC_BOUNDARY) {
            int nodes=scene==CinematicScene.GALAXY?32:12;
            for(int i=0;i<nodes;i++) {
                double angle=i*Math.PI*2/nodes+view.age()*.002;float r=4+i*.4f;
                canvas.sceneBillboard(Layer.GLOW,(float)Math.cos(angle)*r,(float)Math.sin(angle)*r*.35f,3,.12f,.12f,0,Canvas.argb(.7,color));
            }
        }
        if(scene==CinematicScene.STELLAR_MOTHERSHIP) {
            int nodes=view.state().scale().alignmentNodes();
            for(int i=0;i<nodes;i++) {
                double angle=i*Math.PI*2/nodes+view.age()*.003;float r=5+(float)StrikeView.smooth(view.progress())*8;
                canvas.sceneBillboard(Layer.HULL_HELIOS,(float)Math.cos(angle)*r,(float)Math.sin(angle)*r*.4f,4,1.8f,1.8f,.3f,Canvas.argb(1,0x655d43));
                canvas.sceneBillboard(Layer.GLOW,(float)Math.cos(angle)*r,(float)Math.sin(angle)*r*.4f,5,.22f,.22f,0,Canvas.argb(.6,view.color()));
            }
        }
    }
}
