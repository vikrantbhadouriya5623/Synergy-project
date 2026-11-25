package com.autosorter.service;

import com.autosorter.model.SortingResult;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/** FILE HANDLING: appends every sorted item to logs/sorting_log.csv. IO errors never stop the machine. */
public class CsvSortingRepository implements SortingRepository {
    private static final String HEADER = "timestamp,cycle,colour,bin,r,g,b,distance_cm,servo_angle,cycle_ms,status";
    private final Path file;

    public CsvSortingRepository(Path file) {
        this.file = file;
        try {
            if (file.getParent() != null) Files.createDirectories(file.getParent());
            if (Files.notExists(file)) Files.writeString(file, HEADER + System.lineSeparator(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("[CSV] cannot initialise log file: " + e.getMessage());
        }
    }

    @Override
    public synchronized void append(SortingResult result) {
        try (BufferedWriter w = Files.newBufferedWriter(file, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            w.write(result.toCsvLine());
            w.newLine();
        } catch (IOException e) {
            System.err.println("[CSV] write failed (machine keeps running): " + e.getMessage());
        }
    }

    @Override
    public synchronized long countRecords() {
        try (BufferedReader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            return Math.max(0, r.lines().count() - 1);      // minus header
        } catch (IOException e) {
            return 0;
        }
    }

    @Override
    public String describeLocation() { return file.toAbsolutePath().toString(); }
}
