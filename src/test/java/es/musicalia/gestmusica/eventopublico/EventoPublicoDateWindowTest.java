package es.musicalia.gestmusica.eventopublico;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EventoPublicoDateWindowTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 7, 1);
    private static final LocalDate HORIZON = LocalDate.of(2026, 8, 15);

    private final EventoPublicoDateWindow dateWindow = new EventoPublicoDateWindow(Clock.fixed(
        Instant.parse("2026-07-01T10:15:30Z"),
        ZoneId.of("Europe/Madrid")
    ));

    @Test
    void publicHorizon_debeIncluirElDia45() {
        assertEquals(TODAY, dateWindow.today());
        assertEquals(HORIZON, dateWindow.publicHorizon());
    }

    @Test
    void clampHasta_debeUsarHorizonteCuandoEsNull() {
        assertEquals(HORIZON, dateWindow.clampHasta(null));
    }

    @Test
    void clampHasta_debeMantenerFechasDentroDelHorizonteYRecortarLasPosteriores() {
        assertEquals(LocalDate.of(2026, 8, 10), dateWindow.clampHasta(LocalDate.of(2026, 8, 10)));
        assertEquals(HORIZON, dateWindow.clampHasta(HORIZON));
        assertEquals(HORIZON, dateWindow.clampHasta(LocalDate.of(2026, 9, 30)));
    }

    @Test
    void effectiveUpcomingWindow_debeUsarHoyPorDefectoYRecortarLimitesSolicitados() {
        EventoPublicoDateWindow.DateRange defaultRange = dateWindow.effectiveUpcomingWindow(null, null);
        EventoPublicoDateWindow.DateRange requestedRange = dateWindow.effectiveUpcomingWindow(LocalDate.of(2026, 6, 28), LocalDate.of(2026, 9, 1));

        assertEquals(TODAY, defaultRange.fechaDesde());
        assertEquals(HORIZON, defaultRange.fechaHasta());
        assertEquals(TODAY, requestedRange.fechaDesde());
        assertEquals(HORIZON, requestedRange.fechaHasta());
    }

    @Test
    void effectiveUpcomingWindow_debeRecortarDesdeAnteriorAHoy() {
        EventoPublicoDateWindow.DateRange requestedRange = dateWindow.effectiveUpcomingWindow(
            LocalDate.of(2026, 6, 28),
            LocalDate.of(2026, 8, 10)
        );

        assertEquals(TODAY, requestedRange.fechaDesde());
        assertEquals(LocalDate.of(2026, 8, 10), requestedRange.fechaHasta());
    }
}
