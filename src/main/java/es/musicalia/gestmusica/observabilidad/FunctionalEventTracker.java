package es.musicalia.gestmusica.observabilidad;

import java.util.Collections;
import java.util.Map;

public interface FunctionalEventTracker {

    default void track(String eventName, String outcome, Long userId, String username) {
        track(eventName, outcome, userId, username, Collections.emptyMap());
    }

    void track(String eventName, String outcome, Long userId, String username, Map<String, ?> attributes);
}
