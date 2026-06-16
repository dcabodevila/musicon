package es.musicalia.gestmusica.eventopublico;

import es.musicalia.gestmusica.generic.CodigoNombreRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface EventoPublicoCatalogoFacade {

    EventoPublicoCatalogoView prepararCatalogoPublico(EventoPublicoCatalogoRequest request);

    List<QuickLinkView> obtenerQuickLinksPublicos();

    List<QuickLinkView> obtenerQuickLinksPublicos(String provincia, String municipio);

    record EventoPublicoCatalogoRequest(
        String provincia,
        String municipio,
        Long idArtista,
        LocalDate fechaDesde,
        LocalDate fechaHasta,
        Pageable pageable,
        int page
    ) {}

    record EventoPublicoCatalogoView(
        Page<EventoPublicoDto> paginaEventos,
        List<EventoPublicoDto> eventosCatalogo,
        List<String> provincias,
        List<CodigoNombreRecord> municipiosProvincia,
        List<EventoPublicoDto> artistasDisponibles,
        List<QuickLinkView> quickLinks,
        String titulo,
        String descripcion
    ) {}

    record QuickLinkView(String label, String href, String tone, boolean dynamic) {}
}
