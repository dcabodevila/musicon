package es.musicalia.gestmusica.eventopublico;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class EventoPublicoDateWindow {

    public static final long PUBLIC_UPCOMING_HORIZON_DAYS = 45L;

    private final Clock clock;

    public LocalDate today() {
        return LocalDate.now(clock);
    }

    public LocalDate publicHorizon() {
        return today().plusDays(PUBLIC_UPCOMING_HORIZON_DAYS);
    }

    public LocalDate effectiveDesde(LocalDate requestedDesde) {
        LocalDate today = today();
        if (requestedDesde == null || requestedDesde.isBefore(today)) {
            return today;
        }
        return requestedDesde;
    }

    public LocalDate clampHasta(LocalDate requestedHasta) {
        LocalDate horizon = publicHorizon();
        if (requestedHasta == null || requestedHasta.isAfter(horizon)) {
            return horizon;
        }
        return requestedHasta;
    }

    public DateRange effectiveUpcomingWindow(LocalDate requestedDesde, LocalDate requestedHasta) {
        return new DateRange(effectiveDesde(requestedDesde), clampHasta(requestedHasta));
    }

    public record DateRange(LocalDate fechaDesde, LocalDate fechaHasta) {
    }
}
