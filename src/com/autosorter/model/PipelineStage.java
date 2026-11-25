package com.autosorter.model;

/** The 5-stage sorting pipeline card row on the dashboard. */
public enum PipelineStage {
    OBJECT_DETECTED(1, "Object Detected", "HC-SR04 / Detection"),
    COLOUR_READ(2, "TCS3200 Colour Read", "RGB Sampling"),
    COLOUR_IDENTIFIED(3, "Colour Identified", "Classification"),
    SERVO_SORTING(4, "Servo Sorting", "Servo Deflection"),
    OUTPUT_BIN(5, "Output Bin", "Sorted Successfully");

    private final int number;
    private final String title;
    private final String subtitle;

    PipelineStage(int number, String title, String subtitle) {
        this.number = number;
        this.title = title;
        this.subtitle = subtitle;
    }

    public int getNumber() { return number; }
    public String getTitle() { return title; }
    public String getSubtitle() { return subtitle; }
}
