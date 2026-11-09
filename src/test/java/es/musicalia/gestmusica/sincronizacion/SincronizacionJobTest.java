package es.musicalia.gestmusica.sincronizacion;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SincronizacionJobTest {

    @Mock
    private SincronizacionService sincronizacionService;

    @InjectMocks
    private SincronizacionJob job;

    @Test
    void scheduledRunSyncsFromTodayWithModificationsOfTheLastHour() {
        when(sincronizacionService.sincronizarOcupacionesDesde(any(), any())).thenReturn(new SincronizacionResult());
        LocalDate diaAntes = LocalDate.now();
        LocalDateTime antes = LocalDateTime.now();

        job.ejecutarSincronizacion();

        LocalDateTime despues = LocalDateTime.now();
        ArgumentCaptor<LocalDate> fecha = ArgumentCaptor.forClass(LocalDate.class);
        ArgumentCaptor<LocalDateTime> modificadoDesde = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(sincronizacionService).sincronizarOcupacionesDesde(fecha.capture(), modificadoDesde.capture());
        assertThat(fecha.getValue()).isBetween(diaAntes, despues.toLocalDate());
        assertThat(modificadoDesde.getValue()).isBetween(antes.minusHours(1), despues.minusHours(1));
    }

    @Test
    void scheduledRunSwallowsServiceFailures() {
        when(sincronizacionService.sincronizarOcupacionesDesde(any(), any())).thenThrow(new IllegalStateException("boom"));

        assertThatCode(() -> job.ejecutarSincronizacion()).doesNotThrowAnyException();
    }

    @Test
    void scheduleIsDrivenByCronProperty() throws Exception {
        Scheduled scheduled = SincronizacionJob.class.getMethod("ejecutarSincronizacion").getAnnotation(Scheduled.class);

        assertThat(scheduled).isNotNull();
        assertThat(scheduled.cron()).isEqualTo("${sincronizacion.cron.expression}");
    }
}
