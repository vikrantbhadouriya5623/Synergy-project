package com.autosorter.hardware;

import com.autosorter.model.Colour;

/** SERVO-01 (MG996R). METHOD OVERRIDING: shows the bin position name next to the angle. */
public class DiverterServo extends ServoActuator {
    private volatile Colour lastTarget = Colour.RED;

    public DiverterServo() {
        super("SERVO-01 (MG996R)", "Sorting Diverter Flap", "PWM Pin D9 (50Hz)");
    }

    public void deflectTo(Colour c) {
        setAngle(c.getFlapAngle());
        lastTarget = c;
    }

    @Override
    public String describeValue() { return getAngle() + "\u00b0 (" + lastTarget.getLabel() + " Position)"; }

    @Override
    public String getCalibrationLabel() { return "MG996R Flap Diverter 0\u00b0 Datum..."; }
}
