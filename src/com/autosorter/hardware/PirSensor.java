package com.autosorter.hardware;

import java.util.LinkedHashMap;
import java.util.Map;

/** PIR motion sensor on pin D2: latches "motion" for the feed-delay time after an item drops in the chute. */
public class PirSensor extends Sensor {
    private static final double FEED_DELAY_SEC = 2.4;
    private volatile long motionUntilMillis = 0;

    public PirSensor() {
        super("PIR-01", "PIR Intake Motion Interlock...");
    }

    public void triggerMotion() {
        motionUntilMillis = System.currentTimeMillis() + (long) (FEED_DELAY_SEC * 1000);
    }

    public boolean isMotionDetected() { return System.currentTimeMillis() < motionUntilMillis; }

    @Override
    public double readRaw() { return isMotionDetected() ? 1.0 : 0.0; }

    @Override
    public boolean calibrate() { return isOnline(); }

    @Override
    protected Map<String, Object> readings() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("detection", isMotionDetected() ? "ACTIVE" : "ARMED");
        m.put("feedDelaySec", FEED_DELAY_SEC);
        m.put("position", "Chute Intake #1");
        m.put("interlock", "Auto-Feed");
        return m;
    }
}
