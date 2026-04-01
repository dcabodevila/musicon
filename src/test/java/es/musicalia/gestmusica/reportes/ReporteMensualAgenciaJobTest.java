package es.musicalia.gestmusica.reportes;

import es.musicalia.gestmusica.acceso.Acceso;
import es.musicalia.gestmusica.acceso.AccesoService;
import es.musicalia.gestmusica.agencia.Agencia;
import es.musicalia.gestmusica.listado.ListadoChartDataFactory;
import es.musicalia.gestmusica.listado.ListadoRecord;
import es.musicalia.gestmusica.listado.ListadoService;
import es.musicalia.gestmusica.mail.EmailService;
import es.musicalia.gestmusica.mensaje.MensajeService;
import es.musicalia.gestmusica.usuario.Usuario;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReporteMensualAgenciaJobTest {

    @Test
    void enviarReporteParaAgencia_sinDatosEnTresMeses_enviaFallbackSinDatosCero() throws Exception {
        AccesoService accesoService = mock(AccesoService.class);
        ListadoService listadoService = mock(ListadoService.class);
        EmailService emailService = mock(EmailService.class);
        MensajeService mensajeService = mock(MensajeService.class);
        ListadoChartDataFactory chartDataFactory = new ListadoChartDataFactory();

        ReporteMensualAgenciaJob job = new ReporteMensualAgenciaJob(
                accesoService,
                listadoService,
                chartDataFactory,
                emailService,
                mensajeService
        );

        Acceso acceso = mock(Acceso.class);
        Agencia agencia = mock(Agencia.class);
        Usuario usuario = mock(Usuario.class);

        when(acceso.getAgencia()).thenReturn(agencia);
        when(acceso.getUsuario()).thenReturn(usuario);
        when(agencia.getId()).thenReturn(10L);
        when(agencia.getNombre()).thenReturn("Agencia Test");
        when(usuario.getEmail()).thenReturn("agencia@test.com");
        when(usuario.getId()).thenReturn(7L);

        when(listadoService.obtenerListadoEntreFechas(any()))
                .thenReturn(List.of(mock(ListadoRecord.class))) // mes anterior
                .thenReturn(List.of()); // últimos tres meses

        when(listadoService.obtenerListadosPorMes(any())).thenReturn(List.of());

        job.enviarReporteParaAgencia(
                acceso,
                "Marzo 2026",
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31),
                LocalDate.of(2025, 12, 1),
                LocalDate.of(2026, 3, 31)
        );

        verify(emailService).enviarReporteMensualAgencia(
                eq("agencia@test.com"),
                eq("Agencia Test"),
                eq("Marzo 2026"),
                eq(1L),
                eq(List.of(Map.of("mes", "Sin datos", "cantidad", 0L)))
        );
    }
}
