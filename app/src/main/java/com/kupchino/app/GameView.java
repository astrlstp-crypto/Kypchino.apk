package com.kupchino.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;
import java.util.ArrayList;
import java.util.Random;

public class GameView extends View {
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Random rnd = new Random();
    private final ArrayList<Pipe> pipes = new ArrayList<>();
    private final SharedPreferences prefs;
    private float birdX, birdY, vy;\n    private float birdAnimTime = 0f;
    private float spawnTimer = 0;
    private long lastTime;
    private boolean running = false;
    private boolean gameOver = false;
    private boolean paused = false;
    private int score = 0;
    private int best;
    private String activeSlot = "A";
    private int birdColor = Color.rgb(255,215,40);
    private int beakColor = Color.rgb(255,120,20);
    private int hat = 0;
    private final int[] pipeColors = {
        Color.rgb(255,70,90), Color.rgb(255,150,40), Color.rgb(255,220,40),
        Color.rgb(70,210,120), Color.rgb(70,170,255), Color.rgb(130,90,255), Color.rgb(235,80,220)
    };

    public GameView(Context c) {
        super(c);
        setBackgroundColor(Color.rgb(120,205,245));
        prefs = c.getSharedPreferences("flappy", Context.MODE_PRIVATE);
        activeSlot = prefs.getString("active_slot", "A");
        best = prefs.getInt("best_" + activeSlot, 0);
        setFocusable(true);
    }

    public void setBirdColor(int c){ birdColor = c; invalidate(); }
    public void setBeakColor(int c){ beakColor = c; invalidate(); }
    public void setHat(int h){ hat = h; invalidate(); }
    public void pauseGame(){ if(running && !gameOver){ paused = true; running = false; invalidate(); } }
    public void resumeGame(){ if(paused && !gameOver){ paused = false; running = true; lastTime = System.nanoTime(); invalidate(); } }
    public void restartGame(){ reset(); running = true; paused = false; vy = -dp(330); lastTime = System.nanoTime(); invalidate(); }
    public boolean isPaused(){ return paused; }
    public int getScore(){ return score; }
    public int getBest(){ return best; }
    public String getActiveSlot(){ return activeSlot; }
    public int getSavedBest(String slot){ return prefs.getInt("best_" + slot, 0); }
    public void saveToSlot(String slot){
        int old = prefs.getInt("best_" + slot, 0);
        int value = Math.max(old, Math.max(best, score));
        prefs.edit().putInt("best_" + slot, value).putString("active_slot", slot).apply();
        activeSlot = slot;
        best = value;
        invalidate();
    }
    public void loadSlot(String slot){
        activeSlot = slot;
        best = prefs.getInt("best_" + slot, 0);
        prefs.edit().putString("active_slot", slot).apply();
        invalidate();
    }

    private float dp(float v){ return v * getResources().getDisplayMetrics().density; }

    private void reset(){
        pipes.clear(); score = 0; vy = 0; spawnTimer = 0; gameOver = false; running = false; paused = false;
        birdX = getWidth() * .27f; birdY = getHeight() * .45f; birdAnimTime = 0f;
        lastTime = System.nanoTime(); invalidate();
    }

    @Override protected void onSizeChanged(int w,int h,int ow,int oh){ reset(); }

    @Override public boolean onTouchEvent(MotionEvent e){
        if(e.getAction()!=MotionEvent.ACTION_DOWN) return true;
        if(paused) return true;
        if(gameOver){ reset(); running = true; vy = -dp(330); lastTime = System.nanoTime(); }
        else { if(!running){ running = true; lastTime = System.nanoTime(); } vy = -dp(330); }
        invalidate(); return true;
    }

    @Override protected void onDraw(Canvas c){
        super.onDraw(c);
        float dt = 0f;
        long now = System.nanoTime();
        if(running && !gameOver){ dt = Math.min(.035f, (now-lastTime)/1_000_000_000f); }
        lastTime = now;

        drawBackground(c);
        if(running && !gameOver) update(dt);
        drawPipes(c);
        drawBird(c);
        drawHud(c);
        if(running && !gameOver) postInvalidateOnAnimation();
    }

    private void update(float dt){
        vy += dp(950) * dt;
        birdY += vy * dt;
        spawnTimer += dt;
        if(spawnTimer > 1.45f){ spawnTimer = 0; spawnPipe(); }
        float speed = dp(155);
        for(int i=pipes.size()-1;i>=0;i--){
            Pipe q = pipes.get(i); q.x -= speed*dt;
            if(!q.counted && q.x + dp(70) < birdX){ q.counted=true; score++; }
            if(q.x < -dp(90)) pipes.remove(i);
        }
        float wrapTop = -dp(22);\n        float wrapBottom = getHeight() - dp(58) + dp(22);\n        if(birdY < wrapTop){\n            birdY = wrapBottom;\n        } else if(birdY > wrapBottom){\n            birdY = wrapTop;\n        }
        float br = dp(18);
        for(Pipe q:pipes){
            float pw=dp(72), gap=dp(175);
            if(birdX+br > q.x && birdX-br < q.x+pw){
                if(birdY-br < q.gapY-gap/2 || birdY+br > q.gapY+gap/2){ die(); break; }
            }
        }
    }

    private void die(){ running=false; gameOver=true; invalidate(); }

    private void spawnPipe(){
        float margin=dp(120), gap=dp(175);
        float min=margin+gap/2, max=getHeight()-dp(90)-gap/2;
        float gy=min+rnd.nextFloat()*Math.max(1,max-min);
        pipes.add(new Pipe(getWidth()+dp(20), gy, rnd.nextInt(pipeColors.length)));
    }

    private void drawBackground(Canvas c){
        p.setStyle(Paint.Style.FILL);
        p.setColor(Color.rgb(120,205,245)); c.drawRect(0,0,getWidth(),getHeight(),p);
        p.setColor(Color.argb(80,255,255,255));
        c.drawOval(new RectF(dp(25),dp(55),dp(150),dp(100)),p);
        c.drawOval(new RectF(getWidth()-dp(180),dp(115),getWidth()-dp(35),dp(165)),p);
        p.setColor(Color.rgb(220,190,120)); c.drawRect(0,getHeight()-dp(45),getWidth(),getHeight(),p);
        p.setColor(Color.rgb(95,205,90)); c.drawRect(0,getHeight()-dp(58),getWidth(),getHeight()-dp(45),p);
    }

    private void drawPipes(Canvas c){
        float pw=dp(72), gap=dp(175);
        for(Pipe q:pipes){
            int col=pipeColors[q.color]; p.setColor(col); p.setStyle(Paint.Style.FILL);
            float topBottom=q.gapY-gap/2, bottomTop=q.gapY+gap/2;
            c.drawRect(q.x,0,q.x+pw,topBottom,p);
            c.drawRect(q.x,bottomTop,q.x+pw,getHeight()-dp(58),p);
            p.setColor(brighten(col));
            c.drawRect(q.x-dp(7),topBottom-dp(24),q.x+pw+dp(7),topBottom,p);
            c.drawRect(q.x-dp(7),bottomTop,q.x+pw+dp(7),bottomTop+dp(24),p);
            p.setColor(Color.argb(70,0,0,0));
            c.drawRect(q.x+pw-dp(10),0,q.x+pw,topBottom-dp(24),p);
            c.drawRect(q.x+pw-dp(10),bottomTop+dp(24),q.x+pw,getHeight()-dp(58),p);
        }
    }

    private int brighten(int c){
        return Color.rgb(Math.min(255,Color.red(c)+35),Math.min(255,Color.green(c)+35),Math.min(255,Color.blue(c)+35));
    }

    private void drawBird(Canvas c){
        float r=dp(19), x=birdX, y=birdY;
        float tilt = Math.max(-25f, Math.min(55f, vy / dp(8f)));
        float wingBob = (float)Math.sin(birdAnimTime * 16f) * r * .28f;

        c.save();
        c.rotate(tilt, x, y);

        p.setColor(birdColor);
        c.drawOval(new RectF(x-r*1.2f,y-r,x+r*1.2f,y+r),p);

        p.setColor(darken(birdColor));
        c.drawOval(new RectF(x-r*.85f,y-r*.05f+wingBob,x+r*.15f,y+r*.78f+wingBob),p);

        p.setColor(Color.WHITE);
        c.drawCircle(x+r*.55f,y-r*.35f,r*.42f,p);
        p.setColor(Color.BLACK);
        c.drawCircle(x+r*.67f,y-r*.35f,r*.16f,p);

        Path beak=new Path();
        beak.moveTo(x+r*.95f,y-r*.1f);
        beak.lineTo(x+r*1.75f,y+r*.15f);
        beak.lineTo(x+r*.95f,y+r*.45f);
        beak.close();
        p.setColor(beakColor);
        c.drawPath(beak,p);

        drawHat(c,x,y-r*.9f,r);
        c.restore();
    }

    private int darken(int c){ return Color.rgb((int)(Color.red(c)*.75),(int)(Color.green(c)*.75),(int)(Color.blue(c)*.75)); }

    private void drawHat(Canvas c,float x,float y,float r){
        p.setStyle(Paint.Style.FILL);
        if(hat==1){
            p.setColor(Color.rgb(40,60,220)); c.drawRect(x-r,y-r*.65f,x+r*.65f,y,p); c.drawRect(x+r*.4f,y-r*.1f,x+r*1.1f,y+r*.18f,p);
        } else if(hat==2){
            p.setColor(Color.rgb(90,100,110)); c.drawRect(x-r,y-r*.7f,x+r,y,p); p.setColor(Color.LTGRAY); c.drawRect(x-r*.75f,y-r*.45f,x+r*.75f,y-r*.2f,p);
        } else if(hat==3){
            Path h=new Path(); h.moveTo(x,y-r*1.6f); h.lineTo(x-r*1.05f,y); h.lineTo(x+r*1.05f,y); h.close(); p.setColor(Color.rgb(245,130,25)); c.drawPath(h,p);
            p.setColor(Color.rgb(220,70,30)); c.drawCircle(x-r*.25f,y-r*.45f,r*.13f,p); c.drawCircle(x+r*.35f,y-r*.3f,r*.11f,p);
        }
    }

    private void drawHud(Canvas c){
        p.setColor(Color.WHITE); p.setTextAlign(Paint.Align.CENTER); p.setFakeBoldText(true); p.setTextSize(dp(34));
        c.drawText(String.valueOf(score),getWidth()/2f,dp(55),p);
        p.setTextSize(dp(15)); c.drawText("BEST "+best+"  SAVE "+activeSlot,getWidth()/2f,dp(78),p);
        if(paused){
            p.setColor(Color.argb(190,0,0,0)); c.drawRoundRect(new RectF(dp(35),getHeight()*.35f,getWidth()-dp(35),getHeight()*.58f),dp(18),dp(18),p);
            p.setColor(Color.WHITE); p.setTextSize(dp(26)); c.drawText("ПАУЗА",getWidth()/2f,getHeight()*.44f,p);
            p.setTextSize(dp(15)); c.drawText("Выбери действие в меню",getWidth()/2f,getHeight()*.50f,p);
        } else if(!running){
            p.setColor(Color.argb(180,0,0,0)); c.drawRoundRect(new RectF(dp(35),getHeight()*.32f,getWidth()-dp(35),getHeight()*.62f),dp(18),dp(18),p);
            p.setColor(Color.WHITE); p.setTextSize(dp(25));
            c.drawText(gameOver?"ИГРА ОКОНЧЕНА":"FLAPPY КУПЧИНО",getWidth()/2f,getHeight()*.42f,p);
            p.setTextSize(dp(16)); c.drawText(gameOver?"Тапни, чтобы заново":"Тапни, чтобы лететь",getWidth()/2f,getHeight()*.49f,p);
            if(gameOver){ p.setTextSize(dp(18)); c.drawText("Счёт: "+score,getWidth()/2f,getHeight()*.55f,p); }
        }
    }

    static class Pipe { float x,gapY; int color; boolean counted=false; Pipe(float x,float y,int c){this.x=x;gapY=y;color=c;} }
}
