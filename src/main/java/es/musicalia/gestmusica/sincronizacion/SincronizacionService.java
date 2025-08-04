package es.musicalia.gestmusica.sincronizacion;

import es.musicalia.gestmusica.artista.ArtistaAgenciaRecord;
import es.musicalia.gestmusica.artista.ArtistaRepository;
import es.musicalia.gestmusica.localizacion.Municipio;
import es.musicalia.gestmusica.localizacion.MunicipioRepository;
import es.musicalia.gestmusica.localizacion.Provincia;
import es.musicalia.gestmusica.localizacion.ProvinciaRepository;
import es.musicalia.gestmusica.ocupacion.*;
import es.musicalia.gestmusica.util.ConstantsGestmusica;
import es.musicalia.gestmusica.util.DefaultResponseBody;
import es.musicalia.gestmusicalegacy.ocupacion.OcupacionLegacy;
import es.musicalia.gestmusicalegacy.ocupacion.OcupacionLegacyService;
import io.micrometer.common.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@Slf4j
public class SincronizacionService {

    private final OcupacionService ocupacionService;
    private final OcupacionLegacyService ocupacionLegacyService;
    private final ArtistaRepository artistaRepository;
    private final SincronizacionRepository sincronizacionRepository;
    private final ProvinciaRepository provinciaRepository;
    private final MunicipioRepository municipioRepository;
    private final OcupacionRepository ocupacionRepository;

    @Autowired
    public SincronizacionService(
            OcupacionService ocupacionService,
            @Qualifier("ocupacionLegacyService")
            OcupacionLegacyService ocupacionLegacyService, ArtistaRepository artistaRepository, SincronizacionRepository sincronizacionRepository, ProvinciaRepository provinciaRepository, MunicipioRepository municipioRepository, OcupacionRepository ocupacionRepository) {
        this.ocupacionService = ocupacionService;
        this.ocupacionLegacyService = ocupacionLegacyService;
        this.artistaRepository = artistaRepository;
        this.sincronizacionRepository = sincronizacionRepository;
        this.provinciaRepository = provinciaRepository;
        this.municipioRepository = municipioRepository;
        this.ocupacionRepository = ocupacionRepository;
    }


    @Transactional
    public SincronizacionResult sincronizarOcupacionesArtista(LocalDate fechaDesde, LocalDateTime fechaModificacionDesde, Integer idArtistaLegacy) {
        SincronizacionResult result = new SincronizacionResult();

        try {
            // 1. Obtener datos del sistema legacy
            final List<OcupacionLegacy> ocupacionesLegacy =
                    ocupacionLegacyService.findOcupacionLegacyArtistaFromGestmusicaLegacyDesdeMofidicaic(fechaDesde, fechaModificacionDesde, idArtistaLegacy);

            // 2. Procesar cada ocupación
            for (final OcupacionLegacy legacyOcupacion : ocupacionesLegacy) {
                Sincronizacion nuevaSincronizacion = toSincronizacion(legacyOcupacion);
                DefaultResponseBody response = sincronizarOcupacionIndividual(legacyOcupacion, result);
                nuevaSincronizacion.setProcesado(response.isSuccess());

                if (!response.isSuccess()){
                    nuevaSincronizacion.setCodigoError(response.getMessage() != null ? (response.getMessage().length() > 800 ? response.getMessage().substring(0, 800) : response.getMessage()) : null);
                }

                this.sincronizacionRepository.save(nuevaSincronizacion);

            }

            result.setEliminadas(eliminarOcupacionesBorradasLegacy(fechaDesde));

            log.info("Sincronización completada: {} exitosas, {} errores, {} eliminadas",
                    result.getExitosas(), result.getErrores().size(), result.getEliminadas());

        } catch (Exception e) {
            log.error("Error general en sincronización", e);
            result.setErrorGeneral(e.getMessage());
        }

        return result;
    }

    /**
     * Sincronización unidireccional: Legacy → Nueva
     */
    @Transactional
    public SincronizacionResult sincronizarOcupacionesDesde(LocalDate fechaDesde, LocalDateTime fechaModificacionDesde) {
        SincronizacionResult result = new SincronizacionResult();
        
        try {
            // 1. Obtener datos del sistema legacy
            final List<OcupacionLegacy> ocupacionesLegacy =
                ocupacionLegacyService.findOcupacionLegacyFromGestmusicaLegacyDesdeMofidicaic(fechaDesde, fechaModificacionDesde);
            
            // 2. Procesar cada ocupación
            for (final OcupacionLegacy legacyOcupacion : ocupacionesLegacy) {
                Sincronizacion nuevaSincronizacion = toSincronizacion(legacyOcupacion);
                DefaultResponseBody response = sincronizarOcupacionIndividual(legacyOcupacion, result);
                nuevaSincronizacion.setProcesado(response.isSuccess());

                if (!response.isSuccess()){
                    nuevaSincronizacion.setCodigoError(response.getMessage() != null ? (response.getMessage().length() > 800 ? response.getMessage().substring(0, 800) : response.getMessage()) : null);
                }

                this.sincronizacionRepository.save(nuevaSincronizacion);

            }

            result.setEliminadas(eliminarOcupacionesBorradasLegacy(fechaDesde));

            log.info("Sincronización completada: {} exitosas, {} errores, {} eliminadas",
                    result.getExitosas(), result.getErrores().size(), result.getEliminadas());
            
        } catch (Exception e) {
            log.error("Error general en sincronización", e);
            result.setErrorGeneral(e.getMessage());
        }
        
        return result;
    }

    private int eliminarOcupacionesBorradasLegacy(LocalDate fechaDesde) {
        int eliminadas = 0;
        Optional<Set<Integer>> listaIdsOcupaciones = this.ocupacionLegacyService.findIdsOcupacionesFromDate(fechaDesde);

        try {

            if (listaIdsOcupaciones.isPresent() && !listaIdsOcupaciones.get().isEmpty()) {
                List<Ocupacion> listaOcupacionesEliminar = this.ocupacionRepository.findByIdOcupacionLegacyNotIn(fechaDesde.atStartOfDay(), listaIdsOcupaciones.get()).orElse(new ArrayList<>());

                for (Ocupacion ocupacion : listaOcupacionesEliminar) {
                    ocupacion.setActivo(false);
                    this.ocupacionRepository.save(ocupacion);
                    eliminadas++;
                }

            }
        }catch (Exception e){
            log.error("Error al eliminar ocupaciones borradas", e);
        }
        return eliminadas;
    }


    public static Sincronizacion toSincronizacion(OcupacionLegacy legacyOcupacion) {
        return Sincronizacion.builder()
                .idArtista(Long.valueOf(legacyOcupacion.getIdArtista()))
                .accion("sincronizar")
                .fecha(legacyOcupacion.getFecha().toString())
                .descripcion(legacyOcupacion.getObservaciones())
                .poblacion(legacyOcupacion.getPoblacion())
                .municipio(legacyOcupacion.getAyuntamiento())
                .provincia(legacyOcupacion.getIdProvincia() != null ? legacyOcupacion.getIdProvincia().toString() : "")
                .pais(legacyOcupacion.getPais())
                .lugar(legacyOcupacion.getLugar())
                .estado(legacyOcupacion.getEstado() != null ? legacyOcupacion.getEstado().name() : "")
                .fechaRecepcion(LocalDateTime.now())
                .matinal(legacyOcupacion.getMt())
                .soloMatinal(legacyOcupacion.getSmt())
                .build();
    }

    private DefaultResponseBody sincronizarOcupacionIndividual(OcupacionLegacy legacy, SincronizacionResult result) {
        try {
            // Usar caché para artistas
            Optional<ArtistaAgenciaRecord> optionalArtista = this.artistaRepository.findArtistaByIdArtistaGestmanager(legacy.getIdArtista().longValue());

            if (optionalArtista.isEmpty()) {
                return DefaultResponseBody.builder()
                    .success(false)
                    .message("No existe el id artista: " + legacy.getIdArtista())
                    .messageType("danger")
                    .idEntidad(legacy.getIdArtista().longValue())
                    .build();
            }

            ArtistaAgenciaRecord artista = optionalArtista.get();
            
            // Verificar existencia usando caché
            Optional<Ocupacion> existente = ocupacionService.buscarPorIdOcupacionLegacy(legacy.getIdOcupacion());

            if (existente.isPresent()) {
                procesarOcupacionExistente(existente.get(), legacy, artista, result);
            } else {
                procesarNuevaOcupacion(legacy, artista, result);
            }

            result.incrementarExitosas();
            return crearResponseExitoso(legacy);

        } catch (Exception e) {
            log.error("Error procesando ocupación: {}", legacy.getIdArtista(), e);
            return crearResponseError(legacy);
        }
    }

    private void procesarOcupacionExistente(Ocupacion ocupacion, OcupacionLegacy legacy, 
                                          ArtistaAgenciaRecord artista, SincronizacionResult result) throws ModificacionOcupacionException {

        actualizarOcupacionExistente(ocupacion, legacy, artista);
        result.incrementarActualizadas();

    }

    private boolean puedeModificarse(Ocupacion ocupacion) {
        return true;
    }

    @Transactional
    private void actualizarOcupacionExistente(Ocupacion ocupacion, OcupacionLegacy legacy, 
                                             ArtistaAgenciaRecord artista) throws ModificacionOcupacionException {
        OcupacionSaveDto dto = getOcupacionSaveDto(legacy, artista.idAgencia(), artista.id());
        dto.setId(ocupacion.getId());
        dto.setImporte(ocupacion.getImporte());
        dto.setIva(ocupacion.getIva());
        dto.setPorcentajeRepre(ocupacion.getPorcentajeRepre());
        dto.setTextoOrquestasDeGalicia(ocupacion.getTextoOrquestasDeGalicia());

        this.ocupacionService.guardarOcupacion(dto, true, true);
    }

    private DefaultResponseBody crearResponseError(OcupacionLegacy legacy) {
        return DefaultResponseBody.builder()
            .success(false)
            .message("Error inesperado guardando la ocupacion: " + legacy.getIdArtista())
            .messageType("danger")
            .idEntidad(legacy.getIdOcupacion().longValue())
            .build();
    }

    private DefaultResponseBody crearResponseExitoso(OcupacionLegacy legacy) {
        return DefaultResponseBody.builder()
            .success(true)
            .message("Ocupacion guardada" + legacy.getIdArtista())
            .messageType("success")
            .idEntidad(legacy.getIdArtista().longValue())
            .build();
    }

    private void procesarNuevaOcupacion(OcupacionLegacy legacy, ArtistaAgenciaRecord artista, 
                                         SincronizacionResult result) throws ModificacionOcupacionException {
        DefaultResponseBody response = guardarOcupacionSincronizacion(legacy, artista.idAgencia(), artista.id());
        if (response.isSuccess()) {
            result.incrementarCreadas();
        }
    }

    private Boolean isOcupacionProvisional(String pais){
        return pais!=null && pais.equalsIgnoreCase("OCUPADO");
    }

    private Boolean isOcupacionVacaciones(String pais){
        return pais!=null && pais.equalsIgnoreCase("VACACIONES");
    }

    private Boolean isOcupacionReserva(String pais){
        return pais!=null && (pais.equalsIgnoreCase("RESERVADO") || pais.equalsIgnoreCase("RESERVA"));
    }

    private Boolean isOcupacionPortugal(String pais){
        return pais!=null && pais.equalsIgnoreCase("PORTUGAL");
    }

    private DefaultResponseBody guardarOcupacionSincronizacion(OcupacionLegacy legacy, Long idAgencia, Long idArtista) throws ModificacionOcupacionException {

        if (isOcupacionVacaciones(legacy.getPais())){
            return DefaultResponseBody.builder().success(false).message("Ocupación de tipo vacaciones no implementada " + legacy.getIdArtista()).messageType("danger").idEntidad(legacy.getIdOcupacion().longValue()).build();
        }

        OcupacionSaveDto ocupacionSaveDto = getOcupacionSaveDto(legacy, idAgencia, idArtista);

        final Ocupacion ocupacion = this.ocupacionService.guardarOcupacion(ocupacionSaveDto, true, true);

        return DefaultResponseBody.builder().success(true).message("Ocupación guardada correctamente").messageType("success").idEntidad(ocupacion.getId()).build();
    }



    private OcupacionSaveDto getOcupacionSaveDto(OcupacionLegacy legacy, Long idAgencia, Long idArtista) {
        OcupacionSaveDto ocupacionSaveDto = new OcupacionSaveDto();
        ocupacionSaveDto.setIdOcupacionLegacy(legacy.getIdOcupacion());
        ocupacionSaveDto.setIdArtista(idArtista);

        ocupacionSaveDto.setIdAgencia(idAgencia);
        Optional<Provincia> optionalProvincia = this.provinciaRepository.findProvinciaByIdProvinciaLegacy(legacy.getIdProvincia());

        if (optionalProvincia.isPresent()){
            final Provincia p = optionalProvincia.get();
            ocupacionSaveDto.setIdProvincia(p.getId());
            ocupacionSaveDto.setIdCcaa(p.getCcaa()!=null ? p.getCcaa().getId() : ConstantsGestmusica.ID_CCAA_PROVISIONAL);
        }
        else {
            ocupacionSaveDto.setIdProvincia(ConstantsGestmusica.ID_PROVINCIA_PROVISIONAL);
            ocupacionSaveDto.setIdCcaa(ConstantsGestmusica.ID_CCAA_PROVISIONAL);
        }
        ocupacionSaveDto.setIdMunicipio(ConstantsGestmusica.ID_MUNICIPIO_PROVISIONAL);

        final String nombreMunicipio = normalizarNombre(legacy.getAyuntamiento());

        if (StringUtils.isNotEmpty(nombreMunicipio) && StringUtils.isNotBlank(nombreMunicipio)){
            Optional<List<Municipio>> optionalMunicipio = this.municipioRepository.findMunicipioByNombreLike(nombreMunicipio, ocupacionSaveDto.getIdProvincia());
            optionalMunicipio.ifPresent(municipios -> {
                if (!municipios.isEmpty()) {
                    Municipio closestMatch = municipios.stream()
                            .min((m1, m2) -> Math.abs(m1.getNombre().length() - nombreMunicipio.length()) -
                                    Math.abs(m2.getNombre().length() - nombreMunicipio.length()))
                            .get();
                    ocupacionSaveDto.setIdMunicipio(closestMatch.getId());
                }
            });

        }

        final Boolean isProvisional =  isOcupacionProvisional(legacy.getPais());
        ocupacionSaveDto.setProvisional(isProvisional);
        if (isProvisional || isOcupacionReserva(legacy.getPais())){
            ocupacionSaveDto.setIdCcaa(ConstantsGestmusica.ID_CCAA_PROVISIONAL);
            ocupacionSaveDto.setIdProvincia(ConstantsGestmusica.ID_PROVINCIA_PROVISIONAL);
            ocupacionSaveDto.setIdMunicipio(ConstantsGestmusica.ID_MUNICIPIO_PROVISIONAL);
        }

        if (isOcupacionPortugal(legacy.getPais())){
            ocupacionSaveDto.setIdCcaa(ConstantsGestmusica.ID_CCAA_PORTUGAL);
            ocupacionSaveDto.setIdProvincia(ConstantsGestmusica.ID_PROVINCIA_PORTUGAL);
            ocupacionSaveDto.setIdMunicipio(ConstantsGestmusica.ID_MUNICIPIO_PORTUGAL);
        }


        ocupacionSaveDto.setFecha(legacy.getFecha().atStartOfDay());
        ocupacionSaveDto.setEstado(legacy.getEstado().name());
        ocupacionSaveDto.setIdTipoOcupacion(legacy.getEstado()!=null? TipoOcupacionEnum.findByDescripcion(legacy.getEstado().name()).getId() : TipoOcupacionEnum.OCUPADO.getId());

        ocupacionSaveDto.setLocalidad(legacy.getPoblacion());
        ocupacionSaveDto.setLugar(legacy.getLugar());

        ocupacionSaveDto.setObservaciones(legacy.getObservaciones());
        ocupacionSaveDto.setImporte(BigDecimal.ZERO);
        ocupacionSaveDto.setIva(BigDecimal.ZERO);
        ocupacionSaveDto.setPorcentajeRepre(BigDecimal.ZERO);
        ocupacionSaveDto.setMatinal(legacy.getMt());
        ocupacionSaveDto.setSoloMatinal(legacy.getSmt());
        return ocupacionSaveDto;
    }

    private String normalizarNombre(String nombre) {
        if (nombre == null) return null;

        nombre = nombre.toUpperCase()
                .replaceAll("^(EL|LA|LOS|LAS|O|A|OS|AS)\\s+", "")
                .replaceAll("[ÁÀÄÂÃ]", "A")
                .replaceAll("[ÉÈËÊ]", "E")
                .replaceAll("[ÍÌÏÎ]", "I")
                .replaceAll("[ÓÒÖÔÕ]", "O")
                .replaceAll("[ÚÙÜÛ]", "U");


        log.info("Nombre normalizado: " + nombre);
        return nombre;
    }

}