package com.labhub.server.security;

import at.favre.lib.crypto.bcrypt.BCrypt;

/**
 * Provides BCrypt password hashing and verification.
 *
 * <p>Algorithm: BCrypt (at.favre.lib:bcrypt:0.10.2)
 * Cost factor: 12 - provides ~300ms hashing time on modern hardware,
 * appropriate for a university application. Increase for production.
 *
 * <p>Plaintext passwords MUST NOT be stored or logged anywhere.
 */
public final class PasswordHasher {

    /** BCrypt work factor. Higher = slower = more brute-force resistant. */
    private static final int COST_FACTOR = 12;

    private PasswordHasher() { }

    /**
     * Hashes a plaintext password using BCrypt.
     * Result is safe to store in the database.
     *
     * @param plaintext the raw password; must not be null or empty
     * @return BCrypt hash string
     */
    public static String hash(String plaintext) {
        if (plaintext == null || plaintext.isEmpty()) {
            throw new IllegalArgumentException("Password must not be null or empty.");
        }
        return BCrypt.withDefaults().hashToString(COST_FACTOR, plaintext.toCharArray());
    }

    /**
     * Verifies a plaintext password against a stored BCrypt hash.
     *
     * @param plaintext   the candidate password
     * @param storedHash  the hash stored in the database
     * @return true if the password matches; false otherwise
     */
    public static boolean verify(String plaintext, String storedHash) {
        if (plaintext == null || storedHash == null) {
            return false;
        }
        BCrypt.Result result = BCrypt.verifyer()
                .verify(plaintext.toCharArray(), storedHash);
        return result.verified;
    }
}
