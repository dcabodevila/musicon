package es.musicalia.gestmusica.api;

import es.musicalia.gestmusica.artista.Artista;
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
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class SincronizacionService {


    private final OcupacionService ocupacionService;
    private final OcupacionLegacyService ocupacionLegacyService;
    private final ArtistaRepository artistaRepository;
    private final SincronizacionRepository sincronizacionRepository;
    private final ProvinciaRepository provinciaRepository;
    private final MunicipioRepository municipioRepository;

    @Autowired
    public SincronizacionService(
            OcupacionService ocupacionService,
            @Qualifier("ocupacionLegacyService")
            OcupacionLegacyService ocupacionLegacyService, ArtistaRepository artistaRepository, SincronizacionRepository sincronizacionRepository, ProvinciaRepository provinciaRepository, MunicipioRepository municipioRepository) {
        this.ocupacionService = ocupacionService;
        this.ocupacionLegacyService = ocupacionLegacyService;
        this.artistaRepository = artistaRepository;
        this.sincronizacionRepository = sincronizacionRepository;
        this.provinciaRepository = provinciaRepository;
        this.municipioRepository = municipioRepository;
    }

    /**
     * Sincronización unidireccional: Legacy → Nueva
     */
    @Transactional
    public SincronizacionResult sincronizarOcupacionesDesde(LocalDate fechaDesde) {
        SincronizacionResult result = new SincronizacionResult();
        
        try {
            // 1. Obtener datos del sistema legacy
            List<OcupacionLegacy> ocupacionesLegacy =
                ocupacionLegacyService.findOcupacionLegacyFromGestmusicaLegacy(fechaDesde);
            
            // 2. Procesar cada ocupación
            for (OcupacionLegacy legacyOcupacion : ocupacionesLegacy) {
                Sincronizacion nuevaSincronizacion = toSincronizacion(legacyOcupacion);
                nuevaSincronizacion = this.sincronizacionRepository.saveAndFlush(nuevaSincronizacion);

                try {
                    sincronizarOcupacionIndividual(legacyOcupacion, result);

                    nuevaSincronizacion.setProcesado(Boolean.TRUE);
                } catch (SincronizacionException e) {
                    log.error("Error SincronizacionException ocupación ID: {}",
                            legacyOcupacion.getIdOcupacion(), e);
                    result.addError(legacyOcupacion.getIdOcupacion(), e.getMessage());
                    nuevaSincronizacion.setProcesado(false);
                    nuevaSincronizacion.setCodigoError(e.getMessage() != null ? (e.getMessage().length() > 800 ? e.getMessage().substring(0, 800) : e.getMessage()) : null);
                }

                catch (Exception e) {
                    log.error("Error sincronizando ocupación ID: {}", 
                             legacyOcupacion.getIdOcupacion(), e);
                    result.addError(legacyOcupacion.getIdOcupacion(), e.getMessage());
                    nuevaSincronizacion.setProcesado(false);
                    nuevaSincronizacion.setCodigoError(e.getMessage() != null ? (e.getMessage().length() > 800 ? e.getMessage().substring(0, 800) : e.getMessage()) : null);
                }
                this.sincronizacionRepository.save(nuevaSincronizacion);

            }
            
            log.info("Sincronización completada: {} exitosas, {} errores", 
                    result.getExitosas(), result.getErrores().size());
            
        } catch (Exception e) {
            log.error("Error general en sincronización", e);
            result.setErrorGeneral(e.getMessage());
        }
        
        return result;
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

    private void sincronizarOcupacionIndividual(OcupacionLegacy legacy, SincronizacionResult result) throws SincronizacionException, ModificacionOcupacionException {


        Optional<Artista> optionalArtista = this.artistaRepository.findArtistaByIdArtistaGestmanager(legacy.getIdArtista().longValue());
        
        if (optionalArtista.isPresent()){
            final Artista artista = optionalArtista.get();
            // Verificar si ya existe en el sistema nuevo
            Optional<Ocupacion> existente = ocupacionService
                    .buscarPorIdOcupacionLegacy(legacy.getIdOcupacion());

            if (existente.isPresent()) {
                final Ocupacion ocupacion = existente.get();
                if (ocupacion.getUsuarioModificacion()==null || ConstantsGestmusica.USUARIO_SINCRONIZACION.equalsIgnoreCase(ocupacion.getUsuarioModificacion())){
                    OcupacionSaveDto ocupacionSaveDto = getOcupacionSaveDto(legacy, artista.getAgencia().getId(), artista.getId());
                    ocupacionSaveDto.setId(ocupacion.getId());
                    this.ocupacionService.guardarOcupacion(ocupacionSaveDto, true);
                    result.incrementarActualizadas();
                }
                else {
                    result.incrementarSinCambios();
                }


            } else {

                DefaultResponseBody response = guardarOcupacionSincronizacion(legacy, artista.getAgencia().getId(), artista.getId());

                if (response.isSuccess()){
                    result.incrementarCreadas();
                }



            }

            result.incrementarExitosas();
        } 
        else {
            throw new SincronizacionException("No existe el id artista: "+ legacy.getIdArtista());
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

    private DefaultResponseBody guardarOcupacionSincronizacion(OcupacionLegacy legacy, Long idAgencia, Long idArtista) throws ModificacionOcupacionException, SincronizacionException {

        if (isOcupacionVacaciones(legacy.getPais())){
            throw new SincronizacionException("Ocupación de tipo vacaciones no implementada "+ legacy.getIdArtista());
        }

        OcupacionSaveDto ocupacionSaveDto = getOcupacionSaveDto(legacy, idAgencia, idArtista);

        final Ocupacion ocupacion = this.ocupacionService.guardarOcupacion(ocupacionSaveDto, true);

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
            ocupacionSaveDto.setIdCcaa(p.getCcaa()!=null ? p.getCcaa().getId() : ConstantsGestmusica.ID_COMUNIDAD_OTROS);
        }
        else {
            ocupacionSaveDto.setIdProvincia(ConstantsGestmusica.ID_PROVINCIA_OTROS);
            ocupacionSaveDto.setIdCcaa(ConstantsGestmusica.ID_COMUNIDAD_OTROS);
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
            // Ensure Portugal occupations also have a municipality set
            ocupacionSaveDto.setIdMunicipio(ConstantsGestmusica.ID_MUNICIPIO_PROVISIONAL);
        }


        ocupacionSaveDto.setFecha(legacy.getFecha().atStartOfDay());
        ocupacionSaveDto.setEstado(legacy.getEstado().name());
        ocupacionSaveDto.setIdTipoOcupacion(legacy.getEstado()!=null? TipoOcupacionEnum.findByDescripcion(legacy.getEstado().name()).getId() : TipoOcupacionEnum.OCUPADO.getId());

        ocupacionSaveDto.setLocalidad(legacy.getPoblacion());
        ocupacionSaveDto.setLugar(legacy.getLugar());
        ocupacionSaveDto.setProvisional(Boolean.FALSE);

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