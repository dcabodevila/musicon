package es.musicalia.gestmusica.reportes;

import es.musicalia.gestmusica.acceso.Acceso;
import es.musicalia.gestmusica.acceso.AccesoService;
import es.musicalia.gestmusica.usuario.EnvioEmailException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReporteServiceImpl implements ReporteService {

    private final AccesoService accesoService;
    private final ReporteMensualAgenciaJob reporteMensualAgenciaJob;

    @Override
    @Transactional(readOnly = false)
    public void enviarReportePorIdAgencia(Long idAgencia) throws EnvioEmailException {
        log.info("Iniciando envío de reporte mensual para agencia con ID: {}", idAgencia);

        // Buscar el acceso activo de la agencia
        Acceso acceso = accesoService.findAccesoActivoByAgenciaId(idAgencia)
                .orElseThrow(() -> new EnvioEmailException("No se encontró acceso activo para la agencia con ID: " + idAgencia));

        // Calcular fechas
        YearMonth mesAnterior = YearMonth.now().minusMonths(1);
        LocalDate inicioMesAnterior = mesAnterior.atDay(1);
        LocalDate finMesAnterior = mesAnterior.atEndOfMonth();

        YearMonth tresMesesAtras = YearMonth.now().minusMonths(3);
        LocalDate inicioTresMeses = tresMesesAtras.atDay(1);
        LocalDate finTresMeses = mesAnterior.atEndOfMonth();

        // Formatear periodo
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM yyyy", new Locale("es", "ES"));
        String periodo = mesAnterior.format(formatter);
        periodo = periodo.substring(0, 1).toUpperCase() + periodo.substring(1);

        // Ejecutar reporte
        reporteMensualAgenciaJob.enviarReporteParaAgencia(acceso, periodo, inicioMesAnterior, finMesAnterior, inicioTresMeses, finTresMeses);

        log.info("Reporte mensual para agencia {} completado exitosamente", acceso.getAgencia().getNombre());
    }
}
