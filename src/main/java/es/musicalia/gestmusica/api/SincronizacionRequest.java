package es.musicalia.gestmusica.api;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SincronizacionRequest {
    private Long idArtista;
    private String descripcion;
    private String fecha; // Formato DD-MM-AAAA
    private String poblacion;
    private String municipio;
    private String provincia;
    private String pais;
    private String nombreLocal;
    private String accion; // alta, Mod_fecha_agr, etc.
    private String estado; // O o R
    private String indicadores; // Ejemplo: "101"

}
