package es.musicalia.gestmusica.tarifa;

import es.musicalia.gestmusica.artista.Artista;
import jakarta.persistence.*;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TarifaSaveDto {
    private Long id;
    private long idArtista;
    private LocalDateTime fechaDesde;
    private LocalDateTime fechaHasta;
    private BigDecimal importe;

    private Boolean activo;

    public TarifaSaveDto(long idArtista, LocalDateTime fechaDesde, LocalDateTime fechaHasta, BigDecimal importe) {
        this.idArtista = idArtista;
        this.fechaDesde = fechaDesde;
        this.fechaHasta = fechaHasta;
        this.importe = importe;
        this.activo = true;

    }

    public TarifaSaveDto(long idArtista) {
        this.idArtista = idArtista;
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

    public LocalDateTime getFechaDesde() {
        return fechaDesde;
    }

    public void setFechaDesde(LocalDateTime fechaDesde) {
        this.fechaDesde = fechaDesde;
    }

    public LocalDateTime getFechaHasta() {
        return fechaHasta;
    }

    public void setFechaHasta(LocalDateTime fechaHasta) {
        this.fechaHasta = fechaHasta;
    }

    public BigDecimal getImporte() {
        return importe;
    }

    public void setImporte(BigDecimal importe) {
        this.importe = importe;
    }

    public Boolean getActivo() {
        return activo;
    }
    public void setActivo(Boolean activo) {
        this.activo = activo;
    }
}
