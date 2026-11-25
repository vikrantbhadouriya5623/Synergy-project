package com.autosorter.exception;

public class InvalidCommandException extends AutoSorterException {
    public InvalidCommandException(String message) { super(message, 400); }
}
