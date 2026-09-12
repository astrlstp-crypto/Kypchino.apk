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
    private static final String LAMBO_GAME = "com.LamboCheat8.SchoolBoyRunaway";
    private TextView status;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(buildUi());
    }

    private View buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(dp(22), dp(42), dp(22), dp(26));
        root.setBackgroundColor(Color.rgb(7, 8, 13));

        TextView logo = new TextView(this);
        logo.setText("🐈🔑"); logo.setTextSize(48); logo.setGravity(Gravity.CENTER);
        root.addView(logo, new LinearLayout.LayoutParams(-1, dp(76)));

        TextView title = new TextView(this);
        title.setText("LamboCheat V8"); title.setTextColor(Color.WHITE); title.setTextSize(30); title.setGravity(Gravity.CENTER);
        title.getPaint().setFakeBoldText(true);
        root.addView(title, new LinearLayout.LayoutParams(-1, dp(52)));

        TextView sub = new TextView(this);
        sub.setText("SchoolBoy LamboV8 • main-thread bridge");
        sub.setTextColor(Color.rgb(159, 169, 191)); sub.setTextSize(14); sub.setGravity(Gravity.CENTER);
        root.addView(sub, new LinearLayout.LayoutParams(-1, dp(48)));

        Button cheat = new Button(this);
        cheat.setText("CHEAT"); cheat.setTextColor(Color.WHITE); cheat.setTextSize(24); cheat.getPaint().setFakeBoldText(true); cheat.setAllCaps(false);
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[]{0xFF8D4CFF, 0xFF3E7BFF});
        bg.setCornerRadius(dp(22)); cheat.setBackground(bg);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, dp(72)); cp.setMargins(0, dp(24), 0, dp(12));
        root.addView(cheat, cp); cheat.setOnClickListener(v -> enableAndLaunch());

        status = new TextView(this);
        status.setText("Нажми CHEAT → разреши окно поверх приложений → SchoolBoy LamboV8 откроется сама");
        status.setTextColor(Color.rgb(153, 164, 187)); status.setTextSize(13); status.setGravity(Gravity.CENTER);
        root.addView(status, new LinearLayout.LayoutParams(-1, dp(70)));

        Button launch = secondary("Запустить SchoolBoy LamboV8");
        root.addView(launch, new LinearLayout.LayoutParams(-1, dp(56))); launch.setOnClickListener(v -> launchGame());

        TextView note = new TextView(this);
        note.setText("V8: проверка связи не вызывает игровые функции, а команды выполняются на игровом main thread — это фикс вылета V7.\n\n" +
                "T — открыть/скрыть меню • ✕ СКРЫТЬ — оставить только T • Выйти полностью — убрать всё.");
        note.setTextColor(Color.rgb(125, 136, 158)); note.setTextSize(12); note.setPadding(0, dp(20), 0, 0);
        root.addView(note, new LinearLayout.LayoutParams(-1, -2));
        return root;
    }

    private Button secondary(String text) {
        Button b = new Button(this); b.setText(text); b.setTextColor(Color.WHITE); b.setTextSize(14); b.setAllCaps(false);
        GradientDrawable d = new GradientDrawable(); d.setColor(0xFF151926); d.setStroke(dp(1), 0xFF30374A); d.setCornerRadius(dp(18)); b.setBackground(d);
        return b;
    }

    private void enableAndLaunch() {
        if (!Settings.canDrawOverlays(this)) {
            status.setText("Разреши LamboCheat V8 показываться поверх других приложений ❤️");
            startActivityForResult(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName())), OVERLAY_REQUEST);
            return;
        }
        Intent service = new Intent(this, CheatOverlayService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(service); else startService(service);
        status.setText("LamboCheat V8 запущен 🐈 — открываю игру…");
        status.postDelayed(this::launchGame, 300);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == OVERLAY_REQUEST) {
            if (Settings.canDrawOverlays(this)) enableAndLaunch();
            else status.setText("Без разрешения «поверх других приложений» меню не появится.");
        }
    }

    private void launchGame() {
        Intent i = getPackageManager().getLaunchIntentForPackage(LAMBO_GAME);
        if (i != null) { i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); startActivity(i); }
        else Toast.makeText(this, "SchoolBoy LamboV8 не установлена ❤️", Toast.LENGTH_LONG).show();
    }

    private int dp(float n) { return Math.round(n * getResources().getDisplayMetrics().density); }
}
