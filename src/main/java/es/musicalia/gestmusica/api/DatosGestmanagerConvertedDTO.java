package es.musicalia.gestmusica.api;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DatosGestmanagerConvertedDTO {

        private Long idArtistaGestmanager;
        private TipoAccionGestmanagerEnum accion;
        private LocalDateTime fecha;
        private String descripcion;
        private String poblacion;
        private String municipio;
        private String provincia;
        private String pais;
        private String nombreLocal;
        private TipoEstadoGestmanagerEnum estado;
        private Boolean matinal;
        private Boolean soloMatinal;
}
