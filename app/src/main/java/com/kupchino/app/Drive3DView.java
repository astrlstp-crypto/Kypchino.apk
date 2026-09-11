package com.kupchino.app;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * One-app container for Kupchino Drive. The OpenGL renderer and all controls
 * live inside the main Kupchino APK; no second application is launched.
 */
public class Drive3DView extends FrameLayout {
    private final DriveUltra3DView game;
    private final TextView hud;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private final Runnable hudUpdater = new Runnable() {
        @Override public void run() {
            hud.setText("🚗 KUPCHINO DRIVE 3D\n"+
                    "Графика: " + DriveUltra3DView.QUALITY_NAME + " • HIGH REFRESH\n"+
                    game.getSpeedKmh() + " км/ч    $" + game.getMoney() +
                    (game.isFreeMode()?"    FREE DRIVE":"    PARKING MISSION"));
            handler.postDelayed(this,200);
        }
    };

    public Drive3DView(Context context) {
        super(context);
        setBackgroundColor(Color.BLACK);

        game = new DriveUltra3DView(context);
        addView(game,new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.MATCH_PARENT));

        hud = new TextView(context);
        hud.setTextColor(Color.WHITE);
        hud.setTextSize(14);
        hud.setPadding(dp(12),dp(8),dp(12),dp(8));
        hud.setBackground(makeBg(0xB0161A20,16));
        hud.getPaint().setFakeBoldText(true);
        FrameLayout.LayoutParams hp=new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT,Gravity.TOP|Gravity.START);
        hp.setMargins(dp(10),dp(10),dp(10),0);
        addView(hud,hp);

        LinearLayout modes=new LinearLayout(context);
        modes.setOrientation(LinearLayout.HORIZONTAL);
        modes.setGravity(Gravity.CENTER);
        Button free=controlButton("🆓 FREE");
        Button mission=controlButton("🅿 МИССИЯ");
        modes.addView(free,new LinearLayout.LayoutParams(0,dp(48),1f));
        modes.addView(mission,new LinearLayout.LayoutParams(0,dp(48),1f));
        FrameLayout.LayoutParams mp=new FrameLayout.LayoutParams(dp(250),dp(48),Gravity.TOP|Gravity.END);
        mp.setMargins(0,dp(10),dp(10),0);
        addView(modes,mp);
        free.setOnClickListener(v->game.setFreeMode(!game.isFreeMode()));
        mission.setOnClickListener(v->game.newMission());

        LinearLayout bottom=new LinearLayout(context);
        bottom.setOrientation(LinearLayout.HORIZONTAL);
        bottom.setGravity(Gravity.CENTER);
        bottom.setPadding(dp(8),0,dp(8),dp(12));
        Button left=controlButton("◀");
        Button right=controlButton("▶");
        Button brake=controlButton("BRAKE");
        Button gas=controlButton("GAS");
        LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(0,dp(74),1f);
        bp.setMargins(dp(4),0,dp(4),0);
        bottom.addView(left,bp); bottom.addView(right,bp); bottom.addView(brake,bp); bottom.addView(gas,bp);
        FrameLayout.LayoutParams bottomParams=new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT,dp(90),Gravity.BOTTOM);
        addView(bottom,bottomParams);

        bindHold(left,0); bindHold(right,1); bindHold(brake,2); bindHold(gas,3);
        handler.post(hudUpdater);
    }

    private void bindHold(Button button,int action){
        button.setOnTouchListener((v,e)->{
            boolean down=e.getAction()==MotionEvent.ACTION_DOWN || e.getAction()==MotionEvent.ACTION_MOVE;
            if(e.getAction()==MotionEvent.ACTION_UP || e.getAction()==MotionEvent.ACTION_CANCEL) down=false;
            if(action==0)game.setLeft(down);
            else if(action==1)game.setRight(down);
            else if(action==2)game.setBrake(down);
            else game.setGas(down);
            if(e.getAction()==MotionEvent.ACTION_UP)v.performClick();
            return true;
        });
    }

    private Button controlButton(String text){
        Button b=new Button(getContext());
        b.setText(text);
        b.setTextColor(Color.WHITE);
        b.setTextSize(15);
        b.setAllCaps(false);
        b.getPaint().setFakeBoldText(true);
        b.setBackground(makeBg(0xB5222730,18));
        return b;
    }

    private GradientDrawable makeBg(int color,int radiusDp){
        GradientDrawable g=new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(radiusDp));
        g.setStroke(dp(1),0x66FFFFFF);
        return g;
    }

    private int dp(int v){ return Math.round(v*getResources().getDisplayMetrics().density); }

    @Override protected void onAttachedToWindow(){
        super.onAttachedToWindow();
        game.onResume();
    }

    @Override protected void onDetachedFromWindow(){
        handler.removeCallbacks(hudUpdater);
        game.onPause();
        super.onDetachedFromWindow();
    }
}
