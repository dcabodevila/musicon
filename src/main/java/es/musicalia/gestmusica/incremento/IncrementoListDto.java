package es.musicalia.gestmusica.incremento;

import java.math.BigDecimal;

public class IncrementoListDto {
    private Long id;
    private long idArtista;

    private String descripcionArtista;
    private long idProvincia;

    private String descripcionProvincia;

    private long idTipoIncremento;

    private String decripcionTipoIncremento;

    private BigDecimal incremento;
    public IncrementoListDto(long id, long idArtista, String descripcionArtista, long idProvincia, String descripcionProvincia, long idTipoIncremento, String decripcionTipoIncremento, BigDecimal importe) {

        this.id = id;
        this.idArtista = idArtista;
        this.descripcionArtista =descripcionArtista;
        this.idProvincia = idProvincia;
        this.descripcionProvincia = descripcionProvincia;
        this.idTipoIncremento = idTipoIncremento;
        this.decripcionTipoIncremento = decripcionTipoIncremento;
        this.incremento = importe;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public long getIdArtista() {
        return idArtista;
    }

    public void setIdArtista(long idArtista) {
        this.idArtista = idArtista;
    }

    public long getIdProvincia() {
        return idProvincia;
    }

    public void setIdProvincia(long idProvincia) {
        this.idProvincia = idProvincia;
    }

    public long getIdTipoIncremento() {
        return idTipoIncremento;
    }

    public void setIdTipoIncremento(long idTipoIncremento) {
        this.idTipoIncremento = idTipoIncremento;
    }

    public BigDecimal getIncremento() {
        return incremento;
    }

    public void setIncremento(BigDecimal incremento) {
        this.incremento = incremento;
    }

    public String getDescripcionArtista() {
        return descripcionArtista;
    }

    public void setDescripcionArtista(String descripcionArtista) {
        this.descripcionArtista = descripcionArtista;
    }

    public String getDescripcionProvincia() {
        return descripcionProvincia;
    }

    public void setDescripcionProvincia(String descripcionProvincia) {
        this.descripcionProvincia = descripcionProvincia;
    }

    public String getDecripcionTipoIncremento() {
        return decripcionTipoIncremento;
    }

    public void setDecripcionTipoIncremento(String decripcionTipoIncremento) {
        this.decripcionTipoIncremento = decripcionTipoIncremento;
    }
}