package es.musicalia.gestmusica.releasenotes;

import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.regex.Pattern;

@Component
public class ReleaseNotesVersionPolicy {

    private static final Pattern VERSION_X_Y = Pattern.compile("^\\d+\\.\\d+$");
    private static final Pattern VERSION_X_Y_Z = Pattern.compile("^\\d+\\.\\d+\\.\\d+$");

    public Optional<String> toEffectiveVersion(String rawVersion) {
        if (rawVersion == null) {
            return Optional.empty();
        }

        String candidate = rawVersion.trim();
        if (VERSION_X_Y.matcher(candidate).matches()) {
            return Optional.of(candidate + ".0");
        }

        if (VERSION_X_Y_Z.matcher(candidate).matches()) {
            String[] parts = candidate.split("\\.");
            return Optional.of(parts[0] + "." + parts[1] + ".0");
        }

        return Optional.empty();
    }

    public Optional<String> toEffectivePrefix(String rawVersion) {
        return toEffectiveVersion(rawVersion).map(effective -> {
            String[] parts = effective.split("\\.");
            return parts[0] + "." + parts[1] + ".";
        });
    }
}
