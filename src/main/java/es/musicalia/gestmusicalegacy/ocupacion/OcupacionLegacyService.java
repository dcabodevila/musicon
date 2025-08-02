package es.musicalia.gestmusicalegacy.ocupacion;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface OcupacionLegacyService {
    List<OcupacionLegacy> findOcupacionLegacyFromGestmusicaLegacy(LocalDate localDate);

    Optional<Set<Integer>> findIdsOcupacionesFromDate(LocalDate localDate);
}
