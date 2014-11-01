package org.catinthedark.audiothermometer;

import android.app.Activity;
import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;


public class MyActivity extends Activity {
    private Thread t;
    private Thread recorderThread;
    final int sr = 44100;
    final double fr = 4000;
    int amp = 10000;

    boolean isRunning = true;
    TextView tv;
    double amplitude = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);

        AudioManager manager = (AudioManager)this.getSystemService(Context.AUDIO_SERVICE);
        manager.setStreamVolume(AudioManager.STREAM_MUSIC, manager.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0);

        tv = (TextView)findViewById(R.id.temperature);
        recorderThread = new Thread() {
            public void run() {
                SoundMeter soundMeter = new SoundMeter();
                soundMeter.start();
                while (isRunning) {
                    amplitude = soundMeter.getMaxAmplitude();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            tv.setText(String.valueOf(amplitude));
                        }
                    });
                }
                soundMeter.stop();
            }
        };
        recorderThread.start();


        t = new Thread() {
            public void run() {
                // set process priority
                setPriority(Thread.MAX_PRIORITY);
                // set the buffer size
                int buffsize = AudioTrack.getMinBufferSize(sr,
                        AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT);
                // create an audiotrack object
                AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                        sr, AudioFormat.CHANNEL_OUT_STEREO,
                        AudioFormat.ENCODING_PCM_16BIT, buffsize,
                        AudioTrack.MODE_STREAM);

                short samples[] = new short[buffsize];
                double twopi = 8.*Math.atan(1.);

                double phl = 0.0;
                double phr = 0.0;

                // start audio
                audioTrack.play();

                // synthesis loop
                while(isRunning){
                    for(int i=0; i < buffsize; i++){
                        if (i%2 == 0) {
                            samples[i] = (short) (amp*Math.sin(phl));
                            phl += twopi*fr/sr;
                        } else {
                            samples[i] = (short) (-amp*Math.sin(phr));
                            phr += twopi*fr/sr;
                        }
                    }
                    audioTrack.write(samples, 0, buffsize);
                }
                audioTrack.stop();
                audioTrack.release();
            }
        };
        t.start();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.my, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
