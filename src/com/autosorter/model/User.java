package com.autosorter.model;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Operator account. The passcode is stored only as a SHA-256 hash (ENCAPSULATION + security). */
public class User {
    private final String operatorId;
    private final String passwordHash;
    private final Role role;

    public User(String operatorId, String plainPassword, Role role) {
        this.operatorId = operatorId.toLowerCase();
        this.passwordHash = hash(plainPassword);
        this.role = role;
    }

    public String getOperatorId() { return operatorId; }
    public Role getRole() { return role; }

    public boolean authenticate(String plainToken) {
        return plainToken != null && passwordHash.equals(hash(plainToken));
    }

    public boolean hasCalibrationPrivilege() { return role.hasCalibrationPrivilege(); }

    private static String hash(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(s.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
