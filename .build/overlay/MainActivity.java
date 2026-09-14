package com.example.arabicprank;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private FrameLayout root;
    private CountDownTimer warningTimer;
    private CountDownTimer imageTimer;
    private ToneGenerator toneGenerator;
    private final Handler musicHandler = new Handler(Looper.getMainLooper());
    private int musicStep = 0;
    private boolean musicPlaying = false;

    private final int[] spookyTones = {
            ToneGenerator.TONE_DTMF_1, ToneGenerator.TONE_DTMF_4,
            ToneGenerator.TONE_DTMF_7, ToneGenerator.TONE_DTMF_4,
            ToneGenerator.TONE_DTMF_2, ToneGenerator.TONE_DTMF_5,
            ToneGenerator.TONE_DTMF_8, ToneGenerator.TONE_DTMF_5
    };

    private final Runnable musicTick = new Runnable() {
        @Override public void run() {
            if (!musicPlaying || toneGenerator == null) return;
            toneGenerator.startTone(spookyTones[musicStep % spookyTones.length], 280);
            musicStep++;
            musicHandler.postDelayed(this, 500);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window window = getWindow();
        window.setStatusBarColor(Color.BLACK);
        window.setNavigationBarColor(Color.BLACK);
        showIntro();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private TextView makeText(String text, float sp, int color) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(sp);
        tv.setTextColor(color);
        tv.setGravity(Gravity.CENTER);
        tv.setTextDirection(View.TEXT_DIRECTION_RTL);
        tv.setPadding(dp(18), dp(10), dp(18), dp(10));
        return tv;
    }

    private Button makeButton(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextSize(20);
        b.setAllCaps(false);
        b.setTextColor(Color.WHITE);
        b.setBackgroundColor(Color.rgb(120, 0, 0));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(58), 1f);
        p.setMargins(dp(8), dp(12), dp(8), dp(12));
        b.setLayoutParams(p);
        return b;
    }

    private void startCountdownMusic() {
        stopCountdownMusic();
        try {
            toneGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC, 75);
            musicPlaying = true;
            musicStep = 0;
            musicHandler.post(musicTick);
        } catch (RuntimeException ignored) {
            toneGenerator = null;
        }
    }

    private void stopCountdownMusic() {
        musicPlaying = false;
        musicHandler.removeCallbacks(musicTick);
        if (toneGenerator != null) {
            try { toneGenerator.stopTone(); } catch (RuntimeException ignored) {}
            toneGenerator.release();
            toneGenerator = null;
        }
    }

    private void showIntro() {
        if (warningTimer != null) warningTimer.cancel();
        if (imageTimer != null) imageTimer.cancel();
        stopCountdownMusic();

        root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setPadding(dp(24), dp(24), dp(24), dp(24));

        TextView title = makeText("مرحبًا!", 44, Color.rgb(235, 0, 0));
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        box.addView(title, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView hint = makeText("هل تريد المتابعة؟", 22, Color.WHITE);
        box.addView(hint, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        Button yes = makeButton("نعم");
        Button no = makeButton("لا");
        row.addView(yes);
        row.addView(no);
        box.addView(row, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        FrameLayout.LayoutParams boxParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
        boxParams.setMargins(dp(20), dp(20), dp(20), dp(20));
        root.addView(box, boxParams);
        setContentView(root);

        yes.setOnClickListener(v -> showFakeWarning());
        no.setOnClickListener(v -> finishAndRemoveTask());
    }

    private void showFakeWarning() {
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(18), dp(6), dp(18), dp(6));

        TextView message = makeText("تم إتلاف ملفاتك وسيتم حذفها خلال 30 ثانية", 20, Color.WHITE);
        content.addView(message);
        TextView timerText = makeText("30", 34, Color.rgb(255, 30, 30));
        timerText.setTypeface(null, android.graphics.Typeface.BOLD);
        content.addView(timerText);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("تحذير")
                .setView(content)
                .setNegativeButton("إغلاق", (d, which) -> {
                    stopCountdownMusic();
                    finishAndRemoveTask();
                }).create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.rgb(255, 70, 70)));
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
        startCountdownMusic();

        warningTimer = new CountDownTimer(30_000, 1_000) {
            @Override public void onTick(long millisUntilFinished) {
                long seconds = Math.max(1, (millisUntilFinished + 999) / 1000);
                timerText.setText(String.valueOf(seconds));
            }
            @Override public void onFinish() {
                stopCountdownMusic();
                if (dialog.isShowing()) dialog.dismiss();
                showNoCommandImage();
            }
        }.start();
    }

    private void showNoCommandImage() {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        FrameLayout screen = new FrameLayout(this);
        screen.setBackgroundColor(Color.BLACK);
        ImageView image = new ImageView(this);
        image.setImageResource(R.drawable.no_command);
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        image.setBackgroundColor(Color.BLACK);
        screen.addView(image, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(screen);

        imageTimer = new CountDownTimer(60_000, 1_000) {
            @Override public void onTick(long millisUntilFinished) {}
            @Override public void onFinish() {
                Toast.makeText(MainActivity.this, "مزحة فقط — لم يتم حذف أي ملفات", Toast.LENGTH_LONG).show();
                screen.postDelayed(() -> finishAndRemoveTask(), 2800);
            }
        }.start();
    }

    @Override
    protected void onDestroy() {
        if (warningTimer != null) warningTimer.cancel();
        if (imageTimer != null) imageTimer.cancel();
        stopCountdownMusic();
        super.onDestroy();
    }
}
