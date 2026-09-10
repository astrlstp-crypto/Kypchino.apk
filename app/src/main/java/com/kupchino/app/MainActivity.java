package com.kupchino.app;

import android.app.Activity;
import android.app.AlertDialog;
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

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
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
        play.setTextSize(18);
        play.setOnClickListener(v->showGamesMenu());
        LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(-1,-2);
        bp.setMargins(0,20,0,0);
        root.addView(play,bp);

        setContentView(root);
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
        Button back=btn("← Назад");

        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);
        p.setMargins(0,18,0,0);

        mines.setOnClickListener(v->showComingSoon("💣 Сапёрное Купчино"));
        flappy.setOnClickListener(v->showGame());
        tetris.setOnClickListener(v->showComingSoon("🧱 Тетрипчино"));
        back.setOnClickListener(v->showGamesMenu());

        root.addView(mines,p);
        root.addView(flappy,p);
        root.addView(tetris,p);
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
        final String[] items = {"▶ Продолжить","↻ Заново","💾 Сохранения","⌂ В меню"};
        new AlertDialog.Builder(this)
            .setTitle("⏸ Пауза")
            .setItems(items, (d,which)->{
                if(which==0) game.resumeGame();
                else if(which==1) game.restartGame();
                else if(which==2) showSavesMenu(game);
                else showHome();
            })
            .setOnCancelListener(d->game.resumeGame())
            .show();
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
}
