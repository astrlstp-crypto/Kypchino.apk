package com.kupchino.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
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
 * Kupchino Drive MAX renderer.
 * Uses OpenGL ES 2.0, native surface resolution and runtime-generated 2048x2048 textures.
 * Road layout is fictional, but is designed like a regional road network: avenues,
 * local streets, ring roads, roundabouts, diagonal connectors and long highways.
 */
public class DriveUltra3DView extends GLSurfaceView implements GLSurfaceView.Renderer {
    private volatile boolean gas, brake, left, right, freeMode, requestNewMission;

    private float carX = 0f, carZ = -35f, carYaw = 0f, speed = 0f;
    private int money = 0, missionIndex = 3;
    private float parkedTime = 0f;
    private long lastNs;

    private int program;
    private int aPos, aNormal, aTex;
    private int uMvp, uModel, uColor, uLightDir, uFogColor, uFogNear, uFogFar;
    private int uTexture, uUseTexture, uTexScale;

    private final float[] projection = new float[16];
    private final float[] view = new float[16];
    private final float[] model = new float[16];
    private final float[] vp = new float[16];
    private final float[] mvp = new float[16];

    private Mesh cube;
    private int texAsphalt, texGrass, texBuilding, texGravel;

    private final List<Road> roads = new ArrayList<>();
    private final List<Building> buildings = new ArrayList<>();
    private final List<float[]> parking = new ArrayList<>();

    private static final float[] SKY = {0.47f, 0.68f, 0.91f, 1f};
    private static final int MAT_SOLID=0, MAT_ASPHALT=1, MAT_GRASS=2, MAT_BUILDING=3, MAT_GRAVEL=4;
    public static final String QUALITY_NAME = "MAX 2048";

    public DriveUltra3DView(Context context) {
        super(context);
        setEGLContextClientVersion(2);
        setEGLConfigChooser(8,8,8,8,24,0);
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
        aPos = GLES20.glGetAttribLocation(program,"aPosition");
        aNormal = GLES20.glGetAttribLocation(program,"aNormal");
        aTex = GLES20.glGetAttribLocation(program,"aTexCoord");
        uMvp = GLES20.glGetUniformLocation(program,"uMVP");
        uModel = GLES20.glGetUniformLocation(program,"uModel");
        uColor = GLES20.glGetUniformLocation(program,"uColor");
        uLightDir = GLES20.glGetUniformLocation(program,"uLightDir");
        uFogColor = GLES20.glGetUniformLocation(program,"uFogColor");
        uFogNear = GLES20.glGetUniformLocation(program,"uFogNear");
        uFogFar = GLES20.glGetUniformLocation(program,"uFogFar");
        uTexture = GLES20.glGetUniformLocation(program,"uTexture");
        uUseTexture = GLES20.glGetUniformLocation(program,"uUseTexture");
        uTexScale = GLES20.glGetUniformLocation(program,"uTexScale");

        cube = Mesh.cube();
        texAsphalt = createTexture(1,2048);
        texGrass = createTexture(2,2048);
        texBuilding = createTexture(3,2048);
        texGravel = createTexture(4,2048);
        lastNs=System.nanoTime();
    }

    @Override public void onSurfaceChanged(GL10 gl,int width,int height) {
        GLES20.glViewport(0,0,width,height);
        float ratio=width/(float)Math.max(1,height);
        Matrix.perspectiveM(projection,0,60f,ratio,0.10f,1500f);
    }

    @Override public void onDrawFrame(GL10 gl) {
        long now=System.nanoTime();
        float dt=Math.min(0.033f,Math.max(0.001f,(now-lastNs)/1_000_000_000f));
        lastNs=now;
        updatePhysics(dt);

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT|GLES20.GL_DEPTH_BUFFER_BIT);
        GLES20.glUseProgram(program);
        GLES20.glUniform3f(uLightDir,-0.30f,0.88f,0.32f);
        GLES20.glUniform4f(uFogColor,SKY[0],SKY[1],SKY[2],1f);
        GLES20.glUniform1f(uFogNear,180f);
        GLES20.glUniform1f(uFogFar,760f);
        GLES20.glUniform1i(uTexture,0);

        float yawRad=(float)Math.toRadians(carYaw);
        float fx=(float)Math.sin(yawRad), fz=(float)Math.cos(yawRad);
        float camX=carX-fx*13.5f, camZ=carZ-fz*13.5f;
        Matrix.setLookAtM(view,0,camX,6.8f,camZ,carX+fx*10f,1.45f,carZ+fz*10f,0,1,0);
        Matrix.multiplyMM(vp,0,projection,0,view,0);

        // Textured terrain.
        drawBox(0f,-0.70f,0f,1040f,0.5f,1040f,0f,1,1,1,1,MAT_GRASS,84f,84f);

        // Road network with textured shoulders and asphalt.
        for(Road r:roads) drawRoad(r);

        // High-quality road markings.
        for(Road r:roads){
            if(r.width<8.0f) continue;
            float dx=r.x2-r.x1,dz=r.z2-r.z1;
            float len=(float)Math.sqrt(dx*dx+dz*dz);
            int count=Math.max(1,(int)(len/12f));
            float yaw=(float)Math.toDegrees(Math.atan2(dx,dz));
            for(int i=0;i<count;i+=2){
                float t=(i+0.5f)/count;
                drawBox(r.x1+dx*t,0.075f,r.z1+dz*t,0.10f,0.018f,4.4f,yaw,
                        0.96f,0.95f,0.86f,1f,MAT_SOLID,1,1);
            }
        }

        // Cull detailed buildings by camera distance to keep MAX smooth.
        final float drawRadius2=430f*430f;
        for(Building b:buildings){
            float dx=b.x-carX,dz=b.z-carZ;
            if(dx*dx+dz*dz>drawRadius2) continue;
            float repeatX=Math.max(1f,b.w/5.5f), repeatY=Math.max(1f,b.h/4.0f);
            drawBox(b.x,b.h*0.5f,b.z,b.w*0.5f,b.h*0.5f,b.d*0.5f,b.yaw,
                    b.r,b.g,b.b,1f,MAT_BUILDING,repeatX,repeatY);
            if(b.h>13f){
                drawBox(b.x,b.h+0.48f,b.z,b.w*0.22f,0.48f,b.d*0.22f,b.yaw,
                        0.44f,0.45f,0.47f,1f,MAT_GRAVEL,2f,2f);
            }
            // soft fake contact shadow
            drawBox(b.x,0.035f,b.z,b.w*0.58f,0.018f,b.d*0.58f,b.yaw,
                    0.08f,0.10f,0.08f,0.34f,MAT_SOLID,1,1);
        }

        // Dense roadside greenery near central highway.
        for(int i=-31;i<=31;i++){
            float z=i*24f;
            if(Math.abs(z-carZ)>330f) continue;
            drawTree(-24f,z);
            drawTree(24f,z+10f);
        }

        // Mission parking marker.
        if(!freeMode&&!parking.isEmpty()){
            float[] s=parking.get(missionIndex%parking.size());
            drawBox(s[0],0.10f,s[1],3.2f,0.08f,5.0f,s[2],0.10f,0.95f,0.28f,1f,MAT_SOLID,1,1);
            drawBox(s[0],1.6f,s[1],0.10f,1.6f,0.10f,0,0.20f,1f,0.38f,1f,MAT_SOLID,1,1);
        }

        drawCar();
    }

    private void drawCar(){
        drawBox(carX,0.07f,carZ,1.42f,0.045f,2.72f,carYaw,0.03f,0.04f,0.05f,0.50f,MAT_SOLID,1,1);
        drawBox(carX,0.56f,carZ,1.18f,0.42f,2.28f,carYaw,0.84f,0.055f,0.045f,1f,MAT_SOLID,1,1);
        drawBox(localX(0,0.10f),1.18f,localZ(0,0.10f),0.88f,0.34f,0.95f,carYaw,0.10f,0.22f,0.32f,1f,MAT_SOLID,1,1);
        // bonnet and bumper details
        drawBox(localX(0,1.78f),0.70f,localZ(0,1.78f),1.02f,0.12f,0.42f,carYaw,0.72f,0.035f,0.03f,1f,MAT_SOLID,1,1);
        drawBox(localX(0,2.30f),0.42f,localZ(0,2.30f),0.94f,0.07f,0.12f,carYaw,0.07f,0.07f,0.08f,1f,MAT_SOLID,1,1);
        drawWheel(-1.20f,1.48f); drawWheel(1.20f,1.48f);
        drawWheel(-1.20f,-1.48f); drawWheel(1.20f,-1.48f);
    }

    private void updatePhysics(float dt){
        if(requestNewMission){
            requestNewMission=false;
            missionIndex=(missionIndex+3)%Math.max(1,parking.size());
            freeMode=false; parkedTime=0;
        }
        if(gas) speed+=13.5f*dt;
        if(brake){ if(speed>1f)speed-=20f*dt; else speed-=7f*dt; }
        if(!gas&&!brake){
            float drag=isNearRoad(carX,carZ)?2.2f:7.5f;
            if(speed>0)speed=Math.max(0,speed-drag*dt); else if(speed<0)speed=Math.min(0,speed+drag*dt);
        }
        float max=isNearRoad(carX,carZ)?52f:19f;
        speed=Math.max(-11f,Math.min(max,speed));
        float steer=(right?1f:0f)-(left?1f:0f);
        float steerStrength=17f+Math.min(35f,Math.abs(speed));
        if(Math.abs(speed)>0.45f) carYaw+=steer*steerStrength*dt*(speed>=0?1f:-1f);
        if(carYaw>180)carYaw-=360; if(carYaw<-180)carYaw+=360;
        float a=(float)Math.toRadians(carYaw);
        carX+=(float)Math.sin(a)*speed*dt;
        carZ+=(float)Math.cos(a)*speed*dt;
        carX=Math.max(-990f,Math.min(990f,carX));
        carZ=Math.max(-990f,Math.min(990f,carZ));

        if(!freeMode&&!parking.isEmpty()){
            float[] s=parking.get(missionIndex%parking.size());
            float dx=carX-s[0],dz=carZ-s[1];
            if(dx*dx+dz*dz<28f&&Math.abs(speed)<1.8f){
                parkedTime+=dt;
                if(parkedTime>1.3f){ money+=500; missionIndex=(missionIndex+5)%parking.size(); parkedTime=0; }
            }else parkedTime=0;
        }
    }

    private float localX(float side,float forward){
        float a=(float)Math.toRadians(carYaw);
        return carX+(float)Math.cos(a)*side+(float)Math.sin(a)*forward;
    }
    private float localZ(float side,float forward){
        float a=(float)Math.toRadians(carYaw);
        return carZ-(float)Math.sin(a)*side+(float)Math.cos(a)*forward;
    }
    private void drawWheel(float side,float forward){
        drawBox(localX(side,forward),0.40f,localZ(side,forward),0.22f,0.35f,0.48f,carYaw,
                0.025f,0.025f,0.03f,1f,MAT_SOLID,1,1);
    }
    private void drawTree(float x,float z){
        drawBox(x,1.7f,z,0.18f,1.7f,0.18f,0,0.30f,0.18f,0.08f,1,MAT_SOLID,1,1);
        drawBox(x,4.0f,z,1.15f,1.40f,1.15f,15f,0.10f,0.43f,0.13f,1,MAT_SOLID,1,1);
        drawBox(x+0.75f,4.15f,z-0.35f,0.72f,0.90f,0.72f,-12f,0.12f,0.49f,0.16f,1,MAT_SOLID,1,1);
    }

    private void drawRoad(Road r){
        float dx=r.x2-r.x1,dz=r.z2-r.z1;
        float len=(float)Math.sqrt(dx*dx+dz*dz);
        float x=(r.x1+r.x2)*0.5f,z=(r.z1+r.z2)*0.5f;
        float yaw=(float)Math.toDegrees(Math.atan2(dx,dz));
        drawBox(x,-0.005f,z,r.width*0.64f,0.035f,len*0.5f+1.0f,yaw,1,1,1,1,MAT_GRAVEL,
                Math.max(1f,r.width/5f),Math.max(2f,len/10f));
        drawBox(x,0.03f,z,r.width*0.5f,0.04f,len*0.5f,yaw,1,1,1,1,MAT_ASPHALT,
                Math.max(1f,r.width/6f),Math.max(2f,len/8f));
    }

    private boolean isNearRoad(float x,float z){
        for(Road r:roads){
            float vx=r.x2-r.x1,vz=r.z2-r.z1,wx=x-r.x1,wz=z-r.z1;
            float vv=vx*vx+vz*vz;
            float t=vv==0?0:(wx*vx+wz*vz)/vv;
            t=Math.max(0,Math.min(1,t));
            float px=r.x1+vx*t,pz=r.z1+vz*t,dx=x-px,dz=z-pz;
            float limit=r.width*0.64f+2.3f;
            if(dx*dx+dz*dz<limit*limit)return true;
        }
        return false;
    }

    private void drawBox(float x,float y,float z,float sx,float sy,float sz,float yaw,
                         float r,float g,float b,float alpha,int material,float texX,float texY){
        Matrix.setIdentityM(model,0);
        Matrix.translateM(model,0,x,y,z);
        Matrix.rotateM(model,0,yaw,0,1,0);
        Matrix.scaleM(model,0,sx,sy,sz);
        Matrix.multiplyMM(mvp,0,vp,0,model,0);
        GLES20.glUniformMatrix4fv(uMvp,1,false,mvp,0);
        GLES20.glUniformMatrix4fv(uModel,1,false,model,0);
        GLES20.glUniform4f(uColor,r,g,b,alpha);
        GLES20.glUniform2f(uTexScale,texX,texY);
        if(material==MAT_SOLID){
            GLES20.glUniform1f(uUseTexture,0f);
        }else{
            GLES20.glUniform1f(uUseTexture,1f);
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,textureFor(material));
        }
        cube.draw(aPos,aNormal,aTex);
    }

    private int textureFor(int material){
        if(material==MAT_ASPHALT)return texAsphalt;
        if(material==MAT_GRASS)return texGrass;
        if(material==MAT_BUILDING)return texBuilding;
        return texGravel;
    }

    private int createTexture(int kind,int size){
        Bitmap bm=Bitmap.createBitmap(size,size,Bitmap.Config.ARGB_8888);
        Canvas c=new Canvas(bm);
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        Random rnd=new Random(88000+kind);

        if(kind==1){ // asphalt
            c.drawColor(Color.rgb(53,55,57));
            for(int i=0;i<9000;i++){
                int v=43+rnd.nextInt(38); p.setColor(Color.rgb(v,v,v));
                float x=rnd.nextInt(size),y=rnd.nextInt(size),s=1+rnd.nextInt(4);
                c.drawRect(x,y,x+s,y+s,p);
            }
            p.setStrokeWidth(2f); p.setColor(Color.rgb(39,40,41));
            for(int i=0;i<110;i++){
                float x=rnd.nextInt(size),y=rnd.nextInt(size);
                c.drawLine(x,y,x+rnd.nextInt(90)-45,y+rnd.nextInt(90)-45,p);
            }
        }else if(kind==2){ // grass
            c.drawColor(Color.rgb(61,105,48));
            for(int i=0;i<7000;i++){
                int g=82+rnd.nextInt(65), rr=38+rnd.nextInt(32), bb=31+rnd.nextInt(28);
                p.setColor(Color.rgb(rr,g,bb));
                float x=rnd.nextInt(size),y=rnd.nextInt(size);
                c.drawCircle(x,y,1+rnd.nextInt(5),p);
            }
        }else if(kind==3){ // facade + windows
            c.drawColor(Color.rgb(188,177,164));
            p.setStrokeWidth(5f); p.setColor(Color.rgb(151,141,132));
            for(int y=0;y<size;y+=128)c.drawLine(0,y,size,y,p);
            for(int x=0;x<size;x+=256)c.drawLine(x,0,x,size,p);
            for(int y=28;y<size;y+=128){
                for(int x=28;x<size;x+=128){
                    int glow=rnd.nextInt(100);
                    p.setColor(glow<22?Color.rgb(242,213,136):Color.rgb(70,101,125));
                    c.drawRect(x,y,x+62,y+72,p);
                    p.setColor(Color.rgb(38,48,55));
                    c.drawRect(x+29,y,x+33,y+72,p);
                }
            }
        }else{ // gravel / shoulder
            c.drawColor(Color.rgb(125,117,103));
            for(int i=0;i<9000;i++){
                int v=92+rnd.nextInt(62); p.setColor(Color.rgb(v,v-5,v-12));
                float x=rnd.nextInt(size),y=rnd.nextInt(size),s=2+rnd.nextInt(7);
                c.drawCircle(x,y,s,p);
            }
        }

        int[] ids=new int[1];
        GLES20.glGenTextures(1,ids,0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,ids[0]);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_MIN_FILTER,GLES20.GL_LINEAR_MIPMAP_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_MAG_FILTER,GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_WRAP_S,GLES20.GL_REPEAT);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_WRAP_T,GLES20.GL_REPEAT);
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D,0,bm,0);
        GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D);
        bm.recycle();
        return ids[0];
    }

    private void buildWorldData(){
        // Long regional highways.
        addRoad(0,-980,0,980,11f);
        addRoad(-980,0,980,0,11f);
        addRoad(-820,-520,820,-520,9.5f);
        addRoad(-820,520,820,520,9.5f);
        addRoad(-520,-820,-520,820,8.5f);
        addRoad(520,-820,520,820,8.5f);

        // Large ring road.
        addRoad(-430,-430,430,-430,10f); addRoad(430,-430,430,430,10f);
        addRoad(430,430,-430,430,10f); addRoad(-430,430,-430,-430,10f);

        // Regional diagonal exits.
        addRoad(-430,-430,-950,-850,8f); addRoad(430,-430,950,-850,8f);
        addRoad(-430,430,-950,850,8f); addRoad(430,430,950,850,8f);
        addRoad(-650,-260,-960,80,7.5f); addRoad(650,260,960,-80,7.5f);

        // Dense city grid with intersections.
        for(int i=-4;i<=4;i++){
            float o=i*92f;
            addRoad(-410,o,410,o,6.8f);
            addRoad(o,-410,o,410,6.8f);
        }

        // Two roundabouts.
        addRoundabout(250,250,50,18,7.3f);
        addRoundabout(-250,-250,42,16,6.8f);

        parking.add(new float[]{32,-276,90}); parking.add(new float[]{-120,95,90});
        parking.add(new float[]{250,-92,0}); parking.add(new float[]{-250,184,0});
        parking.add(new float[]{430,110,0}); parking.add(new float[]{-430,-130,0});
        parking.add(new float[]{120,430,90}); parking.add(new float[]{-180,-430,90});
        parking.add(new float[]{530,520,90}); parking.add(new float[]{-520,-520,90});
        parking.add(new float[]{760,0,0}); parking.add(new float[]{0,760,90});

        Random rnd=new Random(20260911);
        for(int gx=-10;gx<=10;gx++){
            for(int gz=-10;gz<=10;gz++){
                float x=gx*72f+rnd.nextFloat()*20f-10f;
                float z=gz*72f+rnd.nextFloat()*20f-10f;
                if(isNearRoad(x,z))continue;
                float w=12+rnd.nextFloat()*21,d=12+rnd.nextFloat()*21,h=6+rnd.nextFloat()*40;
                float base=0.70f+rnd.nextFloat()*0.22f;
                buildings.add(new Building(x,z,w,d,h,(rnd.nextInt(3)-1)*5f,base,base*0.98f,base*0.94f));
            }
        }
        // Regional roadside warehouses and service buildings.
        for(int i=0;i<62;i++){
            float z=-930+i*30f;
            if(Math.abs(z)<480)continue;
            float side=(i%2==0)?-1:1;
            float x=side*(45+(i%5)*13f);
            buildings.add(new Building(x,z,22+(i%4)*7,24+(i%3)*5,7+(i%6)*2,0,0.76f,0.78f,0.80f));
        }
    }

    private void addRoundabout(float cx,float cz,float rad,int segments,float width){
        for(int i=0;i<segments;i++){
            double a=i*Math.PI*2/segments,b=(i+1)*Math.PI*2/segments;
            addRoad(cx+(float)Math.cos(a)*rad,cz+(float)Math.sin(a)*rad,
                    cx+(float)Math.cos(b)*rad,cz+(float)Math.sin(b)*rad,width);
        }
    }
    private void addRoad(float x1,float z1,float x2,float z2,float width){ roads.add(new Road(x1,z1,x2,z2,width)); }

    private static int buildProgram(String vs,String fs){
        int v=compile(GLES20.GL_VERTEX_SHADER,vs),f=compile(GLES20.GL_FRAGMENT_SHADER,fs);
        int p=GLES20.glCreateProgram();
        GLES20.glAttachShader(p,v); GLES20.glAttachShader(p,f); GLES20.glLinkProgram(p);
        int[] ok=new int[1]; GLES20.glGetProgramiv(p,GLES20.GL_LINK_STATUS,ok,0);
        if(ok[0]==0)throw new RuntimeException("GL link: "+GLES20.glGetProgramInfoLog(p));
        GLES20.glDeleteShader(v); GLES20.glDeleteShader(f); return p;
    }
    private static int compile(int type,String src){
        int s=GLES20.glCreateShader(type); GLES20.glShaderSource(s,src); GLES20.glCompileShader(s);
        int[] ok=new int[1]; GLES20.glGetShaderiv(s,GLES20.GL_COMPILE_STATUS,ok,0);
        if(ok[0]==0)throw new RuntimeException("GL shader: "+GLES20.glGetShaderInfoLog(s));
        return s;
    }

    private static final String VERTEX_SHADER=
            "uniform mat4 uMVP;\n"+
            "uniform mat4 uModel;\n"+
            "uniform vec2 uTexScale;\n"+
            "attribute vec3 aPosition;\n"+
            "attribute vec3 aNormal;\n"+
            "attribute vec2 aTexCoord;\n"+
            "varying vec3 vNormal;\n"+
            "varying float vDepth;\n"+
            "varying vec2 vUV;\n"+
            "void main(){\n"+
            " gl_Position=uMVP*vec4(aPosition,1.0);\n"+
            " vNormal=normalize(mat3(uModel)*aNormal);\n"+
            " vDepth=gl_Position.w;\n"+
            " vUV=aTexCoord*uTexScale;\n"+
            "}";

    private static final String FRAGMENT_SHADER=
            "precision mediump float;\n"+
            "uniform vec4 uColor;\n"+
            "uniform vec3 uLightDir;\n"+
            "uniform vec4 uFogColor;\n"+
            "uniform float uFogNear;\n"+
            "uniform float uFogFar;\n"+
            "uniform sampler2D uTexture;\n"+
            "uniform float uUseTexture;\n"+
            "varying vec3 vNormal;\n"+
            "varying float vDepth;\n"+
            "varying vec2 vUV;\n"+
            "void main(){\n"+
            " vec4 tex=texture2D(uTexture,vUV);\n"+
            " vec4 base=mix(uColor,vec4(tex.rgb*uColor.rgb,uColor.a),uUseTexture);\n"+
            " float diff=max(dot(normalize(vNormal),normalize(uLightDir)),0.0);\n"+
            " float light=0.35+0.65*diff;\n"+
            " vec4 lit=vec4(base.rgb*light,base.a);\n"+
            " float fog=clamp((vDepth-uFogNear)/(uFogFar-uFogNear),0.0,1.0);\n"+
            " gl_FragColor=mix(lit,uFogColor,fog);\n"+
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
        final FloatBuffer vertices; final int count;
        Mesh(float[] data){
            ByteBuffer bb=ByteBuffer.allocateDirect(data.length*4).order(ByteOrder.nativeOrder());
            vertices=bb.asFloatBuffer(); vertices.put(data).position(0); count=data.length/8;
        }
        void draw(int pos,int normal,int tex){
            vertices.position(0); GLES20.glEnableVertexAttribArray(pos); GLES20.glVertexAttribPointer(pos,3,GLES20.GL_FLOAT,false,32,vertices);
            vertices.position(3); GLES20.glEnableVertexAttribArray(normal); GLES20.glVertexAttribPointer(normal,3,GLES20.GL_FLOAT,false,32,vertices);
            vertices.position(6); GLES20.glEnableVertexAttribArray(tex); GLES20.glVertexAttribPointer(tex,2,GLES20.GL_FLOAT,false,32,vertices);
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES,0,count);
            GLES20.glDisableVertexAttribArray(pos); GLES20.glDisableVertexAttribArray(normal); GLES20.glDisableVertexAttribArray(tex);
        }
        static Mesh cube(){
            float[] raw={
                    -1,-1,1,0,0,1, 1,-1,1,0,0,1, 1,1,1,0,0,1, -1,-1,1,0,0,1, 1,1,1,0,0,1, -1,1,1,0,0,1,
                    1,-1,-1,0,0,-1, -1,-1,-1,0,0,-1, -1,1,-1,0,0,-1, 1,-1,-1,0,0,-1, -1,1,-1,0,0,-1, 1,1,-1,0,0,-1,
                    -1,-1,-1,-1,0,0, -1,-1,1,-1,0,0, -1,1,1,-1,0,0, -1,-1,-1,-1,0,0, -1,1,1,-1,0,0, -1,1,-1,-1,0,0,
                    1,-1,1,1,0,0, 1,-1,-1,1,0,0, 1,1,-1,1,0,0, 1,-1,1,1,0,0, 1,1,-1,1,0,0, 1,1,1,1,0,0,
                    -1,1,1,0,1,0, 1,1,1,0,1,0, 1,1,-1,0,1,0, -1,1,1,0,1,0, 1,1,-1,0,1,0, -1,1,-1,0,1,0,
                    -1,-1,-1,0,-1,0, 1,-1,-1,0,-1,0, 1,-1,1,0,-1,0, -1,-1,-1,0,-1,0, 1,-1,1,0,-1,0, -1,-1,1,0,-1,0
            };
            float[] uv={0,0,1,0,1,1,0,0,1,1,0,1};
            float[] out=new float[36*8];
            for(int i=0;i<36;i++){
                int ri=i*6,oi=i*8,ui=(i%6)*2;
                for(int k=0;k<6;k++)out[oi+k]=raw[ri+k];
                out[oi+6]=uv[ui]; out[oi+7]=uv[ui+1];
            }
            return new Mesh(out);
        }
    }
}
