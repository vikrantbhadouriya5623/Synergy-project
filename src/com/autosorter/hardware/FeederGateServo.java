package com.autosorter.hardware;

/** SERVO-02 (SG90). METHOD OVERRIDING: jog() pulses the gate instead of stepping the angle. */
public class FeederGateServo extends ServoActuator {
    private int pulses = 0;

    public FeederGateServo() {
        super("SERVO-02 (SG90)", "Chute Feeder Gate Release", "PWM Pin D10 (50Hz)");
    }

    @Override
    public String describeValue() { return getAngle() + "\u00b0 (Open)"; }

    @Override
    public String jog() {
        pulses++;
        return getId() + " gate pulsed (open-close-open), pulse #" + pulses;
    }

    @Override
    public String jogButtonLabel() { return "PULSE GATE"; }

    @Override
    public String getCalibrationLabel() { return "Feeder Gate Datum..."; }
}
