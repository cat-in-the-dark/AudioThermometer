package org.catinthedark.audiothermometer;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.*;
import android.os.Process;
import android.util.Log;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;


public class MyActivity extends Activity {
    TextView tv;
    Pair<Long, Long> peakHarmonic = new Pair<Long, Long>(0l, 0l);

    private WaveformView mWaveformView;
    private short[] buffer;

    private RecordingThread recordingThread;
    private PlayingThread playingThread;
    private String TAG = "AudioThermometer";

    private synchronized boolean shouldSendSynchro() {
        return sendSynchro;
    }

    private synchronized void setSendSynchro(boolean shouldSendSynchro) {
        this.sendSynchro = shouldSendSynchro;
    }

    private boolean sendSynchro = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);

        AudioManager manager = (AudioManager)this.getSystemService(Context.AUDIO_SERVICE);
        manager.setStreamVolume(AudioManager.STREAM_MUSIC, manager.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0);

        tv = (TextView)findViewById(R.id.temperature);
        mWaveformView = (WaveformView) findViewById(R.id.waveform_view);
    }

    @Override
    protected void onResume() {
        super.onResume();

        recordingThread = new RecordingThread();
        recordingThread.start();

        playingThread = new PlayingThread();
        playingThread.start();
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (recordingThread != null) {
            recordingThread.stopRunning();
            recordingThread = null;
        }

        if (playingThread != null) {
            playingThread.stopRunning();
            playingThread = null;
        }
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
        if (id == R.id.action_help) {
            showHelp();
        }
        return super.onOptionsItemSelected(item);
    }

    private void showHelp() {
        Intent helpIntent = new Intent(this, HelpActivity.class);
        startActivity(helpIntent);
    }

    private float realTemp(int lCh, int rCh) {
        float frch = (float)rCh;
        float flch = (float)lCh;
        return 1 / (float)((Math.log(10000 * flch / frch) - Math.log((float)10000)) / (float)4300 + 1 / (float)(25 + 273)) - 273;
    }

    private class RecordingThread extends Thread {
        private boolean running = true;

        @Override
        public void run() {
            Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);

            /**
             * Shows that there was interference in last received signal
             * (neither signal nor synchroimpulse)
             * This may happen when one part of data is signal
             * and another part of data is synchroimpulse
             * or whole data is piece of sh^Winterference.
             */
            boolean wasInterference = false;

            /**
             * Shows if received data should contain signal with
             * volume level different from last received signal
             */
            boolean volumeShouldChange = true;

            /**
             * Shows if we increased amplitude of signal in one of channels
             */
            boolean channelIncreased = false;
            double lastAmplitude = 0;


            SoundMeter soundMeter = new SoundMeter();
            soundMeter.start();
            while (shouldContinue()) {
                buffer = soundMeter.getBuffer();
                peakHarmonic = soundMeter.getPeakHarmonic(buffer);

                if (!wasInterference) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mWaveformView.updateAudioData(buffer);
                            tv.setText(String.format("%.2f", realTemp(Constants.getAMPLITUDE_L(), Constants.getAMPLITUDE_R())));
                        }
                    });
                } else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mWaveformView.updateAudioData(buffer);
                            tv.setText("Interference");
                        }
                    });
                }

                Log.d(TAG, peakHarmonic.first.toString());
                if (SoundMeter.isSignal(peakHarmonic)) {
                    Log.d(TAG, "signal");
                    wasInterference = false;
                    if (volumeShouldChange) {
                        Log.d(TAG, "volume should change");
                        if (peakHarmonic.second > lastAmplitude) {
                            Log.d(TAG, "amplitude increasing");
                            // we doing something wrong. Stop doing it and do something else
                            if (channelIncreased) {
                                decreaseAmplitude();
                                Log.d(TAG, "down right channel");
                            } else {
                                Log.d(TAG, "up right channel");
                                increseAmplitude();
                            }
                            channelIncreased = !channelIncreased;
                        } else {
                            Log.d(TAG, "amplitude falling");
                            // we doing it all right! keep doing it!
                            if (channelIncreased) {
                                Log.d(TAG, "up right channel");
                                increseAmplitude();
                            } else {
                                Log.d(TAG, "down right channel");
                                decreaseAmplitude();
                            }
                        }
                        Log.d(TAG, "sending synchro");
                        setSendSynchro(true);
                        lastAmplitude = peakHarmonic.second;
                    }
                    volumeShouldChange = false;
                } else if (SoundMeter.isSynchro(peakHarmonic)) {
                    Log.d(TAG, "received synchro");
                    wasInterference = false;
                    volumeShouldChange = true;
                } else {
                    Log.d(TAG, "received interference");
                    if (wasInterference) {
                        continue;
                    } else {
                        Log.d(TAG, "Received 2nd interference in a row");
                        wasInterference = true;
                        continue;
                    }
                }
            }
            soundMeter.stop();
        }

        private void increseAmplitude() {
            Constants.setAMPLITUDE_R(Constants.getAMPLITUDE_R() + 10);
        }

        private void decreaseAmplitude() {
            Constants.setAMPLITUDE_R(Constants.getAMPLITUDE_R() - 10);
        }

        private synchronized boolean shouldContinue() {
            return running;
        }

        public synchronized void stopRunning() {
            running = false;
        }
    }

    private class PlayingThread extends Thread {
        private boolean running = true;

        private synchronized boolean shouldContinue() {
            return running;
        }

        public synchronized void stopRunning() {
            running = false;
        }

        @Override
        public void run() {
            // set process priority
            Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);
            // set the buffer size
            int buffsize = AudioTrack.getMinBufferSize(Constants.SAMPLING_RATE,
                    AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT);
            // create an audiotrack object
            AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                    Constants.SAMPLING_RATE, AudioFormat.CHANNEL_OUT_STEREO,
                    AudioFormat.ENCODING_PCM_16BIT, buffsize,
                    AudioTrack.MODE_STREAM);

            short samples[] = new short[buffsize];

            double phl = 0.0;
            double phr = 0.0;
            double phs = 0.0;

            // start audio
            audioTrack.play();

            // synthesis loop
            while (shouldContinue()) {
                if (shouldSendSynchro()) {
                    for (int time = 0; time < 2; time++) {
                        for (int i = 0; i < buffsize; i++) {
                            if (i % 2 == 0) {
                                samples[i] = (short) (Constants.AMPLITUDE_SYNCHRO * Math.sin(phs));
                                samples[i + 1] = (short) (Constants.AMPLITUDE_SYNCHRO * Math.sin(phs));
                                phs += Constants.TWO_PI * Constants.SYNCHRO_FREQUENCY / Constants.SAMPLING_RATE;
                            }
                        }
                        audioTrack.write(samples, 0, buffsize);
                    }
                    setSendSynchro(false);
                } else {
                    for (int i = 0; i < buffsize; i++) {
                        if (i % 2 == 0) {
                            samples[i] = (short) (Constants.getAMPLITUDE_L() * Math.sin(phl));
                            phl += Constants.TWO_PI * Constants.SIGNAL_FREQUENCY / Constants.SAMPLING_RATE;
                        } else {
                            samples[i] = (short) (-Constants.getAMPLITUDE_R() * Math.sin(phr));
                            phr += Constants.TWO_PI * Constants.SIGNAL_FREQUENCY / Constants.SAMPLING_RATE;
                        }
                    }
                    audioTrack.write(samples, 0, buffsize);
                }
            }
            audioTrack.stop();
            audioTrack.release();
        }
    }
}
