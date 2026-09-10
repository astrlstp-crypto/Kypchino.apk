package com.kupchino.app;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;

public class CalmMusic {
    private volatile boolean playing=false;
    private Thread thread;
    private AudioTrack track;

    public void start(){
        if(playing) return;
        playing=true;
        thread=new Thread(()->{
            int sr=22050;
            int min=AudioTrack.getMinBufferSize(sr,AudioFormat.CHANNEL_OUT_MONO,AudioFormat.ENCODING_PCM_16BIT);
            int size=Math.max(min,4096);
            track=new AudioTrack(
                new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build(),
                new AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(sr).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build(),
                size,AudioTrack.MODE_STREAM,0);
            track.setVolume(0.12f);
            track.play();
            short[] buf=new short[1024];
            double phase1=0,phase2=0;
            double[] roots={220.00,261.63,196.00,174.61};
            long sample=0;
            while(playing){
                for(int i=0;i<buf.length;i++,sample++){
                    double t=sample/(double)sr;
                    int chord=(int)(t/4.0)%roots.length;
                    double f=roots[chord];
                    phase1+=2*Math.PI*f/sr;
                    phase2+=2*Math.PI*(f*1.5)/sr;
                    double slow=0.65+0.35*Math.sin(2*Math.PI*0.08*t);
                    double v=(Math.sin(phase1)*0.55+Math.sin(phase2)*0.22)*slow;
                    buf[i]=(short)(v*5500);
                }
                track.write(buf,0,buf.length);
            }
            try{track.stop();}catch(Exception ignored){}
            track.release();
            track=null;
        },"KupchinoCalmMusic");
        thread.start();
    }

    public void stop(){
        playing=false;
        if(thread!=null){try{thread.join(250);}catch(Exception ignored){}}
        thread=null;
    }

    public boolean isPlaying(){ return playing; }
}
