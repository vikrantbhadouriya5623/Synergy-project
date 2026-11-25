package com.autosorter.service;

import com.autosorter.model.Colour;
import com.autosorter.model.RgbReading;

import java.util.List;
import java.util.Map;

/** STRATEGY INTERFACE: the sorting machine depends on this abstraction, not on one algorithm. */
public interface ColourClassifier {
    Colour classify(RgbReading rgb);
    double confidence(RgbReading rgb, Colour decided);
    List<Map<String, Object>> describeRules();
}
