package org.catinthedark.audiothermometer;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;
import android.util.Pair;

import org.jtransforms.fft.DoubleFFT_1D;

/**
 * Created by ilya on 01.11.14.
 */
public class SoundMeter {
    private int minSize;
    private AudioRecord recorder;
    private final DoubleFFT_1D fft;

    public SoundMeter() {
        minSize = AudioRecord.getMinBufferSize(Constants.SAMPLING_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        fft = new DoubleFFT_1D(minSize);
        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, Constants.SAMPLING_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, minSize);
        Log.d("SoundMeter", String.valueOf(minSize));
    }

    public void start() {
        recorder.startRecording();
    }

    public void stop() {
        recorder.stop();
        recorder.release();
    }

    public short[] getBuffer() {
        short[] buffer = new short[minSize];
        recorder.read(buffer, 0, minSize);
        return buffer;
    }

    public Pair<Long, Long> getPeakHarmonic(short[] buffer) {
        if (buffer.length != minSize) {
            throw new RuntimeException("Wrong buffer size");
        }

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

        long freq = peakIndex * Constants.SAMPLING_RATE / minSize;
        return new Pair<Long, Long>(freq, (long)peakMagnitude);
    }

    public static boolean isSignal(Pair<Long, Long> peakHarmonic) {
        return (peakHarmonic.first > Constants.SIGNAL_FREQ_MIN
                && peakHarmonic.first < Constants.SIGNAL_FREQ_MAX);
    }

    public static boolean isSynchro(Pair<Long, Long> peakHarmonic) {
        return peakHarmonic.first > Constants.SYNCHRO_FREQ_MIN
                && peakHarmonic.first < Constants.SYNCHRO_FREQ_MAX;
    }
}
