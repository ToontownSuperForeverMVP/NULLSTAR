package com.nullstar.core;

/** Constant-memory, world-chunk-aligned traversal. Every visited or virtually skipped cell is charged. */
public final class DestructionJob {
    @FunctionalInterface public interface Visitor { boolean visit(int x,int y,int z,boolean remove,boolean scorch); }
    @FunctionalInterface public interface ColumnLoaded { boolean test(int x,int z); }
    public record Checkpoint(CraterGeometry geometry,int originX,int originZ,long cursor,long changed,
                             int tileX,int tileZ,int x,int z,int y) {}
    private final CraterGeometry geometry;
    private final int originX,originZ,minTileX,maxTileX,minTileZ,maxTileZ,height;
    private final long total;
    private long cursor,changed;
    private int tileX,tileZ,x,z,y;
    private boolean cancelled;
    private CraterGeometry.Column column;
    private int columnX=Integer.MIN_VALUE,columnZ=Integer.MIN_VALUE;
    public DestructionJob(CraterGeometry geometry) {this(geometry,0,0);}
    public DestructionJob(CraterGeometry geometry,int originX,int originZ) {
        this.geometry=geometry;this.originX=originX;this.originZ=originZ;
        minTileX=(originX-geometry.radius())>>4;maxTileX=(originX+geometry.radius())>>4;
        minTileZ=(originZ-geometry.radius())>>4;maxTileZ=(originZ+geometry.radius())>>4;
        height=geometry.floor()+geometry.ceiling()+2;
        total=(long)(geometry.radius()*2+1)*(geometry.radius()*2+1)*height;
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
        while(used<budget&&!done()) {
            if(!loaded.test(x,z)) {
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
        return used;
    }
    public Checkpoint checkpoint(){return new Checkpoint(geometry,originX,originZ,cursor,changed,tileX,tileZ,x,z,y);}
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
        job.cursor=state.cursor;job.changed=state.changed;return job;
    }
    public void cancel(){cancelled=true;}
    public boolean cancelled(){return cancelled;}
    public boolean done(){return cancelled||cursor>=total;}
    public double progress(){return cursor/(double)total;}
    public long scanned(){return cursor;}
    public long changed(){return changed;}
    public long total(){return total;}
    public CraterGeometry geometry(){return geometry;}
}
