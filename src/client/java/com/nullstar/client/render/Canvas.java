package com.nullstar.client.render;

import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Scene-facing drawing API. Scenes work in coordinates relative to an origin (normally the strike target), which
 * keeps float precision high; the canvas converts them to camera-relative positions for the frame.
 */
public final class Canvas {
    private final VertexSink[] sinks=new VertexSink[Layer.values().length];
    private double cx,cy,cz;
    private float ox,oy,oz;
    /** Camera basis for camera-facing primitives. */
    float rx,ry,rz,ux,uy,uz;
    private final Matrix3f normalMatrix=new Matrix3f();
    private final Vector3f p=new Vector3f(),n=new Vector3f();
    private double glow=1;

    public Canvas(){for(Layer layer:Layer.values())sinks[layer.ordinal()]=new VertexSink(layer.budget);}
    VertexSink sink(Layer layer){return sinks[layer.ordinal()];}

    void begin(Vec3 camera,Quaternionf orientation,double glowIntensity) {
        for(var sink:sinks)sink.clear();
        cx=camera.x;cy=camera.y;cz=camera.z;glow=glowIntensity;
        Vector3f right=orientation.transform(new Vector3f(1,0,0)),up=orientation.transform(new Vector3f(0,1,0));
        rx=right.x;ry=right.y;rz=right.z;ux=up.x;uy=up.y;uz=up.z;
    }
    /** Sets the local origin in world coordinates. */
    public void origin(Vec3 world){origin(world.x,world.y,world.z);}
    public void origin(double x,double y,double z){ox=(float)(x-cx);oy=(float)(y-cy);oz=(float)(z-cz);}
    /** Camera right/up basis, for primitives laid out in screen space (lensed arcs, rings). */
    public float rightX(){return rx;}
    public float rightY(){return ry;}
    public float rightZ(){return rz;}
    public float upX(){return ux;}
    public float upY(){return uy;}
    public float upZ(){return uz;}
    /** Origin-relative position of the camera, useful for view-dependent effects. */
    public Vec3 cameraLocal(){return new Vec3(-ox,-oy,-oz);}

    // ------------------------------------------------------------------ colour helpers
    public static int argb(double alpha,int rgb){return ((int)Math.round(Math.max(0,Math.min(1,alpha))*255)<<24)|(rgb&0xffffff);}
    public static int lerpRgb(int a,int b,double t) {
        t=Math.max(0,Math.min(1,t));
        int r=(int)(((a>>16)&255)+(((b>>16)&255)-((a>>16)&255))*t),g=(int)(((a>>8)&255)+(((b>>8)&255)-((a>>8)&255))*t),bl=(int)((a&255)+((b&255)-(a&255))*t);
        return (r<<16)|(g<<8)|bl;
    }
    private int energy(Layer layer,int argb) {
        if(layer.opaque()||layer==Layer.SMOKE||layer==Layer.VOID||glow==1)return argb;
        return argb(((argb>>>24)/255.0)*glow,argb);
    }

    // ------------------------------------------------------------------ primitives
    private void vertex(Layer layer,float x,float y,float z,float u,float v,int argb,float nx,float ny,float nz) {
        sink(layer).put(x+ox,y+oy,z+oz,u,v,energy(layer,argb),nx,ny,nz);
    }
    public void quad(Layer layer,float ax,float ay,float az,float bx,float by,float bz,float cx,float cy,float cz,float dx,float dy,float dz,
                     float u0,float v0,float u1,float v1,int ca,int cb,int cc,int cd,float nx,float ny,float nz) {
        var sink=sink(layer);if(!sink.reserve(4))return;
        vertex(layer,ax,ay,az,u0,v0,ca,nx,ny,nz);vertex(layer,bx,by,bz,u1,v0,cb,nx,ny,nz);
        vertex(layer,cx,cy,cz,u1,v1,cc,nx,ny,nz);vertex(layer,dx,dy,dz,u0,v1,cd,nx,ny,nz);
    }
    /** Camera-facing quad. Rotation is in radians around the view axis. */
    public void billboard(Layer layer,float x,float y,float z,float halfWidth,float halfHeight,float rotation,int argb,float u0,float v0,float u1,float v1) {
        float c=(float)Math.cos(rotation),s=(float)Math.sin(rotation);
        float ax=(rx*c+ux*s)*halfWidth,ay=(ry*c+uy*s)*halfWidth,az=(rz*c+uz*s)*halfWidth;
        float bx=(ux*c-rx*s)*halfHeight,by=(uy*c-ry*s)*halfHeight,bz=(uz*c-rz*s)*halfHeight;
        quad(layer,x-ax-bx,y-ay-by,z-az-bz,x+ax-bx,y+ay-by,z+az-bz,x+ax+bx,y+ay+by,z+az+bz,x-ax+bx,y-ay+by,z-az+bz,
                u0,v1,u1,v0,argb,argb,argb,argb,0,0,0);
    }
    public void billboard(Layer layer,float x,float y,float z,float size,float rotation,int argb) {
        billboard(layer,x,y,z,size,size,rotation,argb,0,0,1,1);
    }
    /** Smoke sheet frame (2x2 atlas). */
    public void smoke(float x,float y,float z,float size,float rotation,int argb,int frame) {
        float u=(frame&1)*.5f,v=((frame>>1)&1)*.5f;
        billboard(Layer.SMOKE,x,y,z,size,size,rotation,argb,u,v,u+.5f,v+.5f);
    }
    /** Camera-facing segment, widened perpendicular to both its axis and the view direction. */
    public void streak(Layer layer,float ax,float ay,float az,float bx,float by,float bz,float wa,float wb,int ca,int cb,float u0,float v0,float u1,float v1) {
        float dx=bx-ax,dy=by-ay,dz=bz-az;
        float vx=-(ax+bx)*.5f-ox,vy=-(ay+by)*.5f-oy,vz=-(az+bz)*.5f-oz;
        float sx=dy*vz-dz*vy,sy=dz*vx-dx*vz,sz=dx*vy-dy*vx,len=(float)Math.sqrt(sx*sx+sy*sy+sz*sz);
        if(len<1e-6f)return;
        sx/=len;sy/=len;sz/=len;
        quad(layer,ax-sx*wa,ay-sy*wa,az-sz*wa,ax+sx*wa,ay+sy*wa,az+sz*wa,bx+sx*wb,by+sy*wb,bz+sz*wb,bx-sx*wb,by-sy*wb,bz-sz*wb,
                u0,v0,u1,v1,ca,ca,cb,cb,0,0,0);
    }
    /** Connected camera-facing ribbon through points (x,y,z triples). */
    public void ribbon(Layer layer,float[] points,int count,float width0,float width1,int color0,int color1,float vScroll) {
        for(int i=0;i+1<count;i++) {
            float t0=(float)i/(count-1),t1=(float)(i+1)/(count-1);
            streak(layer,points[i*3],points[i*3+1],points[i*3+2],points[i*3+3],points[i*3+4],points[i*3+5],
                    width0+(width1-width0)*t0,width0+(width1-width0)*t1,mix(color0,color1,t0),mix(color0,color1,t1),0,t0+vScroll,1,t1+vScroll);
        }
    }
    public static int mix(int a,int b,double t) {
        int alpha=(int)Math.round((a>>>24)+((b>>>24)-(a>>>24))*Math.max(0,Math.min(1,t)));
        return (alpha<<24)|lerpRgb(a,b,t);
    }
    /** Open tube from A to B with radial normals (fresnel-ready). u wraps the circumference, v runs along the axis. */
    public void tube(Layer layer,float ax,float ay,float az,float bx,float by,float bz,float ra,float rb,int segments,int ca,int cb,
                     float uRepeat,float uOffset,float v0,float v1) {
        float dx=bx-ax,dy=by-ay,dz=bz-az,len=(float)Math.sqrt(dx*dx+dy*dy+dz*dz);
        if(len<1e-5f||!sink(layer).reserve(segments*4))return;
        dx/=len;dy/=len;dz/=len;
        float px,py,pz;
        if(Math.abs(dy)<.9f){px=dz;py=0;pz=-dx;}else{px=1;py=0;pz=0;}
        float pl=(float)Math.sqrt(px*px+py*py+pz*pz);px/=pl;py/=pl;pz/=pl;
        float qx=dy*pz-dz*py,qy=dz*px-dx*pz,qz=dx*py-dy*px;
        for(int i=0;i<segments;i++) {
            double a0=i*Math.PI*2/segments,a1=(i+1)*Math.PI*2/segments;
            float c0=(float)Math.cos(a0),s0=(float)Math.sin(a0),c1=(float)Math.cos(a1),s1=(float)Math.sin(a1);
            float n0x=px*c0+qx*s0,n0y=py*c0+qy*s0,n0z=pz*c0+qz*s0,n1x=px*c1+qx*s1,n1y=py*c1+qy*s1,n1z=pz*c1+qz*s1;
            float u0=uOffset+uRepeat*i/segments,u1=uOffset+uRepeat*(i+1)/segments;
            vertex(layer,ax+n0x*ra,ay+n0y*ra,az+n0z*ra,u0,v0,ca,n0x,n0y,n0z);
            vertex(layer,ax+n1x*ra,ay+n1y*ra,az+n1z*ra,u1,v0,ca,n1x,n1y,n1z);
            vertex(layer,bx+n1x*rb,by+n1y*rb,bz+n1z*rb,u1,v1,cb,n1x,n1y,n1z);
            vertex(layer,bx+n0x*rb,by+n0y*rb,bz+n0z*rb,u0,v1,cb,n0x,n0y,n0z);
        }
    }
    /** Flat ring in the plane orthogonal to (nx,ny,nz). u wraps around, v runs inner to outer. */
    public void annulus(Layer layer,float x,float y,float z,float nx,float ny,float nz,float inner,float outer,int segments,
                        int cInner,int cOuter,float uRepeat,float uOffset,float vInner,float vOuter) {
        if(!sink(layer).reserve(segments*4))return;
        float px,py,pz;
        if(Math.abs(ny)<.9f){px=nz;py=0;pz=-nx;}else{px=1;py=0;pz=0;}
        float pl=(float)Math.sqrt(px*px+py*py+pz*pz);px/=pl;py/=pl;pz/=pl;
        float qx=ny*pz-nz*py,qy=nz*px-nx*pz,qz=nx*py-ny*px;
        for(int i=0;i<segments;i++) {
            double a0=i*Math.PI*2/segments,a1=(i+1)*Math.PI*2/segments;
            float c0=(float)Math.cos(a0),s0=(float)Math.sin(a0),c1=(float)Math.cos(a1),s1=(float)Math.sin(a1);
            float d0x=px*c0+qx*s0,d0y=py*c0+qy*s0,d0z=pz*c0+qz*s0,d1x=px*c1+qx*s1,d1y=py*c1+qy*s1,d1z=pz*c1+qz*s1;
            float u0=uOffset+uRepeat*i/segments,u1=uOffset+uRepeat*(i+1)/segments;
            vertex(layer,x+d0x*inner,y+d0y*inner,z+d0z*inner,u0,vInner,cInner,nx,ny,nz);
            vertex(layer,x+d1x*inner,y+d1y*inner,z+d1z*inner,u1,vInner,cInner,nx,ny,nz);
            vertex(layer,x+d1x*outer,y+d1y*outer,z+d1z*outer,u1,vOuter,cOuter,nx,ny,nz);
            vertex(layer,x+d0x*outer,y+d0y*outer,z+d0z*outer,u0,vOuter,cOuter,nx,ny,nz);
        }
    }
    /** Draws a prebuilt mesh with a local transform (origin-relative). */
    public void mesh(Layer layer,Mesh mesh,Matrix4f transform,int argb,float du,float dv) {
        if(!sink(layer).reserve(mesh.vertices))return;
        transform.normal(normalMatrix);
        float[] d=mesh.data;
        for(int i=0,k=0;k<mesh.vertices;k++,i+=8) {
            transform.transformPosition(p.set(d[i],d[i+1],d[i+2]));normalMatrix.transform(n.set(d[i+5],d[i+6],d[i+7]));
            float len=n.length();if(len>1e-6f)n.div(len);
            vertex(layer,p.x,p.y,p.z,d[i+3]+du,d[i+4]+dv,argb,n.x,n.y,n.z);
        }
    }
    public void sphere(Layer layer,float x,float y,float z,float radius,int argb,float du,float dv,boolean detailed) {
        mesh(layer,detailed?Mesh.UNIT_SPHERE:Mesh.UNIT_SPHERE_LOW,new Matrix4f().translation(x,y,z).scale(radius),argb,du,dv);
    }
    /** Jagged lightning polyline from A to B; deterministic for a given seed. */
    public void arc(Layer layer,float ax,float ay,float az,float bx,float by,float bz,float jitter,int segments,float width,int color,long seed) {
        float[] pts=new float[(segments+1)*3];
        java.util.Random random=new java.util.Random(seed);
        for(int i=0;i<=segments;i++) {
            float t=(float)i/segments,j=(i==0||i==segments)?0:jitter*(float)Math.sin(t*Math.PI);
            pts[i*3]=ax+(bx-ax)*t+(random.nextFloat()-.5f)*2*j;
            pts[i*3+1]=ay+(by-ay)*t+(random.nextFloat()-.5f)*2*j;
            pts[i*3+2]=az+(bz-az)*t+(random.nextFloat()-.5f)*2*j;
        }
        ribbon(layer,pts,segments+1,width,width*.4f,color,argb((color>>>24)/255.0*.6,color),0);
    }
    int totalVertices(){int total=0;for(var sink:sinks)total+=sink.vertices();return total;}
    int droppedVertices(){int total=0;for(var sink:sinks)total+=sink.dropped();return total;}
}
