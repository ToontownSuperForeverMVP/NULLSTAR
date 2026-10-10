// Copyright (C) 2026 ToontownSuperForeverMVP and NULLSTAR contributors
// SPDX-License-Identifier: GPL-3.0-only

package com.nullstar.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.OverlayTexture;

/**
 * Reusable, bounded vertex staging for one material. Scenes fill it while collecting submits; the custom-geometry
 * callback copies it into the frame's buffer later in the same frame. Arrays grow to the cap once and are then
 * reused, so steady-state frames allocate nothing here.
 */
public final class VertexSink {
    private static final int FLOATS=8;
    private final int capacity;
    private float[] data=new float[FLOATS*512];
    private int[] colors=new int[512];
    private int vertices,dropped;
    VertexSink(int capacityVertices){this.capacity=capacityVertices-capacityVertices%4;}
    public void clear(){vertices=0;dropped=0;}
    public int vertices(){return vertices;}
    public int dropped(){return dropped;}
    public boolean isEmpty(){return vertices==0;}
    /** Reserves room for a whole primitive so quads are never split when the budget is reached. */
    public boolean reserve(int count) {
        if(vertices+count>capacity){dropped+=count;return false;}
        if((vertices+count)*FLOATS>data.length) {
            int size=Math.min(capacity,Math.max(vertices+count,colors.length*2));
            data=java.util.Arrays.copyOf(data,size*FLOATS);colors=java.util.Arrays.copyOf(colors,size);
        }
        return true;
    }
    /** Callers must {@link #reserve} first. */
    void put(float x,float y,float z,float u,float v,int argb,float nx,float ny,float nz) {
        int i=vertices*FLOATS;
        data[i]=x;data[i+1]=y;data[i+2]=z;data[i+3]=u;data[i+4]=v;data[i+5]=nx;data[i+6]=ny;data[i+7]=nz;
        colors[vertices++]=argb;
    }
    void flush(VertexConsumer consumer) {
        for(int k=0,i=0;k<vertices;k++,i+=FLOATS) {
            consumer.addVertex(data[i],data[i+1],data[i+2]).setUv(data[i+3],data[i+4]).setColor(colors[k])
                    .setNormal(data[i+5],data[i+6],data[i+7]).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0);
        }
    }
}
