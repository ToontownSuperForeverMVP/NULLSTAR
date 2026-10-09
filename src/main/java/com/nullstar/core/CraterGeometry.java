package com.nullstar.core;

/** Deterministic geometry with smooth angular noise and layered radial falloff; no world access. */
public record CraterGeometry(String shape, int radius, int depth, double irregularity, long seed, double innerRadius) {
    public CraterGeometry(String shape,int radius,int depth,double irregularity,long seed) {this(shape,radius,depth,irregularity,seed,0);}
    public CraterGeometry {
        if (!java.util.List.of("crater", "sphere", "ellipsoid", "basin", "ring").contains(shape) || radius < 1 || radius > 1024
                || depth < 1 || depth > 384 || !Double.isFinite(irregularity) || irregularity < 0 || irregularity > .2
                || !Double.isFinite(innerRadius) || innerRadius<0 || innerRadius>=radius)
            throw new IllegalArgumentException("Geometry exceeds hard safety limits");
    }
    public int ceiling() { return shape.equals("crater") ? Math.max(3, radius / 2) : shape.equals("basin")?Math.min(64,Math.max(8,depth/2)):shape.equals("ring")?Math.min(16,depth): (shape.equals("sphere") ? radius : depth); }
    public int floor() { return shape.equals("sphere") ? radius : depth; }
    public double boundary(double x, double z) {
        double angle = Math.atan2(z, x), phase = (seed & 65535) * .001;
        double noise = .55 * Math.sin(angle * 5 + phase) + .3 * Math.sin(angle * 9 - phase) + .15 * Math.sin(angle * 13 + phase * 2);
        return radius * (1 - irregularity * (.5 + noise * .5));
    }
    public double intensity(double x, double y, double z) {
        double r = Math.hypot(x, z) / boundary(x, z);
        if (r >= 1) return 0;
        if (shape.equals("ring")) {
            double distance=Math.hypot(x,z);
            if(distance<innerRadius)return 0;
            double t=(distance-innerRadius)/(radius-innerRadius);
            double cut=depth*(.25+.75*Math.sin(t*Math.PI));
            return y>=-cut && y<=ceiling()?Math.max(.01,1-t):0;
        }
        if (!shape.equals("crater") && !shape.equals("basin")) {
            double vertical = y / (shape.equals("sphere") ? radius : (double) depth);
            return Math.max(0, 1 - r * r - vertical * vertical);
        }
        double bowl = depth * Math.pow(1 - r * r, .65);
        if(shape.equals("basin")) bowl=depth*Math.pow(1-r*r,.4)*(.94+.06*Math.sin(r*27+(seed&255)));
        if (y < -bowl || y > ceiling()) return 0;
        return Math.max(.01, (1 - r) * (y < 0 ? Math.max(.01, 1 + y / bowl) : 1));
    }
    public record Column(boolean valid,double lower,double upper,boolean strict) {
        public boolean removes(int y){return valid&&(strict?y>lower&&y<upper:y>=lower&&y<=upper);}
        public boolean shell(int y){return !removes(y)&&removes(y+1);}
    }
    /** Vertical interval classification avoids repeating angular noise/trigonometry for each cell. */
    public Column column(int x,int z) {
        double distance=Math.hypot(x,z),r=distance/boundary(x,z);
        if(r>=1)return new Column(false,0,0,false);
        if(shape.equals("ring")) {
            if(distance<innerRadius)return new Column(false,0,0,false);
            double t=(distance-innerRadius)/(radius-innerRadius);
            return new Column(true,-depth*(.25+.75*Math.sin(t*Math.PI)),ceiling(),false);
        }
        if(shape.equals("sphere")||shape.equals("ellipsoid")) {
            double h=Math.sqrt(1-r*r)*(shape.equals("sphere")?radius:depth);
            return new Column(true,-h,h,true);
        }
        double bowl=depth*Math.pow(1-r*r,shape.equals("basin")?.4:.65);
        if(shape.equals("basin"))bowl*=.94+.06*Math.sin(r*27+(seed&255));
        return new Column(true,-bowl,ceiling(),false);
    }
    public boolean removes(int x, int y, int z) { return column(x,z).removes(y); }
    public boolean shell(int x, int y, int z) { return !removes(x, y, z) && removes(x, y + 1, z); }
    /** Shared by admission accounting, execution and inert operator footprint inspection. */
    public static CraterGeometry forCue(WeaponDefinition.Impact impact,int radius,int depth,int radiusCap,long seed,WeaponDefinition.Cue cue) {
        int outer=Math.max(1,Math.min(radiusCap,(int)Math.round(radius*cue.radiusScale())));
        double inner=radius*cue.innerRadiusScale();if(inner>=outer)return null;
        int cut=Math.max(1,Math.min(depth,(int)Math.round(depth*Math.min(1,cue.radiusScale())*cue.effectiveDepthScale())));
        return new CraterGeometry(cue.shape()==null?impact.shape():cue.shape(),outer,cut,impact.irregularity(),seed+cue.tick(),inner);
    }
}
