package es.musicalia.gestmusica.releasenotes;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReleaseNotesVersionPolicyTest {

    private final ReleaseNotesVersionPolicy policy = new ReleaseNotesVersionPolicy();

    @Test
    void toEffectiveVersion_debeNormalizarXYZAXY0() {
        assertEquals(Optional.of("1.2.0"), policy.toEffectiveVersion("1.2.9"));
    }

    @Test
    void toEffectiveVersion_debeNormalizarXYAXY0() {
        assertEquals(Optional.of("2.4.0"), policy.toEffectiveVersion("2.4"));
    }

    @Test
    void toEffectiveVersion_debeRechazarFormatosInvalidos() {
        assertTrue(policy.toEffectiveVersion("v1.2.3").isEmpty());
        assertTrue(policy.toEffectiveVersion("1.2.3-beta").isEmpty());
        assertTrue(policy.toEffectiveVersion("1.a.3").isEmpty());
        assertTrue(policy.toEffectiveVersion(null).isEmpty());
    }

    @Test
    void toEffectivePrefix_debeConstruirPrefijoMajorMinor() {
        assertEquals(Optional.of("1.2."), policy.toEffectivePrefix("1.2.7"));
    }
}
