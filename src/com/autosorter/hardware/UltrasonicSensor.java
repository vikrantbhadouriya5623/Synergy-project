package com.autosorter.hardware;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * HC-SR04 simulation (TRIG = D9, ECHO = D10, MAX_DISTANCE = 200 cm as in the Arduino sketch).
 * With an empty belt it sees the far wall (~25 cm); with an item in view it reads ~8.4 cm,
 * which is below the 10 cm threshold and therefore means "object present".
 * Echo time follows the real physics: distance(cm) = echo(us) / 58.2.
 */
public class UltrasonicSensor extends Sensor {
    public static final double THRESHOLD_CM = 10.0;
    private static final double EMPTY_BELT_CM = 25.0;
    private static final double ITEM_CM = 8.4;
    private static final int HISTORY = 12;

    private final Deque<Double> history = new ArrayDeque<>();   // COLLECTIONS: bounded sample buffer
    private volatile boolean objectInView = false;
    private volatile double lastObjectDistance = ITEM_CM;
    private volatile double baselineCm = ITEM_CM;

    public UltrasonicSensor() {
        super("HCSR04-01", "HC-SR04 Ultrasonic Distance Datum...");
    }

    @Override
    public double readRaw() {
        double d = (objectInView ? baselineCm : EMPTY_BELT_CM) + rng.nextGaussian() * 0.25;
        d = Math.max(2.0, Math.min(200.0, d));
        if (objectInView) lastObjectDistance = d;
        synchronized (history) {
            history.addLast(d);
            while (history.size() > HISTORY) history.removeFirst();
        }
        return d;
    }

    @Override
    public boolean calibrate() {
        baselineCm = ITEM_CM;      // re-zero datum at 8.4 cm
        return isOnline();
    }

    public void setObjectInView(boolean v) { this.objectInView = v; }
    public boolean isObjectPresent() { return objectInView && lastObjectDistance < THRESHOLD_CM; }
    public double getLastObjectDistance() { return lastObjectDistance; }
    public static double echoMicros(double cm) { return cm * 58.2; }

    /** Last 12 samples converted to bar heights (closer = taller), for the waveform widget. */
    public List<Integer> waveformHeights() {
        List<Integer> out = new ArrayList<>();
        synchronized (history) {
            for (double d : history) out.add((int) Math.max(8, Math.min(100, Math.round((1 - d / 30.0) * 100))));
        }
        while (out.size() < HISTORY) out.add(0, 12);
        return out;
    }

    @Override
    protected Map<String, Object> readings() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("distanceCm", Math.round(lastObjectDistance * 10.0) / 10.0);
        m.put("triggerPulse", "10 \u03bcs TTL");
        m.put("echoTimeUs", Math.round(echoMicros(lastObjectDistance)));
        m.put("thresholdCm", THRESHOLD_CM);
        m.put("objectPresent", isObjectPresent());
        return m;
    }
}
