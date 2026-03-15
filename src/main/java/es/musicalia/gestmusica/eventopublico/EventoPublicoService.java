package es.musicalia.gestmusica.eventopublico;

import es.musicalia.gestmusica.ocupacion.Ocupacion;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface EventoPublicoService {

    /**
     * Obtiene todos los eventos públicos de un artista (ocupaciones confirmadas futuras)
     * @param idArtista ID del artista
     * @return Lista de eventos públicos
     */
    List<EventoPublicoDto> obtenerEventosPublicosPorArtista(Long idArtista);

    /**
     * Obtiene un evento público específico por su ID de ocupación
     * @param idOcupacion ID de la ocupación
     * @return Evento público si existe y es público
     */
    Optional<EventoPublicoDto> obtenerEventoPublico(Long idOcupacion);

    /**
     * Obtiene eventos públicos por provincia
     * @param provincia Nombre de la provincia
     * @param fechaDesde Fecha desde (opcional)
     * @param fechaHasta Fecha hasta (opcional)
     * @return Lista de eventos públicos
     */
    List<EventoPublicoDto> obtenerEventosPublicosPorProvincia(String provincia, LocalDate fechaDesde, LocalDate fechaHasta);

    /**
     * Obtiene eventos publicos con filtros combinables.
     * @param provincia Nombre de provincia (opcional)
     * @param municipio Nombre de municipio (opcional)
     * @param idArtista ID de artista (opcional)
     * @param fechaDesde Fecha desde (opcional)
     * @param fechaHasta Fecha hasta (opcional)
     * @return Lista de eventos publicos filtrados
     */
    List<EventoPublicoDto> obtenerEventosPublicosFiltrados(
        String provincia,
        String municipio,
        Long idArtista,
        LocalDate fechaDesde,
        LocalDate fechaHasta);

    /**
     * Obtiene todos los eventos públicos para el sitemap
     * @return Lista de todos los eventos públicos futuros
     */
    List<EventoPublicoDto> obtenerTodosEventosPublicos();

    /**
     * Convierte una entidad Ocupacion a DTO público
     * @param ocupacion Entidad ocupación
     * @return DTO de evento público
     */
    EventoPublicoDto convertirAEventoPublico(Ocupacion ocupacion);
}
