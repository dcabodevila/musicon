package es.musicalia.gestmusica.api;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "sincronizacion", schema="gestmusica")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Sincronizacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long idArtista;

    @Column(length = 1000)
    private String descripcion;

    private String fecha; // Formato DD-MM-AAAA
    private String poblacion;
    private String municipio;
    private String provincia;
    private String pais;

    @Column(name = "nombre_local")
    private String nombreLocal;

    private String accion;
    private String estado;
    private String indicadores;

    private LocalDateTime fechaRecepcion;
    @Column(name= "caddatos", length = 1000)
    private String cadDatos;

    private Boolean procesado;

    @Column(name= "codigo_error", length = 1000)
    private String codigoError;
}

