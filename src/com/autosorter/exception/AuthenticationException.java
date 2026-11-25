package com.autosorter.exception;

public class AuthenticationException extends AutoSorterException {
    public AuthenticationException(String message) { super(message, 401); }
}
