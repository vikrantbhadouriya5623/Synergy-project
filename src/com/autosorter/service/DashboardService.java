package com.autosorter.service;

import com.autosorter.hardware.Actuator;
import com.autosorter.model.*;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Read-model builder: turns the live objects inside SortingMachine into the exact JSON blocks the
 * frontend tabs need. Handlers stay thin and this class has no HTTP knowledge (separation of concerns).
 */
public class DashboardService {
    private static final long SEED_RUNTIME_SECONDS = 3 * 3600 + 42 * 60 + 19;     // 03:42:19 as in the UI
    private static final Map<Colour, String> UI_KEY = new EnumMap<>(Colour.class);
    static {
        UI_KEY.put(Colour.RED, "red");
        UI_KEY.put(Colour.GREEN, "green");
        UI_KEY.put(Colour.YELLOW, "yellow");
        UI_KEY.put(Colour.OTHER, "blue");           // the HTML ids for Bin #4 use "blue"
    }

    private final SortingMachine machine;

    public DashboardService(SortingMachine machine) { this.machine = machine; }

    // ====================== TAB 1: DASHBOARD ======================
    public Map<String, Object> dashboard() {
        SortingStatistics stats = machine.getStats();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("serverTimeUtc", Instant.now().toString());
        out.put("machine", machineBlock());

        Map<String, Object> pipeline = new LinkedHashMap<>();
        pipeline.put("activeCycle", stats.getTotal() + 1);
        pipeline.put("currentStage", machine.getCurrentStage().getNumber());
        List<Map<String, Object>> stages = new ArrayList<>();
        for (PipelineStage s : PipelineStage.values()) {
            Map<String, Object> st = new LinkedHashMap<>();
            st.put("number", s.getNumber());
            st.put("title", s.getTitle());
            st.put("subtitle", s.getSubtitle());
            stages.add(st);
        }
        pipeline.put("stages", stages);
        out.put("pipeline", pipeline);

        Map<String, Object> counters = new LinkedHashMap<>();
        counters.put("total", stats.getTotal());
        counters.put("rateText", "+1 item / 3s");
        List<Map<String, Object>> bins = new ArrayList<>();
        for (Colour c : Colour.values()) {
            Map<String, Object> b = new LinkedHashMap<>();
            b.put("bin", c.getBin());
            b.put("key", UI_KEY.get(c));
            b.put("label", c.getLabel());
            b.put("hex", c.getHex());
            b.put("count", stats.getCount(c));
            b.put("percent", stats.getPercent(c));
            bins.add(b);
        }
        counters.put("bins", bins);
        out.put("counters", counters);

        Map<String, Object> sensors = new LinkedHashMap<>();
        Map<String, Object> tcs = new LinkedHashMap<>(machine.getColourSensor().toJson());
        Colour cls = machine.getLatestClassification();
        tcs.put("classification", cls.getLabel() + " [Bin #" + cls.getBin() + "]");
        tcs.put("classificationColour", cls.getLabel());
        tcs.put("classificationHex", cls.getHex());
        tcs.put("confidence", machine.getLatestConfidence());
        sensors.put("tcs3200", tcs);
        sensors.put("hcsr04", machine.getUltrasonic().toJson());
        sensors.put("pir", machine.getPir().toJson());
        out.put("sensors", sensors);

        out.put("analytics", analytics());
        SortingResult latest = stats.getLatest();
        out.put("latestResult", latest == null ? null : latest.toJson());
        out.put("telemetryLine", "STATUS // CONVEYOR: " + machine.getMotor().getRpm() + " RPM \u2022 TCS3200: "
                + (machine.isRunning() ? "ACTIVE" : "STANDBY") + " \u2022 DEFLECT: "
                + (machine.getState() == MachineState.EMERGENCY_HALT ? "LOCKED" : "AUTO"));
        return out;
    }

    private Map<String, Object> machineBlock() {
        MachineState s = machine.getState();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("state", s.name());
        m.put("banner", s.getBanner());
        m.put("canvasTag", s.getCanvasTag());
        m.put("running", s == MachineState.RUNNING);
        m.put("emergency", s == MachineState.EMERGENCY_HALT);
        return m;
    }

    private Map<String, Object> analytics() {
        SortingStatistics stats = machine.getStats();
        int perMinute = stats.itemsInLastSeconds(60);
        int duty = Math.min(100, (int) Math.round(15 + perMinute * 1.15));
        Map<String, Object> a = new LinkedHashMap<>();
        a.put("accuracyPercent", stats.getAccuracyPercent());
        a.put("calibrationDriftPercent", Math.round(machine.getColourSensor().getDriftPercent() * 100.0) / 100.0);
        a.put("throughputUnitsPerMin", 30);                         // rated belt throughput
        a.put("measuredUnitsLastMinute", perMinute);
        a.put("throughputLabel", switch (machine.getState()) {
            case RUNNING -> "OPTIMAL";
            case PAUSED -> "IDLE";
            case EMERGENCY_HALT -> "HALTED";
        });
        a.put("servoDutyPercent", machine.isRunning() ? duty : 0);
        a.put("servoDutyLabel", duty > 80 ? "High" : "Normal");
        return a;
    }

    // ====================== TAB 2: MACHINE STATUS ======================
    public Map<String, Object> machineStatus() {
        double up = machine.getUptime().toSeconds();
        double temp = 33.8 + (machine.isRunning() ? 0.6 : 0.0) + 0.15 * Math.sin(up / 20.0);
        temp = Math.round(temp * 10.0) / 10.0;
        double bus = 5.04 + 0.01 * Math.sin(up / 7.0);
        double logic = 3.31 + 0.005 * Math.cos(up / 9.0);
        long runtime = SEED_RUNTIME_SECONDS + machine.getUptime().toSeconds();

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("busStatus", "BUS HEALTHY");
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("chassisTempC", temp);
        metrics.put("thermalMarginC", Math.round((60.0 - temp) * 10.0) / 10.0);
        metrics.put("busVoltageV", Math.round(bus * 100.0) / 100.0);
        metrics.put("logicSupplyV", Math.round(logic * 100.0) / 100.0);
        metrics.put("cycleRuntime", formatHms(Duration.ofSeconds(runtime)));
        out.put("metrics", metrics);

        List<Map<String, Object>> acts = new ArrayList<>();
        for (Actuator a : machine.getActuators()) acts.add(a.toJson());      // polymorphic toJson()
        out.put("actuators", acts);
        out.put("machine", machineBlock());
        return out;
    }

    // ====================== TAB 3: COLOUR DETECTION ======================
    public Map<String, Object> colourDetection() {
        RgbReading rgb = machine.getColourSensor().getLastReading();
        Map<String, Object> out = new LinkedHashMap<>();
        List<Map<String, Object>> ch = new ArrayList<>();
        ch.add(channel("red", "RED FILTER CHANNEL", "S2:LOW / S3:LOW", rgb.getR(), 180, 255));
        ch.add(channel("green", "GREEN FILTER CHANNEL", "S2:HIGH / S3:HIGH", rgb.getG(), 30, 190));
        ch.add(channel("blue", "BLUE FILTER CHANNEL", "S2:LOW / S3:HIGH", rgb.getB(), 25, 210));
        out.put("channels", ch);
        out.put("rules", machine.getClassifier().describeRules());
        out.put("lastClassification", machine.getLatestClassification().getLabel());
        out.put("confidence", machine.getLatestConfidence());
        out.put("driftPercent", Math.round(machine.getColourSensor().getDriftPercent() * 100.0) / 100.0);
        return out;
    }

    private Map<String, Object> channel(String key, String label, String pins, int raw, int min, int max) {
        Map<String, Object> c = new LinkedHashMap<>();
        c.put("key", key);
        c.put("label", label);
        c.put("pins", pins);
        c.put("raw", raw);
        c.put("percent", (int) Math.round(raw * 100.0 / 255.0));
        c.put("rangeMin", min);
        c.put("rangeMax", max);
        c.put("rangeText", "Dynamic range threshold: " + min + " - " + max);
        return c;
    }

    // ====================== TAB 4: SENSOR TELEMETRY ======================
    public Map<String, Object> sensorTelemetry() {
        RgbReading rgb = machine.getColourSensor().getLastReading();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("busRefreshHz", 100);

        Map<String, Object> us = new LinkedHashMap<>();
        us.put("targetCm", Math.round(machine.getUltrasonic().getLastObjectDistance() * 10.0) / 10.0);
        us.put("waveform", machine.getUltrasonic().waveformHeights());
        us.put("windowLabel", "T-minus 3000ms");
        out.put("ultrasonic", us);

        int clear = (int) Math.round(machine.getColourSensor().readRaw() * 100.0 / 255.0);
        List<Map<String, Object>> bars = new ArrayList<>();
        bars.add(bar("RED", (int) Math.round(rgb.getR() * 100.0 / 255.0)));
        bars.add(bar("GREEN", (int) Math.round(rgb.getG() * 100.0 / 255.0)));
        bars.add(bar("BLUE", (int) Math.round(rgb.getB() * 100.0 / 255.0)));
        bars.add(bar("CLEAR", clear));
        Map<String, Object> optical = new LinkedHashMap<>();
        optical.put("peakNm", machine.getColourSensor().getWavelengthNm());
        optical.put("bars", bars);
        optical.put("scalingText", "Scaling: 20% Frequency Output");
        optical.put("snrDb", Math.round((32 + 0.5 * Math.sin(machine.getUptime().toMillis() / 3000.0)) * 10.0) / 10.0);
        out.put("optical", optical);
        return out;
    }

    private Map<String, Object> bar(String label, int percent) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("label", label);
        m.put("percent", percent);
        return m;
    }

    // ====================== LOGS ======================
    public Map<String, Object> logs(int limit) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (SortingResult r : machine.getStats().getRecent(limit)) rows.add(r.toJson());
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("rows", rows);
        out.put("persistedRecords", machine.getRepository().countRecords());
        out.put("file", machine.getRepository().describeLocation());
        return out;
    }

    private static String formatHms(Duration d) {
        long s = d.getSeconds();
        return String.format("%02d:%02d:%02d", s / 3600, (s % 3600) / 60, s % 60);
    }
}
