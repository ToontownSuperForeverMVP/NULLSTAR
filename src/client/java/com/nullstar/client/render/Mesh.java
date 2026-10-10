// Copyright (C) 2026 ToontownSuperForeverMVP and NULLSTAR contributors
// SPDX-License-Identifier: GPL-3.0-only

package com.nullstar.client.render;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import java.util.ArrayDeque;
import java.util.Arrays;

/**
 * Immutable procedural mesh (quads, x y z u v nx ny nz per vertex). Expensive machine geometry is built once and
 * then drawn every frame by transform only.
 */
public final class Mesh {
    final float[] data;
    final int vertices;
    private Mesh(float[] data,int vertices){this.data=data;this.vertices=vertices;}
    public int vertexCount(){return vertices;}

    public static final Mesh UNIT_SPHERE=new Builder().sphere(1,16,10).build();
    public static final Mesh UNIT_SPHERE_LOW=new Builder().sphere(1,10,6).build();
    public static final Mesh UNIT_CUBE=new Builder().uvScale(1).box(-.5f,-.5f,-.5f,.5f,.5f,.5f).build();

    /** Composes primitives with a transform stack. Texture coordinates are projected from local size for even texel density. */
    public static final class Builder {
        private float[] data=new float[8*256];
        private int vertices;
        private final ArrayDeque<Matrix4f> stack=new ArrayDeque<>();
        private Matrix4f matrix=new Matrix4f();
        private final Matrix3f normal=new Matrix3f();
        private final Vector3f p=new Vector3f(),n=new Vector3f();
        private float uvScale=1/6f;
        public Builder uvScale(float scale){uvScale=scale;return this;}
        public Builder push(){stack.push(new Matrix4f(matrix));return this;}
        public Builder pop(){matrix=stack.pop();return this;}
        public Builder translate(float x,float y,float z){matrix.translate(x,y,z);return this;}
        public Builder rotateX(float radians){matrix.rotateX(radians);return this;}
        public Builder rotateY(float radians){matrix.rotateY(radians);return this;}
        public Builder rotateZ(float radians){matrix.rotateZ(radians);return this;}
        public Builder scale(float x,float y,float z){matrix.scale(x,y,z);return this;}

        private void vertex(float x,float y,float z,float u,float v,float nx,float ny,float nz) {
            if((vertices+1)*8>data.length)data=Arrays.copyOf(data,data.length*2);
            matrix.transformPosition(p.set(x,y,z));matrix.normal(normal).transform(n.set(nx,ny,nz));
            float length=n.length();
            if(length>1e-9f&&Float.isFinite(length))n.div(length);else n.set(0,1,0);
            int i=vertices++*8;
            data[i]=p.x;data[i+1]=p.y;data[i+2]=p.z;data[i+3]=u;data[i+4]=v;data[i+5]=n.x;data[i+6]=n.y;data[i+7]=n.z;
        }
        public Builder quad(float[] a,float[] b,float[] c,float[] d,float u0,float v0,float u1,float v1) {
            // Newell's method stays well-defined for degenerate (triangle-like) quads such as caps and crystal tips.
            float nx=0,ny=0,nz=0;
            float[][] ring={a,b,c,d};
            for(int i=0;i<4;i++) {
                float[] p0=ring[i],p1=ring[(i+1)%4];
                nx+=(p0[1]-p1[1])*(p0[2]+p1[2]);ny+=(p0[2]-p1[2])*(p0[0]+p1[0]);nz+=(p0[0]-p1[0])*(p0[1]+p1[1]);
            }
            if(nx*nx+ny*ny+nz*nz<1e-12f){nx=0;ny=1;nz=0;}
            vertex(a[0],a[1],a[2],u0,v0,nx,ny,nz);vertex(b[0],b[1],b[2],u1,v0,nx,ny,nz);
            vertex(c[0],c[1],c[2],u1,v1,nx,ny,nz);vertex(d[0],d[1],d[2],u0,v1,nx,ny,nz);
            return this;
        }
        public Builder box(float x0,float y0,float z0,float x1,float y1,float z1) {
            float s=uvScale;
            quad(v(x0,y1,z0),v(x0,y1,z1),v(x1,y1,z1),v(x1,y1,z0),x0*s,z0*s,x1*s,z1*s);
            quad(v(x0,y0,z0),v(x1,y0,z0),v(x1,y0,z1),v(x0,y0,z1),x0*s,z0*s,x1*s,z1*s);
            quad(v(x0,y0,z0),v(x0,y1,z0),v(x1,y1,z0),v(x1,y0,z0),y0*s,x0*s,y1*s,x1*s);
            quad(v(x0,y0,z1),v(x1,y0,z1),v(x1,y1,z1),v(x0,y1,z1),x0*s,y0*s,x1*s,y1*s);
            quad(v(x0,y0,z0),v(x0,y0,z1),v(x0,y1,z1),v(x0,y1,z0),z0*s,y0*s,z1*s,y1*s);
            quad(v(x1,y0,z0),v(x1,y1,z0),v(x1,y1,z1),v(x1,y0,z1),y0*s,z0*s,y1*s,z1*s);
            return this;
        }
        /** Tapered prism around +Y from y0 to y1 with optional caps. */
        public Builder frustum(float r0,float r1,float y0,float y1,int segments,boolean caps) {
            float s=uvScale,circumference=(float)(Math.PI*2*Math.max(r0,r1));
            for(int i=0;i<segments;i++) {
                double a0=i*Math.PI*2/segments,a1=(i+1)*Math.PI*2/segments;
                float c0=(float)Math.cos(a0),s0=(float)Math.sin(a0),c1=(float)Math.cos(a1),s1=(float)Math.sin(a1);
                float u0=circumference*s*i/segments,u1=circumference*s*(i+1)/segments;
                quad(v(c0*r0,y0,s0*r0),v(c1*r0,y0,s1*r0),v(c1*r1,y1,s1*r1),v(c0*r1,y1,s0*r1),u0,y0*s,u1,y1*s);
                if(caps) {
                    quad(v(0,y1,0),v(c0*r1,y1,s0*r1),v(c1*r1,y1,s1*r1),v(0,y1,0),0,0,r1*s,r1*s);
                    quad(v(0,y0,0),v(c1*r0,y0,s1*r0),v(c0*r0,y0,s0*r0),v(0,y0,0),0,0,r0*s,r0*s);
                }
            }
            return this;
        }
        /** Rectangular-section ring band between angles a0..a1 (radians) around +Y. */
        public Builder ring(float inner,float outer,float y0,float y1,double a0,double a1,int segments) {
            float s=uvScale;
            for(int i=0;i<segments;i++) {
                double b0=a0+(a1-a0)*i/segments,b1=a0+(a1-a0)*(i+1)/segments;
                float c0=(float)Math.cos(b0),s0=(float)Math.sin(b0),c1=(float)Math.cos(b1),s1=(float)Math.sin(b1);
                float u0=(float)(b0*outer*s),u1=(float)(b1*outer*s);
                quad(v(c0*outer,y0,s0*outer),v(c1*outer,y0,s1*outer),v(c1*outer,y1,s1*outer),v(c0*outer,y1,s0*outer),u0,y0*s,u1,y1*s);
                quad(v(c0*inner,y1,s0*inner),v(c1*inner,y1,s1*inner),v(c1*inner,y0,s1*inner),v(c0*inner,y0,s0*inner),u0,y0*s,u1,y1*s);
                quad(v(c0*inner,y1,s0*inner),v(c0*outer,y1,s0*outer),v(c1*outer,y1,s1*outer),v(c1*inner,y1,s1*inner),u0,inner*s,u1,outer*s);
                quad(v(c0*inner,y0,s0*inner),v(c1*inner,y0,s1*inner),v(c1*outer,y0,s1*outer),v(c0*outer,y0,s0*outer),u0,inner*s,u1,outer*s);
            }
            float ca=(float)Math.cos(a0),sa=(float)Math.sin(a0),cb=(float)Math.cos(a1),sb=(float)Math.sin(a1);
            quad(v(ca*inner,y0,sa*inner),v(ca*outer,y0,sa*outer),v(ca*outer,y1,sa*outer),v(ca*inner,y1,sa*inner),0,0,(outer-inner)*s,(y1-y0)*s);
            quad(v(cb*inner,y0,sb*inner),v(cb*inner,y1,sb*inner),v(cb*outer,y1,sb*outer),v(cb*outer,y0,sb*outer),0,0,(outer-inner)*s,(y1-y0)*s);
            return this;
        }
        /** Thick spherical-shell patch: azimuth a0..a1, polar p0..p1 (0 = +Y). Used for armoured reactor petals. */
        public Builder shellPatch(float inner,float outer,double a0,double a1,double p0,double p1,int segA,int segP) {
            float s=uvScale;
            for(int i=0;i<segA;i++)for(int j=0;j<segP;j++) {
                double b0=a0+(a1-a0)*i/segA,b1=a0+(a1-a0)*(i+1)/segA,q0=p0+(p1-p0)*j/segP,q1=p0+(p1-p0)*(j+1)/segP;
                for(float r:new float[]{outer,inner}) {
                    float[] A=sph(r,b0,q0),B=sph(r,b1,q0),C=sph(r,b1,q1),D=sph(r,b0,q1);
                    if(r==outer)quad(A,D,C,B,(float)(b0*r*s),(float)(q0*r*s),(float)(b1*r*s),(float)(q1*r*s));
                    else quad(A,B,C,D,(float)(b0*r*s),(float)(q0*r*s),(float)(b1*r*s),(float)(q1*r*s));
                }
            }
            for(int i=0;i<segA;i++) {
                double b0=a0+(a1-a0)*i/segA,b1=a0+(a1-a0)*(i+1)/segA;
                for(double q:new double[]{p0,p1})quad(sph(inner,b0,q),sph(inner,b1,q),sph(outer,b1,q),sph(outer,b0,q),0,0,(outer-inner)*s,.2f);
            }
            for(int j=0;j<segP;j++) {
                double q0=p0+(p1-p0)*j/segP,q1=p0+(p1-p0)*(j+1)/segP;
                for(double b:new double[]{a0,a1})quad(sph(inner,b,q0),sph(outer,b,q0),sph(outer,b,q1),sph(inner,b,q1),0,0,(outer-inner)*s,.2f);
            }
            return this;
        }
        public Builder sphere(float radius,int segA,int segP) {
            for(int i=0;i<segA;i++)for(int j=0;j<segP;j++) {
                double b0=Math.PI*2*i/segA,b1=Math.PI*2*(i+1)/segA,q0=Math.PI*j/segP,q1=Math.PI*(j+1)/segP;
                float[] A=sph(radius,b0,q0),B=sph(radius,b1,q0),C=sph(radius,b1,q1),D=sph(radius,b0,q1);
                float u0=(float)i/segA,u1=(float)(i+1)/segA,v0=(float)j/segP,v1=(float)(j+1)/segP;
                vertex(A[0],A[1],A[2],u0,v0,A[0],A[1],A[2]);vertex(D[0],D[1],D[2],u0,v1,D[0],D[1],D[2]);
                vertex(C[0],C[1],C[2],u1,v1,C[0],C[1],C[2]);vertex(B[0],B[1],B[2],u1,v0,B[0],B[1],B[2]);
            }
            return this;
        }
        /** Elongated hexagonal bipyramid: the Glassfall crystal silhouette, tip toward -Y. */
        public Builder crystal(float radius,float top,float bottom,int sides,float twist) {
            for(int i=0;i<sides;i++) {
                double a0=i*Math.PI*2/sides,a1=(i+1)*Math.PI*2/sides;
                float[] e0={(float)Math.cos(a0)*radius,0,(float)Math.sin(a0)*radius},e1={(float)Math.cos(a1)*radius,0,(float)Math.sin(a1)*radius};
                float[] up={(float)Math.cos(a0+twist)*radius*.18f,top,(float)Math.sin(a0+twist)*radius*.18f};
                quad(e0,up,up,e1,(float)i/sides,0,(float)(i+1)/sides,1);
                quad(e1,v(0,-bottom,0),v(0,-bottom,0),e0,(float)i/sides,0,(float)(i+1)/sides,1);
            }
            return this;
        }
        private static float[] sph(float r,double azimuth,double polar){return new float[]{(float)(Math.sin(polar)*Math.cos(azimuth)*r),(float)(Math.cos(polar)*r),(float)(Math.sin(polar)*Math.sin(azimuth)*r)};}
        private static float[] v(float x,float y,float z){return new float[]{x,y,z};}
        public Mesh build(){return new Mesh(Arrays.copyOf(data,vertices*8),vertices);}
    }
}
