package es.musicalia.gestmusica.eventopublico;

import es.musicalia.gestmusica.ocupacion.Ocupacion;
import es.musicalia.gestmusica.ocupacion.OcupacionEstadoEnum;
import es.musicalia.gestmusica.ocupacion.OcupacionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventoPublicoServiceImpl implements EventoPublicoService {

    private final OcupacionRepository ocupacionRepository;

    @Override
    public List<EventoPublicoDto> obtenerEventosPublicosPorArtista(Long idArtista) {
        log.info("Obteniendo eventos públicos para artista: {}", idArtista);

        Specification<Ocupacion> spec = Specification.where(null);

        // Solo ocupaciones confirmadas (estado OCUPADO)
        spec = spec.and((root, query, cb) ->
            cb.equal(root.get("ocupacionEstado").get("id"), OcupacionEstadoEnum.OCUPADO.getId()));

        // Solo del artista especificado
        spec = spec.and((root, query, cb) ->
            cb.equal(root.get("artista").get("id"), idArtista));

        // Solo activas
        spec = spec.and((root, query, cb) ->
            cb.isTrue(root.get("activo")));

        // Solo futuras (desde hoy)
        spec = spec.and((root, query, cb) ->
            cb.greaterThanOrEqualTo(root.get("fecha"), LocalDateTime.now()));

        // Ordenar por fecha
        List<Ocupacion> ocupaciones = ocupacionRepository.findAll(spec);

        return ocupaciones.stream()
            .map(this::convertirAEventoPublico)
            .collect(Collectors.toList());
    }

    @Override
    public Optional<EventoPublicoDto> obtenerEventoPublico(Long idOcupacion) {
        log.info("Obteniendo evento público: {}", idOcupacion);

        Optional<Ocupacion> ocupacion = ocupacionRepository.findById(idOcupacion);

        // Verificar que sea un evento público (ocupado, activo y futuro)
        return ocupacion
            .filter(o -> OcupacionEstadoEnum.OCUPADO.getId().equals(o.getOcupacionEstado().getId()))
            .filter(Ocupacion::isActivo)
            .filter(o -> o.getFecha().isAfter(LocalDateTime.now().minusDays(1))) // Permitir eventos del día actual
            .map(this::convertirAEventoPublico);
    }

    @Override
    public List<EventoPublicoDto> obtenerEventosPublicosPorProvincia(String provincia, LocalDate fechaDesde, LocalDate fechaHasta) {
        log.info("Obteniendo eventos públicos para provincia: {}", provincia);

        Specification<Ocupacion> spec = Specification.where(null);

        // Solo ocupaciones confirmadas
        spec = spec.and((root, query, cb) ->
            cb.equal(root.get("ocupacionEstado").get("id"), OcupacionEstadoEnum.OCUPADO.getId()));

        // Solo activas
        spec = spec.and((root, query, cb) ->
            cb.isTrue(root.get("activo")));

        // Filtrar por provincia (case insensitive)
        if (provincia != null && !provincia.isBlank()) {
            spec = spec.and((root, query, cb) ->
                cb.like(cb.lower(root.get("provincia").get("nombre")),
                    "%" + provincia.toLowerCase() + "%"));
        }

        // Filtrar por rango de fechas
        LocalDateTime desde = (fechaDesde != null)
            ? fechaDesde.atStartOfDay()
            : LocalDateTime.now();

        spec = spec.and((root, query, cb) ->
            cb.greaterThanOrEqualTo(root.get("fecha"), desde));

        if (fechaHasta != null) {
            LocalDateTime hasta = fechaHasta.atTime(LocalTime.MAX);
            spec = spec.and((root, query, cb) ->
                cb.lessThanOrEqualTo(root.get("fecha"), hasta));
        }

        List<Ocupacion> ocupaciones = ocupacionRepository.findAll(spec);

        return ocupaciones.stream()
            .map(this::convertirAEventoPublico)
            .collect(Collectors.toList());
    }

    @Override
    public List<EventoPublicoDto> obtenerTodosEventosPublicos() {
        log.info("Obteniendo todos los eventos públicos para sitemap");

        Specification<Ocupacion> spec = Specification.where(null);

        // Solo ocupaciones confirmadas
        spec = spec.and((root, query, cb) ->
            cb.equal(root.get("ocupacionEstado").get("id"), OcupacionEstadoEnum.OCUPADO.getId()));

        // Solo activas
        spec = spec.and((root, query, cb) ->
            cb.isTrue(root.get("activo")));

        // Solo futuras
        spec = spec.and((root, query, cb) ->
            cb.greaterThanOrEqualTo(root.get("fecha"), LocalDateTime.now()));

        List<Ocupacion> ocupaciones = ocupacionRepository.findAll(spec);

        return ocupaciones.stream()
            .map(this::convertirAEventoPublico)
            .collect(Collectors.toList());
    }

    @Override
    public EventoPublicoDto convertirAEventoPublico(Ocupacion ocupacion) {
        // Determinar franja horaria
        int hora = ocupacion.getFecha().getHour();
        boolean esMatinal = ocupacion.isMatinal() || ocupacion.isSoloMatinal();
        boolean esNoche = !ocupacion.isSoloMatinal();

        // Información adicional
        String informacion = ocupacion.getTextoOrquestasDeGalicia();

        return EventoPublicoDto.builder()
            .id(ocupacion.getId())
            .idArtista(ocupacion.getArtista().getId())
            .nombreArtista(ocupacion.getArtista().getNombre())
            .fecha(ocupacion.getFecha())
            .horaActuacion(ocupacion.getHoraActuacion())
            .lugar(ocupacion.getLugar() != null ? ocupacion.getLugar() : ocupacion.getPoblacion())
            .municipio(ocupacion.getMunicipio() != null ? ocupacion.getMunicipio().getNombre() : "")
            .provincia(ocupacion.getProvincia() != null ? ocupacion.getProvincia().getNombre() : "")
            .matinal(esMatinal)
            .noche(esNoche)
            .informacionAdicional(informacion)
            .build();
    }
}
