package com.autosorter.model;

public enum Role {
    EXAMINER(true),
    STUDENT(false);

    private final boolean calibrationPrivilege;

    Role(boolean calibrationPrivilege) { this.calibrationPrivilege = calibrationPrivilege; }

    public boolean hasCalibrationPrivilege() { return calibrationPrivilege; }
}
