package es.musicalia.gestmusica.tarifa;


import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface TarifaService {
	List<TarifaDto> findByArtistaId(long idArtista, LocalDateTime start, LocalDateTime end);

    TarifaDto findByArtistaIdAndDate(long idArtista, LocalDate fecha);

    void saveTarifa(TarifaSaveDto tarifaSaveDto);

    byte[] getInformeTarifaAnual(TarifaAnualDto tarifaAnualDto);

    List<TarifaArtistaCcaaDto> findTarifasByFechaAndNumComponentesArtista(Long idArtista, LocalDate fecha);

    ByteArrayOutputStream exportTarifaAnualToExcel(TarifaAnualDto tarifaAnualDto);

    boolean agenciaTieneTarifasActivas(Long idAgencia);
}
