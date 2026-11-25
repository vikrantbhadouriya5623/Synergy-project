package com.autosorter.service;

import com.autosorter.exception.AuthenticationException;
import com.autosorter.exception.AuthorizationException;
import com.autosorter.exception.InvalidCommandException;
import com.autosorter.model.Role;
import com.autosorter.model.User;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Handles login, bearer-token sessions and role checks. Thread-safe (ConcurrentHashMap). */
public class AuthService {
    private static final Duration SESSION_TTL = Duration.ofHours(8);

    private record Session(User user, Instant expires) {}

    private final Map<String, User> users = new ConcurrentHashMap<>();
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();

    public AuthService() {
        register(new User("engineer@autosorter.lab", "viva2025", Role.EXAMINER));   // demo creds shown on login page
        register(new User("student@autosorter.lab", "student123", Role.STUDENT));
        register(new User("Circuitech_T08", "colorsortingrobot", Role.EXAMINER));  // login from the earlier IoT dashboard
    }

    public void register(User u) { users.put(u.getOperatorId(), u); }

    public Map<String, Object> login(String operatorId, String passcode) {
        if (operatorId == null || operatorId.isBlank() || passcode == null || passcode.isBlank())
            throw new InvalidCommandException("Operator ID and passcode are required");
        User u = users.get(operatorId.trim().toLowerCase());
        if (u == null || !u.authenticate(passcode))
            throw new AuthenticationException("Invalid terminal credentials");
        String token = UUID.randomUUID().toString();
        sessions.put(token, new Session(u, Instant.now().plus(SESSION_TTL)));
        return Map.of("token", token, "operatorId", u.getOperatorId(), "role", u.getRole().name());
    }

    public User validate(String token) {
        if (token == null) throw new AuthenticationException("Missing bearer token");
        Session s = sessions.get(token);
        if (s == null || Instant.now().isAfter(s.expires())) {
            sessions.remove(token);
            throw new AuthenticationException("Session expired - please sign in again");
        }
        return s.user();
    }

    public void logout(String token) { if (token != null) sessions.remove(token); }

    public void requireCalibrationPrivilege(User u) {
        if (!u.hasCalibrationPrivilege())
            throw new AuthorizationException("Role " + u.getRole() + " has no calibration privilege");
    }
}
