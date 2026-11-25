package com.autosorter.hardware;

/**
 * INTERFACE (abstraction): anything that can take part in the "Calibrate All" routine.
 * Sensors AND actuators implement it, so SortingMachine can calibrate a mixed
 * List&lt;Calibratable&gt; without knowing the concrete types (polymorphism).
 */
public interface Calibratable {
    String getCalibrationLabel();   // text shown in the calibration modal
    boolean calibrate();            // true = calibration passed
}
