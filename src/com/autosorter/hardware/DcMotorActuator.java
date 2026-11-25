package com.autosorter.hardware;

/** MOTOR-DC-01: 24 V geared belt motor behind an L298N H-bridge. Speed steps through a fixed list. */
public class DcMotorActuator extends Actuator {
    private static final int[] SPEEDS = {25, 30, 35, 40};
    private volatile int speedIndex = 1;                // 30 RPM default

    public DcMotorActuator() {
        super("MOTOR-DC-01", "Belt Conveyor Drive", "L298N H-Bridge IN1/IN2");
    }

    public int getRpm() { return isPowered() ? SPEEDS[speedIndex] : 0; }
    public int getDutyPercent() { return (int) Math.round(SPEEDS[speedIndex] * 65.0 / 30.0); }   // 30 RPM = 65 %

    @Override
    public String describeValue() {
        return isPowered() ? SPEEDS[speedIndex] + " RPM [Duty " + getDutyPercent() + "%]" : "0 RPM [Duty 0%]";
    }

    @Override
    public String feedback() { return isPowered() ? "Running" : "Stopped"; }

    @Override
    public String jog() {
        speedIndex = (speedIndex + 1) % SPEEDS.length;
        return getId() + " speed set to " + SPEEDS[speedIndex] + " RPM";
    }

    @Override
    public String jogButtonLabel() { return "SPEED STEP"; }

    @Override
    public String getCalibrationLabel() { return "Belt Motor L298N Speed Regulator..."; }

    @Override
    public boolean calibrate() { speedIndex = 1; return true; }
}
