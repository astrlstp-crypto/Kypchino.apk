package com.kupchino.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.opengl.EGL14;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/** Performance + full landscape mobile UI for Kupchino Drive LIFE/MAX. */
public class DrivePerformance3DView extends DriveLife3DView {
    private volatile int targetFps = 120;
    private volatile boolean vsyncEnabled = true;
    private long lastFrameStartNs = 0L;
    private boolean uiInstalled = false;
    private final Handler ui = new Handler(Looper.getMainLooper());
    private TextView hud;
    private Button vehicleButton;
    private Button interactButton;

    public DrivePerformance3DView(Context context) {
        super(context);
        if(context instanceof Activity){
            postDelayed(() -> installLandscapeUi((Activity)context), 80);
        }
    }

    public void setPerformance(int fps, boolean vsync) {
        targetFps = Math.max(30, Math.min(240, fps));
        vsyncEnabled = vsync;
        lastFrameStartNs = 0L;
        queueEvent(this::applySwapInterval);
    }
    public int getTargetFps(){ return targetFps; }
    public boolean isVsyncEnabled(){ return vsyncEnabled; }

    @Override public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        super.onSurfaceCreated(gl, config);
        applySwapInterval();
        lastFrameStartNs = 0L;
    }

    @Override public void onDrawFrame(GL10 gl) {
        paceFrame();
        super.onDrawFrame(gl);
    }

    private void applySwapInterval(){
        try{ EGL14.eglSwapInterval(EGL14.eglGetCurrentDisplay(),vsyncEnabled?1:0); }catch(Throwable ignored){}
    }

    private void paceFrame(){
        int fps=Math.max(30,Math.min(240,targetFps));
        long target=1_000_000_000L/fps,now=System.nanoTime();
        if(lastFrameStartNs!=0){
            long remain=target-(now-lastFrameStartNs);
            if(remain>0){
                try{Thread.sleep(remain/1_000_000L,(int)(remain%1_000_000L));}catch(InterruptedException e){Thread.currentThread().interrupt();}
            }
        }
        lastFrameStartNs=System.nanoTime();
    }

    private int dp(float v){ return Math.round(v*getResources().getDisplayMetrics().density); }

    private Button gameButton(String text){
        Button b=new Button(getContext());
        b.setText(text); b.setAllCaps(false); b.setTextColor(Color.WHITE); b.setTextSize(14);
        GradientDrawable bg=new GradientDrawable(); bg.setColor(0xCC161B22); bg.setCornerRadius(dp(16)); bg.setStroke(dp(1),0x887A8CA0); b.setBackground(bg);
        b.setPadding(dp(8),0,dp(8),0);
        return b;
    }

    private void hold(View v, java.util.function.Consumer<Boolean> setter){
        v.setOnTouchListener((view,e)->{
            int a=e.getActionMasked(); boolean down=a!=MotionEvent.ACTION_UP&&a!=MotionEvent.ACTION_CANCEL;
            setter.accept(down); return true;
        });
    }

    private void installLandscapeUi(Activity a){
        if(uiInstalled || a.isFinishing())return;
        uiInstalled=true;
        a.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);

        if(getParent() instanceof ViewGroup)((ViewGroup)getParent()).removeView(this);
        FrameLayout root=new FrameLayout(a); root.setBackgroundColor(Color.BLACK);
        root.addView(this,new FrameLayout.LayoutParams(-1,-1));

        hud=new TextView(a); hud.setTextColor(Color.WHITE); hud.setTextSize(13); hud.setPadding(dp(12),dp(7),dp(12),dp(7));
        hud.setBackgroundColor(0x9910151B); hud.setGravity(Gravity.LEFT|Gravity.CENTER_VERTICAL);
        FrameLayout.LayoutParams hp=new FrameLayout.LayoutParams(dp(620),dp(58),Gravity.TOP|Gravity.LEFT); hp.setMargins(dp(12),dp(10),0,0); root.addView(hud,hp);

        Button pause=gameButton("⏸ ПАУЗА");
        FrameLayout.LayoutParams pp=new FrameLayout.LayoutParams(dp(132),dp(54),Gravity.TOP|Gravity.RIGHT); pp.setMargins(0,dp(10),dp(12),0); root.addView(pause,pp);
        pause.setOnClickListener(v->showPause(a));

        vehicleButton=gameButton("🚪 ВЫЙТИ ИЗ МАШИНЫ");
        FrameLayout.LayoutParams vp=new FrameLayout.LayoutParams(dp(190),dp(52),Gravity.TOP|Gravity.CENTER_HORIZONTAL); vp.setMargins(0,dp(72),0,0); root.addView(vehicleButton,vp);
        vehicleButton.setOnClickListener(v->{ showMessage(toggleVehicle()); refreshHud(); });

        interactButton=gameButton("🟢 ВОЙТИ / ДЕЙСТВИЕ");
        FrameLayout.LayoutParams ip=new FrameLayout.LayoutParams(dp(190),dp(52),Gravity.TOP|Gravity.CENTER_HORIZONTAL); ip.setMargins(0,dp(130),0,0); root.addView(interactButton,ip);
        interactButton.setOnClickListener(v->{
            String result=interactNearby();
            showMessage(result);
            refreshHud();
        });

        LinearLayout arrows=new LinearLayout(a); arrows.setOrientation(LinearLayout.HORIZONTAL); arrows.setGravity(Gravity.CENTER);
        Button l=gameButton("◀"); Button r=gameButton("▶"); l.setTextSize(24); r.setTextSize(24);
        LinearLayout.LayoutParams aw=new LinearLayout.LayoutParams(dp(92),dp(74)); aw.setMargins(dp(4),0,dp(4),0); arrows.addView(l,aw); arrows.addView(r,aw);
        FrameLayout.LayoutParams arp=new FrameLayout.LayoutParams(dp(205),dp(80),Gravity.BOTTOM|Gravity.LEFT); arp.setMargins(dp(18),0,0,dp(140)); root.addView(arrows,arp);
        hold(l,this::setLeft); hold(r,this::setRight);

        FrameLayout brakeHit=new FrameLayout(a); brakeHit.setClickable(true);
        Button brake=gameButton("BRAKE"); brake.setTextSize(13);
        FrameLayout.LayoutParams bpSmall=new FrameLayout.LayoutParams(dp(92),dp(104),Gravity.BOTTOM|Gravity.LEFT); brakeHit.addView(brake,bpSmall);
        FrameLayout.LayoutParams bhp=new FrameLayout.LayoutParams(dp(178),dp(128),Gravity.BOTTOM|Gravity.LEFT); bhp.setMargins(dp(20),0,0,dp(12)); root.addView(brakeHit,bhp);
        hold(brakeHit,this::setBrake); hold(brake,this::setBrake);

        Button gas=gameButton("GAS"); gas.setTextSize(18);
        FrameLayout.LayoutParams gp=new FrameLayout.LayoutParams(dp(178),dp(128),Gravity.BOTTOM|Gravity.RIGHT); gp.setMargins(0,0,dp(20),dp(12)); root.addView(gas,gp);
        hold(gas,this::setGas);

        TextView brakeLabel=new TextView(a); brakeLabel.setText("ТОРМОЗ"); brakeLabel.setTextColor(0xFFCBD4DF); brakeLabel.setTextSize(11); brakeLabel.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams blp=new FrameLayout.LayoutParams(dp(100),dp(28),Gravity.BOTTOM|Gravity.LEFT); blp.setMargins(dp(20),0,0,dp(115)); root.addView(brakeLabel,blp);

        a.setContentView(root);
        startHudLoop();
    }

    private void startHudLoop(){ ui.removeCallbacks(hudUpdater); ui.post(hudUpdater); }
    private final Runnable hudUpdater=new Runnable(){ public void run(){ refreshHud(); ui.postDelayed(this,180); }};

    private void refreshHud(){
        if(hud==null)return;
        String near=getNearbyPlaceName();
        String mode=isFreeMode()?"FREE":"МИССИЯ "+getMissionNumber()+"/250";
        String state=isOnFoot()?"🚶 ПЕШКОМ":"🚗 "+getSelectedCarName();
        hud.setText(state+"   •   "+mode+"   •   $"+getMoney()+"   •   "+getSpeedKmh()+" км/ч   •   HP "+getHealth()+"   •   Сила "+getStrength()+"/100"+(near.isEmpty()?"":"\n📍 "+near+" — зелёный круг рядом"));
        if(vehicleButton!=null)vehicleButton.setText(isOnFoot()?"🚗 СЕСТЬ В МАШИНУ":"🚪 ВЫЙТИ ИЗ МАШИНЫ");
    }

    private void showPause(Activity a){
        setPaused(true);
        LinearLayout box=new LinearLayout(a); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(dp(24),dp(18),dp(24),dp(18)); box.setBackgroundColor(0xFF10151C);
        TextView title=new TextView(a); title.setText("KUPCHINO DRIVE • ПАУЗА 🥶"); title.setTextColor(Color.WHITE); title.setTextSize(24); title.setGravity(Gravity.CENTER); title.getPaint().setFakeBoldText(true); box.addView(title,new LinearLayout.LayoutParams(-1,dp(58)));
        TextView stats=new TextView(a); stats.setTextColor(0xFFD9E5F2); stats.setTextSize(14); stats.setGravity(Gravity.CENTER); stats.setText("Миссия "+getMissionNumber()+"/250   •   $"+getMoney()+"   •   "+getSelectedCarName()+"\nСила "+getStrength()+"/100   •   хват "+String.format(java.util.Locale.US,"%.1f",getGripKg())+" кг   •   HP "+getHealth()+"/100"); box.addView(stats,new LinearLayout.LayoutParams(-1,dp(60)));

        Button cont=gameButton("▶ ПРОДОЛЖИТЬ"); Button mission=gameButton("🅿 НОВАЯ МИССИЯ"); Button free=gameButton("🆓 FREE MODE"); Button shop=gameButton("🚗 МАГАЗИН ТАЧЕК • 40"); Button player=gameButton("💪 ПЕРСОНАЖ / ПОКАЗАТЕЛИ"); Button exit=gameButton("← ВЫЙТИ В КУПЧИНО");
        Button[] bs={cont,mission,free,shop,player,exit}; for(Button b:bs){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(52));p.setMargins(0,dp(6),0,0);box.addView(b,p);}
        AlertDialog dlg=new AlertDialog.Builder(a).setView(box).create();
        dlg.setOnCancelListener(d->setPaused(false));
        cont.setOnClickListener(v->{setPaused(false);dlg.dismiss();});
        mission.setOnClickListener(v->{newMission();setPaused(false);dlg.dismiss();});
        free.setOnClickListener(v->{setFreeMode(true);setPaused(false);dlg.dismiss();});
        shop.setOnClickListener(v->{dlg.dismiss();showCarShop(a);});
        player.setOnClickListener(v->new AlertDialog.Builder(a).setTitle("💪 Персонаж").setMessage(clinicReport()+"\n\nЧем больше сила из качалки, тем меньше урон при столкновениях.").setPositiveButton("OK",null).show());
        exit.setOnClickListener(v->{dlg.dismiss();leaveDrive(a);});
        dlg.show();
    }

    private void showCarShop(Activity a){
        setPaused(true);
        LinearLayout list=new LinearLayout(a); list.setOrientation(LinearLayout.VERTICAL); list.setPadding(dp(16),dp(12),dp(16),dp(12)); list.setBackgroundColor(0xFF0F141A);
        TextView t=new TextView(a);t.setText("🚗 МАГАЗИН KUPCHINO • $"+getMoney());t.setTextColor(Color.WHITE);t.setTextSize(22);t.setGravity(Gravity.CENTER);t.getPaint().setFakeBoldText(true);list.addView(t,new LinearLayout.LayoutParams(-1,dp(58)));
        final AlertDialog[] holder=new AlertDialog[1];
        for(int i=0;i<getCarCount();i++){
            final int idx=i;
            String status=isCarOwned(i)?(getSelectedCar()==i?" ✅ ВЫБРАНА":" • КУПЛЕНА"):(" • $"+getCarPrice(i));
            Button b=gameButton((i+1)+". "+getCarName(i)+status);
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(50));p.setMargins(0,dp(4),0,0);list.addView(b,p);
            b.setOnClickListener(v->{String m=buyOrSelectCar(idx);showMessage(m);if(holder[0]!=null)holder[0].dismiss();setPaused(false);refreshHud();});
        }
        ScrollView scroll=new ScrollView(a); scroll.addView(list);
        AlertDialog dlg=new AlertDialog.Builder(a).setView(scroll).setNegativeButton("Назад",(d,w)->setPaused(false)).create();holder[0]=dlg;dlg.setOnCancelListener(d->setPaused(false));dlg.show();
    }

    private void showMessage(String text){
        if(text==null||text.isEmpty())return;
        Context c=getContext(); if(c instanceof Activity)new AlertDialog.Builder(c).setMessage(text).setPositiveButton("OK",null).show();
    }

    private void leaveDrive(Activity a){
        setPaused(true); ui.removeCallbacks(hudUpdater);
        try{onPause();}catch(Exception ignored){}
        a.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        a.recreate();
    }

    @Override protected void onDetachedFromWindow(){ ui.removeCallbacks(hudUpdater); super.onDetachedFromWindow(); }
}
