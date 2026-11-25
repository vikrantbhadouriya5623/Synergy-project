package com.autosorter.service;

import com.autosorter.model.SortingResult;

/** Persistence abstraction. A JDBC implementation can be dropped in later without touching the machine. */
public interface SortingRepository {
    void append(SortingResult result);
    long countRecords();
    String describeLocation();
}
