package es.musicalia.gestmusica.eventopublico;

import es.musicalia.gestmusica.generic.CodigoNombreRecord;
import es.musicalia.gestmusica.localizacion.LocalizacionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventoPublicoCatalogoFacadeImpl implements EventoPublicoCatalogoFacade {

    private static final String PROVINCIA_CORUNA_CANONICA = "Coruña";
    private static final String PROVINCIA_CORUNA_ALIAS = "A Coruña";
    private static final Set<String> PROVINCIAS_EXCLUIDAS_PUBLICAS = Set.of("provisional", "otras");
    private static final Set<String> MUNICIPIOS_EXCLUIDOS_PUBLICOS = Set.of("provisional", "sin asignar");
    private static final int MAX_DYNAMIC_QUICK_LINKS = 7;

    private final EventoPublicoService eventoPublicoService;
    private final LocalizacionService localizacionService;

    @Override
    public EventoPublicoCatalogoView prepararCatalogoPublico(EventoPublicoCatalogoRequest request) {
        LocalDate hoy = LocalDate.now();
        List<EventoPublicoDto> eventosCatalogo = eventoPublicoService.obtenerEventosPublicosFiltrados(
            null, null, null, hoy, hoy.plusDays(EventoPublicoConstantes.HORIZONTE_DIAS_PUBLICOS));

        String provinciaConsulta = normalizarProvinciaParaConsulta(request.provincia());
        var paginaEventos = eventoPublicoService.obtenerEventosPublicosFiltradosPaginados(
            provinciaConsulta, request.municipio(), request.idArtista(), request.fechaDesde(), request.fechaHasta(), request.pageable());

        List<String> provincias = obtenerProvinciasPublicasOrdenadas();

        List<CodigoNombreRecord> municipiosProvincia = (request.provincia() != null && !request.provincia().isBlank())
            ? obtenerMunicipiosPublicosPorProvincia(request.provincia())
            : List.of();

        List<EventoPublicoDto> artistasDisponibles = obtenerArtistasOrdenados(eventosCatalogo);

        return new EventoPublicoCatalogoView(
            paginaEventos,
            eventosCatalogo,
            provincias,
            municipiosProvincia,
            artistasDisponibles,
            construirQuickLinks(eventosCatalogo),
            construirTituloListado(request.provincia(), request.municipio(), request.idArtista(), (int) paginaEventos.getTotalElements()),
            construirDescripcionListado(request.provincia(), request.municipio(), request.idArtista())
        );
    }

    @Override
    public List<QuickLinkView> obtenerQuickLinksPublicos() {
        return obtenerQuickLinksPublicos(null, null);
    }

    @Override
    public List<QuickLinkView> obtenerQuickLinksPublicos(String provincia, String municipio) {
        LocalDate hoy = LocalDate.now();
        List<EventoPublicoDto> eventosCatalogo = eventoPublicoService.obtenerEventosPublicosFiltrados(
            null, null, null, hoy, hoy.plusDays(EventoPublicoConstantes.HORIZONTE_DIAS_PUBLICOS));
        return construirQuickLinks(eventosCatalogo, provincia, municipio);
    }

    @Override
    public String normalizarProvinciaCanonica(String provincia) {
        if (provincia == null) return "";
        return PROVINCIA_CORUNA_ALIAS.equalsIgnoreCase(provincia.trim()) ? PROVINCIA_CORUNA_CANONICA : provincia.trim();
    }

    @Override
    public String normalizarProvinciaParaConsulta(String provincia) {
        if (provincia == null) return "";
        String provinciaTrim = provincia.trim();
        return PROVINCIA_CORUNA_CANONICA.equalsIgnoreCase(provinciaTrim) ? PROVINCIA_CORUNA_ALIAS : provinciaTrim;
    }

    @Override
    public boolean esProvinciaExcluidaPublica(String provincia) {
        return PROVINCIAS_EXCLUIDAS_PUBLICAS.contains(normalizarProvinciaCanonica(provincia).toLowerCase(Locale.ROOT));
    }

    @Override
    public List<String> obtenerProvinciasPublicasOrdenadas() {
        return localizacionService.findAllProvincias().stream()
            .map(CodigoNombreRecord::nombre)
            .filter(nombre -> nombre != null && !nombre.isBlank())
            .map(this::normalizarProvinciaCanonica)
            .filter(nombre -> !esProvinciaExcluidaPublica(nombre))
            .distinct()
            .sorted(String.CASE_INSENSITIVE_ORDER)
            .collect(Collectors.toList());
    }

    @Override
    public List<CodigoNombreRecord> obtenerMunicipiosPublicosPorProvincia(String provincia) {
        if (provincia == null || provincia.isBlank()) {
            return List.of();
        }
        return localizacionService.findMunicipiosByProvinciaNombre(normalizarProvinciaParaConsulta(provincia)).stream()
            .filter(municipio -> municipio.nombre() != null && !municipio.nombre().isBlank())
            .filter(municipio -> !esMunicipioExcluidoPublico(municipio.nombre()))
            .toList();
    }

    @Override
    public List<EventoPublicoDto> obtenerArtistasOrdenados(List<EventoPublicoDto> eventos) {
        return eventos.stream()
            .filter(Objects::nonNull)
            .filter(e -> e.getIdArtista() != null)
            .collect(Collectors.toMap(
                EventoPublicoDto::getIdArtista,
                e -> EventoPublicoDto.builder().idArtista(e.getIdArtista()).nombreArtista(e.getNombreArtista()).build(),
                (existing, replacement) -> existing,
                LinkedHashMap::new
            ))
            .values().stream()
            .sorted(Comparator.comparing(EventoPublicoDto::getNombreArtista, String.CASE_INSENSITIVE_ORDER))
            .collect(Collectors.toList());
    }

    private List<QuickLinkView> construirQuickLinks(List<EventoPublicoDto> eventosCatalogo) {
        return construirQuickLinks(eventosCatalogo, null, null);
    }

    private List<QuickLinkView> construirQuickLinks(List<EventoPublicoDto> eventosCatalogo, String provincia, String municipio) {
        LocalDate hoy = LocalDate.now();
        LocalDate viernes = calcularViernesObjetivo(hoy);
        LocalDate domingo = calcularDomingoObjetivo(hoy);

        List<QuickLinkView> quickLinks = new ArrayList<>();
        quickLinks.add(new QuickLinkView("Fiestas hoy", "/eventos/hoy", "today", false));
        quickLinks.add(new QuickLinkView(
            "Fiestas este fin de semana",
            "/eventos?desde=" + viernes + "&hasta=" + domingo,
            "weekend",
            false
        ));

        List<EventoPublicoDto> eventosValidos = eventosCatalogo.stream()
            .filter(Objects::nonNull)
            .filter(evento -> evento.getFecha() != null && !evento.getFecha().toLocalDate().isBefore(hoy))
            .filter(evento -> evento.getProvincia() != null && !evento.getProvincia().isBlank())
            .filter(evento -> evento.getMunicipio() != null && !evento.getMunicipio().isBlank())
            .filter(evento -> !esProvinciaExcluidaPublica(evento.getProvincia()))
            .filter(evento -> !esMunicipioExcluidoPublico(evento.getMunicipio()))
            .toList();

        List<QuickLinkView> dinamicos = new ArrayList<>();
        String provinciaContexto = normalizarProvinciaCanonica(provincia);
        String municipioContexto = municipio == null ? "" : municipio.trim();

        if (!provinciaContexto.isBlank()) {
            dinamicos.addAll(obtenerQuickLinksMunicipioContextuales(eventosValidos, provinciaContexto, municipioContexto));
        } else {
            dinamicos.addAll(obtenerQuickLinksProvincia(eventosValidos));
            dinamicos.addAll(obtenerQuickLinksMunicipio(eventosValidos));
        }

        quickLinks.addAll(dinamicos.stream().limit(MAX_DYNAMIC_QUICK_LINKS).toList());
        return quickLinks;
    }

    private List<QuickLinkView> obtenerQuickLinksProvincia(List<EventoPublicoDto> eventosValidos) {
        return eventosValidos.stream()
            .collect(Collectors.groupingBy(
                evento -> normalizarProvinciaCanonica(evento.getProvincia()),
                Collectors.collectingAndThen(Collectors.toList(), lista -> new ConteoQuickLink(
                    normalizarProvinciaCanonica(lista.get(0).getProvincia()),
                    lista.size(),
                    lista.stream().map(e -> e.getFecha().toLocalDate()).min(LocalDate::compareTo).orElse(LocalDate.MAX)
                ))
            ))
            .values().stream()
            .sorted(Comparator.comparingLong(ConteoQuickLink::totalActuaciones).reversed()
                .thenComparing(ConteoQuickLink::primeraFecha)
                .thenComparing(ConteoQuickLink::nombre, String.CASE_INSENSITIVE_ORDER))
            .limit(4)
            .map(provincia -> new QuickLinkView(
                "Fiestas en " + provincia.nombre(),
                "/eventos/provincia/" + UriUtils.encodePath(provincia.nombre(), StandardCharsets.UTF_8),
                "province",
                true
            ))
            .toList();
    }

    private List<QuickLinkView> obtenerQuickLinksMunicipio(List<EventoPublicoDto> eventosValidos) {
        return eventosValidos.stream()
            .collect(Collectors.groupingBy(
                evento -> new MunicipioQuickLinkKey(evento.getMunicipio(), normalizarProvinciaCanonica(evento.getProvincia())),
                Collectors.collectingAndThen(Collectors.toList(), lista -> new ConteoQuickLink(
                    lista.get(0).getMunicipio(),
                    lista.size(),
                    lista.stream().map(e -> e.getFecha().toLocalDate()).min(LocalDate::compareTo).orElse(LocalDate.MAX)
                ))
            ))
            .entrySet().stream()
            .sorted(Comparator.comparingLong((Map.Entry<MunicipioQuickLinkKey, ConteoQuickLink> e) -> e.getValue().totalActuaciones()).reversed()
                .thenComparing(e -> e.getValue().primeraFecha())
                .thenComparing(e -> e.getKey().municipio(), String.CASE_INSENSITIVE_ORDER)
                .thenComparing(e -> e.getKey().provincia(), String.CASE_INSENSITIVE_ORDER))
            .limit(3)
            .map(entry -> new QuickLinkView(
                "Verbenas en " + entry.getKey().municipio(),
                "/eventos/municipio/" + UriUtils.encodePath(entry.getKey().municipio(), StandardCharsets.UTF_8),
                "municipality",
                true
            ))
            .toList();
    }

    private List<QuickLinkView> obtenerQuickLinksMunicipioContextuales(List<EventoPublicoDto> eventosValidos, String provincia, String municipioActual) {
        return eventosValidos.stream()
            .filter(evento -> provincia.equalsIgnoreCase(normalizarProvinciaCanonica(evento.getProvincia())))
            .filter(evento -> municipioActual.isBlank() || !municipioActual.equalsIgnoreCase(evento.getMunicipio().trim()))
            .collect(Collectors.groupingBy(
                evento -> evento.getMunicipio().trim(),
                Collectors.collectingAndThen(Collectors.toList(), lista -> new ConteoQuickLink(
                    lista.get(0).getMunicipio().trim(),
                    lista.size(),
                    lista.stream().map(e -> e.getFecha().toLocalDate()).min(LocalDate::compareTo).orElse(LocalDate.MAX)
                ))
            ))
            .values().stream()
            .sorted(Comparator.comparingLong(ConteoQuickLink::totalActuaciones).reversed()
                .thenComparing(ConteoQuickLink::primeraFecha)
                .thenComparing(ConteoQuickLink::nombre, String.CASE_INSENSITIVE_ORDER))
            .map(conteo -> new QuickLinkView(
                "Verbenas en " + conteo.nombre(),
                "/eventos/municipio/" + UriUtils.encodePath(conteo.nombre(), StandardCharsets.UTF_8),
                "municipality",
                true
            ))
            .toList();
    }

    static LocalDate calcularViernesObjetivo(LocalDate fechaBase) {
        return switch (fechaBase.getDayOfWeek()) {
            case FRIDAY, SATURDAY -> fechaBase.with(DayOfWeek.FRIDAY);
            case SUNDAY -> fechaBase.minusDays(2);
            default -> fechaBase.with(DayOfWeek.FRIDAY);
        };
    }

    static LocalDate calcularDomingoObjetivo(LocalDate fechaBase) {
        return switch (fechaBase.getDayOfWeek()) {
            case FRIDAY, SATURDAY -> fechaBase.with(DayOfWeek.SUNDAY);
            case SUNDAY -> fechaBase;
            default -> fechaBase.with(DayOfWeek.SUNDAY);
        };
    }

    private boolean esMunicipioExcluidoPublico(String municipio) {
        return municipio != null
            && MUNICIPIOS_EXCLUIDOS_PUBLICOS.contains(municipio.trim().toLowerCase(Locale.ROOT));
    }

    private String construirTituloListado(String provincia, String municipio, Long idArtista, int totalEventos) {
        String base = "Orquestas, verbenas y actuaciones musicales en España | Festia";
        if (provincia != null && !provincia.isBlank()) {
            base = "Orquestas, verbenas y fiestas en " + provincia + " | Festia";
        } else if (municipio != null && !municipio.isBlank()) {
            base = "Fiestas y verbenas en " + municipio + " | Festia";
        } else if (idArtista != null) {
            base = "Próximas actuaciones del artista | Festia";
        }
        return totalEventos > 0 ? base : "Sin eventos disponibles | Festia";
    }

    private String construirDescripcionListado(String provincia, String municipio, Long idArtista) {
        if (provincia != null && !provincia.isBlank()) {
            return "Consulta actuaciones musicales, orquestas y verbenas en " + provincia + " con fechas y municipios actualizados.";
        }
        if (municipio != null && !municipio.isBlank()) {
            return "Descubre próximas fiestas, verbenas y actuaciones musicales en " + municipio + ".";
        }
        if (idArtista != null) {
            return "Explora las próximas actuaciones públicas del artista con fechas y localizaciones actualizadas.";
        }
        return "Consulta fiestas, verbenas, orquestas y actuaciones musicales de próximos eventos en España.";
    }

    private record ConteoQuickLink(String nombre, long totalActuaciones, LocalDate primeraFecha) {}
    private record MunicipioQuickLinkKey(String municipio, String provincia) {}
}
