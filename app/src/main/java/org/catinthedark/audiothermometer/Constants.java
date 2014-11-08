package org.catinthedark.audiothermometer;

/**
 * Created by ilya on 07.11.14.
 */
public class Constants {
    public static final int SAMPLING_RATE = 44100;
    public static final double TWO_PI = 8. * Math.atan(1.);
    public static final int SYNCHRO_FREQUENCY = 6000;
    public static final int SYNCHRO_FREQ_MIN = SYNCHRO_FREQUENCY - 100;
    public static final int SYNCHRO_FREQ_MAX = SYNCHRO_FREQUENCY + 100;
    public static final int SIGNAL_FREQUENCY = 4000;
    public static final int SIGNAL_FREQ_MIN = SIGNAL_FREQUENCY - 100;
    public static final int SIGNAL_FREQ_MAX = SIGNAL_FREQUENCY + 100;
    public static final int AMPLITUDE_SYNCHRO = 1000;

    private static int AMPLITUDE_L = 1000;
    private static int AMPLITUDE_R = 1000;

    public synchronized static int getAMPLITUDE_L() {
        return AMPLITUDE_L;
    }

    public synchronized static void setAMPLITUDE_L(int AMPLITUDE_L) {
        Constants.AMPLITUDE_L = AMPLITUDE_L;
    }

    public synchronized static int getAMPLITUDE_R() {
        return AMPLITUDE_R;
    }

    public synchronized static void setAMPLITUDE_R(int AMPLITUDE_R) {
        Constants.AMPLITUDE_R = AMPLITUDE_R;
    }
}
