package com.kupchino.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.graphics.drawable.GradientDrawable;
import android.view.Window;
import android.view.WindowManager;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    private final int[] birdColors = {0xFFFFD728,0xFFFF5D73,0xFF4FD1FF,0xFF7EE787,0xFFC77DFF,0xFFFFFFFF};
    private final int[] beakColors = {0xFFFF7814,0xFFFFFF4A,0xFFFF4D4D,0xFF73E6FF,0xFFFFFFFF};
    private int birdIndex=0, beakIndex=0, hatIndex=0;
    private final CalmMusic calmMusic = new CalmMusic();
    private boolean musicEnabled = true;

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        musicEnabled=getSharedPreferences("settings",MODE_PRIVATE).getBoolean("music",true);
        if(musicEnabled) calmMusic.start();
        showHome();
    }

    private Button btn(String s){
        Button b=new Button(this);
        b.setText(s);
        b.setAllCaps(false);
        b.setTextSize(15);
        return b;
    }

    private void showHome(){
        LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(24,24,24,24);
        root.setBackgroundColor(Color.BLACK);

        TextView title=new TextView(this);
        title.setText("КУПЧИНО 🥶❤️");
        title.setTextColor(Color.WHITE);
        title.setTextSize(30);
        title.setGravity(Gravity.CENTER);
        root.addView(title,new LinearLayout.LayoutParams(-1,-2));

        ImageView image=new ImageView(this);
        image.setImageResource(com.kupchino.app.R.drawable.kupchino_icon);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        image.setAdjustViewBounds(true);
        LinearLayout.LayoutParams ip=new LinearLayout.LayoutParams(-1,0,1f);
        ip.setMargins(0,24,0,24);
        root.addView(image,ip);

        TextView text=new TextView(this);
        text.setText("легендарное место 🤑\n+ FLAPPY КУПЧИНО");
        text.setTextColor(Color.WHITE);
        text.setTextSize(20);
        text.setGravity(Gravity.CENTER);
        root.addView(text);

        Button play=btn("▶ Играть");
        Button settings=btn("⚙ Настройки");
        play.setTextSize(18);
        play.setOnClickListener(v->showGamesMenu());
        LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(-1,-2);
        bp.setMargins(0,20,0,0);
        root.addView(play,bp);
        LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,-2);
        sp.setMargins(0,10,0,0);
        root.addView(settings,sp);
        settings.setOnClickListener(v->showSettings());

        setContentView(root);
    }

    private void showSettings(){
        String status=musicEnabled?"ВКЛ":"ВЫКЛ";
        new AlertDialog.Builder(this)
            .setTitle("⚙ Настройки")
            .setMessage("Тихая музыка: "+status+"\nРаботает полностью без интернета.")
            .setPositiveButton(musicEnabled?"🔇 Выключить музыку":"🎵 Включить музыку",(d,w)->{
                musicEnabled=!musicEnabled;
                getSharedPreferences("settings",MODE_PRIVATE).edit().putBoolean("music",musicEnabled).apply();
                if(musicEnabled) calmMusic.start(); else calmMusic.stop();
            })
            .setNegativeButton("Назад",null)
            .show();
    }

    private void showGamesMenu(){
        LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(28,28,28,28);
        root.setBackgroundColor(Color.BLACK);

        TextView title=new TextView(this);
        title.setText("🎮 ИГРЫ КУПЧИНО");
        title.setTextColor(Color.WHITE);
        title.setTextSize(30);
        title.setGravity(Gravity.CENTER);
        root.addView(title,new LinearLayout.LayoutParams(-1,-2));

        Button mines=btn("1. 💣 Сапёрное Купчино");
        Button flappy=btn("2. 🐦 Флаппи Купчино");
        Button tetris=btn("3. 🧱 Тетрипчино");
        Button clicker=btn("4. 🐈 Кликер купчинности");
        Button back=btn("← Назад");

        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);
        p.setMargins(0,18,0,0);

        mines.setOnClickListener(v->showMinesweeper());
        flappy.setOnClickListener(v->showGame());
        tetris.setOnClickListener(v->showTetris());
        clicker.setOnClickListener(v->showClicker());
        back.setOnClickListener(v->showHome());

        root.addView(mines,p);
        root.addView(flappy,p);
        root.addView(tetris,p);
        root.addView(clicker,p);
        root.addView(back,p);
        setContentView(root);
    }

    private void showComingSoon(String name){
        new AlertDialog.Builder(this)
            .setTitle(name)
            .setMessage("Игра добавлена в меню. Полную версию сделаем следующим обновлением 🤑")
            .setPositiveButton("ОК", null)
            .show();
    }

    private void showClicker(){
        LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.BLACK);

        Button back=btn("← Игры");
        back.setOnClickListener(v->showGamesMenu());
        root.addView(back,new LinearLayout.LayoutParams(-1,-2));

        root.addView(new ClickerView(this),new LinearLayout.LayoutParams(-1,0,1f));
        setContentView(root);
    }

    private void showMinesweeper(){
        LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.BLACK);
        Button back=btn("← Игры");
        back.setOnClickListener(v->showGamesMenu());
        root.addView(back,new LinearLayout.LayoutParams(-1,-2));
        root.addView(new MinesweeperView(this),new LinearLayout.LayoutParams(-1,0,1f));
        setContentView(root);
    }

    private void showTetris(){
        LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.BLACK);

        Button back=btn("← Игры");
        back.setOnClickListener(v->showGamesMenu());
        root.addView(back,new LinearLayout.LayoutParams(-1,-2));

        TetrisView t=new TetrisView(this);
        root.addView(t,new LinearLayout.LayoutParams(-1,0,1f));

        LinearLayout dock=new LinearLayout(this);
        dock.setOrientation(LinearLayout.VERTICAL);
        dock.setGravity(Gravity.CENTER);
        dock.setPadding(12,20,12,72);
        dock.setBackgroundColor(0xFF2F333A);

        TextView dockTitle=new TextView(this);
        dockTitle.setText("УПРАВЛЕНИЕ ТЕТРИПЧИНО");
        dockTitle.setTextColor(Color.WHITE);
        dockTitle.setGravity(Gravity.CENTER);
        dockTitle.setTextSize(13);
        dockTitle.setPadding(0,0,0,8);
        dock.addView(dockTitle,new LinearLayout.LayoutParams(-1,-2));

        LinearLayout controls=new LinearLayout(this);
        controls.setGravity(Gravity.CENTER);
        Button left=btn("◀");
        Button rotate=btn("⟳");
        Button down=btn("▼");
        Button right=btn("▶");
        Button drop=btn("⤓");
        Button restart=btn("↻");
        left.setMinHeight(64); rotate.setMinHeight(64); down.setMinHeight(64); right.setMinHeight(64); drop.setMinHeight(64); restart.setMinHeight(64);
        left.setOnClickListener(v->t.left());
        rotate.setOnClickListener(v->t.rotate());
        down.setOnClickListener(v->t.down());
        right.setOnClickListener(v->t.right());
        drop.setOnClickListener(v->t.drop());
        restart.setOnClickListener(v->t.restart());
        LinearLayout.LayoutParams w=new LinearLayout.LayoutParams(0,-2,1f);
        controls.addView(left,w);controls.addView(rotate,w);controls.addView(down,w);
        controls.addView(right,w);controls.addView(drop,w);controls.addView(restart,w);
        dock.addView(controls,new LinearLayout.LayoutParams(-1,-2));

        root.addView(dock,new LinearLayout.LayoutParams(-1,-2));
        setContentView(root);
    }

    private void showGame(){
        LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.BLACK);

        GameView game=new GameView(this);
        game.setBirdColor(birdColors[birdIndex]);
        game.setBeakColor(beakColors[beakIndex]);
        game.setHat(hatIndex);

        LinearLayout bar=new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER);

        Button back=btn("⌂");
        Button pause=btn("⏸");
        Button bird=btn("Птица 🎨");
        Button beak=btn("Клюв 🎨");
        Button hat=btn("Шляпа: нет");

        back.setOnClickListener(v->showHome());
        pause.setOnClickListener(v->{
            game.pauseGame();
            showPauseMenu(game);
        });
        bird.setOnClickListener(v->{
            birdIndex=(birdIndex+1)%birdColors.length;
            game.setBirdColor(birdColors[birdIndex]);
        });
        beak.setOnClickListener(v->{
            beakIndex=(beakIndex+1)%beakColors.length;
            game.setBeakColor(beakColors[beakIndex]);
        });
        hat.setOnClickListener(v->{
            hatIndex=(hatIndex+1)%4;
            game.setHat(hatIndex);
            String[] n={"нет","кепка","шлем","Dorito"};
            hat.setText("Шляпа: "+n[hatIndex]);
        });

        LinearLayout.LayoutParams w=new LinearLayout.LayoutParams(0,-2,1f);
        bar.addView(back,w);
        bar.addView(pause,w);
        bar.addView(bird,w);
        bar.addView(beak,w);
        bar.addView(hat,w);

        root.addView(bar,new LinearLayout.LayoutParams(-1,-2));
        root.addView(game,new LinearLayout.LayoutParams(-1,0,1f));
        setContentView(root);
    }

    private void showPauseMenu(GameView game){
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setGravity(Gravity.CENTER_HORIZONTAL);
        panel.setPadding(34,28,34,28);

        GradientDrawable bg = new GradientDrawable();
        // Серый фон: 68% прозрачности = 32% непрозрачности.
        bg.setColor(Color.argb(82, 90, 90, 90));
        bg.setCornerRadius(32f);
        bg.setStroke(2, Color.argb(150,255,255,255));
        panel.setBackground(bg);

        TextView title = new TextView(this);
        title.setText("⏸ FLAPPY КУПЧИНО");
        title.setTextColor(Color.WHITE);
        title.setTextSize(26);
        title.setGravity(Gravity.CENTER);
        title.setShadowLayer(8,0,2,Color.BLACK);
        panel.addView(title,new LinearLayout.LayoutParams(-1,-2));

        TextView sub = new TextView(this);
        sub.setText("ПАУЗА • SAVE "+game.getActiveSlot()+" • BEST "+game.getBest());
        sub.setTextColor(0xFFE8E8E8);
        sub.setTextSize(14);
        sub.setGravity(Gravity.CENTER);
        sub.setPadding(0,6,0,18);
        panel.addView(sub,new LinearLayout.LayoutParams(-1,-2));

        Button resume=btn("▶  ПРОДОЛЖИТЬ");
        Button restart=btn("↻  ЗАНОВО");
        Button saves=btn("💾  СОХРАНЕНИЯ A / B / C");
        Button bird=btn("🐦  ЦВЕТ ПТИЧКИ 🎨");
        Button beak=btn("🟠  ЦВЕТ КЛЮВА 🎨");
        Button hat=btn("🧢  ГОЛОВНОЙ УБОР");
        Button home=btn("⌂  В МЕНЮ ИГР");

        Button[] buttons={resume,restart,saves,bird,beak,hat,home};
        for(Button b:buttons){
            b.setTextSize(16);
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);
            lp.setMargins(0,8,0,0);
            panel.addView(b,lp);
        }

        resume.setOnClickListener(v->{ dialog.dismiss(); game.resumeGame(); });
        restart.setOnClickListener(v->{ dialog.dismiss(); game.restartGame(); });
        saves.setOnClickListener(v->{ dialog.dismiss(); showSavesMenu(game); });
        bird.setOnClickListener(v->{
            birdIndex=(birdIndex+1)%birdColors.length;
            game.setBirdColor(birdColors[birdIndex]);
            bird.setText("🐦  ЦВЕТ ПТИЧКИ "+(birdIndex+1)+"/"+birdColors.length);
        });
        beak.setOnClickListener(v->{
            beakIndex=(beakIndex+1)%beakColors.length;
            game.setBeakColor(beakColors[beakIndex]);
            beak.setText("🟠  ЦВЕТ КЛЮВА "+(beakIndex+1)+"/"+beakColors.length);
        });
        hat.setOnClickListener(v->{
            hatIndex=(hatIndex+1)%4;
            game.setHat(hatIndex);
            String[] n={"нет","кепка","шлем","Dorito"};
            hat.setText("🧢  "+n[hatIndex]);
        });
        home.setOnClickListener(v->{ dialog.dismiss(); showGamesMenu(); });

        dialog.setContentView(panel);
        dialog.setCancelable(false);
        Window w=dialog.getWindow();
        if(w!=null){
            w.setBackgroundDrawableResource(android.R.color.transparent);
            WindowManager.LayoutParams lp=new WindowManager.LayoutParams();
            lp.copyFrom(w.getAttributes());
            lp.width=(int)(getResources().getDisplayMetrics().widthPixels*0.90f);
            lp.height=WindowManager.LayoutParams.WRAP_CONTENT;
            w.setAttributes(lp);
            w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams attrs=w.getAttributes();
            attrs.dimAmount=0.35f;
            w.setAttributes(attrs);
        }
        dialog.show();
    }

    private void showSavesMenu(GameView game){
        String[] slots = {"A","B","C"};
        String[] labels = {
            "A  • рекорд " + game.getSavedBest("A"),
            "B  • рекорд " + game.getSavedBest("B"),
            "C  • рекорд " + game.getSavedBest("C")
        };
        new AlertDialog.Builder(this)
            .setTitle("💾 Сохранения")
            .setMessage("Выбери слот A–C. Текущий результат сохранится вручную. После смерти рекорд сам не обновляется.")
            .setItems(labels, (d,which)->{
                game.saveToSlot(slots[which]);
                game.resumeGame();
            })
            .setNegativeButton("Назад", (d,w)->showPauseMenu(game))
            .setOnCancelListener(d->showPauseMenu(game))
            .show();
    }
    @Override protected void onPause(){
        calmMusic.stop();
        super.onPause();
    }

    @Override protected void onResume(){
        super.onResume();
        if(musicEnabled) calmMusic.start();
    }

    @Override protected void onDestroy(){
        calmMusic.stop();
        super.onDestroy();
    }
}
