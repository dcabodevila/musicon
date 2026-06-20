package es.musicalia.gestmusica.eventopublico;

import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Supplier;

final class EventoPublicoCalendarLinks {

    private EventoPublicoCalendarLinks() {
    }

    static String buildGoogleCalendarUrl(EventoPublicoDto evento, String baseUrl) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");
        String start = evento.getFecha().format(fmt);
        String end = evento.getFecha().plusHours(3).format(fmt);
        String location = buildLocation(evento);

        return "https://calendar.google.com/calendar/render?action=TEMPLATE"
            + "&text=" + UriUtils.encodeQueryParam(evento.getTituloEvento(), StandardCharsets.UTF_8)
            + "&dates=" + start + "/" + end
            + "&details=" + UriUtils.encodeQueryParam(evento.getDescripcionSeo(), StandardCharsets.UTF_8)
            + "&location=" + UriUtils.encodeQueryParam(location, StandardCharsets.UTF_8)
            + "&sprop=url:" + UriUtils.encodeQueryParam(baseUrl + evento.getPathPublico(), StandardCharsets.UTF_8);
    }

    static String buildIcal(EventoPublicoDto evento) {
        return buildIcal(evento, LocalDateTime::now);
    }

    static String buildIcal(EventoPublicoDto evento, Supplier<LocalDateTime> nowSupplier) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");
        String now = nowSupplier.get().format(fmt) + "Z";
        String start = evento.getFecha().format(fmt);
        String end = evento.getFecha().plusHours(3).format(fmt);
        String location = buildLocation(evento);

        return "BEGIN:VCALENDAR\r\n"
            + "VERSION:2.0\r\n"
            + "PRODID:-//festia.es//Festia//ES\r\n"
            + "BEGIN:VEVENT\r\n"
            + "UID:" + evento.getId() + "@festia.es\r\n"
            + "DTSTAMP:" + now + "\r\n"
            + "DTSTART;TZID=Europe/Madrid:" + start + "\r\n"
            + "DTEND;TZID=Europe/Madrid:" + end + "\r\n"
            + "SUMMARY:" + escapeIcal(evento.getTituloEvento()) + "\r\n"
            + "DESCRIPTION:" + escapeIcal(evento.getDescripcionSeo()) + "\r\n"
            + "LOCATION:" + escapeIcal(location) + "\r\n"
            + "END:VEVENT\r\n"
            + "END:VCALENDAR";
    }

    private static String buildLocation(EventoPublicoDto evento) {
        return (evento.getLugarParaMapa() != null ? evento.getLugarParaMapa() + ", " : "")
            + evento.getMunicipio() + ", " + evento.getProvincia();
    }

    private static String escapeIcal(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\\", "\\\\")
            .replace(";", "\\;")
            .replace(",", "\\,")
            .replace("\n", "\\n");
    }
}
