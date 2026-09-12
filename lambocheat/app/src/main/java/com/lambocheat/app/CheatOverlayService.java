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
import java.util.Locale;

public class CheatOverlayService extends Service {
    private static final String CHANNEL = "lambocheats_b1";
    private static final int PORT = 48771;
    private static final long HEALTH_DELAY = 2000L;

    private WindowManager wm;
    private TextView bubble;
    private View panel;
    private WindowManager.LayoutParams bubbleParams;
    private SharedPreferences prefs;
    private final Handler main = new Handler(Looper.getMainLooper());
    private volatile boolean shuttingDown = false;
    private boolean gameSeen = false;
    private int missedPings = 0;

    private LinearLayout content;
    private Button[] tabButtons;
    private int currentTab = 0;

    private final String[] ITEMS = {
            "Анти-жир", "Батарейка", "Гвозди", "Грязная тряпка", "Заколка",
            "Замороженное мясо", "Зарядка", "Ключ", "Ключ от аптечки",
            "Ключи от машины", "Лампочка", "Молоток", "Отвёртка", "Снотворное",
            "Спрей от насекомых", "Стиральный порошок", "Ступенька", "Швабра", "Фонарик"
    };

    private interface ResultCallback { void done(boolean ok); }

    private final Runnable healthCheck = new Runnable() {
        @Override public void run() {
            if (shuttingDown || bubble == null) return;
            new Thread(() -> {
                boolean ok = pingBridge(650);
                main.post(() -> {
                    if (shuttingDown || bubble == null) return;
                    if (ok) {
                        gameSeen = true;
                        missedPings = 0;
                    } else if (gameSeen) {
                        missedPings++;
                        if (missedPings >= 3) {
                            shutdownOverlay();
                            return;
                        }
                    }
                    main.postDelayed(healthCheck, HEALTH_DELAY);
                });
            }, "LamboB1Health").start();
        }
    };

    @Override public void onCreate() {
        super.onCreate();
        prefs = getSharedPreferences("lambocheats_b1", MODE_PRIVATE);
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        createChannel();
        startForeground(101, notification());
        showBubble();
        main.postDelayed(healthCheck, 1500L);
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (!shuttingDown && bubble == null) showBubble();
        return START_NOT_STICKY;
    }

    @Override public IBinder onBind(Intent intent) { return null; }

    @Override public void onTaskRemoved(Intent rootIntent) {
        shutdownOverlay();
        super.onTaskRemoved(rootIntent);
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel c = new NotificationChannel(CHANNEL, "LamboCheats B1", NotificationManager.IMPORTANCE_LOW);
            c.setDescription("LamboCheats B1 in-game menu");
            getSystemService(NotificationManager.class).createNotificationChannel(c);
        }
    }

    private Notification notification() {
        Intent open = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 101, open,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0));
        Notification.Builder b = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, CHANNEL) : new Notification.Builder(this);
        return b.setSmallIcon(android.R.drawable.ic_menu_manage)
                .setContentTitle("LamboCheats B1")
                .setContentText("LC — открыть/скрыть меню")
                .setContentIntent(pi)
                .setOngoing(true)
                .build();
    }

    private void showBubble() {
        if (bubble != null || shuttingDown) return;
        bubble = new TextView(this);
        bubble.setText("LC");
        bubble.setTextColor(Color.WHITE);
        bubble.setTextSize(17);
        bubble.getPaint().setFakeBoldText(true);
        bubble.setGravity(Gravity.CENTER);
        bubble.setBackground(gradient(0xFF8D4CFF, 0xFF3E7BFF, 999, 0xAFFFFFFF));

        bubbleParams = new WindowManager.LayoutParams(dp(56), dp(56),
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        bubbleParams.gravity = Gravity.TOP | Gravity.RIGHT;
        bubbleParams.x = dp(14);
        bubbleParams.y = dp(105);
        wm.addView(bubble, bubbleParams);
        bubble.setOnClickListener(v -> togglePanel());
        bubble.setOnTouchListener(new View.OnTouchListener() {
            float sx, sy; int ox, oy; boolean moved;
            @Override public boolean onTouch(View v, MotionEvent e) {
                if (e.getActionMasked() == MotionEvent.ACTION_DOWN) {
                    sx=e.getRawX(); sy=e.getRawY(); ox=bubbleParams.x; oy=bubbleParams.y; moved=false;
                    return false;
                }
                if (e.getActionMasked() == MotionEvent.ACTION_MOVE) {
                    float dx=e.getRawX()-sx, dy=e.getRawY()-sy;
                    if (Math.abs(dx)>10 || Math.abs(dy)>10) {
                        moved=true; bubbleParams.x=ox-(int)dx; bubbleParams.y=oy+(int)dy;
                        try { wm.updateViewLayout(bubble,bubbleParams); } catch(Exception ignored) {}
                        return true;
                    }
                }
                return moved;
            }
        });
    }

    private void togglePanel() { if (panel == null) showPanel(); else hidePanel(); }

    private void showPanel() {
        if (panel != null || shuttingDown) return;
        LinearLayout outer = new LinearLayout(this);
        outer.setOrientation(LinearLayout.VERTICAL);
        outer.setPadding(dp(8), dp(7), dp(8), dp(8));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xF313161D); bg.setCornerRadius(dp(13)); bg.setStroke(dp(1), 0xFF555B68);
        outer.setBackground(bg);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL); header.setGravity(Gravity.CENTER_VERTICAL);
        TextView logo = label("LC", 23, 0xFF9E67FF); logo.getPaint().setFakeBoldText(true);
        header.addView(logo, new LinearLayout.LayoutParams(dp(48), dp(42)));
        TextView title = label("LamboCheats B1", 18, Color.WHITE); title.getPaint().setFakeBoldText(true);
        header.addView(title, new LinearLayout.LayoutParams(0, dp(42), 1f));
        Button hide = compactButton("◉ Скрыть");
        header.addView(hide, new LinearLayout.LayoutParams(dp(105), dp(38))); hide.setOnClickListener(v -> hidePanel());
        Button exit = compactButton("✕ Выйти");
        LinearLayout.LayoutParams ep = new LinearLayout.LayoutParams(dp(105), dp(38)); ep.setMargins(dp(6),0,0,0);
        header.addView(exit, ep); exit.setOnClickListener(v -> shutdownOverlay());
        outer.addView(header, new LinearLayout.LayoutParams(-1, dp(46)));

        LinearLayout tabs = new LinearLayout(this);
        tabs.setOrientation(LinearLayout.HORIZONTAL);
        String[] names={"ПРЕДМЕТЫ","MODES","NPC","TRANSPORT"};
        tabButtons=new Button[names.length];
        for(int i=0;i<names.length;i++){
            final int index=i; Button b=tabButton(names[i]); tabButtons[i]=b;
            LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(0,dp(42),1f); if(i>0) tp.setMargins(dp(3),0,0,0);
            tabs.addView(b,tp); b.setOnClickListener(v->selectTab(index));
        }
        outer.addView(tabs,new LinearLayout.LayoutParams(-1,dp(44)));

        ScrollView scroll=new ScrollView(this); scroll.setFillViewport(true);
        content=new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL); content.setPadding(dp(4),dp(6),dp(4),dp(6));
        scroll.addView(content,new ScrollView.LayoutParams(-1,-2));
        outer.addView(scroll,new LinearLayout.LayoutParams(-1,0,1f));

        panel=outer;
        int screenW=getResources().getDisplayMetrics().widthPixels;
        int screenH=getResources().getDisplayMetrics().heightPixels;
        int w=Math.min((int)(screenW*0.90f),dp(760));
        int h=Math.min((int)(screenH*0.88f),dp(520));
        WindowManager.LayoutParams p=new WindowManager.LayoutParams(w,h,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        p.gravity=Gravity.CENTER;
        wm.addView(panel,p);
        selectTab(currentTab);
    }

    private void selectTab(int index){
        currentTab=index;
        if(content==null) return;
        content.removeAllViews();
        for(int i=0;i<tabButtons.length;i++) styleTab(tabButtons[i],i==index);
        if(index==0) buildItems();
        else if(index==1) buildModes();
        else if(index==2) buildNpc();
        else buildTransport();
    }

    private void buildItems(){
        section("ПРЕДМЕТЫ", "Нажми на предмет — он выдаётся через настоящий SpawnItems игры.");
        for(int i=0;i<ITEMS.length;i++){
            final int idx=i; Button b=actionButton((i+1)+".  "+ITEMS[i]);
            content.addView(b,lp(dp(46))); b.setOnClickListener(v->sendCommand("ITEM:"+idx,"Выдано: "+ITEMS[idx],null));
        }
    }

    private void buildModes(){
        section("MODES", "Вторая вкладка: режимы игрока и игровые действия.");
        content.addView(toggle("fly","Fly","Свободный полёт","FLY"),lp(dp(60)));
        content.addView(toggle("noclip","Noclip","Проходить сквозь стены (с Fly)","NOCLIP"),lp(dp(60)));
        content.addView(toggle("tyson","Режим Тайсона","NPC перестают тебя ловить","TYSON"),lp(dp(60)));

        Button mom=actionButton("👩 Играть за маму  [BETA]");
        content.addView(mom,lp(dp(48))); mom.setOnClickListener(v->sendCommand("PLAYMOM","Режим мамы вызван",null));
        Button dev=actionButton("🛠 Открыть встроенный Menu Mode игры");
        content.addView(dev,lp(dp(48))); dev.setOnClickListener(v->sendCommand("MENU","Menu Mode открыт",null));

        section("ДВЕРИ / ДЕЙСТВИЯ", "Старые функции B1 тоже оставлены.");
        content.addView(action("🔔 Открыть входную дверь","FRONT"),lp(dp(46)));
        content.addView(action("⬇ Открыть дверь в подвал","BASEMENT"),lp(dp(46)));
        content.addView(action("🔐 Открыть сейф","SAFE"),lp(dp(46)));
        content.addView(action("🚪 Открыть потайник","SECRET"),lp(dp(46)));
        content.addView(action("😡 Разозлить маму","MOM"),lp(dp(46)));
        content.addView(action("😡 Разозлить папу","DAD"),lp(dp(46)));
    }

    private void buildNpc(){
        section("NPC • X / Y / Z", "Меняй размер отдельно по каждой оси. Диапазон 0.25–5.0.");
        content.addView(npcCard("Мама","MOM"),lpWrap());
        content.addView(npcCard("Папа","DAD"),lpWrap());
        content.addView(npcCard("Собака","DOG"),lpWrap());
        content.addView(npcCard("Андрей","ANDREW"),lpWrap());
    }

    private View npcCard(String title,String code){
        LinearLayout card=new LinearLayout(this); card.setOrientation(LinearLayout.VERTICAL); card.setPadding(dp(10),dp(7),dp(10),dp(7)); styleRow(card);
        TextView t=label(title,15,Color.WHITE); t.getPaint().setFakeBoldText(true); card.addView(t,new LinearLayout.LayoutParams(-1,dp(30)));
        float[] values={1f,1f,1f};
        LinearLayout axes=new LinearLayout(this); axes.setOrientation(LinearLayout.HORIZONTAL);
        String[] names={"X","Y","Z"};
        for(int i=0;i<3;i++){
            final int axis=i;
            LinearLayout col=new LinearLayout(this); col.setGravity(Gravity.CENTER); col.setOrientation(LinearLayout.HORIZONTAL);
            TextView a=label(names[i],13,0xFFAEB7C9); col.addView(a,new LinearLayout.LayoutParams(dp(20),dp(34)));
            Button minus=miniButton("−"); col.addView(minus,new LinearLayout.LayoutParams(dp(38),dp(34)));
            TextView val=label("1.00",12,Color.WHITE); val.setGravity(Gravity.CENTER); col.addView(val,new LinearLayout.LayoutParams(dp(50),dp(34)));
            Button plus=miniButton("+"); col.addView(plus,new LinearLayout.LayoutParams(dp(38),dp(34)));
            View.OnClickListener update=v->{
                values[axis]+=(v==plus?0.25f:-0.25f);
                if(values[axis]<0.25f) values[axis]=0.25f; if(values[axis]>5f) values[axis]=5f;
                val.setText(String.format(Locale.US,"%.2f",values[axis]));
                String cmd=String.format(Locale.US,"SCALE:%s:%.2f:%.2f:%.2f",code,values[0],values[1],values[2]);
                sendCommand(cmd,title+" XYZ обновлён",null);
            };
            minus.setOnClickListener(update); plus.setOnClickListener(update);
            axes.addView(col,new LinearLayout.LayoutParams(0,dp(38),1f));
        }
        card.addView(axes,new LinearLayout.LayoutParams(-1,dp(40)));
        Button reset=miniButton("Reset XYZ = 1.0");
        card.addView(reset,new LinearLayout.LayoutParams(-1,dp(34)));
        reset.setOnClickListener(v->{ values[0]=values[1]=values[2]=1f; sendCommand("SCALE:"+code+":1:1:1",title+" размер сброшен",ok->{ if(ok) selectTab(2); }); });
        return card;
    }

    private void buildTransport(){
        section("TRANSPORT", "B1 вызывает реальные SpawnPrefab-компоненты, которые уже лежат в игре.");
        content.addView(transportButton("🚗 Машина","SPAWN:CAR"),lp(dp(58)));
        content.addView(transportButton("🚐 Фургон","SPAWN:VAN"),lp(dp(58)));
        content.addView(transportButton("🚲 Велосипед","SPAWN:BIKE"),lp(dp(58)));
        content.addView(transportButton("🛴 Самокат","SPAWN:SCOOTER"),lp(dp(58)));
        TextView note=label("Если конкретный транспорт в этой сцене не загружен, B1 вернёт FAIL вместо фальшивого «успеха».",11,0xFF9AA5B8);
        content.addView(note,lp(dp(52)));
    }

    private View transportButton(String text,String cmd){
        Button b=actionButton(text+"   • SPAWN"); b.setOnClickListener(v->sendCommand(cmd,text+" заспавнен",null)); return b;
    }

    private View toggle(String key,String title,String subtitle,String commandBase){
        LinearLayout row=new LinearLayout(this); row.setOrientation(LinearLayout.VERTICAL); row.setGravity(Gravity.CENTER_VERTICAL); row.setPadding(dp(12),dp(4),dp(12),dp(4)); styleRow(row);
        TextView mainText=label("",15,Color.WHITE); TextView sub=label(subtitle,10,0xFF9AA5B8);
        row.addView(mainText,new LinearLayout.LayoutParams(-1,dp(28))); row.addView(sub,new LinearLayout.LayoutParams(-1,dp(22)));
        Runnable paint=()->mainText.setText((prefs.getBoolean(key,false)?"● ON    ":"○ OFF   ")+title); paint.run();
        row.setOnClickListener(v->{ boolean desired=!prefs.getBoolean(key,false); row.setEnabled(false);
            sendCommand(commandBase+":"+(desired?"1":"0"),title+(desired?" включён":" выключен"),ok->{ row.setEnabled(true); if(ok) prefs.edit().putBoolean(key,desired).apply(); paint.run(); });
        });
        return row;
    }

    private View action(String text,String command){ Button b=actionButton(text); b.setOnClickListener(v->sendCommand(command,text,null)); return b; }

    private void section(String title,String subtitle){
        TextView h=label(title,16,Color.WHITE); h.getPaint().setFakeBoldText(true); content.addView(h,lp(dp(32)));
        TextView s=label(subtitle,10,0xFF909BAD); content.addView(s,lp(dp(34)));
    }

    private boolean pingBridge(int timeoutMs){
        try(Socket s=new Socket()){
            s.connect(new InetSocketAddress("127.0.0.1",PORT),timeoutMs); s.setSoTimeout(timeoutMs);
            OutputStream out=s.getOutputStream(); out.write("PING".getBytes(StandardCharsets.UTF_8)); out.flush();
            byte[] buf=new byte[32]; int n=s.getInputStream().read(buf);
            return n>0 && "PONGB1".equals(new String(buf,0,n,StandardCharsets.UTF_8).trim());
        }catch(Exception ignored){ return false; }
    }

    private void sendCommand(String command,String friendly,ResultCallback callback){
        new Thread(()->{
            String result; boolean ok=false;
            try(Socket s=new Socket()){
                s.connect(new InetSocketAddress("127.0.0.1",PORT),1200); s.setSoTimeout(3800);
                OutputStream out=s.getOutputStream(); out.write(command.getBytes(StandardCharsets.UTF_8)); out.flush();
                InputStream in=s.getInputStream(); byte[] buf=new byte[64]; int n=in.read(buf);
                String code=n>0?new String(buf,0,n,StandardCharsets.UTF_8).trim():"";
                if("PONGB1".equals(code)){ result="Связь B1 с игрой есть ❤️"; ok=true; }
                else if("OK".equals(code)){ result=friendly+" ❤️"; ok=true; }
                else if("NOT_READY".equals(code)) result="Игра ещё загружается — зайди в игровую сцену и повтори 🥶";
                else if("FAIL".equals(code)) result="Связь есть, но объект/функция в этой сцене не найдены";
                else if("BUSY".equals(code)) result="B1 выполняет прошлую команду — нажми ещё раз через секунду";
                else if("BAD_CMD".equals(code)) result="B1 не знает эту команду";
                else result="Ответ SchoolBoy B1: "+code;
            }catch(Exception e){ result="Нет связи с SchoolBoy runaway B1"; }
            final String r=result; final boolean success=ok;
            main.post(()->{ if(!shuttingDown) Toast.makeText(this,r,Toast.LENGTH_LONG).show(); if(callback!=null) callback.done(success); });
        },"LamboB1Command").start();
    }

    private Button tabButton(String text){ Button b=compactButton(text); b.setTextSize(11); return b; }
    private void styleTab(Button b,boolean selected){ GradientDrawable d=new GradientDrawable(); d.setColor(selected?0xFF342A4D:0xFF20242B); d.setCornerRadius(dp(5)); d.setStroke(dp(selected?2:1),selected?0xFF985CFF:0xFF444A55); b.setBackground(d); b.setTextColor(selected?Color.WHITE:0xFFD2D5DB); }
    private GradientDrawable gradient(int c1,int c2,float radius,int stroke){ GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{c1,c2}); d.setCornerRadius(radius); d.setStroke(dp(1),stroke); return d; }
    private void styleRow(View v){ GradientDrawable d=new GradientDrawable(); d.setColor(0xFF242830); d.setCornerRadius(dp(8)); d.setStroke(dp(1),0xFF4B515D); v.setBackground(d); }
    private Button actionButton(String text){ Button b=new Button(this); b.setText(text); b.setAllCaps(false); b.setTextColor(Color.WHITE); b.setTextSize(13); styleRow(b); return b; }
    private Button compactButton(String text){ Button b=actionButton(text); b.setTextSize(11); return b; }
    private Button miniButton(String text){ Button b=compactButton(text); b.setPadding(dp(2),0,dp(2),0); return b; }
    private TextView label(String text,int sp,int color){ TextView t=new TextView(this); t.setText(text); t.setTextSize(sp); t.setTextColor(color); t.setGravity(Gravity.CENTER_VERTICAL); return t; }
    private LinearLayout.LayoutParams lp(int h){ LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,h); p.setMargins(0,dp(3),0,dp(3)); return p; }
    private LinearLayout.LayoutParams lpWrap(){ LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2); p.setMargins(0,dp(4),0,dp(4)); return p; }

    private void hidePanel(){ if(panel!=null){ try{wm.removeView(panel);}catch(Exception ignored){} panel=null; content=null; tabButtons=null; } }
    private void removeBubble(){ if(bubble!=null){ try{wm.removeView(bubble);}catch(Exception ignored){} bubble=null; } }
    private void shutdownOverlay(){ if(shuttingDown) return; shuttingDown=true; main.removeCallbacks(healthCheck); hidePanel(); removeBubble(); try{stopForeground(true);}catch(Exception ignored){} stopSelf(); }

    @Override public void onDestroy(){ shuttingDown=true; main.removeCallbacksAndMessages(null); hidePanel(); removeBubble(); super.onDestroy(); }
    private int dp(float n){ return Math.round(n*getResources().getDisplayMetrics().density); }
}
