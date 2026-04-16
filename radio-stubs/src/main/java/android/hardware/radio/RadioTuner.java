package android.hardware.radio;

/**
 * Compile-time stub of android.hardware.radio.RadioTuner (@SystemApi).
 */
public abstract class RadioTuner {

    public static final int DIRECTION_UP = 0;
    public static final int DIRECTION_DOWN = 1;

    protected RadioTuner() {}

    public abstract void close();

    public abstract int setConfiguration(RadioManager.BandConfig config);

    @Deprecated
    public abstract int tune(int channel, int subChannel);

    public abstract void tune(ProgramSelector selector);

    public abstract int step(int direction, boolean skipSubChannel);

    @Deprecated
    public abstract int scan(int direction, boolean skipSubChannel);

    public abstract int cancel();

    public abstract int setMute(boolean mute);

    public static class Callback {
        public Callback() {}

        public void onError(int status) {}

        public void onConfigurationChanged(RadioManager.BandConfig config) {}

        public void onProgramInfoChanged(RadioManager.ProgramInfo info) {}

        public void onMetadataChanged(RadioMetadata metadata) {}

        public void onTuneFailed(int result, ProgramSelector selector) {}

        public void onAntennaState(boolean connected) {}

        public void onTrafficAnnouncement(boolean active) {}

        public void onEmergencyAnnouncement(boolean active) {}

        public void onBackgroundScanAvailabilityChange(boolean isAvailable) {}

        public void onBackgroundScanComplete() {}

        public void onProgramListChanged() {}

        public void onCurrentProgramInfoChanged(RadioManager.ProgramInfo info) {}
    }
}
