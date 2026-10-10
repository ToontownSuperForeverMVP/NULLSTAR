// Copyright (C) 2026 ToontownSuperForeverMVP and NULLSTAR contributors
// SPDX-License-Identifier: GPL-3.0-only

package com.nullstar.core;

/**
 * Constant-memory, world-chunk-aligned traversal. Every visited or virtually skipped cell is charged.
 *
 * <p>Part 3 deferral (opt-in through {@link #deferUnloaded()}): a chunk tile found unloaded during the main pass is
 * remembered in a bounded bitset (one bit per tile of the footprint, at most ~17k bits for the largest legal radius)
 * instead of being lost. After the main pass the job probes deferred tiles, one budget unit per probe and a bounded
 * number of probes per step, and excavates each tile completely once it is loaded by normal play. Nothing is ever
 * force-loaded. Re-visiting a partly processed tile is harmless: removed cells are already air and scorching is
 * idempotent.
 */
public final class DestructionJob {
    @FunctionalInterface public interface Visitor { boolean visit(int x,int y,int z,boolean remove,boolean scorch); }
    @FunctionalInterface public interface ColumnLoaded { boolean test(int x,int z); }
    /** {@code deferred} is null for jobs without deferral and for checkpoints written before Part 3. */
    public record Checkpoint(CraterGeometry geometry,int originX,int originZ,long cursor,long changed,
                             int tileX,int tileZ,int x,int z,int y,long[] deferred) {
        public Checkpoint(CraterGeometry geometry,int originX,int originZ,long cursor,long changed,int tileX,int tileZ,int x,int z,int y) {
            this(geometry,originX,originZ,cursor,changed,tileX,tileZ,x,z,y,null);
        }
    }
    private final CraterGeometry geometry;
    private final int originX,originZ,minTileX,maxTileX,minTileZ,maxTileZ,height;
    private final long total;
    private long cursor,changed;
    private int tileX,tileZ,x,z,y;
    private boolean cancelled;
    private CraterGeometry.Column column;
    // Part 3 deferral state; replay progress is not part of the cursor so the planned total stays exact.
    private java.util.BitSet deferred;
    private final int tilesX,tilesZ;
    private int replayIndex=-1,replayProbe,rx,rz,ry;
    private long replayed;
    /** Probes per step once every deferred tile is still unloaded, so a waiting job cannot spin on the budget. */
    static final int PROBES_PER_STEP=16;
    private int columnX=Integer.MIN_VALUE,columnZ=Integer.MIN_VALUE;
    public DestructionJob(CraterGeometry geometry) {this(geometry,0,0);}
    public DestructionJob(CraterGeometry geometry,int originX,int originZ) {
        this.geometry=geometry;this.originX=originX;this.originZ=originZ;
        minTileX=(originX-geometry.radius())>>4;maxTileX=(originX+geometry.radius())>>4;
        minTileZ=(originZ-geometry.radius())>>4;maxTileZ=(originZ+geometry.radius())>>4;
        height=geometry.floor()+geometry.ceiling()+2;
        total=(long)(geometry.radius()*2+1)*(geometry.radius()*2+1)*height;
        tilesX=maxTileX-minTileX+1;tilesZ=maxTileZ-minTileZ+1;
        tileX=minTileX;tileZ=minTileZ;x=left();z=front();y=-geometry.floor()-1;
    }
    private int left(){return Math.max(-geometry.radius(),tileX*16-originX);}
    private int right(){return Math.min(geometry.radius(),tileX*16+15-originX);}
    private int front(){return Math.max(-geometry.radius(),tileZ*16-originZ);}
    private int back(){return Math.min(geometry.radius(),tileZ*16+15-originZ);}
    private void nextColumn() {
        y=-geometry.floor()-1;
        if(++x<=right())return;
        x=left();if(++z<=back())return;
        if(++tileX>maxTileX){tileX=minTileX;tileZ++;}
        x=left();z=front();
    }
    public int step(int budget,Visitor visitor){return step(budget,visitor,(a,b)->true);}
    public int step(int budget,Visitor visitor,ColumnLoaded loaded) {
        if(budget<0)throw new IllegalArgumentException("Negative budget");
        int used=0;
        while(used<budget&&!cancelled&&cursor<total) {
            if(!loaded.test(x,z)) {
                if(deferred!=null)deferred.set((tileZ-minTileZ)*tilesX+(tileX-minTileX));
                int skip=Math.min(budget-used,geometry.ceiling()-y+1);
                cursor+=skip;used+=skip;y+=skip;
                if(y>geometry.ceiling())nextColumn();
                continue;
            }
            int cy=y;cursor++;used++;y++;
            if(column==null||columnX!=x||columnZ!=z){column=geometry.column(x,z);columnX=x;columnZ=z;}
            boolean remove=column.removes(cy),scorch=column.shell(cy);
            int cx=x,cz=z;if(y>geometry.ceiling())nextColumn();
            if((remove||scorch)&&visitor.visit(cx,cy,cz,remove,scorch))changed++;
        }
        if(used<budget&&!cancelled&&cursor>=total&&deferred!=null&&!deferred.isEmpty())used+=replay(budget-used,visitor,loaded);
        return used;
    }
    private int tileLeft(int tx){return Math.max(-geometry.radius(),tx*16-originX);}
    private int tileRight(int tx){return Math.min(geometry.radius(),tx*16+15-originX);}
    private int tileFront(int tz){return Math.max(-geometry.radius(),tz*16-originZ);}
    private int tileBack(int tz){return Math.min(geometry.radius(),tz*16+15-originZ);}
    /** Excavates deferred tiles that have since been loaded; every probe and every cell is charged to the budget. */
    private int replay(int budget,Visitor visitor,ColumnLoaded loaded) {
        int used=0,probes=0;
        while(used<budget&&!deferred.isEmpty()) {
            if(replayIndex<0) {
                if(probes>=PROBES_PER_STEP)break;
                int index=deferred.nextSetBit(replayProbe);
                if(index<0)index=deferred.nextSetBit(0);
                replayProbe=index+1;used++;probes++;
                int tx=minTileX+index%tilesX,tz=minTileZ+index/tilesX;
                if(loaded.test(tileLeft(tx),tileFront(tz))){replayIndex=index;rx=tileLeft(tx);rz=tileFront(tz);ry=-geometry.floor()-1;}
                continue;
            }
            int tx=minTileX+replayIndex%tilesX,tz=minTileZ+replayIndex/tilesX;
            if(!loaded.test(rx,rz)){replayIndex=-1;continue;}
            int cy=ry,cx=rx,cz=rz;used++;ry++;replayed++;
            if(column==null||columnX!=cx||columnZ!=cz){column=geometry.column(cx,cz);columnX=cx;columnZ=cz;}
            boolean remove=column.removes(cy),scorch=column.shell(cy);
            if(ry>geometry.ceiling()) {
                ry=-geometry.floor()-1;
                if(++rx>tileRight(tx)){rx=tileLeft(tx);if(++rz>tileBack(tz)){deferred.clear(replayIndex);replayIndex=-1;}}
            }
            if((remove||scorch)&&visitor.visit(cx,cy,cz,remove,scorch))changed++;
        }
        return used;
    }
    /** Enables Part 3 deferral of tiles found unloaded during the main pass. */
    public DestructionJob deferUnloaded(){if(deferred==null)deferred=new java.util.BitSet(tilesX*tilesZ);return this;}
    public boolean deferring(){return deferred!=null;}
    /** Deferred tiles still waiting to be loaded. */
    public int deferredTiles(){return deferred==null?0:deferred.cardinality();}
    /** Upper-bound cell count still deferred (edge tiles are partly outside the footprint). */
    public long deferredCells(){return (long)deferredTiles()*256*height;}
    public long replayed(){return replayed;}
    /** True once the chunk-ordered main pass has visited every planned cell. */
    public boolean mainPassDone(){return cancelled||cursor>=total;}
    /** Gives up on every remaining deferred tile (deferral window expired or an operator finalised the job). */
    public int abandonDeferred(){int count=deferredTiles();if(deferred!=null)deferred.clear();replayIndex=-1;return count;}
    public Checkpoint checkpoint(){return new Checkpoint(geometry,originX,originZ,cursor,changed,tileX,tileZ,x,z,y,deferred==null?null:deferred.toLongArray());}
    public static DestructionJob restore(Checkpoint state) {
        var job=new DestructionJob(state.geometry,state.originX,state.originZ);
        if(state.cursor<0||state.cursor>job.total||state.changed<0||state.changed>state.cursor
                || state.tileX<job.minTileX||state.tileX>job.maxTileX||state.tileZ<job.minTileZ||state.tileZ>job.maxTileZ+1)
            throw new IllegalArgumentException("Invalid destruction checkpoint");
        job.tileX=state.tileX;job.tileZ=state.tileZ;job.x=state.x;job.z=state.z;job.y=state.y;
        if(state.cursor<job.total && (state.x<job.left()||state.x>job.right()||state.z<job.front()||state.z>job.back()
                ||state.y< -job.geometry.floor()-1||state.y>job.geometry.ceiling()))throw new IllegalArgumentException("Invalid checkpoint coordinates");
        if(state.cursor<job.total) {
            int width=job.geometry.radius()*2+1;
            long rows=Math.max(0,(long)(job.front()+job.geometry.radius()))*width;
            long columns=(long)(job.left()+job.geometry.radius())*(job.back()-job.front()+1);
            long local=(long)(state.z-job.front())*(job.right()-job.left()+1)+state.x-job.left();
            long expected=(rows+columns+local)*job.height+state.y+job.geometry.floor()+1;
            if(expected!=state.cursor)throw new IllegalArgumentException("Checkpoint cursor mismatch");
        }
        job.cursor=state.cursor;job.changed=state.changed;
        if(state.deferred!=null) {
            var bits=java.util.BitSet.valueOf(state.deferred);
            if(bits.length()>job.tilesX*job.tilesZ)throw new IllegalArgumentException("Invalid deferred tiles");
            job.deferred=bits;
        }
        return job;
    }
    public void cancel(){cancelled=true;}
    public boolean cancelled(){return cancelled;}
    public boolean done(){return cancelled||cursor>=total&&(deferred==null||deferred.isEmpty());}
    public double progress(){return cursor/(double)total;}
    public long scanned(){return cursor;}
    public long changed(){return changed;}
    public long total(){return total;}
    public CraterGeometry geometry(){return geometry;}
}
