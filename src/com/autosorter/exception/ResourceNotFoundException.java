package com.autosorter.exception;

public class ResourceNotFoundException extends AutoSorterException {
    public ResourceNotFoundException(String message) { super(message, 404); }
}
