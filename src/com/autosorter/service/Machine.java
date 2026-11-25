package com.autosorter.service;

import com.autosorter.exception.MachineStateException;
import com.autosorter.model.MachineState;

import java.time.Duration;
import java.time.Instant;

/**
 * ABSTRACT BASE CLASS: the generic machine lifecycle (RUNNING / PAUSED / EMERGENCY_HALT)
 * with its safety rules. Concrete machines only decide what a state change does physically.
 * The state field is private, so the ONLY way to change it is through the guarded methods (ENCAPSULATION).
 */
public abstract class Machine {
    private volatile MachineState state = MachineState.RUNNING;
    private final Instant bootTime = Instant.now();

    public synchronized void startMachine() {
        if (state == MachineState.EMERGENCY_HALT)
            throw new MachineStateException("Emergency stop is engaged - use RESET & RESUME OPERATION first");
        changeState(MachineState.RUNNING);
    }

    public synchronized void pauseMachine() {
        if (state == MachineState.EMERGENCY_HALT)
            throw new MachineStateException("Emergency stop is engaged - cannot pause");
        changeState(MachineState.PAUSED);
    }

    public synchronized void emergencyStop() { changeState(MachineState.EMERGENCY_HALT); }

    public synchronized void resetEmergencyStop() {
        if (state != MachineState.EMERGENCY_HALT)
            throw new MachineStateException("Emergency stop is not engaged");
        changeState(MachineState.RUNNING);
    }

    private void changeState(MachineState next) {
        this.state = next;
        onStateChanged(next);
    }

    /** Hook that subclasses implement (e.g. cut motor power, home the servo). */
    protected abstract void onStateChanged(MachineState newState);

    public MachineState getState() { return state; }
    public boolean isRunning() { return state == MachineState.RUNNING; }
    public Duration getUptime() { return Duration.between(bootTime, Instant.now()); }
}
