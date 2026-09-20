package xyz.stormclient.setting;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class NumberSetting extends Setting<Double> {

    private final double min;
    private final double max;
    private final double step;
    private String suffix = "";

    public NumberSetting(String name, double value, double min, double max, double step) {
        super(name, value);
        this.min = min;
        this.max = max;
        this.step = step <= 0 ? 0.01 : step;
    }

    @Override public void set(Double value) {
        double clamped = Math.max(min, Math.min(max, value));
        super.set(round(clamped));
    }

    private double round(double v) {
        double snapped = Math.round(v / step) * step;
        return BigDecimal.valueOf(snapped).setScale(decimals(), RoundingMode.HALF_UP).doubleValue();
    }

    public int decimals() {
        if (step >= 1) return 0;
        String s = BigDecimal.valueOf(step).stripTrailingZeros().toPlainString();
        int dot = s.indexOf('.');
        return dot < 0 ? 0 : s.length() - dot - 1;
    }

    public double min()  { return min; }
    public double max()  { return max; }
    public double step() { return step; }

    public int    getInt()   { return (int) Math.round(get()); }
    public long   getLong()  { return Math.round(get()); }
    public float  getFloat() { return get().floatValue(); }

    /** 0..1 position of the current value, for slider rendering. */
    public double fraction() { return max - min == 0 ? 0 : (get() - min) / (max - min); }

    public void setFraction(double fraction) {
        set(min + (max - min) * Math.max(0, Math.min(1, fraction)));
    }

    public NumberSetting suffix(String suffix) { this.suffix = suffix; return this; }

    @Override public String serialize() { return Double.toString(get()); }

    @Override public void deserialize(String raw) {
        try { set(Double.parseDouble(raw)); } catch (NumberFormatException ignored) { }
    }

    @Override public String display() {
        return (decimals() == 0 ? String.valueOf(getInt())
                                : String.format("%." + decimals() + "f", get())) + suffix;
    }
}
