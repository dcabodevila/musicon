package es.musicalia.gestmusica.eventopublico;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class EventoPublicoCalendarLinksTest {

    @Test
    void buildGoogleCalendarUrl_debeMantenerFechasDuracionLocationYEscapingActual() {
        EventoPublicoDto evento = crearEventoConLugar("Rúa do Franco, Santiago", "Praza do Obradoiro; nivel\\1");

        String calendarUrl = EventoPublicoCalendarLinks.buildGoogleCalendarUrl(evento, "https://festia.es");

        assertThat(calendarUrl)
            .contains("https://calendar.google.com/calendar/render?action=TEMPLATE")
            .contains("text=Actuaci%C3%B3n%20de%20Los%20Sat%C3%A9lites%20en%20R%C3%BAa%20do%20Franco,%20Praza%20do%20Obradoiro;%20nivel%5C1")
            .contains("dates=20260815T213000/20260816T003000")
            .contains("details=Los%20Sat%C3%A9lites%20act%C3%BAa%20en%20R%C3%BAa%20do%20Franco,%20Praza%20do%20Obradoiro;%20nivel%5C1%20(A%20Coru%C3%B1a)%20el%2015%20de%20agosto%20de%202026%20en%20R%C3%BAa%20do%20Franco,%20Santiago.%20Toda%20la%20informaci%C3%B3n%20en%20Festia.")
            .contains("location=R%C3%BAa%20do%20Franco,%20Praza%20do%20Obradoiro;%20nivel%5C1,%20A%20Coru%C3%B1a")
            .contains("sprop=url:https://festia.es/eventos/evento/10-los-satelites-praza-do-obradoiro-nivel-1-2026-08-15");
    }

    @Test
    void buildIcal_debeMantenerCrlfTimezoneDuracionYEscapingActual() {
        EventoPublicoDto evento = crearEventoConLugar("Rúa do Franco, Santiago", "Praza do Obradoiro; nivel\\1");

        String ical = EventoPublicoCalendarLinks.buildIcal(evento, () -> LocalDateTime.of(2026, 8, 1, 9, 45, 30));

        assertThat(ical)
            .contains("BEGIN:VCALENDAR\r\n")
            .contains("DTSTAMP:20260801T094530Z\r\n")
            .contains("DTSTART;TZID=Europe/Madrid:20260815T213000\r\n")
            .contains("DTEND;TZID=Europe/Madrid:20260816T003000\r\n")
            .contains("SUMMARY:Actuación de Los Satélites en Rúa do Franco\\, Praza do Obradoiro\\; nivel\\\\1\r\n")
            .contains("DESCRIPTION:Los Satélites actúa en Rúa do Franco\\, Praza do Obradoiro\\; nivel\\\\1 (A Coruña) el 15 de agosto de 2026 en Rúa do Franco\\, Santiago. Toda la información en Festia.\r\n")
            .contains("LOCATION:Rúa do Franco\\, Praza do Obradoiro\\; nivel\\\\1\\, A Coruña\r\n")
            .endsWith("END:VCALENDAR");
    }

    private EventoPublicoDto crearEventoConLugar(String lugar, String municipio) {
        return EventoPublicoDto.builder()
            .id(10L)
            .idArtista(20L)
            .nombreArtista("Los Satélites")
            .lugar(lugar)
            .municipio(municipio)
            .provincia("A Coruña")
            .fecha(LocalDateTime.of(2026, 8, 15, 21, 30))
            .build();
    }
}
