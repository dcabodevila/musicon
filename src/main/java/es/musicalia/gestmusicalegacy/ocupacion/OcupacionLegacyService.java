package es.musicalia.gestmusicalegacy.ocupacion;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface OcupacionLegacyService {
    List<OcupacionLegacy> findOcupacionLegacyFromGestmusicaLegacy(LocalDate localDate);

    List<OcupacionLegacy> findOcupacionLegacyFromGestmusicaLegacyDesdeMofidicaic(LocalDate localDate, LocalDateTime fechaModificacionDesde);

    Optional<Set<Integer>> findIdsOcupacionesFromDate(LocalDate localDate);
}
