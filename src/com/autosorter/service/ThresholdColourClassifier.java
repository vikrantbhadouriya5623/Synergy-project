package com.autosorter.service;

import com.autosorter.model.Colour;
import com.autosorter.model.RgbReading;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Implements exactly the "Discrete Threshold Decision Matrix" shown in the Colour Detection tab. */
public class ThresholdColourClassifier implements ColourClassifier {

    @Override
    public Colour classify(RgbReading c) {
        int r = c.getR(), g = c.getG(), b = c.getB();
        if (r > 150 && g > 140 && b < 90) return Colour.YELLOW;      // checked first: yellow also has high R
        if (r > g + 50 && r > b + 50) return Colour.RED;
        if (g > r + 30 && g > b + 20) return Colour.GREEN;
        return Colour.OTHER;                                          // default fall-through
    }

    @Override
    public double confidence(RgbReading c, Colour decided) {
        int r = c.getR(), g = c.getG(), b = c.getB();
        double margin = switch (decided) {
            case RED -> Math.min(r - g - 50, r - b - 50);
            case GREEN -> Math.min(g - r - 30, g - b - 20);
            case YELLOW -> Math.min(Math.min(r - 150, g - 140), 90 - b);
            case OTHER -> 40;
        };
        return Math.round(Math.max(0.55, Math.min(0.99, 0.6 + margin / 150.0)) * 100.0) / 100.0;
    }

    @Override
    public List<Map<String, Object>> describeRules() {
        List<Map<String, Object>> rules = new ArrayList<>();
        rules.add(rule(Colour.RED, "R > G + 50 && R > B + 50", "Flap Angle: -18\u00b0"));
        rules.add(rule(Colour.GREEN, "G > R + 30 && G > B + 20", "Flap Angle: 0\u00b0 (Center)"));
        rules.add(rule(Colour.YELLOW, "R > 150 && G > 140 && B < 90", "Flap Angle: +20\u00b0"));
        rules.add(rule(Colour.OTHER, "Default Fall-through", "Pass to Chute End"));
        return rules;
    }

    private Map<String, Object> rule(Colour c, String condition, String action) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("bin", c.getBin());
        m.put("title", "BIN #" + c.getBin() + " [" + c.getLabel() + "]");
        m.put("condition", condition);
        m.put("action", action);
        m.put("flapAngle", c.getFlapAngle());
        return m;
    }
}
