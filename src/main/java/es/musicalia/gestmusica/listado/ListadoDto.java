package es.musicalia.gestmusica.listado;

import jakarta.validation.constraints.NotEmpty;
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
    @NotNull(message = "Indica para quién es el listado.")
    private String solicitadoPara;
    @NotNull(message = "Selecciona una comunidad autónoma.")
    private Long idCcaa;
    @NotNull(message = "Selecciona una provincia.")
    private Long idProvincia;
    private Long idMunicipio;
    private String localidad;
    private String comentario;
    @NotNull(message = "Selecciona un tipo de ocupación.")
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
    @NotEmpty(message = "Selecciona al menos un tipo de artista.")
    private Set<Long> idsTipoArtista = new HashSet<>();
    @NotEmpty(message = "Selecciona al menos una agencia.")
    private Set<Long> idsAgencias = new HashSet<>();
    @NotEmpty(message = "Selecciona al menos una comunidad del artista.")
    private Set<Long> idsComunidades = new HashSet<>();

}
