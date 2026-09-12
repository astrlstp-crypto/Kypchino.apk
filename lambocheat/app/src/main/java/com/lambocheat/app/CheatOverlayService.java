package com.lambocheat.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class CheatOverlayService extends Service {
    private static final String CHANNEL = "lambocheat_overlay";
    private WindowManager wm;
    private TextView bubble;
    private View panel;
    private WindowManager.LayoutParams bubbleParams;
    private SharedPreferences prefs;

    private final String[] ITEMS = {
            "Анти-жир", "Батарейка", "Гвозди", "Грязная тряпка", "Заколка",
            "Замороженное мясо", "Зарядка", "Ключ", "Ключ от аптечки",
            "Ключи от машины", "Лампочка", "Молоток", "Отвёртка", "Снотворное",
            "Спрей от насекомых", "Стиральный порошок", "Ступенька", "Швабра", "Фонарик"
    };

    @Override
    public void onCreate() {
        super.onCreate();
        prefs = getSharedPreferences("cheats", MODE_PRIVATE);
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        createChannel();
        startForeground(73, notification());
        showBubble();
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (bubble == null) showBubble();
        return START_STICKY;
    }

    @Override public IBinder onBind(Intent intent) { return null; }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel c = new NotificationChannel(CHANNEL, "LamboCheat overlay",
                    NotificationManager.IMPORTANCE_LOW);
            c.setDescription("Keeps the LamboCheat floating menu visible");
            getSystemService(NotificationManager.class).createNotificationChannel(c);
        }
    }

    private Notification notification() {
        Intent open = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 1, open,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0));
        Notification.Builder b = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(this, CHANNEL) : new Notification.Builder(this);
        return b.setSmallIcon(android.R.drawable.ic_menu_manage)
                .setContentTitle("LamboCheat запущен 🐈")
                .setContentText("Нажми плавающую T, чтобы открыть меню")
                .setContentIntent(pi)
                .setOngoing(true)
                .build();
    }

    private void showBubble() {
        if (bubble != null) return;
        bubble = new TextView(this);
        bubble.setText("T");
        bubble.setTextColor(Color.WHITE);
        bubble.setTextSize(24);
        bubble.getPaint().setFakeBoldText(true);
        bubble.setGravity(Gravity.CENTER);
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{0xFF974EFF, 0xFF327CFF});
        g.setShape(GradientDrawable.OVAL);
        g.setStroke(dp(2), 0x99FFFFFF);
        bubble.setBackground(g);

        bubbleParams = new WindowManager.LayoutParams(dp(58), dp(58),
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        bubbleParams.gravity = Gravity.TOP | Gravity.RIGHT;
        bubbleParams.x = dp(16);
        bubbleParams.y = dp(150);
        wm.addView(bubble, bubbleParams);

        bubble.setOnClickListener(v -> togglePanel());
        bubble.setOnTouchListener(new View.OnTouchListener() {
            float sx, sy; int ox, oy; long down;
            @Override public boolean onTouch(View v, MotionEvent e) {
                switch (e.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        sx=e.getRawX(); sy=e.getRawY(); ox=bubbleParams.x; oy=bubbleParams.y; down=System.currentTimeMillis(); return false;
                    case MotionEvent.ACTION_MOVE:
                        float dx=e.getRawX()-sx, dy=e.getRawY()-sy;
                        if(Math.abs(dx)>8 || Math.abs(dy)>8){
                            bubbleParams.x=ox-(int)dx; bubbleParams.y=oy+(int)dy; wm.updateViewLayout(bubble,bubbleParams); return true;
                        }
                        break;
                }
                return false;
            }
        });
    }

    private void togglePanel() {
        if (panel == null) showPanel(); else hidePanel();
    }

    private void showPanel() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(14), dp(14), dp(14), dp(14));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xF20A0C12);
        bg.setCornerRadius(dp(22));
        bg.setStroke(dp(1), 0xFF353C53);
        box.setBackground(bg);
        scroll.addView(box);

        TextView title = label("LAMBOCHEAT  🐈", 21, Color.WHITE);
        title.getPaint().setFakeBoldText(true);
        box.addView(title, lp(dp(48)));
        TextView hint = label("CHEAT MENU • всё выключено по умолчанию", 11, 0xFFA2ACC0);
        box.addView(hint, lp(dp(34)));

        box.addView(toggle("levitation", "Левитация", "Fly / зависание"), lp(dp(60)));
        box.addView(toggle("tyson", "🐈 Режим Тайсона", "кот • незаметность • прыжок"), lp(dp(66)));

        Button items = actionButton("🎒 Выдать предмет  ▼");
        box.addView(items, lp(dp(54)));
        LinearLayout itemBox = new LinearLayout(this);
        itemBox.setOrientation(LinearLayout.VERTICAL);
        itemBox.setVisibility(View.GONE);
        for (String item : ITEMS) {
            Button b = smallButton(item);
            itemBox.addView(b, lp(dp(44)));
            b.setOnClickListener(v -> runCommand("give_item:" + item, "Выбран предмет: " + item));
        }
        box.addView(itemBox, new LinearLayout.LayoutParams(-1, -2));
        items.setOnClickListener(v -> itemBox.setVisibility(itemBox.getVisibility()==View.VISIBLE ? View.GONE : View.VISIBLE));

        box.addView(toggle("noclip", "Noclip + Fly", "проходить сквозь стены"), lp(dp(66)));
        box.addView(action("😡 Разозлить маму", "anger_mom"), lp(dp(54)));
        box.addView(action("😡 Разозлить папу", "anger_dad"), lp(dp(54)));
        box.addView(action("🚪 Открыть потайник", "open_secret"), lp(dp(54)));
        box.addView(action("🔐 Открыть сейф", "open_safe"), lp(dp(54)));
        box.addView(action("🏠 Открыть входную дверь", "open_front_door"), lp(dp(54)));

        TextView bridge = label("GAME LINK: menu ready • hook not connected", 10, 0xFF8E98AD);
        bridge.setPadding(0, dp(12), 0, dp(6));
        box.addView(bridge, lp(dp(44)));

        Button close = actionButton("Свернуть меню");
        box.addView(close, lp(dp(52)));
        close.setOnClickListener(v -> hidePanel());

        panel = scroll;
        WindowManager.LayoutParams p = new WindowManager.LayoutParams(dp(330), dp(620),
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        p.gravity = Gravity.TOP | Gravity.RIGHT;
        p.x = dp(18); p.y = dp(72);
        wm.addView(panel, p);
    }

    private View toggle(String key, String title, String subtitle) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(12), dp(4), dp(12), dp(4));
        styleRow(row);
        TextView main = label("", 14, Color.WHITE);
        TextView sub = label(subtitle, 10, 0xFF929CB0);
        row.addView(main, new LinearLayout.LayoutParams(-1, dp(28)));
        row.addView(sub, new LinearLayout.LayoutParams(-1, dp(22)));
        Runnable paint = () -> main.setText((prefs.getBoolean(key,false) ? "● ON   " : "○ OFF  ") + title);
        paint.run();
        row.setOnClickListener(v -> {
            boolean on=!prefs.getBoolean(key,false);
            prefs.edit().putBoolean(key,on).apply();
            paint.run();
            runCommand(key + ":" + (on?"1":"0"), title + (on?" включён":" выключен"));
        });
        return row;
    }

    private View action(String text, String command) {
        Button b = actionButton(text);
        b.setOnClickListener(v -> runCommand(command, text));
        return b;
    }

    private void runCommand(String command, String friendly) {
        // This is the bridge point for a version-specific SchoolBoy Runaway hook.
        // Android overlays/Shizuku alone cannot write another app's Unity state.
        Toast.makeText(this, friendly + "\nМодуль игры пока не подключён", Toast.LENGTH_SHORT).show();
    }

    private void styleRow(View v) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(0xFF151925);
        d.setCornerRadius(dp(15));
        d.setStroke(dp(1), 0xFF2A3143);
        v.setBackground(d);
    }

    private Button actionButton(String text) {
        Button b = new Button(this);
        b.setText(text); b.setAllCaps(false); b.setTextColor(Color.WHITE); b.setTextSize(13);
        styleRow(b); return b;
    }

    private Button smallButton(String text) {
        Button b = actionButton("  " + text);
        b.setTextSize(12);
        return b;
    }

    private TextView label(String text, int sp, int color) {
        TextView t = new TextView(this); t.setText(text); t.setTextSize(sp); t.setTextColor(color); t.setGravity(Gravity.CENTER_VERTICAL); return t;
    }

    private LinearLayout.LayoutParams lp(int h) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, h);
        p.setMargins(0, dp(4), 0, dp(4)); return p;
    }

    private void hidePanel() {
        if (panel != null) { try { wm.removeView(panel); } catch (Exception ignored) {} panel=null; }
    }

    @Override public void onDestroy() {
        hidePanel();
        if (bubble != null) { try { wm.removeView(bubble); } catch (Exception ignored) {} bubble=null; }
        super.onDestroy();
    }

    private int dp(float n) { return Math.round(n * getResources().getDisplayMetrics().density); }
}
