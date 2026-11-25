package com.autosorter.hardware;

import com.autosorter.model.JsonSerializable;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * ABSTRACT BASE CLASS for output devices (servos, DC motor).
 * Each concrete actuator overrides describeValue(), feedback() and jog() differently,
 * and the API layer simply calls them on an Actuator reference (runtime polymorphism).
 */
public abstract class Actuator implements Calibratable, JsonSerializable {
    private final String id;
    private final String function;
    private final String signalType;
    private volatile boolean powered = true;

    protected Actuator(String id, String function, String signalType) {
        this.id = id;
        this.function = function;
        this.signalType = signalType;
    }

    public abstract String describeValue();          // e.g. "-18° (RED Position)"
    public abstract String feedback();               // e.g. "Locked" / "Running"
    public abstract String jog();                    // manual command, returns a human message
    public abstract String jogButtonLabel();         // label of the manual-jog button

    public String getId() { return id; }
    public String getFunction() { return function; }
    public String getSignalType() { return signalType; }
    public boolean isPowered() { return powered; }
    public void setPowered(boolean powered) { this.powered = powered; }

    @Override
    public Map<String, Object> toJson() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("function", function);
        m.put("currentValue", describeValue());
        m.put("signalType", signalType);
        m.put("feedback", feedback());
        m.put("jogLabel", jogButtonLabel());
        return m;
    }
}
