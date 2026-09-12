package com.lambocheat.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class CheatOverlayService extends Service {
    private static final String CHANNEL = "lambocheat_overlay_v5";
    private static final int PORT = 48767;
    private WindowManager wm;
    private TextView bubble;
    private View panel;
    private WindowManager.LayoutParams bubbleParams;
    private SharedPreferences prefs;
    private final Handler main = new Handler(Looper.getMainLooper());

    private final String[] ITEMS = {
            "Анти-жир", "Батарейка", "Гвозди", "Грязная тряпка", "Заколка",
            "Замороженное мясо", "Зарядка", "Ключ", "Ключ от аптечки",
            "Ключи от машины", "Лампочка", "Молоток", "Отвёртка", "Снотворное",
            "Спрей от насекомых", "Стиральный порошок", "Ступенька", "Швабра", "Фонарик"
    };

    @Override public void onCreate() {
        super.onCreate();
        prefs = getSharedPreferences("cheats_v5", MODE_PRIVATE);
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        createChannel();
        startForeground(75, notification());
        showBubble();
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (bubble == null) showBubble();
        return START_NOT_STICKY;
    }

    @Override public IBinder onBind(Intent intent) { return null; }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel c = new NotificationChannel(CHANNEL, "LamboCheat V5", NotificationManager.IMPORTANCE_LOW);
            c.setDescription("LamboCheat V5 floating menu");
            getSystemService(NotificationManager.class).createNotificationChannel(c);
        }
    }

    private Notification notification() {
        Intent open = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 1, open,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0));
        Notification.Builder b = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(this, CHANNEL) : new Notification.Builder(this);
        return b.setSmallIcon(android.R.drawable.ic_menu_manage)
                .setContentTitle("LamboCheat V5 запущен 🐈")
                .setContentText("T — открыть/скрыть меню")
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
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{0xFF974EFF, 0xFF327CFF});
        g.setShape(GradientDrawable.OVAL);
        g.setStroke(dp(2), 0x99FFFFFF);
        bubble.setBackground(g);

        bubbleParams = new WindowManager.LayoutParams(dp(58), dp(58), WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        bubbleParams.gravity = Gravity.TOP | Gravity.RIGHT;
        bubbleParams.x = dp(16);
        bubbleParams.y = dp(150);
        wm.addView(bubble, bubbleParams);

        bubble.setOnClickListener(v -> togglePanel());
        bubble.setOnTouchListener(new View.OnTouchListener() {
            float sx, sy; int ox, oy; boolean moved;
            @Override public boolean onTouch(View v, MotionEvent e) {
                switch (e.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        sx=e.getRawX(); sy=e.getRawY(); ox=bubbleParams.x; oy=bubbleParams.y; moved=false; return false;
                    case MotionEvent.ACTION_MOVE:
                        float dx=e.getRawX()-sx, dy=e.getRawY()-sy;
                        if(Math.abs(dx)>8 || Math.abs(dy)>8){
                            moved=true; bubbleParams.x=ox-(int)dx; bubbleParams.y=oy+(int)dy;
                            try { wm.updateViewLayout(bubble,bubbleParams); } catch(Exception ignored) {}
                            return true;
                        }
                        break;
                }
                return moved;
            }
        });
    }

    private void togglePanel() { if (panel == null) showPanel(); else hidePanel(); }

    private void showPanel() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(14), dp(14), dp(14), dp(14));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xF20A0C12); bg.setCornerRadius(dp(22)); bg.setStroke(dp(1), 0xFF353C53);
        box.setBackground(bg); scroll.addView(box);

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL); top.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = label("LAMBOCHEAT V5 🐈", 19, Color.WHITE); title.getPaint().setFakeBoldText(true);
        top.addView(title, new LinearLayout.LayoutParams(0, dp(48), 1f));
        Button hideTop = actionButton("✕ УБРАТЬ");
        top.addView(hideTop, new LinearLayout.LayoutParams(dp(112), dp(44)));
        hideTop.setOnClickListener(v -> hidePanel());
        box.addView(top, new LinearLayout.LayoutParams(-1, dp(52)));

        TextView hint = label("V5 DIRECT BRIDGE • SchoolBoy LamboV5", 10, 0xFFA2ACC0);
        box.addView(hint, lp(dp(32)));

        Button ping = actionButton("🔌 Проверить связь с игрой");
        box.addView(ping, lp(dp(48)));
        ping.setOnClickListener(v -> sendCommand("PING", "Связь с игрой"));

        box.addView(toggle("levitation", "Левитация", "Fly / зависание", "LEV"), lp(dp(60)));
        box.addView(toggle("tyson", "🐈 Режим Тайсона", "остановить маму и папу", "TYSON"), lp(dp(66)));

        Button items = actionButton("🎒 Выдать предмет  ▼");
        box.addView(items, lp(dp(54)));
        LinearLayout itemBox = new LinearLayout(this);
        itemBox.setOrientation(LinearLayout.VERTICAL); itemBox.setVisibility(View.GONE);
        for (int i=0;i<ITEMS.length;i++) {
            final int idx=i; Button b=smallButton((i+1)+". "+ITEMS[i]); itemBox.addView(b, lp(dp(44)));
            b.setOnClickListener(v -> sendCommand("ITEM:"+idx, "Предмет: "+ITEMS[idx]));
        }
        box.addView(itemBox, new LinearLayout.LayoutParams(-1,-2));
        items.setOnClickListener(v -> itemBox.setVisibility(itemBox.getVisibility()==View.VISIBLE?View.GONE:View.VISIBLE));

        box.addView(toggle("noclip", "Noclip + Fly", "проходить сквозь стены", "NOCLIP"), lp(dp(66)));
        box.addView(action("😡 Разозлить маму", "MOM"), lp(dp(54)));
        box.addView(action("😡 Разозлить папу", "DAD"), lp(dp(54)));
        box.addView(action("🚪 Открыть потайник", "SECRET"), lp(dp(54)));
        box.addView(action("🔐 Открыть сейф", "SAFE"), lp(dp(54)));
        box.addView(action("🏠 Открыть входную дверь", "FRONT"), lp(dp(54)));

        Button hideBig = actionButton("✕ СКРЫТЬ МЕНЮ — оставить T");
        box.addView(hideBig, lp(dp(56)));
        hideBig.setOnClickListener(v -> hidePanel());

        Button exit = actionButton("⏻ Выйти полностью — убрать T");
        box.addView(exit, lp(dp(56)));
        exit.setOnClickListener(v -> { hidePanel(); removeBubble(); stopForeground(true); stopSelf(); });

        panel = scroll;
        WindowManager.LayoutParams p = new WindowManager.LayoutParams(dp(350), dp(650), WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        p.gravity = Gravity.TOP | Gravity.RIGHT; p.x=dp(12); p.y=dp(58);
        wm.addView(panel,p);
    }

    private View toggle(String key, String title, String subtitle, String commandBase) {
        LinearLayout row = new LinearLayout(this); row.setOrientation(LinearLayout.VERTICAL); row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(12),dp(4),dp(12),dp(4)); styleRow(row);
        TextView mainText=label("",14,Color.WHITE); TextView sub=label(subtitle,10,0xFF929CB0);
        row.addView(mainText,new LinearLayout.LayoutParams(-1,dp(28))); row.addView(sub,new LinearLayout.LayoutParams(-1,dp(22)));
        Runnable paint=()->mainText.setText((prefs.getBoolean(key,false)?"● ON   ":"○ OFF  ")+title); paint.run();
        row.setOnClickListener(v->{ boolean on=!prefs.getBoolean(key,false); prefs.edit().putBoolean(key,on).apply(); paint.run();
            sendCommand(commandBase+":"+(on?"1":"0"), title+(on?" включён":" выключен")); });
        return row;
    }

    private View action(String text,String command){ Button b=actionButton(text); b.setOnClickListener(v->sendCommand(command,text)); return b; }

    private void sendCommand(String command, String friendly) {
        new Thread(() -> {
            String result;
            try (Socket s = new Socket()) {
                s.connect(new InetSocketAddress("127.0.0.1", PORT), 1500);
                s.setSoTimeout(2500);
                OutputStream out=s.getOutputStream(); out.write(command.getBytes(StandardCharsets.UTF_8)); out.flush();
                InputStream in=s.getInputStream(); byte[] buf=new byte[32]; int n=in.read(buf);
                String code = n>0 ? new String(buf,0,n,StandardCharsets.UTF_8).trim() : "";
                if ("OK".equals(code)) result = friendly+" ❤️";
                else if ("NOT_READY".equals(code)) result = "Игра ещё загружается — попробуй после входа в дом 🥶";
                else if ("FAIL".equals(code)) result = "Команда дошла, но игровой объект/метод не сработал";
                else if ("BAD_CMD".equals(code)) result = "Неизвестная команда LamboCheat";
                else result = "Ответ игры: "+code;
            } catch (Exception e) {
                result = "Нет связи с SchoolBoy LamboV5. Сначала запусти V5 и дождись игры.";
            }
            final String r=result; main.post(() -> Toast.makeText(this,r,Toast.LENGTH_LONG).show());
        }, "LamboCheatBridgeV5").start();
    }

    private void styleRow(View v){ GradientDrawable d=new GradientDrawable(); d.setColor(0xFF151925); d.setCornerRadius(dp(15)); d.setStroke(dp(1),0xFF2A3143); v.setBackground(d); }
    private Button actionButton(String text){ Button b=new Button(this); b.setText(text); b.setAllCaps(false); b.setTextColor(Color.WHITE); b.setTextSize(13); styleRow(b); return b; }
    private Button smallButton(String text){ Button b=actionButton("  "+text); b.setTextSize(12); return b; }
    private TextView label(String text,int sp,int color){ TextView t=new TextView(this); t.setText(text); t.setTextSize(sp); t.setTextColor(color); t.setGravity(Gravity.CENTER_VERTICAL); return t; }
    private LinearLayout.LayoutParams lp(int h){ LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,h); p.setMargins(0,dp(4),0,dp(4)); return p; }
    private void hidePanel(){ if(panel!=null){ try{wm.removeView(panel);}catch(Exception ignored){} panel=null; } }
    private void removeBubble(){ if(bubble!=null){ try{wm.removeView(bubble);}catch(Exception ignored){} bubble=null; } }

    @Override public void onDestroy(){ hidePanel(); removeBubble(); super.onDestroy(); }
    private int dp(float n){ return Math.round(n*getResources().getDisplayMetrics().density); }
}
