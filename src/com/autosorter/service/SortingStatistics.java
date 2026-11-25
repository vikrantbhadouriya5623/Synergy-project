package com.autosorter.service;

import com.autosorter.model.Colour;
import com.autosorter.model.SortingResult;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * ENCAPSULATION: all counters live here, private and guarded by synchronized methods because
 * the worker thread writes while HTTP threads read. Seeded with the 1,248 "historic" items shown in the UI.
 */
public class SortingStatistics {
    private static final int RECENT_LIMIT = 50;

    private final Map<Colour, Integer> counts = new EnumMap<>(Colour.class);
    private long total;
    private long correct;
    private final Deque<SortingResult> recent = new ArrayDeque<>();
    private final Deque<Long> sortedAtMillis = new ArrayDeque<>();

    public SortingStatistics() {
        counts.put(Colour.RED, 420);
        counts.put(Colour.GREEN, 350);
        counts.put(Colour.YELLOW, 280);
        counts.put(Colour.OTHER, 198);
        total = 1248;
        correct = 1228;     // 98.4 % accuracy to start with
    }

    public synchronized void record(SortingResult r, boolean classifiedCorrectly) {
        counts.merge(r.getColour(), 1, Integer::sum);
        total++;
        if (classifiedCorrectly) correct++;
        recent.addFirst(r);
        while (recent.size() > RECENT_LIMIT) recent.removeLast();
        sortedAtMillis.addLast(System.currentTimeMillis());
    }

    public synchronized long getTotal() { return total; }
    public synchronized int getCount(Colour c) { return counts.getOrDefault(c, 0); }

    public synchronized int getPercent(Colour c) {
        long sum = 0;
        for (int v : counts.values()) sum += v;
        return sum == 0 ? 0 : (int) Math.round(counts.get(c) * 100.0 / sum);
    }

    public synchronized double getAccuracyPercent() {
        return total == 0 ? 100.0 : Math.round(correct * 1000.0 / total) / 10.0;
    }

    public synchronized int itemsInLastSeconds(int seconds) {
        long cutoff = System.currentTimeMillis() - seconds * 1000L;
        while (!sortedAtMillis.isEmpty() && sortedAtMillis.peekFirst() < cutoff) sortedAtMillis.removeFirst();
        return sortedAtMillis.size();
    }

    public synchronized List<SortingResult> getRecent(int limit) {
        List<SortingResult> out = new ArrayList<>();
        for (SortingResult r : recent) { if (out.size() >= limit) break; out.add(r); }
        return Collections.unmodifiableList(out);
    }

    public synchronized SortingResult getLatest() { return recent.peekFirst(); }
}
