package com.kupchino.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class MainActivity extends Activity {
    private final int[] birdColors = {0xFFFFD728,0xFFFF5D73,0xFF4FD1FF,0xFF7EE787,0xFFC77DFF,0xFFFFFFFF};
    private final int[] beakColors = {0xFFFF7814,0xFFFFFF4A,0xFFFF4D4D,0xFF73E6FF,0xFFFFFFFF};
    private final int[] driveFpsOptions = {30,45,60,75,90,120,144,165,240};
    private int birdIndex=0, beakIndex=0, hatIndex=0;
    private final CalmMusic calmMusic = new CalmMusic();
    private boolean musicEnabled=true;
    private boolean vibrationEnabled=true;
    private boolean keepScreenOn=false;
    private boolean fullscreenEnabled=false;
    private boolean driveVsyncEnabled=true;
    private int driveFpsLimit=120;
    private DrivePerformance3DView activeDrive;
    private android.content.SharedPreferences accountPrefs;
    private android.content.SharedPreferences settingsPrefs;

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        settingsPrefs=getSharedPreferences("settings",MODE_PRIVATE);
        accountPrefs=getSharedPreferences("kupchino_account",MODE_PRIVATE);
        musicEnabled=settingsPrefs.getBoolean("music",true);
        vibrationEnabled=settingsPrefs.getBoolean("vibration",true);
        keepScreenOn=settingsPrefs.getBoolean("keep_screen_on",false);
        fullscreenEnabled=settingsPrefs.getBoolean("fullscreen",false);
        driveVsyncEnabled=settingsPrefs.getBoolean("drive_vsync",true);
        driveFpsLimit=settingsPrefs.getInt("drive_fps_limit",120);
        driveFpsLimit=Math.max(30,Math.min(240,driveFpsLimit));
        applyWindowSettings();
        if(musicEnabled) calmMusic.start();
        showHome();
    }

    private Button btn(String text){
        Button b=new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextSize(15);
        b.setHapticFeedbackEnabled(vibrationEnabled);
        return b;
    }

    private LinearLayout base(){
        LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(26,26,26,26);
        root.setBackgroundColor(0xFF080B10);
        return root;
    }

    private void addButton(LinearLayout root, Button b){
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);
        p.setMargins(0,10,0,0);
        root.addView(b,p);
    }

    private TextView title(String text){
        TextView t=new TextView(this);
        t.setText(text);
        t.setTextColor(Color.WHITE);
        t.setTextSize(28);
        t.setGravity(Gravity.CENTER);
        t.getPaint().setFakeBoldText(true);
        t.setPadding(0,8,0,12);
        return t;
    }

    private void stopActiveDrive(){
        if(activeDrive!=null){
            try{ activeDrive.onPause(); }catch(Exception ignored){}
            activeDrive=null;
        }
        WindowManager.LayoutParams lp=getWindow().getAttributes();
        lp.preferredRefreshRate=0f;
        getWindow().setAttributes(lp);
    }

    private void showHome(){
        stopActiveDrive();
        LinearLayout root=base();
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(title("КУПЧИНО 🥶❤️"),new LinearLayout.LayoutParams(-1,-2));

        ImageView image=new ImageView(this);
        image.setImageResource(R.drawable.kupchino_icon);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        image.setAdjustViewBounds(true);
        LinearLayout.LayoutParams ip=new LinearLayout.LayoutParams(-1,0,1f);
        ip.setMargins(0,12,0,12);
        root.addView(image,ip);

        TextView sub=new TextView(this);
        sub.setText("легендарное место 🤑\n+ KUPCHINO DRIVE 3D 🚗\nMAX 2048 • V-SYNC • ДО 240 FPS ✅");
        sub.setTextColor(Color.WHITE);
        sub.setTextSize(18);
        sub.setGravity(Gravity.CENTER);
        root.addView(sub,new LinearLayout.LayoutParams(-1,-2));

        Button drive=btn("🚗 KUPCHINO DRIVE 3D");
        drive.setTextSize(19);
        Button play=btn("🎮 Игры Купчино");
        Button account=btn(accountPrefs.getBoolean("registered",false)?
                "👤 "+accountPrefs.getString("username","Игрок")+" ✅":"👤 Зарегистрировать аккаунт");
        Button settings=btn("⚙ Настройки PRO");

        drive.setOnClickListener(v->showDrive3D());
        play.setOnClickListener(v->showGamesMenu());
        account.setOnClickListener(v->{ if(accountPrefs.getBoolean("registered",false)) showAccountInfo(); else showRegister(); });
        settings.setOnClickListener(v->showSettings());

        addButton(root,drive);
        addButton(root,play);
        addButton(root,account);
        addButton(root,settings);
        setContentView(root);
    }

    private void setDriveButtonHold(Button button, java.util.function.Consumer<Boolean> setter){
        button.setOnTouchListener((v,e)->{
            int action=e.getActionMasked();
            boolean down=action!=MotionEvent.ACTION_UP&&action!=MotionEvent.ACTION_CANCEL;
            setter.accept(down);
            return true;
        });
    }

    private void showDrive3D(){
        stopActiveDrive();

        WindowManager.LayoutParams lp=getWindow().getAttributes();
        lp.preferredRefreshRate=(float)driveFpsLimit;
        getWindow().setAttributes(lp);

        LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.BLACK);

        LinearLayout bar=new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(8,8,8,8);
        bar.setBackgroundColor(0xFF151A20);

        Button back=btn("← Купчино");
        TextView label=new TextView(this);
        label.setText("🚗 DRIVE MAX 2048 • "+driveFpsLimit+" FPS • V-SYNC "+(driveVsyncEnabled?"ON":"OFF"));
        label.setTextColor(Color.WHITE);
        label.setGravity(Gravity.CENTER);
        label.setTextSize(13);
        label.getPaint().setFakeBoldText(true);

        activeDrive=new DrivePerformance3DView(this);
        activeDrive.setPerformance(driveFpsLimit,driveVsyncEnabled);
        DrivePerformance3DView driveView=activeDrive;

        back.setOnClickListener(v->{
            driveView.onPause();
            activeDrive=null;
            showHome();
        });

        bar.addView(back,new LinearLayout.LayoutParams(0,-2,0.32f));
        bar.addView(label,new LinearLayout.LayoutParams(0,-1,0.68f));
        root.addView(bar,new LinearLayout.LayoutParams(-1,-2));
        root.addView(driveView,new LinearLayout.LayoutParams(-1,0,1f));

        LinearLayout missionBar=new LinearLayout(this);
        missionBar.setGravity(Gravity.CENTER);
        missionBar.setPadding(6,4,6,4);
        missionBar.setBackgroundColor(0xFF101419);
        Button free=btn("🆓 FREE DRIVE");
        Button mission=btn("🅿 NEW MISSION");
        free.setOnClickListener(v->{
            driveView.setFreeMode(!driveView.isFreeMode());
            free.setText(driveView.isFreeMode()?"🆓 FREE: ON":"🆓 FREE DRIVE");
        });
        mission.setOnClickListener(v->{ driveView.newMission(); free.setText("🆓 FREE DRIVE"); });
        missionBar.addView(free,new LinearLayout.LayoutParams(0,-2,1f));
        missionBar.addView(mission,new LinearLayout.LayoutParams(0,-2,1f));
        root.addView(missionBar,new LinearLayout.LayoutParams(-1,-2));

        LinearLayout controls=new LinearLayout(this);
        controls.setGravity(Gravity.CENTER);
        controls.setPadding(6,4,6,8);
        controls.setBackgroundColor(0xFF0B0E12);
        Button left=btn("◀");
        Button brake=btn("BRAKE");
        Button gas=btn("GAS");
        Button right=btn("▶");
        left.setTextSize(20); right.setTextSize(20); gas.setTextSize(18); brake.setTextSize(18);
        setDriveButtonHold(left,driveView::setLeft);
        setDriveButtonHold(right,driveView::setRight);
        setDriveButtonHold(gas,driveView::setGas);
        setDriveButtonHold(brake,driveView::setBrake);
        LinearLayout.LayoutParams cw=new LinearLayout.LayoutParams(0,-2,1f);
        controls.addView(left,cw); controls.addView(brake,cw); controls.addView(gas,cw); controls.addView(right,cw);
        root.addView(controls,new LinearLayout.LayoutParams(-1,-2));

        setContentView(root);
    }

    private void applyWindowSettings(){
        if(keepScreenOn) getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        else getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if(fullscreenEnabled) getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        else getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    private TextView section(String text){
        TextView t=new TextView(this);
        t.setText(text);
        t.setTextColor(0xFF8ED8FF);
        t.setTextSize(17);
        t.getPaint().setFakeBoldText(true);
        t.setPadding(0,20,0,6);
        return t;
    }

    private void showFpsPicker(){
        String[] labels=new String[driveFpsOptions.length];
        int selected=0;
        for(int i=0;i<driveFpsOptions.length;i++){
            labels[i]=driveFpsOptions[i]+" FPS"+(driveFpsOptions[i]==240?" 🚀":"");
            if(driveFpsOptions[i]==driveFpsLimit) selected=i;
        }
        final int checked=selected;
        new AlertDialog.Builder(this)
            .setTitle("🎯 Ограничение FPS")
            .setSingleChoiceItems(labels,checked,(dialog,which)->{
                driveFpsLimit=driveFpsOptions[which];
                settingsPrefs.edit().putInt("drive_fps_limit",driveFpsLimit).apply();
                dialog.dismiss();
                showSettings();
            })
            .setNegativeButton("Отмена",null)
            .show();
    }

    private void showSettings(){
        stopActiveDrive();
        LinearLayout root=base();
        root.addView(title("⚙ НАСТРОЙКИ KUPCHINO PRO"));

        root.addView(section("👤 АККАУНТ"));
        Button account=btn(accountPrefs.getBoolean("registered",false)?"👤 Аккаунт: "+accountPrefs.getString("username","Игрок")+" ✅":"👤 Зарегистрировать аккаунт");
        Button logout=btn(accountPrefs.getBoolean("registered",false)?"🚪 Выйти из аккаунта":"ℹ Локальный аккаунт");
        addButton(root,account); addButton(root,logout);

        root.addView(section("🎵 ЗВУК И ОТКЛИК"));
        Button music=btn(musicEnabled?"🎵 Музыка: ВКЛ":"🔇 Музыка: ВЫКЛ");
        Button vibration=btn(vibrationEnabled?"📳 Вибрация: ВКЛ":"📴 Вибрация: ВЫКЛ");
        addButton(root,music); addButton(root,vibration);

        root.addView(section("📱 ЭКРАН"));
        Button full=btn(fullscreenEnabled?"🖥 Полноэкранный режим: ВКЛ":"🖥 Полноэкранный режим: ВЫКЛ");
        Button keep=btn(keepScreenOn?"💡 Не выключать экран: ВКЛ":"💡 Не выключать экран: ВЫКЛ");
        addButton(root,full); addButton(root,keep);

        root.addView(section("🚗 KUPCHINO DRIVE MAX"));
        Button vsync=btn(driveVsyncEnabled?"🖥 V-Sync: ВКЛ ✅":"🖥 V-Sync: ВЫКЛ");
        Button fps=btn("🎯 Лимит FPS: "+driveFpsLimit+" (30–240)");
        Button drive=btn("🚗 Играть в Kupchino Drive 3D");
        addButton(root,vsync); addButton(root,fps); addButton(root,drive);

        Button back=btn("← Назад");
        addButton(root,back);

        account.setOnClickListener(v->{ if(accountPrefs.getBoolean("registered",false)) showAccountInfo(); else showRegister(); });
        logout.setOnClickListener(v->{
            if(accountPrefs.getBoolean("registered",false)){ accountPrefs.edit().clear().apply(); showSettings(); }
            else new AlertDialog.Builder(this).setTitle("Локальный аккаунт").setMessage("Профиль хранится только на этом устройстве.").setPositiveButton("ОК",null).show();
        });
        music.setOnClickListener(v->{ musicEnabled=!musicEnabled; settingsPrefs.edit().putBoolean("music",musicEnabled).apply(); if(musicEnabled) calmMusic.start(); else calmMusic.stop(); showSettings(); });
        vibration.setOnClickListener(v->{ vibrationEnabled=!vibrationEnabled; settingsPrefs.edit().putBoolean("vibration",vibrationEnabled).apply(); showSettings(); });
        full.setOnClickListener(v->{ fullscreenEnabled=!fullscreenEnabled; settingsPrefs.edit().putBoolean("fullscreen",fullscreenEnabled).apply(); applyWindowSettings(); showSettings(); });
        keep.setOnClickListener(v->{ keepScreenOn=!keepScreenOn; settingsPrefs.edit().putBoolean("keep_screen_on",keepScreenOn).apply(); applyWindowSettings(); showSettings(); });
        vsync.setOnClickListener(v->{
            driveVsyncEnabled=!driveVsyncEnabled;
            settingsPrefs.edit().putBoolean("drive_vsync",driveVsyncEnabled).apply();
            showSettings();
        });
        fps.setOnClickListener(v->showFpsPicker());
        drive.setOnClickListener(v->showDrive3D());
        back.setOnClickListener(v->showHome());

        ScrollView scroll=new ScrollView(this);
        scroll.addView(root);
        setContentView(scroll);
    }

    private void showRegister(){
        LinearLayout box=new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(40,10,40,0);
        EditText name=new EditText(this);
        name.setHint("Имя игрока");
        EditText pin=new EditText(this);
        pin.setHint("PIN (4–8 цифр)");
        pin.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        box.addView(name); box.addView(pin);
        AlertDialog dlg=new AlertDialog.Builder(this)
            .setTitle("👤 Регистрация Купчино")
            .setMessage("Создай локальный профиль. Не используй пароль от Google или других сайтов.")
            .setView(box)
            .setPositiveButton("Зарегистрироваться",null)
            .setNegativeButton("Отмена",null)
            .create();
        dlg.setOnShowListener(x->dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            String n=name.getText().toString().trim();
            String p=pin.getText().toString().trim();
            if(n.length()<2){ name.setError("Минимум 2 символа"); return; }
            if(p.length()<4||p.length()>8){ pin.setError("Нужно 4–8 цифр"); return; }
            accountPrefs.edit().putBoolean("registered",true).putString("username",n).putString("pin",p).apply();
            dlg.dismiss();
            new AlertDialog.Builder(this).setTitle("SUCCESS ✅").setMessage("Аккаунт "+n+" создан ❤️").setPositiveButton("ОК",(d,w)->showHome()).show();
        }));
        dlg.show();
    }

    private void showAccountInfo(){
        String n=accountPrefs.getString("username","Игрок");
        new AlertDialog.Builder(this).setTitle("👤 "+n).setMessage("Локальный аккаунт Купчино активен ✅").setPositiveButton("ОК",null).show();
    }

    private void showGamesMenu(){
        stopActiveDrive();
        LinearLayout root=base();
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(title("🎮 ИГРЫ КУПЧИНО"));
        Button mines=btn("1. 💣 Сапёрное Купчино");
        Button flappy=btn("2. 🐦 Флаппи Купчино");
        Button tetris=btn("3. 🧱 Тетрипчино");
        Button clicker=btn("4. 🐈 Кликер купчинности");
        Button drive=btn("5. 🚗 KUPCHINO DRIVE 3D");
        Button back=btn("← Назад");
        mines.setOnClickListener(v->showMinesweeper());
        flappy.setOnClickListener(v->showGame());
        tetris.setOnClickListener(v->showTetris());
        clicker.setOnClickListener(v->showClicker());
        drive.setOnClickListener(v->showDrive3D());
        back.setOnClickListener(v->showHome());
        addButton(root,mines); addButton(root,flappy); addButton(root,tetris); addButton(root,clicker); addButton(root,drive); addButton(root,back);
        setContentView(root);
    }

    private void showClicker(){
        LinearLayout root=base();
        Button back=btn("← Игры"); back.setOnClickListener(v->showGamesMenu());
        root.addView(back,new LinearLayout.LayoutParams(-1,-2));
        root.addView(new ClickerView(this),new LinearLayout.LayoutParams(-1,0,1f));
        setContentView(root);
    }

    private void showMinesweeper(){
        LinearLayout root=base();
        Button back=btn("← Игры"); back.setOnClickListener(v->showGamesMenu());
        root.addView(back,new LinearLayout.LayoutParams(-1,-2));
        root.addView(new MinesweeperView(this),new LinearLayout.LayoutParams(-1,0,1f));
        setContentView(root);
    }

    private void showTetris(){
        LinearLayout root=base();
        Button back=btn("← Игры"); back.setOnClickListener(v->showGamesMenu());
        root.addView(back,new LinearLayout.LayoutParams(-1,-2));
        TetrisView t=new TetrisView(this);
        root.addView(t,new LinearLayout.LayoutParams(-1,0,1f));
        LinearLayout controls=new LinearLayout(this);
        controls.setGravity(Gravity.CENTER);
        Button left=btn("◀"), rotate=btn("⟳"), down=btn("▼"), right=btn("▶"), drop=btn("⤓"), restart=btn("↻");
        left.setOnClickListener(v->t.left()); rotate.setOnClickListener(v->t.rotate()); down.setOnClickListener(v->t.down()); right.setOnClickListener(v->t.right()); drop.setOnClickListener(v->t.drop()); restart.setOnClickListener(v->t.restart());
        LinearLayout.LayoutParams w=new LinearLayout.LayoutParams(0,-2,1f);
        controls.addView(left,w); controls.addView(rotate,w); controls.addView(down,w); controls.addView(right,w); controls.addView(drop,w); controls.addView(restart,w);
        root.addView(controls,new LinearLayout.LayoutParams(-1,-2));
        setContentView(root);
    }

    private void showGame(){
        LinearLayout root=base();
        GameView game=new GameView(this);
        game.setBirdColor(birdColors[birdIndex]); game.setBeakColor(beakColors[beakIndex]); game.setHat(hatIndex);
        LinearLayout bar=new LinearLayout(this);
        Button back=btn("⌂"), pause=btn("⏸"), bird=btn("Птица 🎨"), beak=btn("Клюв 🎨"), hat=btn("Шляпа");
        back.setOnClickListener(v->showGamesMenu());
        pause.setOnClickListener(v->{
            game.pauseGame();
            new AlertDialog.Builder(this).setTitle("⏸ FLAPPY КУПЧИНО").setMessage("Пауза").setPositiveButton("▶ Продолжить",(d,w)->game.resumeGame()).setNegativeButton("↻ Заново",(d,w)->game.restartGame()).setOnCancelListener(d->game.resumeGame()).show();
        });
        bird.setOnClickListener(v->{ birdIndex=(birdIndex+1)%birdColors.length; game.setBirdColor(birdColors[birdIndex]); });
        beak.setOnClickListener(v->{ beakIndex=(beakIndex+1)%beakColors.length; game.setBeakColor(beakColors[beakIndex]); });
        hat.setOnClickListener(v->{ hatIndex=(hatIndex+1)%4; game.setHat(hatIndex); });
        LinearLayout.LayoutParams w=new LinearLayout.LayoutParams(0,-2,1f);
        bar.addView(back,w); bar.addView(pause,w); bar.addView(bird,w); bar.addView(beak,w); bar.addView(hat,w);
        root.addView(bar,new LinearLayout.LayoutParams(-1,-2));
        root.addView(game,new LinearLayout.LayoutParams(-1,0,1f));
        setContentView(root);
    }

    @Override protected void onPause(){
        if(activeDrive!=null) activeDrive.onPause();
        calmMusic.stop();
        super.onPause();
    }
    @Override protected void onResume(){
        super.onResume();
        if(activeDrive!=null) activeDrive.onResume();
        if(musicEnabled) calmMusic.start();
    }
    @Override protected void onDestroy(){
        if(activeDrive!=null) activeDrive.onPause();
        calmMusic.stop();
        super.onDestroy();
    }
}
