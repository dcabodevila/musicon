package es.musicalia.gestmusica.auth.model;

import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;

/**
 * Server-side, single-use permission to reset the password of {@code email}.
 * It is stored in the HTTP session after the reset code is verified and never sent to the client.
 */
public record ResetPasswordGrant(String email, Instant expiresAt) implements Serializable {

    public static final String SESSION_ATTRIBUTE = ResetPasswordGrant.class.getName();
    public static final Duration VALIDITY = Duration.ofMinutes(15);

    public static ResetPasswordGrant forEmail(String email) {
        return new ResetPasswordGrant(email, Instant.now().plus(VALIDITY));
    }

    public boolean isExpired() {
        return !Instant.now().isBefore(expiresAt);
    }
}
