package es.musicalia.gestmusica.passkey;

import java.time.Instant;

/**
 * Passkey shown to its owner: device name and dates, never key material.
 */
public record PasskeyDto(String id, String etiqueta, Instant fechaCreacion, Instant fechaUltimoUso) {
}
