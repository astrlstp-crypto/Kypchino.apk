package com.kupchino.app;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * True 3D Kupchino Drive renderer. Everything is inside the main Kupchino APK.
 * The road graph is a large fictional Russian-style regional network: city streets,
 * ring road, junctions, diagonals and long regional highways. It is not an exact map.
 */
public class DriveUltra3DView extends GLSurfaceView implements GLSurfaceView.Renderer {
    private final Context context;

    private volatile boolean gas;
    private volatile boolean brake;
    private volatile boolean left;
    private volatile boolean right;
    private volatile boolean freeMode;
    private volatile boolean requestNewMission;

    private float carX = 0f;
    private float carZ = -35f;
    private float carYaw = 0f;
    private float speed = 0f;
    private int money = 0;
    private int missionIndex = 3;
    private float parkedTime = 0f;
    private long lastNs;

    private int program;
    private int aPos, aNormal;
    private int uMvp, uModel, uColor, uLightDir, uFogColor, uFogNear, uFogFar;

    private final float[] projection = new float[16];
    private final float[] view = new float[16];
    private final float[] model = new float[16];
    private final float[] vp = new float[16];
    private final float[] mvp = new float[16];

    private Mesh cube;
    private final List<Road> roads = new ArrayList<>();
    private final List<Building> buildings = new ArrayList<>();
    private final List<float[]> parking = new ArrayList<>();

    private static final float[] SKY = {0.52f, 0.72f, 0.92f, 1f};

    public DriveUltra3DView(Context context) {
        super(context);
        this.context = context;
        setEGLContextClientVersion(2);
        setEGLConfigChooser(8, 8, 8, 8, 24, 0);
        setRenderer(this);
        setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        setPreserveEGLContextOnPause(true);
        buildWorldData();
    }

    public void setGas(boolean v){ gas=v; }
    public void setBrake(boolean v){ brake=v; }
    public void setLeft(boolean v){ left=v; }
    public void setRight(boolean v){ right=v; }
    public void setFreeMode(boolean v){ freeMode=v; parkedTime=0f; }
    public boolean isFreeMode(){ return freeMode; }
    public void newMission(){ requestNewMission=true; }
    public int getMoney(){ return money; }
    public int getSpeedKmh(){ return Math.round(Math.abs(speed)*3.6f); }

    @Override public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_CULL_FACE);
        GLES20.glCullFace(GLES20.GL_BACK);
        GLES20.glClearColor(SKY[0],SKY[1],SKY[2],SKY[3]);
        program = buildProgram(VERTEX_SHADER, FRAGMENT_SHADER);
        aPos = GLES20.glGetAttribLocation(program, "aPosition");
        aNormal = GLES20.glGetAttribLocation(program, "aNormal");
        uMvp = GLES20.glGetUniformLocation(program, "uMVP");
        uModel = GLES20.glGetUniformLocation(program, "uModel");
        uColor = GLES20.glGetUniformLocation(program, "uColor");
        uLightDir = GLES20.glGetUniformLocation(program, "uLightDir");
        uFogColor = GLES20.glGetUniformLocation(program, "uFogColor");
        uFogNear = GLES20.glGetUniformLocation(program, "uFogNear");
        uFogFar = GLES20.glGetUniformLocation(program, "uFogFar");
        cube = Mesh.cube();
        lastNs = System.nanoTime();
    }

    @Override public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0,0,width,height);
        float ratio = width / (float)Math.max(1,height);
        Matrix.perspectiveM(projection,0,58f,ratio,0.12f,1200f);
    }

    @Override public void onDrawFrame(GL10 gl) {
        long now=System.nanoTime();
        float dt=Math.min(0.035f,Math.max(0.001f,(now-lastNs)/1_000_000_000f));
        lastNs=now;
        updatePhysics(dt);

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT|GLES20.GL_DEPTH_BUFFER_BIT);
        GLES20.glUseProgram(program);
        GLES20.glUniform3f(uLightDir,-0.35f,0.85f,0.28f);
        GLES20.glUniform4f(uFogColor,SKY[0],SKY[1],SKY[2],1f);
        GLES20.glUniform1f(uFogNear,110f);
        GLES20.glUniform1f(uFogFar,520f);

        float yawRad=(float)Math.toRadians(carYaw);
        float fx=(float)Math.sin(yawRad), fz=(float)Math.cos(yawRad);
        float camX=carX-fx*12f;
        float camZ=carZ-fz*12f;
        float camY=6.4f;
        Matrix.setLookAtM(view,0,camX,camY,camZ,carX+fx*8f,1.4f,carZ+fz*8f,0f,1f,0f);
        Matrix.multiplyMM(vp,0,projection,0,view,0);

        // Terrain.
        drawBox(0f,-0.65f,0f,720f,0.5f,720f,0f,0.21f,0.42f,0.18f,1f);

        // Roads and shoulders.
        for(Road r:roads){
            drawRoad(r,0.34f,0.35f,0.37f);
        }

        // Lane markings on the main corridors.
        for(Road r:roads){
            if(r.width < 8.5f) continue;
            float dx=r.x2-r.x1, dz=r.z2-r.z1;
            float len=(float)Math.sqrt(dx*dx+dz*dz);
            int count=Math.max(1,(int)(len/14f));
            for(int i=0;i<count;i+=2){
                float t=(i+0.5f)/count;
                float x=r.x1+dx*t, z=r.z1+dz*t;
                float yaw=(float)Math.toDegrees(Math.atan2(dx,dz));
                drawBox(x,0.035f,z,0.14f,0.025f,5.2f,yaw,0.92f,0.92f,0.84f,1f);
            }
        }

        // Buildings with simple daylight variation.
        for(Building b:buildings){
            drawBox(b.x,b.h*0.5f,b.z,b.w*0.5f,b.h*0.5f,b.d*0.5f,b.yaw,b.r,b.g,b.b,1f);
            // rooftop block makes silhouettes less boxy
            if(b.h>11f) drawBox(b.x,b.h+0.45f,b.z,b.w*0.22f,0.45f,b.d*0.22f,b.yaw,b.r*0.82f,b.g*0.82f,b.b*0.82f,1f);
        }

        // Roadside trees / poles as true 3D geometry.
        for(int i=-22;i<=22;i++){
            float z=i*24f;
            drawTree(-24f,z);
            drawTree(24f,z+9f);
        }

        // Parking mission marker.
        if(!freeMode && !parking.isEmpty()){
            float[] spot=parking.get(missionIndex%parking.size());
            drawBox(spot[0],0.10f,spot[1],3.2f,0.08f,5.0f,spot[2],0.10f,0.95f,0.28f,1f);
            drawBox(spot[0],1.4f,spot[1],0.12f,1.4f,0.12f,0f,0.20f,1f,0.38f,1f);
        }

        // Car shadow + body + cabin + wheels.
        drawBox(carX,0.07f,carZ,1.35f,0.05f,2.6f,carYaw,0.05f,0.06f,0.07f,0.65f);
        drawBox(carX,0.55f,carZ,1.15f,0.42f,2.25f,carYaw,0.82f,0.06f,0.05f,1f);
        drawBox(localX(-0.0f,0.0f),1.18f,localZ(-0.0f,0.0f),0.86f,0.34f,0.95f,carYaw,0.13f,0.24f,0.34f,1f);
        drawWheel(-1.20f, 1.45f); drawWheel(1.20f,1.45f);
        drawWheel(-1.20f,-1.45f); drawWheel(1.20f,-1.45f);
    }

    private void updatePhysics(float dt){
        if(requestNewMission){
            requestNewMission=false;
            missionIndex=(missionIndex+3)%Math.max(1,parking.size());
            freeMode=false;
            parkedTime=0f;
        }

        float accel=gas?12.5f:0f;
        float decel=brake?19f:0f;
        if(gas) speed += accel*dt;
        if(brake){
            if(speed>1f) speed-=decel*dt;
            else speed-=6.5f*dt;
        }
        if(!gas&&!brake){
            float drag=isNearRoad(carX,carZ)?2.3f:7.0f;
            if(speed>0) speed=Math.max(0,speed-drag*dt);
            else if(speed<0) speed=Math.min(0,speed+drag*dt);
        }
        float max=isNearRoad(carX,carZ)?46f:18f;
        speed=Math.max(-10f,Math.min(max,speed));

        float steer=(right?1f:0f)-(left?1f:0f);
        float steerStrength=(17f+Math.min(34f,Math.abs(speed)*1.05f));
        if(Math.abs(speed)>0.5f) carYaw += steer*steerStrength*dt*(speed>=0?1f:-1f);
        if(carYaw>180f)carYaw-=360f; if(carYaw<-180f)carYaw+=360f;

        float r=(float)Math.toRadians(carYaw);
        carX += (float)Math.sin(r)*speed*dt;
        carZ += (float)Math.cos(r)*speed*dt;
        carX=Math.max(-670f,Math.min(670f,carX));
        carZ=Math.max(-670f,Math.min(670f,carZ));

        if(!freeMode&&!parking.isEmpty()){
            float[] s=parking.get(missionIndex%parking.size());
            float dx=carX-s[0], dz=carZ-s[1];
            if(dx*dx+dz*dz<28f && Math.abs(speed)<1.8f){
                parkedTime+=dt;
                if(parkedTime>1.3f){
                    money+=500;
                    missionIndex=(missionIndex+5)%parking.size();
                    parkedTime=0f;
                }
            }else parkedTime=0f;
        }
    }

    private float localX(float side,float forward){
        float r=(float)Math.toRadians(carYaw);
        return carX+(float)Math.cos(r)*side+(float)Math.sin(r)*forward;
    }
    private float localZ(float side,float forward){
        float r=(float)Math.toRadians(carYaw);
        return carZ-(float)Math.sin(r)*side+(float)Math.cos(r)*forward;
    }
    private void drawWheel(float side,float forward){
        drawBox(localX(side,forward),0.42f,localZ(side,forward),0.20f,0.34f,0.46f,carYaw,0.025f,0.025f,0.03f,1f);
    }

    private void drawTree(float x,float z){
        drawBox(x,1.7f,z,0.18f,1.7f,0.18f,0f,0.30f,0.19f,0.09f,1f);
        drawBox(x,4.0f,z,1.10f,1.35f,1.10f,15f,0.10f,0.42f,0.13f,1f);
    }

    private void drawRoad(Road r,float cr,float cg,float cb){
        float dx=r.x2-r.x1, dz=r.z2-r.z1;
        float len=(float)Math.sqrt(dx*dx+dz*dz);
        float x=(r.x1+r.x2)*0.5f, z=(r.z1+r.z2)*0.5f;
        float yaw=(float)Math.toDegrees(Math.atan2(dx,dz));
        // gravel/shoulder
        drawBox(x,-0.01f,z,r.width*0.62f,0.035f,len*0.5f+1.0f,yaw,0.46f,0.43f,0.37f,1f);
        // asphalt
        drawBox(x,0.02f,z,r.width*0.5f,0.04f,len*0.5f,yaw,cr,cg,cb,1f);
    }

    private boolean isNearRoad(float x,float z){
        for(Road r:roads){
            float vx=r.x2-r.x1, vz=r.z2-r.z1;
            float wx=x-r.x1, wz=z-r.z1;
            float vv=vx*vx+vz*vz;
            float t=vv==0?0:(wx*vx+wz*vz)/vv;
            t=Math.max(0,Math.min(1,t));
            float px=r.x1+vx*t, pz=r.z1+vz*t;
            float dx=x-px,dz=z-pz;
            float limit=r.width*0.62f+2.2f;
            if(dx*dx+dz*dz<limit*limit) return true;
        }
        return false;
    }

    private void drawBox(float x,float y,float z,float sx,float sy,float sz,float yaw,float r,float g,float b,float a){
        Matrix.setIdentityM(model,0);
        Matrix.translateM(model,0,x,y,z);
        Matrix.rotateM(model,0,yaw,0f,1f,0f);
        Matrix.scaleM(model,0,sx,sy,sz);
        Matrix.multiplyMM(mvp,0,vp,0,model,0);
        GLES20.glUniformMatrix4fv(uMvp,1,false,mvp,0);
        GLES20.glUniformMatrix4fv(uModel,1,false,model,0);
        GLES20.glUniform4f(uColor,r,g,b,a);
        cube.draw(aPos,aNormal);
    }

    private void buildWorldData(){
        // Major north-south regional highway and city avenue.
        addRoad(0,-660,0,660,11f);
        addRoad(-660,0,660,0,11f);
        addRoad(-500,-250,500,-250,9f);
        addRoad(-500,250,500,250,9f);
        addRoad(-250,-500,-250,500,8f);
        addRoad(250,-500,250,500,8f);

        // Ring road around the central city.
        addRoad(-360,-360,360,-360,10f);
        addRoad(360,-360,360,360,10f);
        addRoad(360,360,-360,360,10f);
        addRoad(-360,360,-360,-360,10f);

        // Diagonal regional connectors / junctions.
        addRoad(-360,-360,-650,-610,8f);
        addRoad(360,-360,650,-610,8f);
        addRoad(-360,360,-650,610,8f);
        addRoad(360,360,650,610,8f);
        addRoad(-500,-250,-650,20,7f);
        addRoad(500,250,650,20,7f);

        // Local streets create actual intersections instead of one straight road.
        for(int i=-2;i<=2;i++){
            float o=i*95f;
            addRoad(-330,o,330,o,6.8f);
            addRoad(o,-330,o,330,6.8f);
        }

        // Simplified roundabout near a highway junction.
        float cx=250f, cz=250f, rad=48f;
        for(int i=0;i<16;i++){
            double a=i*Math.PI*2/16.0, b=(i+1)*Math.PI*2/16.0;
            addRoad(cx+(float)Math.cos(a)*rad,cz+(float)Math.sin(a)*rad,
                    cx+(float)Math.cos(b)*rad,cz+(float)Math.sin(b)*rad,7.2f);
        }

        parking.add(new float[]{32,-250,90});
        parking.add(new float[]{-120,95,90});
        parking.add(new float[]{250,-75,0});
        parking.add(new float[]{-250,180,0});
        parking.add(new float[]{360,110,0});
        parking.add(new float[]{-360,-130,0});
        parking.add(new float[]{120,360,90});
        parking.add(new float[]{-180,-360,90});
        parking.add(new float[]{510,250,90});
        parking.add(new float[]{-500,-250,90});

        Random r=new Random(20260911);
        for(int gx=-6;gx<=6;gx++){
            for(int gz=-6;gz<=6;gz++){
                float x=gx*72f+r.nextFloat()*18f-9f;
                float z=gz*72f+r.nextFloat()*18f-9f;
                if(isNearRoad(x,z)) continue;
                float w=12f+r.nextFloat()*18f;
                float d=12f+r.nextFloat()*20f;
                float h=5f+r.nextFloat()*32f;
                float base=0.36f+r.nextFloat()*0.18f;
                buildings.add(new Building(x,z,w,d,h,(r.nextInt(3)-1)*6f,base,base*0.96f,base*0.90f));
            }
        }

        // Industrial / regional roadside buildings farther out.
        for(int i=0;i<36;i++){
            float z=-610+i*34f;
            float side=(i%2==0)?-1f:1f;
            float x=side*(45f+(i%4)*12f);
            if(Math.abs(z)<390f) continue;
            buildings.add(new Building(x,z,20f+(i%3)*6f,24f,7f+(i%5)*2f,0f,0.46f,0.48f,0.50f));
        }
    }

    private void addRoad(float x1,float z1,float x2,float z2,float width){ roads.add(new Road(x1,z1,x2,z2,width)); }

    private static int buildProgram(String vs,String fs){
        int v=compile(GLES20.GL_VERTEX_SHADER,vs);
        int f=compile(GLES20.GL_FRAGMENT_SHADER,fs);
        int p=GLES20.glCreateProgram();
        GLES20.glAttachShader(p,v); GLES20.glAttachShader(p,f); GLES20.glLinkProgram(p);
        int[] ok=new int[1]; GLES20.glGetProgramiv(p,GLES20.GL_LINK_STATUS,ok,0);
        if(ok[0]==0) throw new RuntimeException("GL link: "+GLES20.glGetProgramInfoLog(p));
        GLES20.glDeleteShader(v); GLES20.glDeleteShader(f);
        return p;
    }

    private static int compile(int type,String src){
        int s=GLES20.glCreateShader(type); GLES20.glShaderSource(s,src); GLES20.glCompileShader(s);
        int[] ok=new int[1]; GLES20.glGetShaderiv(s,GLES20.GL_COMPILE_STATUS,ok,0);
        if(ok[0]==0) throw new RuntimeException("GL shader: "+GLES20.glGetShaderInfoLog(s));
        return s;
    }

    private static final String VERTEX_SHADER=
            "uniform mat4 uMVP;\n"+
            "uniform mat4 uModel;\n"+
            "attribute vec3 aPosition;\n"+
            "attribute vec3 aNormal;\n"+
            "varying vec3 vNormal;\n"+
            "varying float vDepth;\n"+
            "void main(){\n"+
            "  vec4 world=uModel*vec4(aPosition,1.0);\n"+
            "  gl_Position=uMVP*vec4(aPosition,1.0);\n"+
            "  vNormal=normalize(mat3(uModel)*aNormal);\n"+
            "  vDepth=gl_Position.w;\n"+
            "}";

    private static final String FRAGMENT_SHADER=
            "precision mediump float;\n"+
            "uniform vec4 uColor;\n"+
            "uniform vec3 uLightDir;\n"+
            "uniform vec4 uFogColor;\n"+
            "uniform float uFogNear;\n"+
            "uniform float uFogFar;\n"+
            "varying vec3 vNormal;\n"+
            "varying float vDepth;\n"+
            "void main(){\n"+
            "  float diff=max(dot(normalize(vNormal),normalize(uLightDir)),0.0);\n"+
            "  float light=0.38+0.62*diff;\n"+
            "  vec4 c=vec4(uColor.rgb*light,uColor.a);\n"+
            "  float fog=clamp((vDepth-uFogNear)/(uFogFar-uFogNear),0.0,1.0);\n"+
            "  gl_FragColor=mix(c,uFogColor,fog);\n"+
            "}";

    private static class Road{
        final float x1,z1,x2,z2,width;
        Road(float x1,float z1,float x2,float z2,float width){this.x1=x1;this.z1=z1;this.x2=x2;this.z2=z2;this.width=width;}
    }
    private static class Building{
        final float x,z,w,d,h,yaw,r,g,b;
        Building(float x,float z,float w,float d,float h,float yaw,float r,float g,float b){this.x=x;this.z=z;this.w=w;this.d=d;this.h=h;this.yaw=yaw;this.r=r;this.g=g;this.b=b;}
    }

    private static class Mesh{
        final FloatBuffer vertices;
        final int count;
        Mesh(float[] data){
            ByteBuffer bb=ByteBuffer.allocateDirect(data.length*4).order(ByteOrder.nativeOrder());
            vertices=bb.asFloatBuffer(); vertices.put(data).position(0); count=data.length/6;
        }
        void draw(int pos,int normal){
            vertices.position(0);
            GLES20.glEnableVertexAttribArray(pos);
            GLES20.glVertexAttribPointer(pos,3,GLES20.GL_FLOAT,false,24,vertices);
            vertices.position(3);
            GLES20.glEnableVertexAttribArray(normal);
            GLES20.glVertexAttribPointer(normal,3,GLES20.GL_FLOAT,false,24,vertices);
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES,0,count);
            GLES20.glDisableVertexAttribArray(pos); GLES20.glDisableVertexAttribArray(normal);
        }
        static Mesh cube(){
            float[] v={
                // front +Z
                -1,-1,1, 0,0,1,  1,-1,1, 0,0,1,  1,1,1, 0,0,1,
                -1,-1,1, 0,0,1,  1,1,1, 0,0,1, -1,1,1, 0,0,1,
                // back -Z
                1,-1,-1, 0,0,-1, -1,-1,-1, 0,0,-1, -1,1,-1, 0,0,-1,
                1,-1,-1, 0,0,-1, -1,1,-1, 0,0,-1, 1,1,-1, 0,0,-1,
                // left -X
                -1,-1,-1, -1,0,0, -1,-1,1, -1,0,0, -1,1,1, -1,0,0,
                -1,-1,-1, -1,0,0, -1,1,1, -1,0,0, -1,1,-1, -1,0,0,
                // right +X
                1,-1,1, 1,0,0, 1,-1,-1, 1,0,0, 1,1,-1, 1,0,0,
                1,-1,1, 1,0,0, 1,1,-1, 1,0,0, 1,1,1, 1,0,0,
                // top +Y
                -1,1,1, 0,1,0, 1,1,1, 0,1,0, 1,1,-1, 0,1,0,
                -1,1,1, 0,1,0, 1,1,-1, 0,1,0, -1,1,-1, 0,1,0,
                // bottom -Y
                -1,-1,-1, 0,-1,0, 1,-1,-1, 0,-1,0, 1,-1,1, 0,-1,0,
                -1,-1,-1, 0,-1,0, 1,-1,1, 0,-1,0, -1,-1,1, 0,-1,0
            };
            return new Mesh(v);
        }
    }
}
