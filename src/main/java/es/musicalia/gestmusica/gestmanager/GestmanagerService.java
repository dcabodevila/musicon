package es.musicalia.gestmusica.gestmanager;


import es.musicalia.gestmusica.api.*;
import es.musicalia.gestmusica.ocupacion.Ocupacion;
import es.musicalia.gestmusica.ocupacion.OcupacionService;
import es.musicalia.gestmusica.util.DefaultResponseBody;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.transaction.annotation.Propagation;


@Service
@Slf4j
public class GestmanagerService {

    private final OcupacionService ocupacionService;
    private final SincronizacionRepository sincronizacionRepository;

    public GestmanagerService(OcupacionService ocupacionService, SincronizacionRepository sincronizacionRepository) {
        this.ocupacionService = ocupacionService;
        this.sincronizacionRepository = sincronizacionRepository;
    }

    // Esta transacción es solo para persistir la sincronización
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected Sincronizacion persistirSincronizacion(DatosGestmanagerDTO datos) {
        return this.sincronizacionRepository.saveAndFlush(toSincronizacion(datos));
    }

    // Esta transacción es para el procesamiento
    @Transactional
    protected void procesarAccion(DatosGestmanagerDTO datos, Sincronizacion sincronizacion) {
        try {
            final DatosGestmanagerConvertedDTO datosGestmanagerConvertedDTO = toDatosConverted(datos);
            switch (datosGestmanagerConvertedDTO.getAccion()) {
                case ALTA -> procesarAlta(datosGestmanagerConvertedDTO, sincronizacion);
                case MODIFICACION -> procesarModificacion(datosGestmanagerConvertedDTO);
                case BAJA -> procesarBorrado(datosGestmanagerConvertedDTO, sincronizacion);
                case MOD_FECHA_AGR -> procesarModificacionFechaAgrupacion(datosGestmanagerConvertedDTO);
                case MOD_AGR -> procesarModificacionAgrupacion(datosGestmanagerConvertedDTO);
                case MOD_FECHA -> procesarModificacionFecha(datosGestmanagerConvertedDTO);
                default -> log.error("Acción desconocida: {}", datosGestmanagerConvertedDTO.getAccion());
            }
        } catch (IllegalArgumentException e) {
            log.error("Acción inválida: {}", datos.accion(), e);
            throw e;
        } catch (Exception e) {
            log.error("Error procesando la acción {}: {}", datos.accion(), e.getMessage(), e);
            throw e;
        }
    }

    // Método principal que coordina todo el proceso
    public void procesarDatosGestmanager(DatosGestmanagerDTO datos) {
        log.info("Procesando datos recibidos: {}", datos);
        
        // Primero persistimos los datos recibidos en una transacción separada
        Sincronizacion sincronizacion = persistirSincronizacion(datos);
        
        // Luego intentamos procesar la acción en otra transacción
        try {
            procesarAccion(datos, sincronizacion);
        } catch (Exception e) {
            // Actualizamos el estado de sincronización en una nueva transacción
            actualizarEstadoSincronizacion(sincronizacion, e);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void actualizarEstadoSincronizacion(Sincronizacion sincronizacion, Exception e) {
        sincronizacion.setCodigoError(e.getMessage().length() > 800 ? e.getMessage().substring(0, 800) : e.getMessage());
        this.sincronizacionRepository.save(sincronizacion);
    }

    private void procesarAlta(DatosGestmanagerConvertedDTO datos, Sincronizacion sincronizacion){
        log.info("Alta: artista={}, fecha={}",datos.getIdArtistaGestmanager(), datos.getFecha());
        try {
            Ocupacion ocupacion = this.ocupacionService.saveOcupacionFromGestmanager(datos);

            sincronizacion.setProcesado(true);

            log.info("Alta de ocupacion realizada: {}",ocupacion.getId());

        } catch (Exception e) {
            log.error("Error dando de alta fecha de Gestmanager: ", e);
            sincronizacion.setProcesado(false);
            sincronizacion.setCodigoError(e.getMessage().length() > 800 ? e.getMessage().substring(0, 800) : e.getMessage());
        }

        this.sincronizacionRepository.save(sincronizacion);

    }

    private void procesarModificacion(DatosGestmanagerConvertedDTO datos) {
//        log.info("Modificación: artista={}, nueva fecha={}, lugar nuevo={}, lugar antiguo={}",
//                datos.getIdArtista(), datos.fecha(), datos.nombre_local(), datos.descripcion());
    }

    private void procesarBorrado(DatosGestmanagerConvertedDTO datos, Sincronizacion sincronizacion) {

        log.info("Borrado: artista={}, fecha={}, lugar={}",
                datos.getIdArtistaGestmanager(), datos.getFecha(), datos.getDescripcion());

        try {
            DefaultResponseBody responseBody = this.ocupacionService.deleteOcupacionFromGestmanager(datos);

            sincronizacion.setProcesado(responseBody.isSuccess());
            sincronizacion.setCodigoError(responseBody.isSuccess()? "" : responseBody.getMessage());


            log.info("Borrado de ocupacion realizado: ");

        } catch (Exception e) {
            log.error("Error borrando fecha de Gestmanager: ", e);
            sincronizacion.setProcesado(false);
            sincronizacion.setCodigoError(e.getMessage().length() > 800 ? e.getMessage().substring(0, 800) : e.getMessage());
        }
        this.sincronizacionRepository.save(sincronizacion);

    }

    private void procesarModificacionFechaAgrupacion(DatosGestmanagerConvertedDTO datos) {
//        log.info("Modificar Fecha y Agrupación: nueva fecha={}, descripción vieja={}",
//                datos.fecha(), datos.descripcion());
    }

    private void procesarModificacionAgrupacion(DatosGestmanagerConvertedDTO datos) {
//        log.info("Modificar Agrupación: artista nuevo={}, lugar={}, agrupación anterior={}",
//                datos.id_artista(), datos.nombre_local(), datos.descripcion());
    }

    private void procesarModificacionFecha(DatosGestmanagerConvertedDTO datos) {
//        log.info("Modificar Fecha: nueva fecha={}, fecha anterior={} ",
//                datos.fecha(), datos.descripcion());
    }

    private DatosGestmanagerConvertedDTO toDatosConverted(DatosGestmanagerDTO dto){
        DatosGestmanagerConvertedDTO datosGestmanagerConvertedDTO = new DatosGestmanagerConvertedDTO();

        datosGestmanagerConvertedDTO.setIdArtistaGestmanager(Long.valueOf(dto.id_artista()));
        datosGestmanagerConvertedDTO.setAccion(TipoAccionGestmanagerEnum.getTipoAccionByCodigo(dto.accion().toUpperCase()));
        datosGestmanagerConvertedDTO.setFecha(LocalDate.parse(dto.fecha(), DateTimeFormatter.ofPattern("dd-MM-yyyy")).atStartOfDay());
        datosGestmanagerConvertedDTO.setDescripcion(dto.descripcion());
        datosGestmanagerConvertedDTO.setPoblacion(dto.poblacion());
        datosGestmanagerConvertedDTO.setMunicipio(dto.municipio());
        datosGestmanagerConvertedDTO.setProvincia(dto.provincia());
        datosGestmanagerConvertedDTO.setPais(dto.pais());
        datosGestmanagerConvertedDTO.setNombreLocal(dto.nombre_local());
        datosGestmanagerConvertedDTO.setEstado(TipoEstadoGestmanagerEnum.valueOf(dto.estado().equals("O") ? "OCUPADO" : "RESERVADO"));

        IndicadoresFlags flags = parseIndicadores(dto.indicadores());
        datosGestmanagerConvertedDTO.setMatinal(flags.isMatinal());
        datosGestmanagerConvertedDTO.setSoloMatinal(flags.isSoloMatinal());
        // No aplica
        //flags.isSalaFiestas()

        return datosGestmanagerConvertedDTO;

    }

    public static Sincronizacion toSincronizacion(DatosGestmanagerDTO datos) {
        return Sincronizacion.builder()
                .idArtista(Long.valueOf(datos.id_artista()))
                .accion(datos.accion())
                .fecha(datos.fecha())
                .descripcion(datos.descripcion())
                .poblacion(datos.poblacion())
                .municipio(datos.municipio())
                .provincia(datos.provincia())
                .pais(datos.pais())
                .nombreLocal(datos.nombre_local())
                .estado(datos.estado())
                .indicadores(datos.indicadores())
                .fechaRecepcion(LocalDateTime.now())
                .cadDatos(construirCadDatos(datos))
                .build();
    }

    private static String construirCadDatos(DatosGestmanagerDTO datos) {
        return String.format("Artista: %s, Acción: %s, Fecha: %s, Local: %s, Ubicación: %s, %s, %s, %s",
                datos.id_artista(),
                datos.accion(),
                datos.fecha(),
                datos.nombre_local(),
                datos.poblacion(),
                datos.municipio(),
                datos.provincia(),
                datos.pais());
    }

    /**
     * Parsea una cadena de indicadores en formato binario (000-111) y devuelve los valores booleanos correspondientes.
     *
     * Formato de indicadores:
     * Posición 1 (izq): Matinal (0=No, 1=Sí)
     * Posición 2 (med): Solo Matinal (0=No, 1=Sí)
     * Posición 3 (der): Sala de Fiestas (0=No, 1=Sí)
     *
     * @param indicadores cadena de 3 dígitos binarios (ej: "101", "000", "111")
     * @return IndicadoresFlags objeto con los valores booleanos interpretados
     * @throws IllegalArgumentException si el formato de indicadores no es válido
     */
    public static IndicadoresFlags parseIndicadores(String indicadores) {
        // Validación de entrada
        if (indicadores == null) {
            throw new IllegalArgumentException("Los indicadores no pueden ser null");
        }

        if (indicadores.length() != 3) {
            throw new IllegalArgumentException("Los indicadores deben ser una cadena de exactamente 3 dígitos");
        }

        if (!indicadores.matches("[01]{3}")) {
            throw new IllegalArgumentException("Los indicadores solo pueden contener 0s y 1s");
        }

        // Parseo de los dígitos individuales
        boolean matinal = indicadores.charAt(0) == '1';      // Primer dígito
        boolean soloMatinal = indicadores.charAt(1) == '1';  // Segundo dígito
        boolean salaFiestas = indicadores.charAt(2) == '1';  // Tercer dígito

        return new IndicadoresFlags(matinal, soloMatinal, salaFiestas);
    }


}