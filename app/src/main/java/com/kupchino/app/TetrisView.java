package com.kupchino.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;
import java.util.Random;

public class TetrisView extends View {
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG); private final Random rnd=new Random();
    private final int W=10,H=20; private final int[][] board=new int[H][W];
    private final int[][][][] shapes={
        {{{1,1,1,1}}},
        {{{1,1},{1,1}}},
        {{{0,1,0},{1,1,1}},{{1,0},{1,1},{1,0}},{{1,1,1},{0,1,0}},{{0,1},{1,1},{0,1}}},
        {{{1,0,0},{1,1,1}},{{1,1},{1,0},{1,0}},{{1,1,1},{0,0,1}},{{0,1},{0,1},{1,1}}},
        {{{0,0,1},{1,1,1}},{{1,0},{1,0},{1,1}},{{1,1,1},{1,0,0}},{{1,1},{0,1},{0,1}}},
        {{{0,1,1},{1,1,0}},{{1,0},{1,1},{0,1}}},
        {{{1,1,0},{0,1,1}},{{0,1},{1,1},{1,0}}}
    };
    private final int[] colors={0,0xFF39C5FF,0xFFFFD54F,0xFFB06CFF,0xFFFF8A3D,0xFF5EDB79,0xFFFF5D73,0xFF58A6FF};
    private int type,rot,x,y,score,lines; private boolean over=false; private long last=0;

    public TetrisView(Context c){super(c);setBackgroundColor(Color.rgb(15,17,22));spawn();}
    private void spawn(){type=rnd.nextInt(shapes.length);rot=0;x=3;y=0;if(collide(x,y,rot)){over=true;}invalidate();}
    private boolean collide(int nx,int ny,int nr){int[][] s=shapes[type][nr%shapes[type].length];for(int r=0;r<s.length;r++)for(int c=0;c<s[r].length;c++)if(s[r][c]!=0){int xx=nx+c,yy=ny+r;if(xx<0||xx>=W||yy>=H||(yy>=0&&board[yy][xx]!=0))return true;}return false;}
    private void lock(){int[][] s=shapes[type][rot%shapes[type].length];for(int r=0;r<s.length;r++)for(int c=0;c<s[r].length;c++)if(s[r][c]!=0&&y+r>=0)board[y+r][x+c]=type+1;clear();spawn();}
    private void clear(){for(int r=H-1;r>=0;r--){boolean full=true;for(int c=0;c<W;c++)if(board[r][c]==0){full=false;break;}if(full){for(int rr=r;rr>0;rr--)System.arraycopy(board[rr-1],0,board[rr],0,W);for(int c=0;c<W;c++)board[0][c]=0;lines++;score+=100;r++;}}}
    public void left(){if(!over&&!collide(x-1,y,rot))x--;invalidate();}
    public void right(){if(!over&&!collide(x+1,y,rot))x++;invalidate();}
    public void rotate(){int nr=(rot+1)%shapes[type].length;if(!over&&!collide(x,y,nr))rot=nr;invalidate();}
    public void down(){if(over)return;if(!collide(x,y+1,rot))y++;else lock();invalidate();}
    public void drop(){if(over)return;while(!collide(x,y+1,rot))y++;lock();}
    public void restart(){for(int r=0;r<H;r++)for(int c=0;c<W;c++)board[r][c]=0;score=lines=0;over=false;spawn();last=System.currentTimeMillis();}
    @Override protected void onDraw(Canvas c){
        super.onDraw(c); long now=System.currentTimeMillis();if(last==0)last=now;if(!over&&now-last>650){down();last=now;}postInvalidateOnAnimation();
        float cell=Math.min(getWidth()/12f,(getHeight()-120)/20f),ox=(getWidth()-W*cell)/2f,oy=80;
        p.setTextAlign(Paint.Align.CENTER);p.setFakeBoldText(true);p.setColor(Color.WHITE);p.setTextSize(34);c.drawText("ТЕТРИПЧИНО",getWidth()/2f,42,p);
        p.setTextSize(16);c.drawText("Счёт "+score+" • линии "+lines,getWidth()/2f,66,p);
        p.setStyle(Paint.Style.STROKE);p.setColor(0xFF454A55);p.setStrokeWidth(2);c.drawRect(ox,oy,ox+W*cell,oy+H*cell,p);p.setStyle(Paint.Style.FILL);
        for(int r=0;r<H;r++)for(int cc=0;cc<W;cc++)if(board[r][cc]!=0){p.setColor(colors[board[r][cc]]);c.drawRect(ox+cc*cell+1,oy+r*cell+1,ox+(cc+1)*cell-1,oy+(r+1)*cell-1,p);}
        if(!over){int[][] s=shapes[type][rot%shapes[type].length];p.setColor(colors[type+1]);for(int r=0;r<s.length;r++)for(int cc=0;cc<s[r].length;cc++)if(s[r][cc]!=0)c.drawRect(ox+(x+cc)*cell+1,oy+(y+r)*cell+1,ox+(x+cc+1)*cell-1,oy+(y+r+1)*cell-1,p);}
        if(over){p.setColor(0xB0000000);c.drawRect(0,getHeight()*.4f,getWidth(),getHeight()*.6f,p);p.setColor(Color.WHITE);p.setTextSize(30);c.drawText("GAME OVER",getWidth()/2f,getHeight()*.48f,p);p.setTextSize(17);c.drawText("Нажми ↻ снизу",getWidth()/2f,getHeight()*.54f,p);}
    }
}
