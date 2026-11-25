package com.autosorter.exception;

/** Base domain exception. It carries the HTTP status the API layer should return. */
public class AutoSorterException extends RuntimeException {
    private final int httpStatus;

    public AutoSorterException(String message, int httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
    }

    public int getHttpStatus() { return httpStatus; }
}
