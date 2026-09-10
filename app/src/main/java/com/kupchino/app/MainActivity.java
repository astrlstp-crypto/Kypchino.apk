package com.kupchino.app;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
 @Override public void onCreate(Bundle b) { super.onCreate(b);
  LinearLayout l=new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); l.setGravity(Gravity.CENTER); l.setPadding(30,30,30,30); l.setBackgroundColor(Color.BLACK);
  TextView t=new TextView(this); t.setText("КУПЧИНО 🥶❤️"); t.setTextColor(Color.WHITE); t.setTextSize(34); t.setGravity(Gravity.CENTER); l.addView(t);
  TextView x=new TextView(this); x.setText("🏢\n\nКУПЧИНО\n\nлегендарное место 🤑❤️"); x.setTextColor(Color.WHITE); x.setTextSize(28); x.setGravity(Gravity.CENTER); l.addView(x);
  setContentView(l);
 }
}
