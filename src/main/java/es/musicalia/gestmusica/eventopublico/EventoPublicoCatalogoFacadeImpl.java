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
    private static final int MAX_DYNAMIC_QUICK_LINKS = 7;

    private final EventoPublicoService eventoPublicoService;
    private final LocalizacionService localizacionService;

    @Override
    public EventoPublicoCatalogoView prepararCatalogoPublico(EventoPublicoCatalogoRequest request) {
        List<EventoPublicoDto> eventosCatalogo = eventoPublicoService.obtenerEventosPublicosFiltrados(
            null, null, null, LocalDate.now(), null);

        String provinciaConsulta = normalizarProvinciaParaConsulta(request.provincia());
        var paginaEventos = eventoPublicoService.obtenerEventosPublicosFiltradosPaginados(
            provinciaConsulta, request.municipio(), request.idArtista(), request.fechaDesde(), request.fechaHasta(), request.pageable());

        List<String> provincias = localizacionService.findAllProvincias().stream()
            .map(prov -> normalizarProvinciaCanonica(prov.nombre()))
            .filter(nombre -> !nombre.isBlank())
            .filter(nombre -> !esProvinciaExcluidaPublica(nombre))
            .distinct()
            .sorted(String.CASE_INSENSITIVE_ORDER)
            .collect(Collectors.toList());

        List<CodigoNombreRecord> municipiosProvincia = (request.provincia() != null && !request.provincia().isBlank())
            ? localizacionService.findMunicipiosByProvinciaNombre(normalizarProvinciaParaConsulta(request.provincia()))
            : List.of();

        List<EventoPublicoDto> artistasDisponibles = eventosCatalogo.stream()
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

    private List<QuickLinkView> construirQuickLinks(List<EventoPublicoDto> eventosCatalogo) {
        LocalDate hoy = LocalDate.now();
        LocalDate sabado = calcularSabadoObjetivo(hoy);
        LocalDate domingo = sabado.plusDays(1);

        List<QuickLinkView> quickLinks = new ArrayList<>();
        quickLinks.add(new QuickLinkView("Fiestas hoy", "/eventos/hoy", "today", false));
        quickLinks.add(new QuickLinkView(
            "Fiestas este fin de semana",
            "/eventos?desde=" + sabado + "&hasta=" + domingo,
            "weekend",
            false
        ));

        List<EventoPublicoDto> eventosValidos = eventosCatalogo.stream()
            .filter(Objects::nonNull)
            .filter(evento -> evento.getFecha() != null && !evento.getFecha().toLocalDate().isBefore(hoy))
            .filter(evento -> evento.getProvincia() != null && !evento.getProvincia().isBlank())
            .filter(evento -> evento.getMunicipio() != null && !evento.getMunicipio().isBlank())
            .filter(evento -> !esProvinciaExcluidaPublica(evento.getProvincia()))
            .toList();

        List<QuickLinkView> dinamicos = new ArrayList<>();
        dinamicos.addAll(obtenerQuickLinksProvincia(eventosValidos));
        dinamicos.addAll(obtenerQuickLinksMunicipio(eventosValidos));

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

    private LocalDate calcularSabadoObjetivo(LocalDate fechaBase) {
        return switch (fechaBase.getDayOfWeek()) {
            case SATURDAY -> fechaBase;
            case SUNDAY -> fechaBase.minusDays(1);
            default -> fechaBase.with(DayOfWeek.SATURDAY);
        };
    }

    private String normalizarProvinciaCanonica(String provincia) {
        if (provincia == null) return "";
        return PROVINCIA_CORUNA_ALIAS.equalsIgnoreCase(provincia.trim()) ? PROVINCIA_CORUNA_CANONICA : provincia.trim();
    }

    private String normalizarProvinciaParaConsulta(String provincia) {
        if (provincia == null) return "";
        String provinciaTrim = provincia.trim();
        return PROVINCIA_CORUNA_CANONICA.equalsIgnoreCase(provinciaTrim) ? PROVINCIA_CORUNA_ALIAS : provinciaTrim;
    }

    private boolean esProvinciaExcluidaPublica(String provincia) {
        return PROVINCIAS_EXCLUIDAS_PUBLICAS.contains(normalizarProvinciaCanonica(provincia).toLowerCase(Locale.ROOT));
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
