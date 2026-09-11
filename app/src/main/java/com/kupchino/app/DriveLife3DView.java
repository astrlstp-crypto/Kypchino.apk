package com.kupchino.app;

import android.content.Context;
import android.content.SharedPreferences;
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
 * Kupchino Drive LIFE/MAX: large road network, on-foot mode, locations,
 * 250 parking missions, 40 fictional cars and persistent player progression.
 */
public class DriveLife3DView extends GLSurfaceView implements GLSurfaceView.Renderer {
    private volatile boolean gas, brake, left, right, freeMode, requestNewMission, paused;
    private volatile boolean onFoot = false;

    private float carX = 0f, carZ = -80f, carYaw = 0f, speed = 0f;
    private float playerX = 2.8f, playerZ = -80f, playerYaw = 0f, walkPhase = 0f;
    private int money, missionNumber, selectedCar, strength, health;
    private int missionSpotIndex = 0;
    private float parkedTime = 0f;
    private long lastNs;
    private String lastEvent = "";

    private final SharedPreferences prefs;

    private int program;
    private int aPos, aNormal, aTex;
    private int uMvp, uModel, uColor, uLightDir, uFogColor, uFogNear, uFogFar;
    private int uTexture, uUseTexture, uTexScale;

    private final float[] projection = new float[16];
    private final float[] view = new float[16];
    private final float[] model = new float[16];
    private final float[] vp = new float[16];
    private final float[] mvp = new float[16];

    private Mesh cube, disk;
    private int texAsphalt, texGrass, texBuilding, texGravel;

    private final List<Road> roads = new ArrayList<>();
    private final List<Building> buildings = new ArrayList<>();
    private final List<Place> places = new ArrayList<>();
    private final List<float[]> parking = new ArrayList<>();

    private static final float MAP_HALF = 2600f;
    private static final float[] SKY = {0.46f, 0.68f, 0.93f, 1f};
    private static final int MAT_SOLID=0, MAT_ASPHALT=1, MAT_GRASS=2, MAT_BUILDING=3, MAT_GRAVEL=4;
    public static final String QUALITY_NAME = "MAX 2048 LIFE";

    private static final String[] CAR_NAMES = {
            "Kupchino Start", "Neva Mini", "Murino City", "Parnas Hatch", "Baltic Sedan",
            "Ladoga Tour", "Nevsky Line", "Polar Wagon", "Vyborg Cross", "Karelia X",
            "Volkhov GT", "Aurora S", "Sputnik R", "Taiga Cross", "Meteor Coupe",
            "Sever Lux", "RusLine Sport", "White Night", "Granite RS", "Borey X",
            "Neva Storm", "Kupchino Turbo", "Murino RS", "Ladoga V8", "Aurora GT",
            "Polar Evo", "Baltic R", "Sirius X", "Taiga Pro", "Vega Sport",
            "Zenit R", "Northern Beast", "Neva Hyper", "Aurora Black", "Granite V12",
            "Borey RS", "Sputnik Ultra", "White Night X", "Kupchino Legend", "KUPCHINO ONE"
    };

    public DriveLife3DView(Context context) {
        super(context);
        prefs = context.getSharedPreferences("drive_life_progress", Context.MODE_PRIVATE);
        money = prefs.getInt("money", 1200);
        missionNumber = Math.max(1, Math.min(250, prefs.getInt("mission", 1)));
        selectedCar = Math.max(0, Math.min(CAR_NAMES.length-1, prefs.getInt("selected_car", 0)));
        strength = Math.max(0, Math.min(100, prefs.getInt("strength", 0)));
        health = Math.max(1, Math.min(100, prefs.getInt("health", 100)));
        prefs.edit().putBoolean("car_owned_0", true).apply();

        setEGLContextClientVersion(2);
        setEGLConfigChooser(8,8,8,8,24,0);
        setRenderer(this);
        setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        setPreserveEGLContextOnPause(true);
        buildWorldData();
        missionSpotIndex = missionNumber % Math.max(1, parking.size());
    }

    public void setGas(boolean v){ gas=v; }
    public void setBrake(boolean v){ brake=v; }
    public void setLeft(boolean v){ left=v; }
    public void setRight(boolean v){ right=v; }
    public void setPaused(boolean v){ paused=v; gas=brake=left=right=false; }
    public boolean isPaused(){ return paused; }
    public void setFreeMode(boolean v){ freeMode=v; parkedTime=0f; if(!v && onFoot) onFoot=false; }
    public boolean isFreeMode(){ return freeMode; }
    public boolean isOnFoot(){ return onFoot; }
    public void newMission(){ requestNewMission=true; }
    public int getMoney(){ return money; }
    public int getSpeedKmh(){ return Math.round(Math.abs(speed)*3.6f); }
    public int getMissionNumber(){ return missionNumber; }
    public int getStrength(){ return strength; }
    public int getHealth(){ return health; }
    public int getAvatarHeightCm(){ return 145; }
    public float getGripKg(){ return 18f + strength * 1.85f; }
    public String getSelectedCarName(){ return CAR_NAMES[selectedCar]; }
    public int getCarCount(){ return CAR_NAMES.length; }
    public String getCarName(int i){ return CAR_NAMES[Math.max(0,Math.min(CAR_NAMES.length-1,i))]; }
    public int getCarPrice(int i){ return i==0?0:700 + i*i*420; }
    public boolean isCarOwned(int i){ return i==0 || prefs.getBoolean("car_owned_"+i,false); }
    public int getSelectedCar(){ return selectedCar; }
    public String getLastEvent(){ return lastEvent; }

    public String buyOrSelectCar(int i){
        if(i<0 || i>=CAR_NAMES.length) return "Нет такой машины";
        if(isCarOwned(i)){
            selectedCar=i;
            prefs.edit().putInt("selected_car",i).apply();
            return "Выбрана: "+CAR_NAMES[i]+" ✅";
        }
        int price=getCarPrice(i);
        if(money<price) return "Нужно $"+price+", а у тебя $"+money;
        money-=price;
        selectedCar=i;
        prefs.edit().putInt("money",money).putBoolean("car_owned_"+i,true).putInt("selected_car",i).apply();
        return "Куплено: "+CAR_NAMES[i]+" за $"+price+" 🥶";
    }

    public String toggleVehicle(){
        if(!freeMode) return "Выйти из машины можно только в FREE MODE";
        if(!onFoot){
            if(Math.abs(speed)>1.2f) return "Сначала полностью останови машину";
            onFoot=true;
            float a=(float)Math.toRadians(carYaw);
            playerX=carX+(float)Math.cos(a)*2.4f;
            playerZ=carZ-(float)Math.sin(a)*2.4f;
            playerYaw=carYaw;
            lastEvent="Ты вышел из машины";
            return lastEvent;
        }
        float dx=playerX-carX,dz=playerZ-carZ;
        if(dx*dx+dz*dz>25f) return "Подойди ближе к машине";
        onFoot=false;
        lastEvent="Ты сел в "+CAR_NAMES[selectedCar];
        return lastEvent;
    }

    public String getNearbyPlaceName(){
        Place p=nearestPlace();
        if(!onFoot || p==null) return "";
        float dx=playerX-p.x,dz=playerZ-p.z;
        if(dx*dx+dz*dz>100f) return "";
        return p.name;
    }

    public String interactNearby(){
        if(!freeMode || !onFoot) return "Выйди из машины в FREE MODE";
        Place p=nearestPlace();
        if(p==null) return "Рядом ничего нет";
        float dx=playerX-p.x,dz=playerZ-p.z;
        if(dx*dx+dz*dz>64f) return "Подойди к зелёному кругу";

        if(p.type==1){
            if(money<100) return "Качалка: тренировка стоит $100";
            money-=100;
            strength=Math.min(100,strength+1);
            health=Math.min(100,health+5);
            saveProgress();
            lastEvent="Тренировка ✅ Сила "+strength+"/100 • хват "+String.format(java.util.Locale.US,"%.1f",getGripKg())+" кг";
            return lastEvent;
        }
        if(p.type==2){
            lastEvent=p.name+" — рост персонажа "+getAvatarHeightCm()+" см • хват "+String.format(java.util.Locale.US,"%.1f",getGripKg())+" кг • здоровье "+health+"/100";
            return lastEvent;
        }
        if(p.type==3){
            if(money>=50){ money-=50; health=Math.min(100,health+15); saveProgress(); }
            lastEvent=p.name+" — перекус восстановил здоровье до "+health+"/100";
            return lastEvent;
        }
        if(p.type==4){
            money+=80;
            saveProgress();
            lastEvent=p.name+" — подработка +$80";
            return lastEvent;
        }
        return p.name;
    }

    public String clinicReport(){
        return "Рост персонажа: "+getAvatarHeightCm()+" см\nСила хвата: "+String.format(java.util.Locale.US,"%.1f",getGripKg())+" кг\nСила: "+strength+"/100\nЗдоровье: "+health+"/100";
    }

    private void saveProgress(){
        prefs.edit().putInt("money",money).putInt("mission",missionNumber).putInt("selected_car",selectedCar)
                .putInt("strength",strength).putInt("health",health).apply();
    }

    @Override public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_CULL_FACE);
        GLES20.glCullFace(GLES20.GL_BACK);
        GLES20.glClearColor(SKY[0],SKY[1],SKY[2],SKY[3]);

        program=buildProgram(VERTEX_SHADER,FRAGMENT_SHADER);
        aPos=GLES20.glGetAttribLocation(program,"aPosition");
        aNormal=GLES20.glGetAttribLocation(program,"aNormal");
        aTex=GLES20.glGetAttribLocation(program,"aTexCoord");
        uMvp=GLES20.glGetUniformLocation(program,"uMVP");
        uModel=GLES20.glGetUniformLocation(program,"uModel");
        uColor=GLES20.glGetUniformLocation(program,"uColor");
        uLightDir=GLES20.glGetUniformLocation(program,"uLightDir");
        uFogColor=GLES20.glGetUniformLocation(program,"uFogColor");
        uFogNear=GLES20.glGetUniformLocation(program,"uFogNear");
        uFogFar=GLES20.glGetUniformLocation(program,"uFogFar");
        uTexture=GLES20.glGetUniformLocation(program,"uTexture");
        uUseTexture=GLES20.glGetUniformLocation(program,"uUseTexture");
        uTexScale=GLES20.glGetUniformLocation(program,"uTexScale");

        cube=Mesh.cube();
        disk=Mesh.disk(32);
        texAsphalt=createTexture(1,2048);
        texGrass=createTexture(2,2048);
        texBuilding=createTexture(3,2048);
        texGravel=createTexture(4,2048);
        lastNs=System.nanoTime();
    }

    @Override public void onSurfaceChanged(GL10 gl,int width,int height){
        GLES20.glViewport(0,0,width,height);
        Matrix.perspectiveM(projection,0,58f,width/(float)Math.max(1,height),0.10f,2200f);
    }

    @Override public void onDrawFrame(GL10 gl){
        long now=System.nanoTime();
        float dt=Math.min(0.033f,Math.max(0.001f,(now-lastNs)/1_000_000_000f));
        lastNs=now;
        if(!paused) update(dt);

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT|GLES20.GL_DEPTH_BUFFER_BIT);
        GLES20.glUseProgram(program);
        GLES20.glUniform3f(uLightDir,-0.32f,0.88f,0.30f);
        GLES20.glUniform4f(uFogColor,SKY[0],SKY[1],SKY[2],1f);
        GLES20.glUniform1f(uFogNear,280f);
        GLES20.glUniform1f(uFogFar,1150f);
        GLES20.glUniform1i(uTexture,0);

        setupCamera();

        drawBox(0,-0.72f,0,MAP_HALF+180,0.5f,MAP_HALF+180,0,1,1,1,1,MAT_GRASS,180,180);
        for(Road r:roads) drawRoad(r);
        drawRoadMarkings();
        drawBuildings();
        drawPlaces();

        if(!freeMode && !parking.isEmpty()) drawMission();
        drawCar();
        if(onFoot) drawPlayer();
    }

    private void setupCamera(){
        float x=onFoot?playerX:carX;
        float z=onFoot?playerZ:carZ;
        float yaw=onFoot?playerYaw:carYaw;
        float a=(float)Math.toRadians(yaw);
        float fx=(float)Math.sin(a),fz=(float)Math.cos(a);
        float back=onFoot?5.2f:13.5f;
        float camY=onFoot?3.3f:6.8f;
        float lookY=onFoot?0.95f:1.45f;
        Matrix.setLookAtM(view,0,x-fx*back,camY,z-fz*back,x+fx*(onFoot?3.5f:10f),lookY,z+fz*(onFoot?3.5f:10f),0,1,0);
        Matrix.multiplyMM(vp,0,projection,0,view,0);
    }

    private void update(float dt){
        if(requestNewMission){
            requestNewMission=false;
            freeMode=false;
            onFoot=false;
            missionSpotIndex=(missionSpotIndex+7)%Math.max(1,parking.size());
            parkedTime=0;
        }
        if(onFoot) updatePlayer(dt); else updateCar(dt);
    }

    private void updatePlayer(float dt){
        float turn=((right?1f:0f)-(left?1f:0f))*105f*dt;
        playerYaw+=turn;
        float move=gas?4.3f:(brake?-2.5f:0f);
        float a=(float)Math.toRadians(playerYaw);
        float nx=playerX+(float)Math.sin(a)*move*dt;
        float nz=playerZ+(float)Math.cos(a)*move*dt;
        if(!isBlocked(nx,nz,0.45f)){
            playerX=clamp(nx,-MAP_HALF+10,MAP_HALF-10);
            playerZ=clamp(nz,-MAP_HALF+10,MAP_HALF-10);
        }
        if(Math.abs(move)>0.1f) walkPhase+=Math.abs(move)*dt*4.8f;
    }

    private void updateCar(float dt){
        if(gas) speed+=14.0f*dt;
        if(brake){
            if(speed>0.7f) speed=Math.max(0,speed-28f*dt);
            else speed=Math.max(-8f,speed-5.8f*dt);
        }
        if(!gas&&!brake){
            float drag=isNearRoad(carX,carZ)?2.1f:7.8f;
            if(speed>0)speed=Math.max(0,speed-drag*dt); else if(speed<0)speed=Math.min(0,speed+drag*dt);
        }
        float carBonus=1f+selectedCar*0.0072f;
        float max=(isNearRoad(carX,carZ)?53f:19f)*carBonus;
        speed=clamp(speed,-10f,max);
        float steer=(right?1f:0f)-(left?1f:0f);
        if(Math.abs(speed)>0.4f) carYaw+=steer*(18f+Math.min(38f,Math.abs(speed)))*dt*(speed>=0?1:-1);
        if(carYaw>180)carYaw-=360; if(carYaw<-180)carYaw+=360;

        float a=(float)Math.toRadians(carYaw);
        float nx=carX+(float)Math.sin(a)*speed*dt;
        float nz=carZ+(float)Math.cos(a)*speed*dt;
        if(isBlocked(nx,nz,1.25f)){
            crash(Math.abs(speed)*3.6f);
            speed*=-0.16f;
        }else{
            carX=clamp(nx,-MAP_HALF+8,MAP_HALF-8);
            carZ=clamp(nz,-MAP_HALF+8,MAP_HALF-8);
        }

        if(!freeMode&&!parking.isEmpty()){
            float[] s=parking.get(missionSpotIndex%parking.size());
            float dx=carX-s[0],dz=carZ-s[1];
            float tolerance=Math.max(2.0f,5.2f-missionNumber*0.012f);
            if(dx*dx+dz*dz<tolerance*tolerance && Math.abs(speed)<1.35f){
                parkedTime+=dt;
                if(parkedTime>1.5f){
                    int reward=250+missionNumber*22;
                    money+=reward;
                    lastEvent="Миссия "+missionNumber+" пройдена +$"+reward;
                    if(missionNumber<250) missionNumber++;
                    missionSpotIndex=(missionSpotIndex+11+missionNumber)%parking.size();
                    parkedTime=0;
                    saveProgress();
                }
            }else parkedTime=0;
        }
    }

    private void crash(float impactKmh){
        float reduction=1f-strength*0.007f;
        int damage=Math.max(1,Math.round(Math.max(0,impactKmh-18f)*0.52f*reduction));
        health=Math.max(0,health-damage);
        lastEvent="Столкновение: -"+damage+" HP";
        if(health<=0){
            health=100;
            carX=0; carZ=-80; carYaw=0; speed=0;
            lastEvent="Персонаж не выдержал удар — восстановление в Купчино";
        }
        saveProgress();
    }

    private boolean isBlocked(float x,float z,float radius){
        for(Building b:buildings){
            if(Math.abs(x-b.x)<b.w*0.5f+radius && Math.abs(z-b.z)<b.d*0.5f+radius) return true;
        }
        return false;
    }

    private void drawBuildings(){
        float focusX=onFoot?playerX:carX,focusZ=onFoot?playerZ:carZ;
        float radius2=(onFoot?500f:650f); radius2*=radius2;
        for(Building b:buildings){
            float dx=b.x-focusX,dz=b.z-focusZ;
            if(dx*dx+dz*dz>radius2)continue;
            drawBox(b.x,b.h*0.5f,b.z,b.w*0.5f,b.h*0.5f,b.d*0.5f,b.yaw,b.r,b.g,b.b,1,MAT_BUILDING,
                    Math.max(1,b.w/5.5f),Math.max(1,b.h/4f));
            drawBox(b.x,0.03f,b.z,b.w*0.58f,0.02f,b.d*0.58f,b.yaw,0.05f,0.06f,0.05f,0.30f,MAT_SOLID,1,1);
        }
    }

    private void drawPlaces(){
        for(Place p:places){
            float fx=onFoot?playerX:carX,fz=onFoot?playerZ:carZ;
            float dx=p.x-fx,dz=p.z-fz;
            if(dx*dx+dz*dz>520f*520f)continue;
            drawDisk(p.x,0.08f,p.z,2.6f,0.12f,1.0f,0.25f,0.88f);
            drawBox(p.x,1.25f,p.z,0.09f,1.25f,0.09f,0,0.10f,1f,0.30f,0.92f,MAT_SOLID,1,1);
        }
    }

    private void drawMission(){
        float[] s=parking.get(missionSpotIndex%parking.size());
        drawBox(s[0],0.075f,s[1],2.8f,0.055f,4.7f,s[2],0.08f,0.95f,0.25f,1,MAT_SOLID,1,1);
        drawBox(s[0],1.25f,s[1],0.08f,1.25f,0.08f,0,0.10f,1f,0.28f,1,MAT_SOLID,1,1);
        if(missionNumber==65){
            drawParkedMissionCar(s[0],s[1],s[2],7.0f,0.16f,0.25f,0.75f);
            drawParkedMissionCar(s[0],s[1],s[2],-7.0f,0.72f,0.18f,0.12f);
        }
    }

    private void drawParkedMissionCar(float x,float z,float yaw,float forward,float r,float g,float b){
        double a=Math.toRadians(yaw);
        float px=x+(float)Math.sin(a)*forward,pz=z+(float)Math.cos(a)*forward;
        drawBox(px,0.50f,pz,1.12f,0.40f,2.25f,yaw,r,g,b,1,MAT_SOLID,1,1);
        drawBox(px,1.10f,pz,0.82f,0.28f,0.92f,yaw,0.14f,0.22f,0.28f,1,MAT_SOLID,1,1);
    }

    private void drawCar(){
        int color=carColor(selectedCar);
        float rr=Color.red(color)/255f,gg=Color.green(color)/255f,bb=Color.blue(color)/255f;
        drawBox(carX,0.07f,carZ,1.42f,0.045f,2.72f,carYaw,0.03f,0.04f,0.05f,0.52f,MAT_SOLID,1,1);
        drawBox(carX,0.56f,carZ,1.18f,0.42f,2.28f,carYaw,rr,gg,bb,1,MAT_SOLID,1,1);
        drawBox(localX(carX,carZ,carYaw,0,0.08f),1.16f,localZ(carX,carZ,carYaw,0,0.08f),0.86f,0.33f,0.94f,carYaw,0.10f,0.22f,0.32f,1,MAT_SOLID,1,1);
        drawWheel(-1.20f,1.48f); drawWheel(1.20f,1.48f); drawWheel(-1.20f,-1.48f); drawWheel(1.20f,-1.48f);
    }

    private void drawPlayer(){
        float swing=(float)Math.sin(walkPhase)*18f;
        drawBox(playerX,0.80f,playerZ,0.25f,0.40f,0.16f,playerYaw,0.15f,0.35f,0.82f,1,MAT_SOLID,1,1);
        drawBox(playerX,1.30f,playerZ,0.20f,0.20f,0.20f,playerYaw,0.90f,0.72f,0.57f,1,MAT_SOLID,1,1);
        drawLimb(-0.13f,0.36f,0.0f,swing);
        drawLimb(0.13f,0.36f,0.0f,-swing);
        drawArm(-0.33f,0.82f,0.0f,-swing);
        drawArm(0.33f,0.82f,0.0f,swing);
    }

    private void drawLimb(float side,float y,float forward,float swing){
        float x=localX(playerX,playerZ,playerYaw,side,forward),z=localZ(playerX,playerZ,playerYaw,side,forward);
        drawBox(x,y,z,0.10f,0.34f,0.11f,playerYaw+swing*0.16f,0.10f,0.12f,0.16f,1,MAT_SOLID,1,1);
    }
    private void drawArm(float side,float y,float forward,float swing){
        float x=localX(playerX,playerZ,playerYaw,side,forward),z=localZ(playerX,playerZ,playerYaw,side,forward);
        drawBox(x,y,z,0.08f,0.30f,0.09f,playerYaw+swing*0.20f,0.86f,0.67f,0.54f,1,MAT_SOLID,1,1);
    }

    private void drawWheel(float side,float forward){
        drawBox(localX(carX,carZ,carYaw,side,forward),0.40f,localZ(carX,carZ,carYaw,side,forward),0.22f,0.35f,0.48f,carYaw,0.02f,0.02f,0.025f,1,MAT_SOLID,1,1);
    }

    private float localX(float ox,float oz,float yaw,float side,float forward){
        float a=(float)Math.toRadians(yaw);
        return ox+(float)Math.cos(a)*side+(float)Math.sin(a)*forward;
    }
    private float localZ(float ox,float oz,float yaw,float side,float forward){
        float a=(float)Math.toRadians(yaw);
        return oz-(float)Math.sin(a)*side+(float)Math.cos(a)*forward;
    }

    private int carColor(int index){
        float hue=(index*37f)%360f;
        return Color.HSVToColor(new float[]{hue,0.72f,0.86f});
    }

    private void drawRoad(Road r){
        float dx=r.x2-r.x1,dz=r.z2-r.z1;
        float len=(float)Math.sqrt(dx*dx+dz*dz);
        float x=(r.x1+r.x2)*0.5f,z=(r.z1+r.z2)*0.5f;
        float yaw=(float)Math.toDegrees(Math.atan2(dx,dz));
        drawBox(x,-0.005f,z,r.width*0.64f,0.035f,len*0.5f+1f,yaw,1,1,1,1,MAT_GRAVEL,Math.max(1,r.width/5),Math.max(2,len/10));
        drawBox(x,0.025f,z,r.width*0.50f,0.035f,len*0.5f,yaw,1,1,1,1,MAT_ASPHALT,Math.max(1,r.width/6),Math.max(2,len/12));
    }

    private void drawRoadMarkings(){
        float focusX=onFoot?playerX:carX,focusZ=onFoot?playerZ:carZ;
        for(Road r:roads){
            if(r.width<8.0f)continue;
            float mx=(r.x1+r.x2)*0.5f,mz=(r.z1+r.z2)*0.5f;
            float ddx=mx-focusX,ddz=mz-focusZ;
            if(ddx*ddx+ddz*ddz>850f*850f)continue;
            float dx=r.x2-r.x1,dz=r.z2-r.z1;
            float len=(float)Math.sqrt(dx*dx+dz*dz);
            int count=Math.max(1,(int)(len/18f));
            float yaw=(float)Math.toDegrees(Math.atan2(dx,dz));
            for(int i=0;i<count;i+=2){
                float t=(i+0.5f)/count;
                drawBox(r.x1+dx*t,0.078f,r.z1+dz*t,0.10f,0.018f,5.5f,yaw,0.96f,0.95f,0.86f,1,MAT_SOLID,1,1);
            }
        }
    }

    private boolean isNearRoad(float x,float z){
        for(Road r:roads){
            float vx=r.x2-r.x1,vz=r.z2-r.z1,wx=x-r.x1,wz=z-r.z1;
            float vv=vx*vx+vz*vz;
            float t=vv==0?0:(wx*vx+wz*vz)/vv;
            t=clamp(t,0,1);
            float px=r.x1+vx*t,pz=r.z1+vz*t,dx=x-px,dz=z-pz;
            float limit=r.width*0.65f+2.4f;
            if(dx*dx+dz*dz<limit*limit)return true;
        }
        return false;
    }

    private Place nearestPlace(){
        if(places.isEmpty())return null;
        Place best=null; float bestD=Float.MAX_VALUE;
        for(Place p:places){
            float dx=playerX-p.x,dz=playerZ-p.z,d=dx*dx+dz*dz;
            if(d<bestD){bestD=d;best=p;}
        }
        return best;
    }

    private void drawBox(float x,float y,float z,float sx,float sy,float sz,float yaw,float r,float g,float b,float alpha,int material,float tx,float ty){
        Matrix.setIdentityM(model,0); Matrix.translateM(model,0,x,y,z); Matrix.rotateM(model,0,yaw,0,1,0); Matrix.scaleM(model,0,sx,sy,sz);
        Matrix.multiplyMM(mvp,0,vp,0,model,0);
        GLES20.glUniformMatrix4fv(uMvp,1,false,mvp,0); GLES20.glUniformMatrix4fv(uModel,1,false,model,0);
        GLES20.glUniform4f(uColor,r,g,b,alpha); GLES20.glUniform2f(uTexScale,tx,ty);
        bindMaterial(material); cube.draw(aPos,aNormal,aTex);
    }

    private void drawDisk(float x,float y,float z,float radius,float r,float g,float b,float alpha){
        Matrix.setIdentityM(model,0); Matrix.translateM(model,0,x,y,z); Matrix.scaleM(model,0,radius,1,radius);
        Matrix.multiplyMM(mvp,0,vp,0,model,0);
        GLES20.glUniformMatrix4fv(uMvp,1,false,mvp,0); GLES20.glUniformMatrix4fv(uModel,1,false,model,0);
        GLES20.glUniform4f(uColor,r,g,b,alpha); GLES20.glUniform2f(uTexScale,1,1); bindMaterial(MAT_SOLID);
        disk.draw(aPos,aNormal,aTex);
    }

    private void bindMaterial(int material){
        if(material==MAT_SOLID){ GLES20.glUniform1f(uUseTexture,0f); return; }
        GLES20.glUniform1f(uUseTexture,1f); GLES20.glActiveTexture(GLES20.GL_TEXTURE0); GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,textureFor(material));
    }
    private int textureFor(int material){ if(material==MAT_ASPHALT)return texAsphalt; if(material==MAT_GRASS)return texGrass; if(material==MAT_BUILDING)return texBuilding; return texGravel; }

    private int createTexture(int kind,int size){
        Bitmap bm=Bitmap.createBitmap(size,size,Bitmap.Config.ARGB_8888); Canvas c=new Canvas(bm); Paint p=new Paint(Paint.ANTI_ALIAS_FLAG); Random rnd=new Random(99000+kind);
        if(kind==1){
            c.drawColor(Color.rgb(48,50,52));
            for(int i=0;i<14000;i++){int v=36+rnd.nextInt(45);p.setColor(Color.rgb(v,v,v));float x=rnd.nextInt(size),y=rnd.nextInt(size),s=1+rnd.nextInt(4);c.drawRect(x,y,x+s,y+s,p);}
            p.setStrokeWidth(2); p.setColor(Color.rgb(28,29,30)); for(int i=0;i<180;i++){float x=rnd.nextInt(size),y=rnd.nextInt(size);c.drawLine(x,y,x+rnd.nextInt(140)-70,y+rnd.nextInt(140)-70,p);}
        }else if(kind==2){
            c.drawColor(Color.rgb(57,103,45));
            for(int i=0;i<12000;i++){int gg=78+rnd.nextInt(72),rr=33+rnd.nextInt(34),bb=24+rnd.nextInt(28);p.setColor(Color.rgb(rr,gg,bb));float x=rnd.nextInt(size),y=rnd.nextInt(size);c.drawCircle(x,y,1+rnd.nextInt(5),p);}
        }else if(kind==3){
            c.drawColor(Color.rgb(181,172,160)); p.setStrokeWidth(5); p.setColor(Color.rgb(145,138,129));
            for(int y=0;y<size;y+=120)c.drawLine(0,y,size,y,p); for(int x=0;x<size;x+=240)c.drawLine(x,0,x,size,p);
            for(int y=22;y<size;y+=120)for(int x=24;x<size;x+=120){p.setColor(rnd.nextInt(100)<20?Color.rgb(245,214,132):Color.rgb(61,96,124));c.drawRect(x,y,x+58,y+70,p);p.setColor(Color.rgb(35,45,52));c.drawRect(x+27,y,x+31,y+70,p);}
        }else{
            c.drawColor(Color.rgb(124,116,103)); for(int i=0;i<12000;i++){int v=90+rnd.nextInt(68);p.setColor(Color.rgb(v,v-5,v-12));float x=rnd.nextInt(size),y=rnd.nextInt(size);c.drawCircle(x,y,2+rnd.nextInt(6),p);}
        }
        int[] ids=new int[1]; GLES20.glGenTextures(1,ids,0); GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,ids[0]);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_MIN_FILTER,GLES20.GL_LINEAR_MIPMAP_LINEAR); GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_MAG_FILTER,GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_WRAP_S,GLES20.GL_REPEAT); GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_WRAP_T,GLES20.GL_REPEAT);
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D,0,bm,0); GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D); bm.recycle(); return ids[0];
    }

    private void buildWorldData(){
        addRoad(0,-2500,0,2500,12); addRoad(-2500,0,2500,0,12);
        addRoad(-2200,-900,2200,-900,10); addRoad(-2200,900,2200,900,10);
        addRoad(-900,-2200,-900,2200,9); addRoad(900,-2200,900,2200,9);
        addRing(0,0,700,11); addRing(0,0,1500,10);
        addRoad(-1500,-1500,-2450,-2350,9); addRoad(1500,-1500,2450,-2350,9); addRoad(-1500,1500,-2450,2350,9); addRoad(1500,1500,2450,2350,9);

        int[] centers={-1500,0,1500};
        for(int cx:centers) for(int cz:centers){
            for(int i=-3;i<=3;i++){
                float o=i*150f;
                addRoad(cx-520,cz+o,cx+520,cz+o,7.0f);
                addRoad(cx+o,cz-520,cx+o,cz+520,7.0f);
            }
        }
        addRoundabout(420,420,65,20,8); addRoundabout(-1180,1180,58,20,7.5f); addRoundabout(1510,-920,62,20,8);

        for(int x=-2250;x<=2250;x+=300){
            parking.add(new float[]{x,-900,90}); parking.add(new float[]{x,900,90});
        }
        for(int z=-2250;z<=2250;z+=300){
            parking.add(new float[]{-900,z,0}); parking.add(new float[]{900,z,0});
        }

        Random rnd=new Random(20260911);
        for(int gx=-16;gx<=16;gx++) for(int gz=-16;gz<=16;gz++){
            float x=gx*150f+rnd.nextFloat()*36f-18f,z=gz*150f+rnd.nextFloat()*36f-18f;
            if(isNearRoad(x,z))continue;
            float w=24+rnd.nextFloat()*34,d=24+rnd.nextFloat()*34,h=7+rnd.nextFloat()*52;
            float base=0.72f+rnd.nextFloat()*0.20f;
            buildings.add(new Building(x,z,w,d,h,(rnd.nextInt(3)-1)*4f,base,base*0.98f,base*0.94f));
        }

        addPlace(-120,-155,"Шестёрочка 24/7",3,44,36,10);
        addPlace(1540,120,"Офис Мурино",4,48,38,16);
        addPlace(180,-1280,"Качалка Купчино",1,52,42,12);
        for(int i=1;i<=20;i++){
            double a=i*Math.PI*2/20.0;
            float radius=1050+(i%3)*260;
            float x=(float)Math.cos(a)*radius, z=(float)Math.sin(a)*radius;
            addPlace(x,z,"Поликлиника №"+i,2,52,42,14+(i%5)*2);
        }
    }

    private void addPlace(float x,float z,String name,int type,float w,float d,float h){
        places.add(new Place(x,z,name,type));
        buildings.add(new Building(x,z-22f,w,d,h,0,0.86f,0.88f,0.90f));
    }
    private void addRing(float cx,float cz,float r,float width){
        int n=28; for(int i=0;i<n;i++){double a=i*Math.PI*2/n,b=(i+1)*Math.PI*2/n;addRoad(cx+(float)Math.cos(a)*r,cz+(float)Math.sin(a)*r,cx+(float)Math.cos(b)*r,cz+(float)Math.sin(b)*r,width);}
    }
    private void addRoundabout(float cx,float cz,float r,int n,float width){
        for(int i=0;i<n;i++){double a=i*Math.PI*2/n,b=(i+1)*Math.PI*2/n;addRoad(cx+(float)Math.cos(a)*r,cz+(float)Math.sin(a)*r,cx+(float)Math.cos(b)*r,cz+(float)Math.sin(b)*r,width);}
    }
    private void addRoad(float x1,float z1,float x2,float z2,float width){roads.add(new Road(x1,z1,x2,z2,width));}
    private static float clamp(float v,float a,float b){return Math.max(a,Math.min(b,v));}

    private static int buildProgram(String vs,String fs){
        int v=compile(GLES20.GL_VERTEX_SHADER,vs),f=compile(GLES20.GL_FRAGMENT_SHADER,fs),p=GLES20.glCreateProgram();
        GLES20.glAttachShader(p,v); GLES20.glAttachShader(p,f); GLES20.glLinkProgram(p); int[] ok=new int[1]; GLES20.glGetProgramiv(p,GLES20.GL_LINK_STATUS,ok,0);
        if(ok[0]==0)throw new RuntimeException("GL link: "+GLES20.glGetProgramInfoLog(p)); GLES20.glDeleteShader(v); GLES20.glDeleteShader(f); return p;
    }
    private static int compile(int type,String src){int s=GLES20.glCreateShader(type);GLES20.glShaderSource(s,src);GLES20.glCompileShader(s);int[] ok=new int[1];GLES20.glGetShaderiv(s,GLES20.GL_COMPILE_STATUS,ok,0);if(ok[0]==0)throw new RuntimeException("GL shader: "+GLES20.glGetShaderInfoLog(s));return s;}

    private static final String VERTEX_SHADER="uniform mat4 uMVP;\nuniform mat4 uModel;\nuniform vec2 uTexScale;\nattribute vec3 aPosition;\nattribute vec3 aNormal;\nattribute vec2 aTexCoord;\nvarying vec3 vNormal;\nvarying float vDepth;\nvarying vec2 vUV;\nvoid main(){gl_Position=uMVP*vec4(aPosition,1.0);vNormal=normalize(mat3(uModel)*aNormal);vDepth=gl_Position.w;vUV=aTexCoord*uTexScale;}";
    private static final String FRAGMENT_SHADER="precision mediump float;\nuniform vec4 uColor;\nuniform vec3 uLightDir;\nuniform vec4 uFogColor;\nuniform float uFogNear;\nuniform float uFogFar;\nuniform sampler2D uTexture;\nuniform float uUseTexture;\nvarying vec3 vNormal;\nvarying float vDepth;\nvarying vec2 vUV;\nvoid main(){vec4 tex=texture2D(uTexture,vUV);vec4 base=mix(uColor,vec4(tex.rgb*uColor.rgb,uColor.a),uUseTexture);float diff=max(dot(normalize(vNormal),normalize(uLightDir)),0.0);float light=0.34+0.66*diff;vec4 lit=vec4(base.rgb*light,base.a);float fog=clamp((vDepth-uFogNear)/(uFogFar-uFogNear),0.0,1.0);gl_FragColor=mix(lit,uFogColor,fog);}";

    private static class Road{final float x1,z1,x2,z2,width;Road(float x1,float z1,float x2,float z2,float width){this.x1=x1;this.z1=z1;this.x2=x2;this.z2=z2;this.width=width;}}
    private static class Building{final float x,z,w,d,h,yaw,r,g,b;Building(float x,float z,float w,float d,float h,float yaw,float r,float g,float b){this.x=x;this.z=z;this.w=w;this.d=d;this.h=h;this.yaw=yaw;this.r=r;this.g=g;this.b=b;}}
    private static class Place{final float x,z;final String name;final int type;Place(float x,float z,String name,int type){this.x=x;this.z=z;this.name=name;this.type=type;}}

    private static class Mesh{
        final FloatBuffer vertices; final int count;
        Mesh(float[] data){ByteBuffer bb=ByteBuffer.allocateDirect(data.length*4).order(ByteOrder.nativeOrder());vertices=bb.asFloatBuffer();vertices.put(data).position(0);count=data.length/8;}
        void draw(int pos,int normal,int tex){vertices.position(0);GLES20.glEnableVertexAttribArray(pos);GLES20.glVertexAttribPointer(pos,3,GLES20.GL_FLOAT,false,32,vertices);vertices.position(3);GLES20.glEnableVertexAttribArray(normal);GLES20.glVertexAttribPointer(normal,3,GLES20.GL_FLOAT,false,32,vertices);vertices.position(6);GLES20.glEnableVertexAttribArray(tex);GLES20.glVertexAttribPointer(tex,2,GLES20.GL_FLOAT,false,32,vertices);GLES20.glDrawArrays(GLES20.GL_TRIANGLES,0,count);GLES20.glDisableVertexAttribArray(pos);GLES20.glDisableVertexAttribArray(normal);GLES20.glDisableVertexAttribArray(tex);}
        static Mesh cube(){
            float[] p={
                    -1,-1,1,0,0,1,0,0, 1,-1,1,0,0,1,1,0, 1,1,1,0,0,1,1,1, -1,-1,1,0,0,1,0,0, 1,1,1,0,0,1,1,1, -1,1,1,0,0,1,0,1,
                    1,-1,-1,0,0,-1,0,0, -1,-1,-1,0,0,-1,1,0, -1,1,-1,0,0,-1,1,1, 1,-1,-1,0,0,-1,0,0, -1,1,-1,0,0,-1,1,1, 1,1,-1,0,0,-1,0,1,
                    -1,-1,-1,-1,0,0,0,0, -1,-1,1,-1,0,0,1,0, -1,1,1,-1,0,0,1,1, -1,-1,-1,-1,0,0,0,0, -1,1,1,-1,0,0,1,1, -1,1,-1,-1,0,0,0,1,
                    1,-1,1,1,0,0,0,0, 1,-1,-1,1,0,0,1,0, 1,1,-1,1,0,0,1,1, 1,-1,1,1,0,0,0,0, 1,1,-1,1,0,0,1,1, 1,1,1,1,0,0,0,1,
                    -1,1,1,0,1,0,0,0, 1,1,1,0,1,0,1,0, 1,1,-1,0,1,0,1,1, -1,1,1,0,1,0,0,0, 1,1,-1,0,1,0,1,1, -1,1,-1,0,1,0,0,1,
                    -1,-1,-1,0,-1,0,0,0, 1,-1,-1,0,-1,0,1,0, 1,-1,1,0,-1,0,1,1, -1,-1,-1,0,-1,0,0,0, 1,-1,1,0,-1,0,1,1, -1,-1,1,0,-1,0,0,1
            }; return new Mesh(p);
        }
        static Mesh disk(int segments){
            float[] data=new float[segments*3*8]; int k=0;
            for(int i=0;i<segments;i++){
                double a=i*Math.PI*2/segments,b=(i+1)*Math.PI*2/segments;
                float[][] pts={{0,0,0,0,1,0,.5f,.5f},{(float)Math.cos(a),0,(float)Math.sin(a),0,1,0,(float)(.5+.5*Math.cos(a)),(float)(.5+.5*Math.sin(a))},{(float)Math.cos(b),0,(float)Math.sin(b),0,1,0,(float)(.5+.5*Math.cos(b)),(float)(.5+.5*Math.sin(b))}};
                for(float[] q:pts)for(float v:q)data[k++]=v;
            }
            return new Mesh(data);
        }
    }
}
