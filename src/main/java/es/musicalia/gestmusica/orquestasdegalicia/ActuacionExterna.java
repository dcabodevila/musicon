package es.musicalia.gestmusica.orquestasdegalicia;

import lombok.Data;

import java.time.LocalDate;

@Data
public class ActuacionExterna {
    private Integer idActuacionExterno;
    private Integer idFormacionExterno;
    private LocalDate fecha;
    private String lugar;
    private String provincia;
    private Boolean vermu;
    private Boolean tarde;
    private Boolean noche;
    private String informacion;
}