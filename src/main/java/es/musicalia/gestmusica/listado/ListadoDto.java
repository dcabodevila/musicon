package es.musicalia.gestmusica.listado;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ListadoDto {
    @NotNull
    private String solicitadoPara;
    @NotNull
    private Long idCcaa;
    @NotNull
    private Long idProvincia;
    private Long idMunicipio;
    private String localidad;
    private String comentario;
    private Long idTipoOcupacion;

    @DateTimeFormat(pattern = "dd-MM-yyyy")
    private LocalDate fechaDesde;

    @DateTimeFormat(pattern = "dd-MM-yyyy")
    private LocalDate fechaHasta;
    @DateTimeFormat(pattern = "dd-MM-yyyy")
    private LocalDate fecha1;
    @DateTimeFormat(pattern = "dd-MM-yyyy")
    private LocalDate fecha2;
    @DateTimeFormat(pattern = "dd-MM-yyyy")
    private LocalDate fecha3;
    @DateTimeFormat(pattern = "dd-MM-yyyy")
    private LocalDate fecha4;
    @DateTimeFormat(pattern = "dd-MM-yyyy")
    private LocalDate fecha5;
    @DateTimeFormat(pattern = "dd-MM-yyyy")
    private LocalDate fecha6;
    @DateTimeFormat(pattern = "dd-MM-yyyy")
    private LocalDate fecha7;
    private Set<Long> idsTipoArtista = new HashSet<>();
    private Set<Long> idsAgencias = new HashSet<>();
    private Set<Long> idsComunidades = new HashSet<>();

}
