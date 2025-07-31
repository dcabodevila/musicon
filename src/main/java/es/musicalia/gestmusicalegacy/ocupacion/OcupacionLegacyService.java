package es.musicalia.gestmusicalegacy.ocupacion;

import java.time.LocalDate;
import java.util.List;

public interface OcupacionLegacyService {
    List<OcupacionLegacy> findOcupacionLegacyFromGestmusicaLegacy(LocalDate localDate);
}
