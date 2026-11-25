package com.autosorter.exception;

public class SensorFaultException extends AutoSorterException {
    public SensorFaultException(String message) { super(message, 503); }
}
