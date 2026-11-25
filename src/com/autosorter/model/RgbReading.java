package com.autosorter.model;

import java.util.LinkedHashMap;
import java.util.Map;

/** Immutable value object: one RGB sample from the TCS3200 (0-255 per channel). */
public final class RgbReading implements JsonSerializable {
    private final int r, g, b;

    public RgbReading(int r, int g, int b) {
        this.r = clamp(r);
        this.g = clamp(g);
        this.b = clamp(b);
    }

    private static int clamp(int v) { return Math.max(0, Math.min(255, v)); }

    public int getR() { return r; }
    public int getG() { return g; }
    public int getB() { return b; }
    public int[] toArray() { return new int[]{r, g, b}; }

    /** Text exactly as the dashboard card prints it: "RGB: 182 / 94 / 61". */
    public String display() { return "RGB: " + r + " / " + g + " / " + b; }

    @Override
    public Map<String, Object> toJson() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("r", r); m.put("g", g); m.put("b", b);
        m.put("display", display());
        return m;
    }
}
