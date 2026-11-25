package com.autosorter.model;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Immutable DTO for one sorted item. All fields are private final and exposed only through
 * getters (ENCAPSULATION). No setters means the history can never be tampered with.
 */
public final class SortingResult implements JsonSerializable {
    private final UUID itemId;
    private final long cycleNumber;
    private final Instant timestamp;
    private final Colour colour;
    private final RgbReading rgb;
    private final double distanceCm;
    private final int servoAngle;
    private final long cycleDurationMs;
    private final boolean success;

    public SortingResult(long cycleNumber, Colour colour, RgbReading rgb, double distanceCm,
                         int servoAngle, long cycleDurationMs, boolean success) {
        this.itemId = UUID.randomUUID();
        this.cycleNumber = cycleNumber;
        this.timestamp = Instant.now();
        this.colour = colour;
        this.rgb = rgb;
        this.distanceCm = distanceCm;
        this.servoAngle = servoAngle;
        this.cycleDurationMs = cycleDurationMs;
        this.success = success;
    }

    public UUID getItemId() { return itemId; }
    public long getCycleNumber() { return cycleNumber; }
    public Instant getTimestamp() { return timestamp; }
    public Colour getColour() { return colour; }
    public int getAssignedBin() { return colour.getBin(); }
    public int[] getRgbVector() { return rgb.toArray(); }
    public RgbReading getRgb() { return rgb; }
    public double getDistanceCm() { return distanceCm; }
    public int getServoAngle() { return servoAngle; }
    public long getCycleDurationMs() { return cycleDurationMs; }
    public boolean isSuccess() { return success; }

    /** One CSV line for the file-handling layer. */
    public String toCsvLine() {
        return String.join(",", timestamp.toString(), String.valueOf(cycleNumber), colour.getLabel(),
                "BIN" + colour.getBin(), String.valueOf(rgb.getR()), String.valueOf(rgb.getG()),
                String.valueOf(rgb.getB()), String.format("%.1f", distanceCm),
                String.valueOf(servoAngle), String.valueOf(cycleDurationMs), success ? "SUCCESS" : "FAILURE");
    }

    @Override
    public Map<String, Object> toJson() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("itemId", itemId.toString());
        m.put("cycle", cycleNumber);
        m.put("timestamp", timestamp.toString());
        m.put("detectedColour", colour.getLabel());
        m.put("sortedToBin", colour.getBin());
        m.put("rgb", rgb.toJson());
        m.put("distanceCm", Math.round(distanceCm * 10.0) / 10.0);
        m.put("servoAngle", servoAngle);
        m.put("cycleDurationMs", cycleDurationMs);
        m.put("status", success ? "Success" : "Failure");
        return m;
    }
}
