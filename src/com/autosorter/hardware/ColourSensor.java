package com.autosorter.hardware;

import com.autosorter.model.Colour;
import com.autosorter.model.RgbReading;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * INHERITANCE: ColourSensor extends Sensor. Simulates the TCS3200 light-to-frequency converter.
 * Pseudo RGB values are generated around a palette per target colour + Gaussian noise + a slow
 * calibration drift, so the classifier produces mostly-correct but occasionally wrong results
 * (which is what drives the "Sorting Accuracy" figure).
 */
public class ColourSensor extends Sensor {
    private static final double GLARE_PROBABILITY = 0.012;   // washed-out sample (reflection)
    private static final Map<Colour, int[][]> PALETTE = new EnumMap<>(Colour.class);
    static {
        PALETTE.put(Colour.RED,    new int[][]{{182, 94, 61}, {196, 80, 70}});
        PALETTE.put(Colour.GREEN,  new int[][]{{45, 178, 82}, {60, 165, 95}});
        PALETTE.put(Colour.YELLOW, new int[][]{{210, 185, 38}, {225, 170, 50}});
        PALETTE.put(Colour.OTHER,  new int[][]{{62, 95, 164}, {120, 118, 122}, {140, 70, 150}});
    }

    private static final int WAVELENGTH_NM = 640;
    private volatile RgbReading lastReading = new RgbReading(182, 94, 61);
    private double driftPercent = 0.12;     // must stay below 0.3 %
    private volatile boolean scanning = false;

    public ColourSensor() {
        super("TCS3200-01", "TCS3200 Optical Sensor Matrix...");
    }

    /** Takes one RGB sample while an item of colour {@code target} is under the gantry. */
    public synchronized RgbReading sample(Colour target) {
        driftPercent = Math.max(0.0, Math.min(0.29, driftPercent + rng.nextGaussian() * 0.015));
        double gain = 1.0 + driftPercent / 100.0;

        int[] base;
        if (rng.nextDouble() < GLARE_PROBABILITY) {
            base = new int[]{125, 125, 128};                      // glare -> ends up as OTHER
        } else {
            int[][] variants = PALETTE.get(target);
            base = variants[rng.nextInt(variants.length)];
        }
        lastReading = new RgbReading(
                (int) Math.round(base[0] * gain + rng.nextGaussian() * 7),
                (int) Math.round(base[1] * gain + rng.nextGaussian() * 7),
                (int) Math.round(base[2] * gain + rng.nextGaussian() * 7));
        return lastReading;
    }

    /** Polymorphic raw value: the "clear" (unfiltered) channel = mean of R,G,B. */
    @Override
    public double readRaw() {
        RgbReading r = lastReading;
        return (r.getR() + r.getG() + r.getB()) / 3.0;
    }

    @Override
    public synchronized boolean calibrate() {      // white-balance routine
        driftPercent = 0.02;
        return isOnline();
    }

    public RgbReading getLastReading() { return lastReading; }
    public synchronized double getDriftPercent() { return driftPercent; }
    public int getWavelengthNm() { return WAVELENGTH_NM; }
    public void setScanning(boolean scanning) { this.scanning = scanning; }
    public boolean isScanning() { return scanning; }

    @Override
    protected Map<String, Object> readings() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("rgb", lastReading.toJson());
        m.put("wavelengthNm", WAVELENGTH_NM);
        m.put("photodiode", "8x8 Matrix");
        m.put("scanning", scanning);
        return m;
    }
}
