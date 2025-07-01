package es.musicalia.gestmusica.listado;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;


import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

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
    @NotNull
    private Long idMunicipio;
    @NotNull
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
    private List<Long> idsTipoArtista = new ArrayList<>();
    private List<Long> idsAgencias = new ArrayList<>();
    private List<Long> idsComunidades = new ArrayList<>();







}
