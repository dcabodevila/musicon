package es.musicalia.gestmusica.eventopublico;

import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
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
        return buildCalendar(evento.getNombreArtista(), List.of(evento), LocalDateTime::now, false);
    }

    static String buildIcal(EventoPublicoDto evento, Supplier<LocalDateTime> nowSupplier) {
        return buildCalendar(evento.getNombreArtista(), List.of(evento), nowSupplier, false);
    }

    static String buildArtistCalendar(String artistName, List<EventoPublicoDto> eventos, Supplier<LocalDateTime> nowSupplier) {
        return buildCalendar(artistName, eventos, nowSupplier, true);
    }

    private static String buildCalendar(String artistName, List<EventoPublicoDto> eventos, Supplier<LocalDateTime> nowSupplier,
                                        boolean normalizeArtistFeedSummary) {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");
        String fallbackDtStamp = nowSupplier.get().format(dateTimeFormatter) + "Z";
        StringBuilder calendar = new StringBuilder()
            .append("BEGIN:VCALENDAR\r\n")
            .append("VERSION:2.0\r\n")
            .append("PRODID:-//festia.es//Festia//ES\r\n")
            .append("METHOD:PUBLISH\r\n")
            .append("CALSCALE:GREGORIAN\r\n")
            .append(foldIcalLine("X-WR-CALNAME:" + escapeIcal("Festia - " + artistName))).append("\r\n")
            .append("X-WR-TIMEZONE:Europe/Madrid\r\n")
            .append("X-PUBLISHED-TTL:PT1H\r\n")
            .append("REFRESH-INTERVAL;VALUE=DURATION:PT1H\r\n")
            .append("BEGIN:VTIMEZONE\r\n")
            .append("TZID:Europe/Madrid\r\n")
            .append("X-LIC-LOCATION:Europe/Madrid\r\n")
            .append("BEGIN:DAYLIGHT\r\n")
            .append("TZOFFSETFROM:+0100\r\n")
            .append("TZOFFSETTO:+0200\r\n")
            .append("TZNAME:CEST\r\n")
            .append("DTSTART:19700329T020000\r\n")
            .append("RRULE:FREQ=YEARLY;BYMONTH=3;BYDAY=-1SU\r\n")
            .append("END:DAYLIGHT\r\n")
            .append("BEGIN:STANDARD\r\n")
            .append("TZOFFSETFROM:+0200\r\n")
            .append("TZOFFSETTO:+0100\r\n")
            .append("TZNAME:CET\r\n")
            .append("DTSTART:19701025T030000\r\n")
            .append("RRULE:FREQ=YEARLY;BYMONTH=10;BYDAY=-1SU\r\n")
            .append("END:STANDARD\r\n")
            .append("END:VTIMEZONE\r\n");

        for (EventoPublicoDto evento : eventos) {
            calendar.append("BEGIN:VEVENT\r\n")
                .append("UID:").append(evento.getId()).append("@festia.es\r\n")
                .append("DTSTAMP:").append(resolveDtStamp(evento, fallbackDtStamp, dateTimeFormatter)).append("\r\n");

            appendEventDates(calendar, evento, dateTimeFormatter);

            appendIcalProperty(calendar, "SUMMARY", resolveSummary(evento, normalizeArtistFeedSummary));
            appendIcalProperty(calendar, "DESCRIPTION", evento.getDescripcionSeo());
            appendIcalProperty(calendar, "LOCATION", buildLocation(evento));
            calendar.append("END:VEVENT\r\n");
        }

        return calendar.append("END:VCALENDAR\r\n").toString();
    }

    private static void appendEventDates(StringBuilder calendar, EventoPublicoDto evento, DateTimeFormatter dateTimeFormatter) {
        if (evento.getHoraActuacion() == null
            && evento.getHoraActuacionHasta() == null
            && LocalTime.MIDNIGHT.equals(evento.getFecha().toLocalTime())) {
            LocalDate fecha = evento.getFecha().toLocalDate();
            calendar.append("DTSTART;VALUE=DATE:").append(fecha.format(DateTimeFormatter.BASIC_ISO_DATE)).append("\r\n")
                .append("DTEND;VALUE=DATE:").append(fecha.plusDays(1).format(DateTimeFormatter.BASIC_ISO_DATE)).append("\r\n");
            return;
        }

        LocalDateTime start = resolveStartDateTime(evento);
        LocalDateTime end = resolveEndDateTime(evento, start);
        calendar.append("DTSTART;TZID=Europe/Madrid:").append(start.format(dateTimeFormatter)).append("\r\n")
            .append("DTEND;TZID=Europe/Madrid:").append(end.format(dateTimeFormatter)).append("\r\n");
    }

    private static LocalDateTime resolveStartDateTime(EventoPublicoDto evento) {
        LocalTime horaActuacion = evento.getHoraActuacion();
        if (horaActuacion == null) {
            return evento.getFecha();
        }
        return evento.getFecha().toLocalDate().atTime(horaActuacion);
    }

    private static LocalDateTime resolveEndDateTime(EventoPublicoDto evento, LocalDateTime start) {
        LocalTime horaActuacionHasta = evento.getHoraActuacionHasta();
        if (horaActuacionHasta == null) {
            return start.plusHours(3);
        }

        LocalDateTime end = start.toLocalDate().atTime(horaActuacionHasta);
        if (!end.isAfter(start)) {
            end = end.plusDays(1);
        }

        return end;
    }

    private static String resolveDtStamp(EventoPublicoDto evento, String fallbackDtStamp, DateTimeFormatter dateTimeFormatter) {
        if (evento.getFechaActualizacion() == null) {
            return fallbackDtStamp;
        }
        return evento.getFechaActualizacion().format(dateTimeFormatter) + "Z";
    }

    private static String buildLocation(EventoPublicoDto evento) {
        return (evento.getLugarParaMapa() != null ? evento.getLugarParaMapa() + ", " : "")
            + evento.getMunicipio() + ", " + evento.getProvincia();
    }

    private static String resolveSummary(EventoPublicoDto evento, boolean normalizeArtistFeedSummary) {
        String titulo = evento.getTituloEvento();
        return normalizeArtistFeedSummary ? stripActuacionDePrefix(titulo) : titulo;
    }

    private static String stripActuacionDePrefix(String titulo) {
        if (titulo == null) {
            return null;
        }
        return titulo.startsWith("Actuación de ") ? titulo.substring("Actuación de ".length()) : titulo;
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

    private static void appendIcalProperty(StringBuilder calendar, String name, String value) {
        calendar.append(foldIcalLine(name + ":" + escapeIcal(value))).append("\r\n");
    }

    private static String foldIcalLine(String line) {
        if (line.getBytes(StandardCharsets.UTF_8).length <= 75) {
            return line;
        }

        StringBuilder folded = new StringBuilder();
        int currentLineOctets = 0;
        for (int offset = 0; offset < line.length(); ) {
            int codePoint = line.codePointAt(offset);
            String character = new String(Character.toChars(codePoint));
            int characterOctets = character.getBytes(StandardCharsets.UTF_8).length;
            int limit = folded.length() == 0 ? 75 : 74;
            if (currentLineOctets > 0 && currentLineOctets + characterOctets > limit) {
                folded.append("\r\n ");
                currentLineOctets = 0;
            }
            folded.append(character);
            currentLineOctets += characterOctets;
            offset += Character.charCount(codePoint);
        }
        return folded.toString();
    }
}
