package org.catinthedark.audiothermometer;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

/**
 * Created by ilya on 01.11.14.
 */
public class SoundMeter {
    private int minSize;
    private AudioRecord recorder;
    int sampleRate = 44100;

    public SoundMeter() {
        minSize = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, minSize);
        Log.d("SoundMeter", String.valueOf(minSize));
    }

    public void start() {
        recorder.startRecording();
    }

    public void stop() {
        recorder.stop();
        recorder.release();
    }

    public long getMaxAmplitude() {
        short[] buffer = new short[minSize];
        recorder.read(buffer, 0, minSize);
        long max = 0;
        for (short s : buffer) {
            max += Math.abs(s);
        }

        return max / minSize;
    }
}
