package com.autosorter.exception;

public class AuthorizationException extends AutoSorterException {
    public AuthorizationException(String message) { super(message, 403); }
}
