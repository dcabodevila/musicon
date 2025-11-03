package es.musicalia.gestmusica.reportes;

import es.musicalia.gestmusica.acceso.Acceso;
import es.musicalia.gestmusica.acceso.AccesoService;
import es.musicalia.gestmusica.agencia.Agencia;
import es.musicalia.gestmusica.listado.ListadoAudienciasDto;
import es.musicalia.gestmusica.listado.ListadoRecord;
import es.musicalia.gestmusica.listado.ListadoService;
import es.musicalia.gestmusica.listado.ListadosPorMesDto;
import es.musicalia.gestmusica.mail.EmailService;
import es.musicalia.gestmusica.mensaje.Mensaje;
import es.musicalia.gestmusica.mensaje.MensajeService;
import es.musicalia.gestmusica.usuario.EnvioEmailException;
import es.musicalia.gestmusica.usuario.Usuario;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReporteMensualAgenciaJob {

    private final AccesoService accesoService;
    private final ListadoService listadoService;
    private final EmailService emailService;
    private final MensajeService mensajeService;

    @Scheduled(cron = "${reporte.mensual.agencia.cron.expression:0 0 9 L * ?}")
    @Transactional(readOnly = false)
    public void enviarReportesMensuales() {
        try {
            log.info("Iniciando envío de reportes mensuales a agencias");

            // Calcular el mes anterior
            YearMonth mesAnterior = YearMonth.now().minusMonths(1);
            LocalDate inicioMesAnterior = mesAnterior.atDay(1);
            LocalDate finMesAnterior = mesAnterior.atEndOfMonth();

            // Calcular últimos 3 meses para el gráfico
            YearMonth tresMesesAtras = YearMonth.now().minusMonths(3);
            LocalDate inicioTresMeses = tresMesesAtras.atDay(1);
            LocalDate finTresMeses = mesAnterior.atEndOfMonth();

            // Formatear periodo para el email
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM yyyy", new Locale("es", "ES"));
            String periodo = mesAnterior.format(formatter);
            periodo = periodo.substring(0, 1).toUpperCase() + periodo.substring(1);

            log.info("Generando reportes para el período: {} (desde {} hasta {})", periodo, inicioMesAnterior, finMesAnterior);

            // Obtener accesos activos con rol AGENCIA
            List<Acceso> accesosAgencia = accesoService.findAccesosActivosByRolCodigo("AGENCIA");

            log.info("Se encontraron {} accesos con rol AGENCIA activos", accesosAgencia.size());

            int emailsEnviados = 0;
            int emailsFallidos = 0;

            for (Acceso acceso : accesosAgencia) {
                try {
                    enviarReporteParaAgencia(acceso, periodo, inicioMesAnterior, finMesAnterior, inicioTresMeses, finTresMeses);
                    emailsEnviados++;
                } catch (EnvioEmailException e) {
                    emailsFallidos++;
                    log.error("Error al enviar reporte mensual a usuario {} de agencia {}: {}",
                            acceso.getUsuario().getEmail(),
                            acceso.getAgencia().getNombre(),
                            e.getMessage());
                } catch (Exception e) {
                    emailsFallidos++;
                    log.error("Error inesperado al procesar reporte para agencia {}: {}",
                            acceso.getAgencia().getNombre(),
                            e.getMessage(), e);
                }
            }

            log.info("Envío de reportes mensuales completado. Exitosos: {}, Fallidos: {}",
                    emailsEnviados, emailsFallidos);

        } catch (Exception e) {
            log.error("Error durante la ejecución del job de reportes mensuales", e);
        }
    }

    public void enviarReporteParaAgencia(Acceso acceso, String periodo, LocalDate inicioMesAnterior, LocalDate finMesAnterior, LocalDate inicioTresMeses, LocalDate finTresMeses) throws EnvioEmailException {
        Agencia agencia = acceso.getAgencia();
        String emailUsuario = acceso.getUsuario().getEmail();

        if (emailUsuario == null || emailUsuario.isBlank()) {
            log.warn("Usuario {} no tiene email configurado, se omite envío para agencia {}",
                    acceso.getUsuario().getId(), agencia.getNombre());
            throw new EnvioEmailException("Usuario sin email configurado");
        }

        // Obtener listados del mes anterior para el total
        ListadoAudienciasDto listadoMesAnterior = new ListadoAudienciasDto(agencia.getId(), inicioMesAnterior, finMesAnterior);
        List<ListadoRecord> listadosMesAnterior = listadoService.obtenerListadoEntreFechas(listadoMesAnterior);
        Long totalListados = (long) listadosMesAnterior.size();

        // Obtener listados de los últimos 3 meses para el gráfico
        ListadoAudienciasDto listadoTresMeses = new ListadoAudienciasDto(agencia.getId(), inicioTresMeses, finTresMeses);
        List<ListadoRecord> listadosTresMeses = listadoService.obtenerListadoEntreFechas(listadoTresMeses);

        // Generar datos del gráfico con los últimos 3 meses
        List<ListadosPorMesDto> listadosPorMes = listadoService.obtenerListadosPorMes(listadosTresMeses);
        List<Map<String, Object>> chartData = convertirListadosPorMesAMap(listadosPorMes);

        log.info("Agencia: {} - Total listados: {} - Email: {}",
                agencia.getNombre(), totalListados, emailUsuario);

        // Enviar email con el reporte
        emailService.enviarReporteMensualAgencia(
                emailUsuario,
                agencia.getNombre(),
                periodo,
                totalListados,
                chartData
        );

        log.info("Reporte enviado exitosamente a {} para agencia {}", emailUsuario, agencia.getNombre());

        // Enviar notificación interna al usuario de la agencia
        enviarNotificacionInternaAgencia(acceso.getUsuario(), agencia.getNombre(), periodo, totalListados);
    }

    private List<Map<String, Object>> convertirListadosPorMesAMap(List<ListadosPorMesDto> listadosPorMes) {
        return listadosPorMes.stream()
                .map(dto -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("mes", dto.getMes());
                    item.put("cantidad", dto.getCantidad());
                    return item;
                })
                .collect(Collectors.toList());
    }

    private void enviarNotificacionInternaAgencia(Usuario usuario, String nombreAgencia, String periodo, Long totalListados) {
        try {
            Mensaje mensaje = new Mensaje();
            mensaje.setUsuarioRemite(usuario);
            mensaje.setUsuarioReceptor(usuario);
            mensaje.setAsunto("Reporte mensual - " + nombreAgencia);
            mensaje.setMensaje(String.format(
                    "Tu agencia %s ha salido en %d listados el mes de %s",
                    nombreAgencia, totalListados, periodo.toLowerCase()
            ));
            mensaje.setImagen("fa-chart-bar text-info");
            mensaje.setUrlEnlace("/listado/audiencia-listados");

            mensajeService.enviarMensaje(mensaje, usuario.getId());
            log.debug("Notificación interna enviada a usuario {} de agencia {}", usuario.getEmail(), nombreAgencia);
        } catch (Exception e) {
            log.error("Error enviando notificación interna al usuario {} de agencia {}: {}",
                    usuario.getEmail(), nombreAgencia, e.getMessage());
        }
    }
}
