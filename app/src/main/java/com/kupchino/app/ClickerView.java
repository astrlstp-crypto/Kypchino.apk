package com.kupchino.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.Locale;

public class ClickerView extends LinearLayout {
    private final SharedPreferences prefs;
    private final TextView coinsText, statsText;
    private final LinearLayout upgradesBox;
    private final int[] levels = new int[1200];

    // Храним огромные числа как log10: 1Vg = 10^57, 1DuVg = 10^135.
    private double coinsLog = Double.NEGATIVE_INFINITY;
    private double baseClickLog = 0.0;
    private int rebirths = 0;
    private int prestiges = 0;

    private static final String[] SUFFIX = {
        "","K","M","B","T","Qa","Qi","Sx","Sp","Oc","No","Dc","Qw","Wr","Rb","Qr",
        "Vvg","Vg","Uvg","Tvg","QaVg","QiVg","OcVg","NoVg","Du","UDu","TDu","QaDu",
        "QiDu","SxDu","UVgDu","Tg","UTg","TTg","QaTg","SxTg","TTgVgDu",
        "TTgUvgDu","TTgTvgDu","TTgQaVgDu","TTgQiVgDu"
    };

    public ClickerView(Context c){
        super(c);
        setOrientation(VERTICAL);
        setBackgroundColor(0xFF111318);
        setPadding(18,18,18,18);
        prefs=c.getSharedPreferences("kupchino_clicker",Context.MODE_PRIVATE);
        load();

        TextView title=new TextView(c);
        title.setText("🐈 КЛИКЕР КУПЧИННОСТИ");
        title.setTextColor(Color.WHITE);
        title.setTextSize(27);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        addView(title,new LayoutParams(-1,-2));

        coinsText=new TextView(c);
        coinsText.setTextColor(0xFFFFD54F);
        coinsText.setTextSize(30);
        coinsText.setTypeface(Typeface.DEFAULT_BOLD);
        coinsText.setGravity(Gravity.CENTER);
        coinsText.setPadding(0,10,0,4);
        addView(coinsText,new LayoutParams(-1,-2));

        statsText=new TextView(c);
        statsText.setTextColor(0xFFD6D8DE);
        statsText.setTextSize(14);
        statsText.setGravity(Gravity.CENTER);
        addView(statsText,new LayoutParams(-1,-2));

        Button tyson=new Button(c);
        tyson.setText("🐈\nТАЙСОН\nТАПАЙ 🤑");
        tyson.setAllCaps(false);
        tyson.setTextSize(30);
        tyson.setMinHeight(240);
        tyson.setOnClickListener(v->{
            addCoins(currentClickLog());
            save();
            refresh();
        });
        LayoutParams tp=new LayoutParams(-1,-2);
        tp.setMargins(0,14,0,10);
        addView(tyson,tp);

        LinearLayout actions=new LinearLayout(c);
        actions.setGravity(Gravity.CENTER);
        Button rebirth=new Button(c);
        rebirth.setText("♻ Перерождение\n1Vg");
        rebirth.setAllCaps(false);
        Button prestige=new Button(c);
        prestige.setText("👑 Престиж\n1DuVg");
        prestige.setAllCaps(false);
        LayoutParams aw=new LayoutParams(0,-2,1f);
        actions.addView(rebirth,aw); actions.addView(prestige,aw);
        addView(actions,new LayoutParams(-1,-2));

        rebirth.setOnClickListener(v->{
            if(hasAtLeast(57)){
                rebirths++;
                coinsLog=Double.NEGATIVE_INFINITY;
                baseClickLog=0;
                for(int i=0;i<levels.length;i++) levels[i]=0;
                save(); rebuildUpgrades(); refresh();
            }
        });
        prestige.setOnClickListener(v->{
            if(hasAtLeast(135)){
                prestiges++;
                rebirths=0;
                coinsLog=Double.NEGATIVE_INFINITY;
                baseClickLog=0;
                for(int i=0;i<levels.length;i++) levels[i]=0;
                save(); rebuildUpgrades(); refresh();
            }
        });

        TextView upTitle=new TextView(c);
        upTitle.setText("⚡ 1200 УЛУЧШЕНИЙ • КАЖДОЕ ДО 20 LVL");
        upTitle.setTextColor(Color.WHITE);
        upTitle.setTextSize(20);
        upTitle.setTypeface(Typeface.DEFAULT_BOLD);
        upTitle.setPadding(0,14,0,8);
        addView(upTitle,new LayoutParams(-1,-2));

        ScrollView scroll=new ScrollView(c);
        upgradesBox=new LinearLayout(c);
        upgradesBox.setOrientation(VERTICAL);
        scroll.addView(upgradesBox,new ScrollView.LayoutParams(-1,-2));
        addView(scroll,new LayoutParams(-1,0,1f));

        rebuildUpgrades();
        refresh();
    }

    private double currentClickLog(){
        // Каждое перерождение = x10, каждый престиж = x100 к общему доходу.
        return baseClickLog + rebirths + prestiges*2.0;
    }

    private void rebuildUpgrades(){
        upgradesBox.removeAllViews();
        for(int i=0;i<1200;i++){
            final int id=i;
            Button b=new Button(getContext());
            b.setAllCaps(false);
            b.setTextSize(14);
            double cost=upgradeCostLog(i);
            int lvl=levels[i];
            int power=(i%5)+2;
            if(lvl>=20){
                b.setText("✅ Улучшение "+(i+1)+"  • LVL 20/20");
                b.setEnabled(false);
            }else{
                double levelCost=cost+lvl*0.18;
                b.setText("⬆ Улучшение "+(i+1)+"  • LVL "+lvl+"/20 • x"+power+"\nЦена: "+formatLog(levelCost));
                b.setOnClickListener(v->{
                    double buyCost=upgradeCostLog(id)+levels[id]*0.18;
                    if(hasAtLeast(buyCost)){
                        subtractCost(buyCost);
                        baseClickLog += Math.log10(power);
                        levels[id]++;
                        save(); rebuildUpgrades(); refresh();
                    }
                });
            }
            LayoutParams lp=new LayoutParams(-1,-2);
            lp.setMargins(0,3,0,3);
            upgradesBox.addView(b,lp);
        }
    }

    private double upgradeCostLog(int i){
        // От 10 монет до сверхогромных цен. Последние апгрейды уходят далеко за Vg.
        return 1.0 + i*(400.0/1199.0);
    }

    private void addCoins(double addLog){
        if(Double.isInfinite(coinsLog)) { coinsLog=addLog; return; }
        double hi=Math.max(coinsLog,addLog), lo=Math.min(coinsLog,addLog);
        if(hi-lo>15){ coinsLog=hi; return; }
        coinsLog=hi+Math.log10(1.0+Math.pow(10.0,lo-hi));
    }

    private boolean hasAtLeast(double costLog){
        return !Double.isInfinite(coinsLog) && coinsLog+1e-10>=costLog;
    }

    private void subtractCost(double costLog){
        if(Double.isInfinite(coinsLog)) return;
        if(coinsLog-costLog>12) return;
        double ratio=Math.pow(10.0,costLog-coinsLog);
        if(ratio>=0.999999999){ coinsLog=Double.NEGATIVE_INFINITY; return; }
        coinsLog=coinsLog+Math.log10(1.0-ratio);
    }

    private String formatLog(double log){
        if(Double.isInfinite(log)) return "0";
        if(Double.isNaN(log)) return "nan";
        if(log>1_000_000) return "Rayo's number";
        int exp=(int)Math.floor(log);
        if(exp>1500) return "Inf";
        int group=Math.max(0,exp/3);
        double mant=Math.pow(10.0,log-group*3);
        if(group<SUFFIX.length){
            if(mant>=100) return String.format(Locale.US,"%.0f%s",mant,SUFFIX[group]);
            if(mant>=10) return String.format(Locale.US,"%.1f%s",mant,SUFFIX[group]);
            return String.format(Locale.US,"%.2f%s",mant,SUFFIX[group]);
        }
        if(group==SUFFIX.length) return "Rayo's number";
        if(group==SUFFIX.length+1) return "Inf";
        return "nan";
    }

    private void refresh(){
        coinsText.setText(formatLog(coinsLog)+" 🪙");
        statsText.setText("За тап: "+formatLog(currentClickLog())+
            "   •   ♻ "+rebirths+"   •   👑 "+prestiges+
            "\nАвтосохранение: ВКЛ ✅");
    }

    private void save(){
        SharedPreferences.Editor e=prefs.edit()
            .putString("coinsLog",Double.toString(coinsLog))
            .putString("baseClickLog",Double.toString(baseClickLog))
            .putInt("rebirths",rebirths)
            .putInt("prestiges",prestiges);
        for(int i=0;i<levels.length;i++) e.putInt("u"+i,levels[i]);
        e.apply();
    }

    private void load(){
        try{coinsLog=Double.parseDouble(prefs.getString("coinsLog","-Infinity"));}catch(Exception e){coinsLog=Double.NEGATIVE_INFINITY;}
        try{baseClickLog=Double.parseDouble(prefs.getString("baseClickLog","0"));}catch(Exception e){baseClickLog=0;}
        rebirths=prefs.getInt("rebirths",0);
        prestiges=prefs.getInt("prestiges",0);
        for(int i=0;i<levels.length;i++) levels[i]=prefs.getInt("u"+i,0);
    }
}
