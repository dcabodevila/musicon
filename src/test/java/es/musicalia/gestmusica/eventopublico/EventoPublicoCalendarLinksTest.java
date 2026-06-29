package es.musicalia.gestmusica.eventopublico;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

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

        String unfolded = unfoldIcal(ical);

        assertThat(unfolded)
            .contains("BEGIN:VCALENDAR\r\n")
            .contains("DTSTAMP:20260801T094530Z\r\n")
            .contains("DTSTART;TZID=Europe/Madrid:20260815T213000\r\n")
            .contains("DTEND;TZID=Europe/Madrid:20260816T003000\r\n")
            .contains("SUMMARY:Actuación de Los Satélites en Rúa do Franco\\, Praza do Obradoiro\\; nivel\\\\1\r\n")
            .contains("DESCRIPTION:Los Satélites actúa en Rúa do Franco\\, Praza do Obradoiro\\; nivel\\\\1 (A Coruña) el 15 de agosto de 2026 en Rúa do Franco\\, Santiago. Toda la información en Festia.\r\n")
            .contains("LOCATION:Rúa do Franco\\, Praza do Obradoiro\\; nivel\\\\1\\, A Coruña\r\n")
            .endsWith("END:VCALENDAR\r\n");
        assertThat(ical.split("\r\n"))
            .allSatisfy(line -> assertThat(line.getBytes(java.nio.charset.StandardCharsets.UTF_8).length).isLessThanOrEqualTo(75));
    }

    @Test
    void buildIcal_debeMantenerPrefijoActuacionDeEnEventoIndividual() {
        EventoPublicoDto evento = crearEventoConLugar("Rúa do Franco, Santiago", "Praza do Obradoiro");

        String ical = EventoPublicoCalendarLinks.buildIcal(evento, () -> LocalDateTime.of(2026, 8, 1, 9, 45, 30));

        assertThat(unfoldIcal(ical))
            .contains("SUMMARY:Actuación de Los Satélites en Rúa do Franco\\, Praza do Obradoiro\r\n");
    }

    @Test
    void buildArtistCalendar_debeGenerarMultiEventoConUidEstableYEscenarioAllDay() {
        EventoPublicoDto eventoConHora = crearEventoConLugar("Rúa do Franco, Santiago", "Praza do Obradoiro");
        EventoPublicoDto eventoAllDay = EventoPublicoDto.builder()
            .id(11L)
            .idArtista(20L)
            .nombreArtista("Los Satélites")
            .lugar("Campo da festa")
            .municipio("Arzúa")
            .provincia("A Coruña")
            .fecha(LocalDateTime.of(2026, 8, 20, 0, 0))
            .fechaActualizacion(LocalDateTime.of(2026, 8, 2, 10, 30))
            .build();

        String ical = EventoPublicoCalendarLinks.buildArtistCalendar(
            "Los Satélites",
            List.of(eventoConHora, eventoAllDay),
            () -> LocalDateTime.of(2026, 8, 1, 9, 45, 30)
        );

        assertThat(unfoldIcal(ical))
            .contains("X-WR-CALNAME:Festia - Los Satélites\r\n")
            .contains("X-PUBLISHED-TTL:PT1H\r\n")
            .contains("REFRESH-INTERVAL;VALUE=DURATION:PT1H\r\n")
            .contains("BEGIN:VTIMEZONE\r\n")
            .contains("TZID:Europe/Madrid\r\n")
            .contains("END:VTIMEZONE\r\n")
            .contains("UID:10@festia.es\r\n")
            .contains("UID:11@festia.es\r\n")
            .contains("DTSTART;TZID=Europe/Madrid:20260815T213000\r\n")
            .contains("DTEND;TZID=Europe/Madrid:20260816T003000\r\n")
            .contains("DTSTART;VALUE=DATE:20260820\r\n")
            .contains("DTEND;VALUE=DATE:20260821\r\n")
            .contains("SUMMARY:Los Satélites en Rúa do Franco\\, Praza do Obradoiro\r\n")
            .contains("SUMMARY:Los Satélites en Campo da festa\\, Arzúa\r\n")
            .doesNotContain("SUMMARY:Actuación de ")
            .contains("LOCATION:Campo da festa\\, Arzúa\\, A Coruña\r\n");
        assertThat(ical.split("BEGIN:VEVENT", -1)).hasSize(3);
    }

    @Test
    void buildArtistCalendar_debePasarAlDiaSiguienteCuandoLaHoraFinNoEsPosteriorAlInicio() {
        EventoPublicoDto evento = EventoPublicoDto.builder()
            .id(7460L)
            .idArtista(20L)
            .nombreArtista("Costa Dorada")
            .lugar("AS ENCROBAS")
            .municipio("Cerceda")
            .provincia("A Coruña")
            .fecha(LocalDateTime.of(2026, 5, 22, 0, 0))
            .horaActuacion(LocalTime.of(11, 0))
            .horaActuacionHasta(LocalTime.of(3, 30))
            .build();

        String ical = EventoPublicoCalendarLinks.buildArtistCalendar(
            "Costa Dorada",
            List.of(evento),
            () -> LocalDateTime.of(2026, 5, 1, 9, 45, 30)
        );

        assertThat(unfoldIcal(ical))
            .contains("UID:7460@festia.es\r\n")
            .contains("DTSTART;TZID=Europe/Madrid:20260522T110000\r\n")
            .contains("DTEND;TZID=Europe/Madrid:20260523T033000\r\n");
    }

    @Test
    void buildArtistCalendar_debeEliminarSoloElPrefijoLiteralExactoActuacionDe() {
        EventoPublicoDto evento = EventoPublicoDto.builder()
            .id(12L)
            .idArtista(20L)
            .nombreArtista("Actuación de Luxe")
            .lugar("Recinto ferial")
            .municipio("Vilalba")
            .provincia("Lugo")
            .fecha(LocalDateTime.of(2026, 9, 10, 23, 0))
            .build();

        String ical = EventoPublicoCalendarLinks.buildArtistCalendar(
            "Actuación de Luxe",
            List.of(evento),
            () -> LocalDateTime.of(2026, 8, 1, 9, 45, 30)
        );

        assertThat(unfoldIcal(ical))
            .contains("SUMMARY:Actuación de Luxe en Recinto ferial\\, Vilalba\r\n")
            .doesNotContain("SUMMARY:Luxe en Recinto ferial\\, Vilalba\r\n");
    }

    private String unfoldIcal(String ical) {
        return ical.replace("\r\n ", "");
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
