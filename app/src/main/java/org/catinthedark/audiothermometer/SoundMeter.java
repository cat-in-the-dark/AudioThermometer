package org.catinthedark.audiothermometer;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import org.jtransforms.fft.DoubleFFT_1D;

/**
 * Created by ilya on 01.11.14.
 */
public class SoundMeter {
    private int minSize;
    private AudioRecord recorder;
    int sampleRate = 44100;
    private final DoubleFFT_1D fft;

    public SoundMeter() {
        minSize = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        fft = new DoubleFFT_1D(minSize);
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

    public long getAmplitude() {
        short[] buffer = new short[minSize];
        recorder.read(buffer, 0, minSize);
        double[] spectrum = new double[minSize];
        for (int i = 0; i < minSize; ++i) {
            spectrum[i] = (double) buffer[i];
        }
        fft.realForward(spectrum);
        double[] magnitude = new double[minSize / 2];
        int peakIndex = 0;
        double peakMagnitude = 0;
        for (int i = 0; i < minSize / 2; i++) {
            magnitude[i] = Math.sqrt(spectrum[2*i] * spectrum[2*i] + spectrum[2*i+1]*spectrum[2*i + 1]);
            // find biggest peak in power spectrum
            if (magnitude[i] > peakMagnitude) {
                peakMagnitude = magnitude[i];
                peakIndex = i;
            }
        }

        long freq = peakIndex * sampleRate / minSize;
        if (freq < 3900 || freq > 4100) {
            Log.d("Thermometer", "Unstable temperature. Increase Volume");
            return 0;
        }

        return (long)peakMagnitude;
    }
}
