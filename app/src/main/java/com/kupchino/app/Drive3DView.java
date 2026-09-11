package com.kupchino.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.MotionEvent;
import android.view.View;
import java.util.Random;

public class Drive3DView extends View {
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Random rng = new Random(777);
    private long lastNs;
    private float speed = 0f;
    private float playerX = 0f;
    private float distance = 0f;
    private float missionDistance = 2200f;
    private float missionX = 0.55f;
    private int money = 0;
    private boolean freeMode = false;
    private boolean left, right, gas, brake;
    private float parkedFor = 0f;

    public Drive3DView(Context context) {
        super(context);
        setFocusable(true);
        p.setTypeface(android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD));
    }

    private float clamp(float v,float a,float b){ return Math.max(a,Math.min(b,v)); }

    @Override protected void onDraw(Canvas c){
        super.onDraw(c);
        long now=System.nanoTime();
        float dt=lastNs==0?0.016f:Math.min(0.04f,(now-lastNs)/1_000_000_000f);
        lastNs=now;
        update(dt);
        drawWorld(c);
        postInvalidateOnAnimation();
    }

    private void update(float dt){
        if(gas) speed += 42f*dt;
        if(brake) speed -= 58f*dt;
        if(!gas && !brake){
            if(speed>0) speed=Math.max(0,speed-18f*dt);
            if(speed<0) speed=Math.min(0,speed+18f*dt);
        }
        speed=clamp(speed,-18f,95f);
        float steer=(right?1f:0f)-(left?1f:0f);
        playerX += steer*(0.75f+Math.abs(speed)/55f)*dt;
        playerX=clamp(playerX,-1.15f,1.15f);
        distance += speed*dt*7f;
        if(distance<0){ distance=0; speed=0; }

        if(!freeMode){
            float dz=missionDistance-distance;
            boolean close=Math.abs(dz)<85f && Math.abs(playerX-missionX)<0.33f && Math.abs(speed)<9f;
            if(close){
                parkedFor+=dt;
                if(parkedFor>1.25f){
                    money+=500;
                    newMission();
                }
            }else parkedFor=0f;
        }
    }

    private void drawWorld(Canvas c){
        int w=getWidth(), h=getHeight();
        if(w<=0||h<=0) return;
        float horizon=h*0.31f;

        p.setShader(new LinearGradient(0,0,0,horizon,0xFF4CA5FF,0xFFD8F1FF,Shader.TileMode.CLAMP));
        c.drawRect(0,0,w,horizon,p); p.setShader(null);
        p.setColor(0xFF5D8F43); c.drawRect(0,horizon,w,h,p);

        drawCity(c,w,h,horizon);
        drawRoad(c,w,h,horizon);
        drawParkingTarget(c,w,h,horizon);
        drawCar(c,w,h);
        drawHud(c,w,h);
        drawControls(c,w,h);
    }

    private void drawRoad(Canvas c,int w,int h,float horizon){
        float center=w*0.5f-playerX*w*0.11f;
        float topHalf=w*0.055f, bottomHalf=w*0.48f;
        Path road=new Path();
        road.moveTo(center-topHalf,horizon);
        road.lineTo(center+topHalf,horizon);
        road.lineTo(center+bottomHalf,h);
        road.lineTo(center-bottomHalf,h);
        road.close();
        p.setColor(0xFF35393D); c.drawPath(road,p);

        p.setColor(0xFFFFFFFF);
        for(int i=0;i<16;i++){
            float phase=((distance*0.045f+i*0.085f)%1f);
            float z=phase;
            float y=horizon+(h-horizon)*z*z;
            float y2=horizon+(h-horizon)*Math.min(1f,z+0.026f)*Math.min(1f,z+0.026f);
            float half1=topHalf+(bottomHalf-topHalf)*z*z;
            float half2=topHalf+(bottomHalf-topHalf)*Math.min(1f,z+0.026f)*Math.min(1f,z+0.026f);
            float lane=center;
            Path dash=new Path();
            dash.moveTo(lane-half1*0.012f,y); dash.lineTo(lane+half1*0.012f,y);
            dash.lineTo(lane+half2*0.014f,y2); dash.lineTo(lane-half2*0.014f,y2); dash.close();
            c.drawPath(dash,p);
        }
        p.setColor(0xFFFFD34D);
        p.setStrokeWidth(5f);
        c.drawLine(center-bottomHalf,h,center-topHalf,horizon,p);
    }

    private void drawCity(Canvas c,int w,int h,float horizon){
        int base=(int)(distance/180f);
        for(int i=1;i<18;i++){
            int idx=base+i;
            float z=((idx*180f-distance)%3240f)/3240f;
            if(z<0) z+=1f;
            float depth=1f-z;
            float y=horizon+(h-horizon)*depth*depth;
            float scale=0.18f+1.5f*depth*depth;
            float bw=55f*scale;
            float bh=(80f+(idx%6)*32f)*scale;
            int col=Color.rgb(65+(idx*37)%90,70+(idx*23)%90,80+(idx*19)%90);
            p.setColor(col);
            float side=(idx%2==0)?-1f:1f;
            float x=w*0.5f+side*(w*0.18f+w*0.38f*depth)-bw/2f;
            c.drawRect(x,y-bh,x+bw,y,p);
            p.setColor(0xFFFFE88A);
            float win=Math.max(2f,4f*scale);
            for(int r=1;r<4;r++) for(int k=1;k<3;k++)
                c.drawRect(x+k*bw/3f-win/2f,y-r*bh/5f-win/2f,x+k*bw/3f+win/2f,y-r*bh/5f+win/2f,p);
        }
    }

    private void drawParkingTarget(Canvas c,int w,int h,float horizon){
        if(freeMode) return;
        float dz=missionDistance-distance;
        if(dz<-100 || dz>2500) return;
        float depth=1f-clamp(dz/2500f,0f,1f);
        float y=horizon+(h-horizon)*depth*depth;
        float roadHalf=w*(0.055f+(0.48f-0.055f)*depth*depth);
        float center=w*0.5f-playerX*w*0.11f;
        float x=center+missionX*roadHalf*0.63f;
        float s=18f+58f*depth;
        p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(Math.max(3f,s*0.09f)); p.setColor(0xFF54FF72);
        c.drawRect(x-s,y-s*0.45f,x+s,y+s*0.45f,p);
        p.setStyle(Paint.Style.FILL);
        p.setColor(0xAA54FF72); c.drawCircle(x,y-s*0.85f,s*0.22f,p);
    }

    private void drawCar(Canvas c,int w,int h){
        float cx=w*0.5f, cy=h*0.77f;
        float steer=((right?1f:0f)-(left?1f:0f))*7f;
        c.save(); c.rotate(steer,cx,cy);
        p.setColor(0xFFE53935); c.drawRoundRect(new RectF(cx-w*0.10f,cy-h*0.045f,cx+w*0.10f,cy+h*0.05f),18,18,p);
        p.setColor(0xFF90CAF9); c.drawRoundRect(new RectF(cx-w*0.058f,cy-h*0.032f,cx+w*0.058f,cy+h*0.005f),10,10,p);
        p.setColor(Color.BLACK);
        c.drawRect(cx-w*0.115f,cy-h*0.02f,cx-w*0.09f,cy+h*0.035f,p);
        c.drawRect(cx+w*0.09f,cy-h*0.02f,cx+w*0.115f,cy+h*0.035f,p);
        c.restore();
    }

    private void drawHud(Canvas c,int w,int h){
        p.setTextSize(Math.max(28f,w*0.055f)); p.setColor(Color.WHITE);
        c.drawText("KUPCHINO DRIVE 3D",18,42,p);
        p.setTextSize(Math.max(22f,w*0.043f));
        c.drawText("$"+money+"   "+Math.round(Math.abs(speed))+" km/h",18,76,p);
        p.setColor(freeMode?0xFF65FF86:0xFFFFE45C);
        c.drawText(freeMode?"FREE DRIVE":"MISSION: PARK",18,108,p);
        if(!freeMode){
            p.setColor(Color.WHITE);
            c.drawText("До парковки: "+Math.max(0,Math.round((missionDistance-distance)/10f))+" m",18,138,p);
        }

        float ms=w*0.24f, mx=w-ms-18, my=18;
        p.setColor(0xAA101820); c.drawRoundRect(new RectF(mx,my,mx+ms,my+ms),18,18,p);
        p.setColor(0xFF4FD1FF); c.drawCircle(mx+ms*0.5f,my+ms*0.74f,7,p);
        if(!freeMode){
            float rel=clamp((missionDistance-distance)/2500f,0f,1f);
            p.setColor(0xFFFFE45C); c.drawCircle(mx+ms*(0.5f+missionX*0.28f),my+ms*(0.70f-rel*0.55f),8,p);
        }
        p.setColor(Color.WHITE); p.setTextSize(18f); c.drawText("MAP",mx+8,my+22,p);
    }

    private void drawControls(Canvas c,int w,int h){
        float top=h*0.84f;
        p.setColor(0x99242A31);
        c.drawRoundRect(new RectF(12,top,w*0.45f,h-12),26,26,p);
        c.drawRoundRect(new RectF(w*0.55f,top,w-12,h-12),26,26,p);
        p.setColor(Color.WHITE); p.setTextSize(Math.max(26f,w*0.06f));
        c.drawText("◀       ▶",w*0.07f,h*0.94f,p);
        c.drawText("BRAKE   GAS",w*0.58f,h*0.94f,p);

        p.setTextSize(18f); p.setColor(0xDDFFFFFF);
        c.drawRoundRect(new RectF(16,h*0.16f,145,h*0.215f),18,18,p);
        c.drawRoundRect(new RectF(155,h*0.16f,300,h*0.215f),18,18,p);
        p.setColor(Color.BLACK);
        c.drawText(freeMode?"MISSIONS":"FREE",35,h*0.196f,p);
        c.drawText("NEW MISSION",168,h*0.196f,p);
    }

    private void newMission(){
        missionDistance=distance+1600f+rng.nextInt(2600);
        missionX=(rng.nextBoolean()?1f:-1f)*(0.35f+rng.nextFloat()*0.38f);
        parkedFor=0f;
    }

    @Override public boolean onTouchEvent(MotionEvent e){
        float x=e.getX(), y=e.getY();
        int w=getWidth(), h=getHeight();
        if(e.getAction()==MotionEvent.ACTION_DOWN){
            if(y>h*0.145f && y<h*0.235f){
                if(x<150){ freeMode=!freeMode; parkedFor=0; invalidate(); return true; }
                if(x<315){ freeMode=false; newMission(); invalidate(); return true; }
            }
            setControl(x,y,w,h,true);
            return true;
        }
        if(e.getAction()==MotionEvent.ACTION_MOVE){
            left=right=gas=brake=false;
            setControl(x,y,w,h,true);
            return true;
        }
        if(e.getAction()==MotionEvent.ACTION_UP || e.getAction()==MotionEvent.ACTION_CANCEL){
            left=right=gas=brake=false;
            return true;
        }
        return true;
    }

    private void setControl(float x,float y,int w,int h,boolean down){
        if(y<h*0.80f) return;
        if(x<w*0.225f) left=down;
        else if(x<w*0.50f) right=down;
        else if(x<w*0.76f) brake=down;
        else gas=down;
    }
}
