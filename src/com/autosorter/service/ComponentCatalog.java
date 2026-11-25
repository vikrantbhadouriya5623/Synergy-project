package com.autosorter.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Static hardware catalogue for the "System Components" tab. */
public final class ComponentCatalog {
    private ComponentCatalog() {}

    public static List<Map<String, Object>> all() {
        List<Map<String, Object>> list = new ArrayList<>();
        list.add(item("Arduino Uno", "MCU Controller",
                "ATmega328P based orchestrator executing tight 50Hz PWM timing for servo deflection and optical frequency pulse polling.",
                "Clock", "16 MHz", "Operating Voltage", "5V DC", "Digital I/O", "14 Pins (6 PWM)"));
        list.add(item("ESP32", "IoT Bridge",
                "Dual-core SoC that bridges sensor telemetry to this Java backend over HTTP and relays failsafe E-stop broadcasts.",
                "Architecture", "Xtensa LX6 32-bit", "Connectivity", "Wi-Fi & Bluetooth BLE", "Logic Voltage", "3.3V DC"));
        list.add(item("TCS3200 Colour Sensor", "Optical Array",
                "Programmable colour light-to-frequency converter with 4 bright white LEDs illuminating sorting chute specimens.",
                "Array", "8x8 photodiode matrix", "Filters", "Red, Green, Blue, Clear", "Frequency Scaling", "20% (S0:HIGH, S1:LOW)"));
        list.add(item("HC-SR04 Ultrasonic Sensor", "Proximity Gate",
                "Non-contact distance measurement verifying specimen presence in the chute prior to optical reading sequence.",
                "Range", "2 cm - 400 cm", "Accuracy", "\u00b13 mm", "Trigger Pulse", "10 \u00b5s TTL"));
        list.add(item("PIR Sensor", "Intake Interlock",
                "Pyroelectric infrared sensor detecting physical object drops onto the primary feeder chute to arm the sorting belt.",
                "Detection Angle", "< 100\u00b0 cone", "Delay Time", "2.4 sec adjustable", "Output Level", "3.3V High / 0V Low"));
        list.add(item("Servo Motors", "Actuators",
                "MG996R high-torque metal gear servo driving the multi-angle sorting diverter flap, accompanied by SG90 micro servos for intake gate release.",
                "Stall Torque", "11 kg-cm (MG996R)", "Deflection", "Discrete -18\u00b0, 0\u00b0, +20\u00b0", "Response Time", "0.17 s / 60\u00b0"));
        list.add(item("Conveyor Belt", "Continuous Feed",
                "High-grip ribbed polyurethane conveyor driven by a 24V DC geared motor with L298N H-Bridge PWM speed regulation.",
                "Throughput", "30 units/min", "Drive", "24V DC Planetary Gear", "Belt Width", "65 mm High-Traction"));
        list.add(item("Output Bins", "4 Receptacles",
                "Four precision sorting receptacles: Bin #1 (Red), Bin #2 (Green), Bin #3 (Yellow), and Bin #4 (Other/Blue).",
                "Capacity", "500 units / bin", "Chutes", "Low-Friction Guides", "Verification", "Optical Drop Sensors"));
        list.add(item("OLED Display", "Local Readout",
                "0.96-inch I2C SSD1306 OLED screen providing on-chassis operator telemetry: colour classification, distance, and belt state.",
                "Resolution", "128 x 64 pixels", "Interface", "I2C (Address 0x3C)", "Refresh", "30 FPS Live Telemetry"));
        return list;
    }

    private static Map<String, Object> item(String name, String badge, String description, String... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", name);
        m.put("badge", badge);
        m.put("description", description);
        List<Map<String, String>> specs = new ArrayList<>();
        for (int i = 0; i + 1 < kv.length; i += 2) specs.add(Map.of("label", kv[i], "value", kv[i + 1]));
        m.put("specs", specs);
        return m;
    }
}
