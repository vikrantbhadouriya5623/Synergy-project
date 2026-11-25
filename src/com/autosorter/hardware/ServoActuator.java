package com.autosorter.hardware;

import com.autosorter.exception.InvalidCommandException;

/** Generic hobby servo (50 Hz PWM). Angle is encapsulated and range-checked in one place. */
public class ServoActuator extends Actuator {
    private static final int MIN_DEG = -45, MAX_DEG = 45;
    private volatile int angle = 0;

    public ServoActuator(String id, String function, String signalType) {
        super(id, function, signalType);
    }

    public void setAngle(int deg) {
        if (deg < MIN_DEG || deg > MAX_DEG)
            throw new InvalidCommandException("Servo angle " + deg + "\u00b0 outside safe range [" + MIN_DEG + ", " + MAX_DEG + "]");
        this.angle = deg;
    }

    public int getAngle() { return angle; }

    @Override
    public String describeValue() { return angle + "\u00b0"; }

    @Override
    public String feedback() { return isPowered() ? "Locked" : "Unpowered"; }

    @Override
    public String jog() {
        int next = angle + 5;
        if (next > MAX_DEG) { angle = 0; return getId() + " limit reached - homed to 0\u00b0"; }
        angle = next;
        return getId() + " jogged +5\u00b0 to " + angle + "\u00b0";
    }

    @Override
    public String jogButtonLabel() { return "JOG +5\u00b0"; }

    @Override
    public String getCalibrationLabel() { return "Servo Flap Diverter 0\u00b0 Datum..."; }

    @Override
    public boolean calibrate() { angle = 0; return true; }
}
