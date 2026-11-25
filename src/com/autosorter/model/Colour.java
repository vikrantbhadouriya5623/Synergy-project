package com.autosorter.model;

/**
 * The four sorting classes shown in the dashboard (Bin #1..#4).
 * An enum is the safest way to encapsulate a fixed set of values together with their data.
 */
public enum Colour {
    RED(1, "RED", -18, "#EF4444"),
    GREEN(2, "GREEN", 0, "#10B981"),
    YELLOW(3, "YELLOW", 20, "#EAB308"),
    OTHER(4, "OTHER", 0, "#64748B");   // fall-through: flap stays neutral, item passes to chute end

    private final int bin;
    private final String label;
    private final int flapAngle;
    private final String hex;

    Colour(int bin, String label, int flapAngle, String hex) {
        this.bin = bin;
        this.label = label;
        this.flapAngle = flapAngle;
        this.hex = hex;
    }

    public int getBin() { return bin; }
    public String getLabel() { return label; }
    public int getFlapAngle() { return flapAngle; }
    public String getHex() { return hex; }
}
