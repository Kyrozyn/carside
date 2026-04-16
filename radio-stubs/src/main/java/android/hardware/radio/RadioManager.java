package android.hardware.radio;

import java.util.List;

/**
 * Compile-time stub of android.hardware.radio.RadioManager (@SystemApi).
 * Real implementation is provided at runtime on devices with BroadcastRadio HAL
 * when the APK is installed as a system/privileged app.
 */
public class RadioManager {

    public static final int STATUS_OK = 0;

    public static final int BAND_AM = 0;
    public static final int BAND_FM = 1;
    public static final int BAND_FM_HD = 2;
    public static final int BAND_AM_HD = 3;

    protected RadioManager() {}

    public int listModules(List<ModuleProperties> modules) {
        throw new UnsupportedOperationException("stub");
    }

    /**
     * Handler param typed as Object to avoid pulling android.os.Handler into this
     * framework-less stub module. At runtime the real RadioManager accepts android.os.Handler.
     */
    public RadioTuner openTuner(int moduleId,
                                BandConfig config,
                                boolean withAudio,
                                RadioTuner.Callback callback,
                                Object handler) {
        throw new UnsupportedOperationException("stub");
    }

    public static class ModuleProperties {
        protected ModuleProperties() {}
        public int getId() { throw new UnsupportedOperationException("stub"); }
        public BandDescriptor[] getBands() { throw new UnsupportedOperationException("stub"); }
    }

    public static abstract class BandDescriptor {
        protected BandDescriptor() {}
        public int getType() { throw new UnsupportedOperationException("stub"); }
        public int getLowerLimit() { throw new UnsupportedOperationException("stub"); }
        public int getUpperLimit() { throw new UnsupportedOperationException("stub"); }
        public int getSpacing() { throw new UnsupportedOperationException("stub"); }
    }

    public static class FmBandDescriptor extends BandDescriptor {
        protected FmBandDescriptor() {}
    }

    public static class AmBandDescriptor extends BandDescriptor {
        protected AmBandDescriptor() {}
    }

    public static abstract class BandConfig {
        protected BandConfig() {}
        public int getType() { throw new UnsupportedOperationException("stub"); }
    }

    public static class FmBandConfig extends BandConfig {
        protected FmBandConfig() {}

        public static class Builder {
            public Builder(FmBandDescriptor descriptor) {}
            public Builder setStereo(boolean stereo) { return this; }
            public Builder setRds(boolean rds) { return this; }
            public FmBandConfig build() { throw new UnsupportedOperationException("stub"); }
        }
    }

    public static class AmBandConfig extends BandConfig {
        protected AmBandConfig() {}

        public static class Builder {
            public Builder(AmBandDescriptor descriptor) {}
            public AmBandConfig build() { throw new UnsupportedOperationException("stub"); }
        }
    }

    public static class ProgramInfo {
        protected ProgramInfo() {}

        /** @deprecated since API 30, use {@link #getSelector()}. */
        @Deprecated
        public int getChannel() { throw new UnsupportedOperationException("stub"); }

        @Deprecated
        public int getSubChannel() { throw new UnsupportedOperationException("stub"); }

        public ProgramSelector getSelector() { throw new UnsupportedOperationException("stub"); }

        public RadioMetadata getMetadata() { throw new UnsupportedOperationException("stub"); }

        public int getSignalStrength() { throw new UnsupportedOperationException("stub"); }
    }
}
