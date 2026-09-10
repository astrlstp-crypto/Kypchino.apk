package com.kupchino.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;
import java.util.Random;

public class MinesweeperView extends View {
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Random rnd=new Random();
    private final int rows=9, cols=9, mineCount=10;
    private final boolean[][] mines=new boolean[rows][cols];
    private final boolean[][] open=new boolean[rows][cols];
    private final boolean[][] flags=new boolean[rows][cols];
    private boolean over=false, won=false;
    private float downX,downY; private long downAt;

    public MinesweeperView(Context c){ super(c); newGame(); setBackgroundColor(Color.rgb(28,31,36)); }
    private void newGame(){
        for(int r=0;r<rows;r++)for(int c=0;c<cols;c++){mines[r][c]=open[r][c]=flags[r][c]=false;}
        int n=0; while(n<mineCount){int r=rnd.nextInt(rows),c=rnd.nextInt(cols); if(!mines[r][c]){mines[r][c]=true;n++;}}
        over=false;won=false;invalidate();
    }
    private int around(int r,int c){
        int n=0; for(int dr=-1;dr<=1;dr++)for(int dc=-1;dc<=1;dc++){int rr=r+dr,cc=c+dc;if(rr>=0&&rr<rows&&cc>=0&&cc<cols&&mines[rr][cc])n++;} return n;
    }
    private void reveal(int r,int c){
        if(r<0||r>=rows||c<0||c>=cols||open[r][c]||flags[r][c])return;
        open[r][c]=true; if(mines[r][c]){over=true;return;}
        if(around(r,c)==0)for(int dr=-1;dr<=1;dr++)for(int dc=-1;dc<=1;dc++)if(dr!=0||dc!=0)reveal(r+dr,c+dc);
        int safe=0,opened=0; for(int y=0;y<rows;y++)for(int x=0;x<cols;x++){if(!mines[y][x])safe++;if(open[y][x]&&!mines[y][x])opened++;}
        if(opened==safe){won=true;over=true;}
    }
    @Override public boolean onTouchEvent(MotionEvent e){
        float cell=Math.min(getWidth()/(float)cols,(getHeight()-120)/(float)rows);
        float ox=(getWidth()-cell*cols)/2f, oy=85;
        if(e.getAction()==MotionEvent.ACTION_DOWN){downX=e.getX();downY=e.getY();downAt=System.currentTimeMillis();return true;}
        if(e.getAction()==MotionEvent.ACTION_UP){
            if(over){newGame();return true;}
            int c=(int)((downX-ox)/cell),r=(int)((downY-oy)/cell);
            if(r<0||r>=rows||c<0||c>=cols)return true;
            if(System.currentTimeMillis()-downAt>450){if(!open[r][c])flags[r][c]=!flags[r][c];}
            else reveal(r,c);
            invalidate();return true;
        } return true;
    }
    @Override protected void onDraw(Canvas c){
        super.onDraw(c); float cell=Math.min(getWidth()/(float)cols,(getHeight()-120)/(float)rows); float ox=(getWidth()-cell*cols)/2f,oy=85;
        p.setTextAlign(Paint.Align.CENTER);p.setFakeBoldText(true);p.setColor(Color.WHITE);p.setTextSize(34);c.drawText("САПЁРНОЕ КУПЧИНО",getWidth()/2f,48,p);
        p.setTextSize(16);c.drawText("Тап = открыть • удержание = флаг",getWidth()/2f,72,p);
        for(int r=0;r<rows;r++)for(int x=0;x<cols;x++){
            float l=ox+x*cell,t=oy+r*cell; p.setStyle(Paint.Style.FILL);
            if(open[r][x])p.setColor(mines[r][x]?0xFFD84A4A:0xFFE7E7E7); else p.setColor(0xFF666B73);
            c.drawRect(l+2,t+2,l+cell-2,t+cell-2,p);
            if(flags[r][x]&&!open[r][x]){p.setTextSize(cell*.5f);p.setColor(Color.YELLOW);c.drawText("⚑",l+cell/2,t+cell*.68f,p);}
            if(open[r][x]){
                if(mines[r][x]){p.setTextSize(cell*.5f);p.setColor(Color.BLACK);c.drawText("●",l+cell/2,t+cell*.68f,p);}
                else {int n=around(r,x); if(n>0){p.setTextSize(cell*.45f);p.setColor(0xFF153A7A);c.drawText(String.valueOf(n),l+cell/2,t+cell*.68f,p);}}
            }
        }
        if(over){
            p.setColor(0xB0000000);c.drawRect(0,getHeight()*.38f,getWidth(),getHeight()*.62f,p);p.setColor(Color.WHITE);p.setTextSize(34);
            c.drawText(won?"ПОБЕДА 🤑":"БАБАХ 💣",getWidth()/2f,getHeight()*.48f,p);p.setTextSize(18);c.drawText("Тапни для новой игры",getWidth()/2f,getHeight()*.54f,p);
        }
    }
}
