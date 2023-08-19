package es.musicalia.gestmusica.incremento;

import java.math.BigDecimal;

public class IncrementoSaveDto {
    private Long id;
    private long idArtista;
    private long idProvincia;

     private long idTipoIncremento;

     private BigDecimal incremento;
    public IncrementoSaveDto(long idArtista, long idProvincia, long idTipoIncremento, BigDecimal importe) {
        this.idArtista = idArtista;
        this.idProvincia = idProvincia;
        this.idTipoIncremento = idTipoIncremento;
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

}