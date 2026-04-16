package android.hardware.radio;

/**
 * Compile-time stub of android.hardware.radio.ProgramSelector (@SystemApi).
 */
public class ProgramSelector {

    protected ProgramSelector() {}

    public static ProgramSelector createAmFmSelector(int band, int frequencyKhz) {
        throw new UnsupportedOperationException("stub");
    }

    public Identifier getPrimaryId() { throw new UnsupportedOperationException("stub"); }

    public static class Identifier {
        protected Identifier() {}
        public long getValue() { throw new UnsupportedOperationException("stub"); }
        public int getType() { throw new UnsupportedOperationException("stub"); }
    }
}
