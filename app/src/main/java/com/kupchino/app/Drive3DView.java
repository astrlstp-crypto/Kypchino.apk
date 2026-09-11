package com.kupchino.app;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * UI shell for the true OpenGL 3D renderer. This stays inside the main Kupchino APK.
 */
public class Drive3DView extends FrameLayout {
    private final DriveUltra3DView world;
    private final TextView status;
    private final Button mode;

    public Drive3DView(Context context){
        super(context);
        setBackgroundColor(Color.BLACK);

        world=new DriveUltra3DView(context);
        addView(world,new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.MATCH_PARENT));

        LinearLayout top=new LinearLayout(context);
        top.setOrientation(LinearLayout.VERTICAL);
        top.setPadding(dp(10),dp(8),dp(10),dp(8));
        GradientDrawable topBg=new GradientDrawable();
        topBg.setColor(0xAA0B1117);
        topBg.setCornerRadius(dp(14));
        top.setBackground(topBg);

        TextView title=new TextView(context);
        title.setText("KUPCHINO DRIVE • 3D MAX");
        title.setTextColor(Color.WHITE);
        title.setTextSize(17);
        title.getPaint().setFakeBoldText(true);
        top.addView(title,new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT));

        status=new TextView(context);
        status.setTextColor(0xFFEAF6FF);
        status.setTextSize(13);
        top.addView(status,new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT));

        FrameLayout.LayoutParams tp=new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT);
        tp.gravity=Gravity.TOP|Gravity.LEFT;
        tp.leftMargin=dp(10); tp.topMargin=dp(10);
        addView(top,tp);

        LinearLayout missionBar=new LinearLayout(context);
        missionBar.setOrientation(LinearLayout.HORIZONTAL);
        missionBar.setGravity(Gravity.CENTER);
        mode=smallButton("🛣 FREE DRIVE");
        Button mission=smallButton("🅿 NEW MISSION");
        missionBar.addView(mode,new LinearLayout.LayoutParams(0,dp(48),1f));
        missionBar.addView(mission,new LinearLayout.LayoutParams(0,dp(48),1f));

        FrameLayout.LayoutParams mp=new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT,dp(48));
        mp.gravity=Gravity.TOP;
        mp.leftMargin=dp(8); mp.rightMargin=dp(8); mp.topMargin=dp(84);
        addView(missionBar,mp);

        mode.setOnClickListener(v->{
            boolean next=!world.isFreeMode();
            world.setFreeMode(next);
            mode.setText(next?"🎯 MISSIONS":"🛣 FREE DRIVE");
        });
        mission.setOnClickListener(v->{ world.newMission(); mode.setText("🛣 FREE DRIVE"); });

        LinearLayout controls=new LinearLayout(context);
        controls.setOrientation(LinearLayout.HORIZONTAL);
        controls.setGravity(Gravity.CENTER);
        Button left=controlButton("◀");
        Button right=controlButton("▶");
        Button brake=controlButton("BRAKE");
        Button gas=controlButton("GAS");
        controls.addView(left,new LinearLayout.LayoutParams(0,dp(72),1f));
        controls.addView(right,new LinearLayout.LayoutParams(0,dp(72),1f));
        controls.addView(brake,new LinearLayout.LayoutParams(0,dp(72),1f));
        controls.addView(gas,new LinearLayout.LayoutParams(0,dp(72),1f));

        bindHold(left,()->world.setLeft(true),()->world.setLeft(false));
        bindHold(right,()->world.setRight(true),()->world.setRight(false));
        bindHold(brake,()->world.setBrake(true),()->world.setBrake(false));
        bindHold(gas,()->world.setGas(true),()->world.setGas(false));

        FrameLayout.LayoutParams cp=new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT,dp(72));
        cp.gravity=Gravity.BOTTOM;
        cp.leftMargin=dp(8); cp.rightMargin=dp(8); cp.bottomMargin=dp(10);
        addView(controls,cp);

        post(statusLoop);
    }

    private final Runnable statusLoop=new Runnable(){
        @Override public void run(){
            if(status!=null && world!=null){
                status.setText("MAX • "+world.getSpeedKmh()+" km/h • $"+world.getMoney()+"\nрегиональные трассы • кольцевая • перекрёстки");
                postDelayed(this,180);
            }
        }
    };

    private Button smallButton(String text){
        Button b=new Button(getContext());
        b.setText(text); b.setAllCaps(false); b.setTextSize(12); b.setTextColor(Color.WHITE);
        GradientDrawable bg=new GradientDrawable();
        bg.setColor(0xCC18222D); bg.setCornerRadius(dp(12));
        b.setBackground(bg);
        return b;
    }

    private Button controlButton(String text){
        Button b=new Button(getContext());
        b.setText(text); b.setAllCaps(false); b.setTextSize(15); b.setTextColor(Color.WHITE);
        b.getPaint().setFakeBoldText(true);
        GradientDrawable bg=new GradientDrawable();
        bg.setColor(0xD9232C35); bg.setCornerRadius(dp(18));
        bg.setStroke(dp(1),0xAAFFFFFF);
        b.setBackground(bg);
        return b;
    }

    private interface Action { void run(); }
    private void bindHold(Button b,Action down,Action up){
        b.setOnTouchListener((v,e)->{
            if(e.getAction()==MotionEvent.ACTION_DOWN){ down.run(); v.setAlpha(0.72f); return true; }
            if(e.getAction()==MotionEvent.ACTION_UP||e.getAction()==MotionEvent.ACTION_CANCEL){ up.run(); v.setAlpha(1f); return true; }
            return true;
        });
    }

    private int dp(float v){ return (int)(v*getResources().getDisplayMetrics().density+0.5f); }

    @Override protected void onDetachedFromWindow(){
        removeCallbacks(statusLoop);
        world.setGas(false); world.setBrake(false); world.setLeft(false); world.setRight(false);
        super.onDetachedFromWindow();
    }
}
