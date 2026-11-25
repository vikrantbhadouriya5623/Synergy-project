package com.autosorter.exception;

public class MachineStateException extends AutoSorterException {
    public MachineStateException(String message) { super(message, 409); }
}
