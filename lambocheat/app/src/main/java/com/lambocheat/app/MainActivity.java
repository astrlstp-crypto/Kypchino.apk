package com.lambocheat.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final int OVERLAY_REQUEST = 1001;
    // B1 game keeps the V8 internal package so it installs as an update instead of duplicating the 230 MB game.
    private static final String B1_GAME = "com.LamboCheat8.SchoolBoyRunaway";
    private TextView status;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(buildUi());
    }

    private View buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(dp(22), dp(38), dp(22), dp(24));
        root.setBackgroundColor(Color.rgb(8, 9, 14));

        TextView logo = new TextView(this);
        logo.setText("LC"); logo.setTextSize(46); logo.setTextColor(0xFF9A63FF); logo.setGravity(Gravity.CENTER);
        logo.getPaint().setFakeBoldText(true);
        root.addView(logo, new LinearLayout.LayoutParams(-1, dp(72)));

        TextView title = new TextView(this);
        title.setText("LamboCheats B1"); title.setTextColor(Color.WHITE); title.setTextSize(30); title.setGravity(Gravity.CENTER);
        title.getPaint().setFakeBoldText(true);
        root.addView(title, new LinearLayout.LayoutParams(-1, dp(50)));

        TextView sub = new TextView(this);
        sub.setText("SchoolBoy runaway B1 • real in-game overlay");
        sub.setTextColor(Color.rgb(159, 169, 191)); sub.setTextSize(14); sub.setGravity(Gravity.CENTER);
        root.addView(sub, new LinearLayout.LayoutParams(-1, dp(44)));

        Button cheat = new Button(this);
        cheat.setText("CHEAT B1"); cheat.setTextColor(Color.WHITE); cheat.setTextSize(23); cheat.getPaint().setFakeBoldText(true); cheat.setAllCaps(false);
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[]{0xFF8D4CFF, 0xFF3E7BFF});
        bg.setCornerRadius(dp(22)); cheat.setBackground(bg);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, dp(70)); cp.setMargins(0, dp(22), 0, dp(12));
        root.addView(cheat, cp); cheat.setOnClickListener(v -> enableAndLaunch());

        status = new TextView(this);
        status.setText("CHEAT B1 → LC появится поверх SchoolBoy runaway B1");
        status.setTextColor(Color.rgb(153, 164, 187)); status.setTextSize(13); status.setGravity(Gravity.CENTER);
        root.addView(status, new LinearLayout.LayoutParams(-1, dp(62)));

        Button launch = secondary("Запустить SchoolBoy runaway B1");
        root.addView(launch, new LinearLayout.LayoutParams(-1, dp(54))); launch.setOnClickListener(v -> launchGame());

        Button stop = secondary("✕ Выключить LamboCheats B1 полностью");
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-1, dp(54)); sp.setMargins(0, dp(8), 0, 0);
        root.addView(stop, sp); stop.setOnClickListener(v -> {
            stopService(new Intent(this, CheatOverlayService.class));
            status.setText("LamboCheats B1 выключен — LC убран ❤️");
        });

        TextView note = new TextView(this);
        note.setText("В игре: ПРЕДМЕТЫ → MODES → NPC → TRANSPORT.\n" +
                "После закрытия игры B1 сам проверяет связь и убирает плавающий LC.");
        note.setTextColor(Color.rgb(125, 136, 158)); note.setTextSize(12); note.setPadding(0, dp(18), 0, 0);
        root.addView(note, new LinearLayout.LayoutParams(-1, -2));
        return root;
    }

    private Button secondary(String text) {
        Button b = new Button(this); b.setText(text); b.setTextColor(Color.WHITE); b.setTextSize(14); b.setAllCaps(false);
        GradientDrawable d = new GradientDrawable(); d.setColor(0xFF171B27); d.setStroke(dp(1), 0xFF343B50); d.setCornerRadius(dp(18)); b.setBackground(d);
        return b;
    }

    private void enableAndLaunch() {
        if (!Settings.canDrawOverlays(this)) {
            status.setText("Разреши LamboCheats B1 показываться поверх других приложений ❤️");
            startActivityForResult(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName())), OVERLAY_REQUEST);
            return;
        }
        Intent service = new Intent(this, CheatOverlayService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(service); else startService(service);
        status.setText("LamboCheats B1 запущен — открываю игру…");
        status.postDelayed(this::launchGame, 350);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == OVERLAY_REQUEST) {
            if (Settings.canDrawOverlays(this)) enableAndLaunch();
            else status.setText("Без разрешения «поверх других приложений» меню не появится.");
        }
    }

    private void launchGame() {
        Intent i = getPackageManager().getLaunchIntentForPackage(B1_GAME);
        if (i != null) { i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); startActivity(i); }
        else Toast.makeText(this, "SchoolBoy runaway B1 не установлена ❤️", Toast.LENGTH_LONG).show();
    }

    private int dp(float n) { return Math.round(n * getResources().getDisplayMetrics().density); }
}
