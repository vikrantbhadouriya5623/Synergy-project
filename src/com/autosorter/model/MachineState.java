package com.autosorter.model;

/** Global machine lifecycle states (matches the top status badge in the UI). */
public enum MachineState {
    RUNNING("SYSTEM STATUS: RUNNING / ONLINE", "ONLINE"),
    PAUSED("SYSTEM STATUS: PAUSED / IDLE", "PAUSED"),
    EMERGENCY_HALT("SYSTEM STATUS: EMERGENCY STOPPED", "HALTED");

    private final String banner;
    private final String canvasTag;

    MachineState(String banner, String canvasTag) {
        this.banner = banner;
        this.canvasTag = canvasTag;
    }

    public String getBanner() { return banner; }
    public String getCanvasTag() { return canvasTag; }
}
