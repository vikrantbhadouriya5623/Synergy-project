package com.autosorter.service;

import com.autosorter.exception.InvalidCommandException;
import com.autosorter.exception.MachineStateException;
import com.autosorter.exception.ResourceNotFoundException;
import com.autosorter.hardware.*;
import com.autosorter.model.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * INHERITANCE: SortingMachine extends Machine (inherits the lifecycle + safety rules).
 * COMPOSITION: it owns the sensors, actuators, classifier, statistics and repository.
 * It is the only class that knows the 5-stage pipeline: detect -> read -> classify -> deflect -> bin.
 */
public class SortingMachine extends Machine {
    private final ColourSensor colourSensor = new ColourSensor();
    private final UltrasonicSensor ultrasonic = new UltrasonicSensor();
    private final PirSensor pir = new PirSensor();
    private final DiverterServo diverter = new DiverterServo();
    private final FeederGateServo gate = new FeederGateServo();
    private final DcMotorActuator motor = new DcMotorActuator();

    private final List<Sensor> sensors = List.of(pir, ultrasonic, colourSensor);
    private final List<Actuator> actuators = List.of(diverter, gate, motor);

    private final ColourClassifier classifier;
    private final SortingStatistics stats;
    private final SortingRepository repository;
    private final long stageMillis;

    private volatile PipelineStage currentStage = PipelineStage.OBJECT_DETECTED;
    private volatile Colour latestClassification = Colour.RED;
    private volatile double latestConfidence = 0.95;

    public SortingMachine(ColourClassifier classifier, SortingStatistics stats,
                          SortingRepository repository, long stageMillis) {
        this.classifier = classifier;
        this.stats = stats;
        this.repository = repository;
        this.stageMillis = stageMillis;
        diverter.setAngle(Colour.RED.getFlapAngle());     // matches the "RED Position" shown at boot
    }

    // ---------- lifecycle hook (called by Machine) ----------
    @Override
    protected void onStateChanged(MachineState s) {
        boolean running = s == MachineState.RUNNING;
        motor.setPowered(running);
        gate.setPowered(running);
        if (s == MachineState.EMERGENCY_HALT) {
            try { diverter.setAngle(0); } catch (InvalidCommandException ignored) { /* 0 deg is always valid */ }
        }
    }

    // ---------- the sorting pipeline ----------
    /** Runs one item through all 5 stages. Aborts (without counting) if E-stop is pressed mid-cycle. */
    public SortingResult processItem(Colour expected) throws InterruptedException {
        long t0 = System.currentTimeMillis();
        try {
            pir.triggerMotion();
            ultrasonic.setObjectInView(true);

            enterStage(PipelineStage.OBJECT_DETECTED);
            double distance = ultrasonic.readChecked();

            enterStage(PipelineStage.COLOUR_READ);
            colourSensor.setScanning(true);
            RgbReading rgb = colourSensor.sample(expected);

            enterStage(PipelineStage.COLOUR_IDENTIFIED);
            Colour decided = classifier.classify(rgb);
            latestClassification = decided;
            latestConfidence = classifier.confidence(rgb, decided);

            enterStage(PipelineStage.SERVO_SORTING);
            diverter.deflectTo(decided);

            enterStage(PipelineStage.OUTPUT_BIN);
            SortingResult result = new SortingResult(stats.getTotal() + 1, decided, rgb, distance,
                    decided.getFlapAngle(), System.currentTimeMillis() - t0, decided == expected);
            stats.record(result, decided == expected);
            repository.append(result);
            return result;
        } finally {
            ultrasonic.setObjectInView(false);
            colourSensor.setScanning(false);
        }
    }

    private void enterStage(PipelineStage stage) throws InterruptedException {
        if (getState() == MachineState.EMERGENCY_HALT)
            throw new MachineStateException("Cycle aborted at " + stage.getTitle() + ": emergency stop engaged");
        currentStage = stage;
        Thread.sleep(stageMillis);
    }

    /** Called by the polling thread: polymorphically reads every sensor through the Sensor type. */
    public void pollSensors() {
        for (Sensor s : sensors) {
            if (s.isOnline()) s.readRaw();
        }
    }

    // ---------- calibration (polymorphism over Calibratable) ----------
    public List<Map<String, Object>> calibrateAll() {
        if (getState() == MachineState.EMERGENCY_HALT)
            throw new MachineStateException("Cannot calibrate while emergency stop is engaged");
        List<Calibratable> devices = new ArrayList<>();
        devices.add(colourSensor);
        devices.add(ultrasonic);
        devices.add(pir);
        devices.add(diverter);
        devices.add(motor);

        List<Map<String, Object>> steps = new ArrayList<>();
        int i = 1;
        for (Calibratable d : devices) {                 // each call dispatches to a different override
            boolean ok = d.calibrate();
            Map<String, Object> step = new LinkedHashMap<>();
            step.put("index", i);
            step.put("total", devices.size());
            step.put("label", "[" + i + "/" + devices.size() + "] " + d.getCalibrationLabel());
            step.put("status", ok ? "OK" : "FAILED");
            steps.add(step);
            i++;
        }
        return steps;
    }

    // ---------- manual actuator jog ----------
    public String jogActuator(String idPrefix) {
        if (getState() == MachineState.EMERGENCY_HALT)
            throw new MachineStateException("Manual jog blocked: emergency stop engaged");
        for (Actuator a : actuators) {
            if (a.getId().toUpperCase().startsWith(idPrefix.toUpperCase())) return a.jog();
        }
        throw new ResourceNotFoundException("Unknown actuator: " + idPrefix);
    }

    // ---------- getters used by the service/API layer ----------
    public ColourSensor getColourSensor() { return colourSensor; }
    public UltrasonicSensor getUltrasonic() { return ultrasonic; }
    public PirSensor getPir() { return pir; }
    public DiverterServo getDiverter() { return diverter; }
    public DcMotorActuator getMotor() { return motor; }
    public List<Actuator> getActuators() { return actuators; }
    public ColourClassifier getClassifier() { return classifier; }
    public SortingStatistics getStats() { return stats; }
    public SortingRepository getRepository() { return repository; }
    public PipelineStage getCurrentStage() { return currentStage; }
    public Colour getLatestClassification() { return latestClassification; }
    public double getLatestConfidence() { return latestConfidence; }
}
