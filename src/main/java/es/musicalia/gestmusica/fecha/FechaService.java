package es.musicalia.gestmusica.fecha;


import java.time.LocalDateTime;
import java.util.List;

public interface FechaService {
	List<FechaDto> findFechaDtoByArtistaId(long idArtista, LocalDateTime start, LocalDateTime end);
}
