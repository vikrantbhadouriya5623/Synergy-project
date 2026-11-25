package com.autosorter.hardware;

import com.autosorter.exception.SensorFaultException;
import com.autosorter.model.JsonSerializable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

/**
 * ABSTRACT BASE CLASS for every sensing peripheral (HC-SR04, PIR, TCS3200).
 *  - ABSTRACTION : declares what every sensor must do (readRaw, calibrate, readings)
 *  - ENCAPSULATION: id / name / online flag are private or protected, accessed through methods
 *  - TEMPLATE METHOD: toJson() is final and fixed; subclasses only fill in readings()
 */
public abstract class Sensor implements Calibratable, JsonSerializable {
    private final String sensorId;
    private final String name;
    private volatile boolean online = true;
    protected final Random rng = new Random();

    protected Sensor(String sensorId, String name) {
        this.sensorId = sensorId;
        this.name = name;
    }

    /** Raw numeric sample (cm for ultrasonic, 0/1 for PIR, clear-channel for colour). */
    public abstract double readRaw();

    /** Sensor-specific fields placed into the JSON snapshot. */
    protected abstract Map<String, Object> readings();

    /** Reads only when the sensor is healthy; otherwise throws (exception-handling demo). */
    public double readChecked() {
        if (!online) throw new SensorFaultException(name + " [" + sensorId + "] is offline");
        return readRaw();
    }

    public String getSensorId() { return sensorId; }
    public String getName() { return name; }
    public boolean isOnline() { return online; }
    public void setOnline(boolean online) { this.online = online; }

    @Override
    public String getCalibrationLabel() { return name; }

    @Override
    public final Map<String, Object> toJson() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", sensorId);
        m.put("name", name);
        m.put("status", online ? "ONLINE" : "OFFLINE");
        m.putAll(readings());
        return m;
    }
}
