package es.musicalia.gestmusica.eventopublico;

import es.musicalia.gestmusica.ocupacion.Ocupacion;
import es.musicalia.gestmusica.ocupacion.OcupacionEstadoEnum;
import es.musicalia.gestmusica.ocupacion.OcupacionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
        log.info("Obteniendo eventos publicos para artista: {}", idArtista);
        return obtenerEventosPublicosFiltrados(null, null, idArtista, LocalDate.now(), null);
    }

    @Override
    public Optional<EventoPublicoDto> obtenerEventoPublico(Long idOcupacion) {
        log.info("Obteniendo evento publico: {}", idOcupacion);

        Optional<Ocupacion> ocupacion = ocupacionRepository.findById(idOcupacion);
        return ocupacion
            .filter(o -> OcupacionEstadoEnum.OCUPADO.getId().equals(o.getOcupacionEstado().getId()))
            .filter(Ocupacion::isActivo)
            .filter(o -> o.getArtista() != null && o.getArtista().isActivo())
            .filter(o -> o.getArtista() != null && o.getArtista().isPublicarEventos())
            .map(this::convertirAEventoPublico);
    }

    @Override
    public List<EventoPublicoDto> obtenerEventosPublicosPorProvincia(String provincia, LocalDate fechaDesde, LocalDate fechaHasta) {
        log.info("Obteniendo eventos publicos para provincia: {}", provincia);
        return obtenerEventosPublicosFiltrados(provincia, null, null, fechaDesde, fechaHasta);
    }

    @Override
    public List<EventoPublicoDto> obtenerEventosPublicosPorMunicipio(String municipio, LocalDate fechaDesde, LocalDate fechaHasta) {
        log.info("Obteniendo eventos publicos para municipio: {}", municipio);
        return obtenerEventosPublicosFiltrados(null, municipio, null, fechaDesde, fechaHasta);
    }

    @Override
    public List<EventoPublicoDto> obtenerEventosPublicosFiltrados(
        String provincia,
        String municipio,
        Long idArtista,
        LocalDate fechaDesde,
        LocalDate fechaHasta) {

        Specification<Ocupacion> spec = buildFiltrosPublicosSpec(provincia, municipio, idArtista, fechaDesde, fechaHasta);
        return ocupacionRepository.findAll(spec, Sort.by(Sort.Direction.ASC, "fecha", "artista.nombre")).stream()
            .map(this::convertirAEventoPublico)
            .collect(Collectors.toList());
    }

    @Override
    public Page<EventoPublicoDto> obtenerEventosPublicosFiltradosPaginados(
        String provincia,
        String municipio,
        Long idArtista,
        LocalDate fechaDesde,
        LocalDate fechaHasta,
        Pageable pageable) {

        Specification<Ocupacion> spec = buildFiltrosPublicosSpec(provincia, municipio, idArtista, fechaDesde, fechaHasta);
        return ocupacionRepository.findAll(spec, pageable)
            .map(this::convertirAEventoPublico);
    }

    @Override
    public List<EventoPublicoDto> obtenerTodosEventosPublicos() {
        log.info("Obteniendo todos los eventos publicos para sitemap");

        Specification<Ocupacion> spec = Specification.where(null);
        spec = spec.and((root, query, cb) ->
            cb.equal(root.get("ocupacionEstado").get("id"), OcupacionEstadoEnum.OCUPADO.getId()));
        spec = spec.and((root, query, cb) -> cb.isTrue(root.get("activo")));
        spec = spec.and((root, query, cb) -> cb.isTrue(root.get("artista").get("activo")));
        spec = spec.and((root, query, cb) -> cb.isTrue(root.get("artista").get("publicarEventos")));
        spec = spec.and((root, query, cb) -> cb.isNotNull(root.get("municipio")));
        spec = spec.and((root, query, cb) -> cb.notEqual(cb.trim(root.get("municipio").get("nombre")), ""));
        spec = spec.and((root, query, cb) -> cb.notLike(cb.lower(root.get("provincia").get("nombre")), "%provisional%"));
        spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("fecha"), LocalDateTime.now()));

        return ocupacionRepository.findAll(spec, Sort.by(Sort.Direction.ASC, "fecha", "artista.nombre")).stream()
            .map(this::convertirAEventoPublico)
            .collect(Collectors.toList());
    }

    @Override
    public EventoPublicoDto convertirAEventoPublico(Ocupacion ocupacion) {
        boolean esMatinal = ocupacion.isMatinal() || ocupacion.isSoloMatinal();
        boolean esNoche = !ocupacion.isSoloMatinal();
        String informacion = ocupacion.getTextoOrquestasDeGalicia();

        return EventoPublicoDto.builder()
            .id(ocupacion.getId())
            .idArtista(ocupacion.getArtista().getId())
            .nombreArtista(ocupacion.getArtista().getNombre())
            .nombreAgencia(ocupacion.getArtista().getAgencia() != null ? ocupacion.getArtista().getAgencia().getNombre() : null)
            .urlOrganizador(obtenerUrlOrganizador(ocupacion))
            .logoArtista(ocupacion.getArtista().getLogo())
            .fecha(ocupacion.getFecha())
            .fechaActualizacion(ocupacion.getFechaModificacion() != null ? ocupacion.getFechaModificacion() : ocupacion.getFechaCreacion())
            .horaActuacion(ocupacion.getHoraActuacionDesde())
            .horaActuacionHasta(ocupacion.getHoraActuacionHasta())
            .lugar(ocupacion.getLugar() != null ? ocupacion.getLugar() : ocupacion.getPoblacion())
            .municipio(ocupacion.getMunicipio() != null ? ocupacion.getMunicipio().getNombre() : "")
            .provincia(ocupacion.getProvincia() != null ? ocupacion.getProvincia().getNombre() : "")
            .matinal(esMatinal)
            .tarde(false)
            .noche(esNoche)
            .informacionAdicional(informacion)
            .build();
    }

    private String obtenerUrlOrganizador(Ocupacion ocupacion) {
        if (ocupacion.getArtista() == null || ocupacion.getArtista().getAgencia() == null
            || ocupacion.getArtista().getAgencia().getAgenciaContacto() == null) {
            return null;
        }

        String web = ocupacion.getArtista().getAgencia().getAgenciaContacto().getWeb();
        if (web == null || web.isBlank()) {
            return null;
        }

        String url = web.trim();
        if (url.startsWith("http://") || url.startsWith("https://")) {
            return url;
        }
        return "https://" + url;
    }

    private Specification<Ocupacion> buildFiltrosPublicosSpec(
        String provincia,
        String municipio,
        Long idArtista,
        LocalDate fechaDesde,
        LocalDate fechaHasta) {

        Specification<Ocupacion> spec = Specification.where(null);
        spec = spec.and((root, query, cb) ->
            cb.equal(root.get("ocupacionEstado").get("id"), OcupacionEstadoEnum.OCUPADO.getId()));
        spec = spec.and((root, query, cb) -> cb.isTrue(root.get("activo")));
        spec = spec.and((root, query, cb) -> cb.isTrue(root.get("artista").get("activo")));
        spec = spec.and((root, query, cb) -> cb.isTrue(root.get("artista").get("publicarEventos")));
        spec = spec.and((root, query, cb) -> cb.isNotNull(root.get("municipio")));
        spec = spec.and((root, query, cb) -> cb.notEqual(cb.trim(root.get("municipio").get("nombre")), ""));
        spec = spec.and((root, query, cb) -> cb.notLike(cb.lower(root.get("provincia").get("nombre")), "%provisional%"));
        spec = spec.and((root, query, cb) -> cb.or(
            cb.isNull(root.get("lugar")),
            cb.notLike(cb.lower(root.get("lugar")), "%provisional%")
        ));

        if (provincia != null && !provincia.isBlank()) {
            String provinciaLower = provincia.toLowerCase();
            spec = spec.and((root, query, cb) ->
                cb.equal(cb.lower(root.get("provincia").get("nombre")), provinciaLower));
        }

        if (municipio != null && !municipio.isBlank()) {
            String municipioLower = municipio.toLowerCase();
            spec = spec.and((root, query, cb) ->
                cb.equal(cb.lower(root.get("municipio").get("nombre")), municipioLower));
        }

        if (idArtista != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("artista").get("id"), idArtista));
        }

        LocalDateTime desde = (fechaDesde != null) ? fechaDesde.atStartOfDay() : LocalDateTime.now();
        spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("fecha"), desde));

        if (fechaHasta != null) {
            LocalDateTime hasta = fechaHasta.atTime(LocalTime.MAX);
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("fecha"), hasta));
        }

        return spec;
    }
}
