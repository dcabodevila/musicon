package es.musicalia.gestmusica.artista;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

public class ArtistaDto {

    private Long id;
    @NotNull
    @NotEmpty
    private String nombre;
    private String cif;
    private String logo;
    @NotNull
    @NotEmpty
    private Long idUsuario;

    private String nombreUsuario;
    @NotNull
    private Long idCcaa;

    private String nombreCcaa;

    private int componentes;
    private int bailarinas;
    private boolean escenario;
    private String medidasEscenario;
    private int ritmo;
    private int viento;
    private int solistas;
    private int luz;
    private int sonido;
    private Long idTipoEscenario;
    private String nombreTipoEscenario;

    @NotNull
    private Long idTipoArtista;
    @NotNull
    @NotEmpty
    private Long idAgencia;

    private String nombreAgencia;
    private String email;
    private String fax;
    private String web;
    private String instagram;
    private String telefono;
    private String facebook;
    private Boolean activo;

    public ArtistaDto(){
        activo=true;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getCif() {
        return cif;
    }

    public void setCif(String cif) {
        this.cif = cif;
    }

    public String getLogo() {
        return logo;
    }

    public void setLogo(String logo) {
        this.logo = logo;
    }

    public Long getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(Long idUsuario) {
        this.idUsuario = idUsuario;
    }

    public Long getIdCcaa() {
        return idCcaa;
    }

    public void setIdCcaa(Long idCcaa) {
        this.idCcaa = idCcaa;
    }

    public int getComponentes() {
        return componentes;
    }

    public void setComponentes(int componentes) {
        this.componentes = componentes;
    }

    public int getBailarinas() {
        return bailarinas;
    }

    public void setBailarinas(int bailarinas) {
        this.bailarinas = bailarinas;
    }

    public boolean isEscenario() {
        return escenario;
    }

    public void setEscenario(boolean escenario) {
        this.escenario = escenario;
    }

    public String getMedidasEscenario() {
        return medidasEscenario;
    }

    public void setMedidasEscenario(String medidasEscenario) {
        this.medidasEscenario = medidasEscenario;
    }

    public int getRitmo() {
        return ritmo;
    }

    public void setRitmo(int ritmo) {
        this.ritmo = ritmo;
    }

    public int getSolistas() {
        return solistas;
    }

    public void setSolistas(int solistas) {
        this.solistas = solistas;
    }

    public int getLuz() {
        return luz;
    }

    public void setLuz(int luz) {
        this.luz = luz;
    }

    public int getSonido() {
        return sonido;
    }

    public void setSonido(int sonido) {
        this.sonido = sonido;
    }

    public Long getIdTipoEscenario() {
        return idTipoEscenario;
    }

    public void setIdTipoEscenario(Long idTipoEscenario) {
        this.idTipoEscenario = idTipoEscenario;
    }

    public Long getIdTipoArtista() {
        return idTipoArtista;
    }

    public void setIdTipoArtista(Long idTipoArtista) {
        this.idTipoArtista = idTipoArtista;
    }

    public Long getIdAgencia() {
        return idAgencia;
    }

    public void setIdAgencia(Long idAgencia) {
        this.idAgencia = idAgencia;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFax() {
        return fax;
    }

    public void setFax(String fax) {
        this.fax = fax;
    }

    public String getWeb() {
        return web;
    }

    public void setWeb(String web) {
        this.web = web;
    }

    public String getInstagram() {
        return instagram;
    }

    public void setInstagram(String instagram) {
        this.instagram = instagram;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getFacebook() {
        return facebook;
    }

    public void setFacebook(String facebook) {
        this.facebook = facebook;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }

    public String getNombreUsuario() {
        return nombreUsuario;
    }

    public void setNombreUsuario(String nombreUsuario) {
        this.nombreUsuario = nombreUsuario;
    }

    public String getNombreTipoEscenario() {
        return nombreTipoEscenario;
    }

    public void setNombreTipoEscenario(String nombreTipoEscenario) {
        this.nombreTipoEscenario = nombreTipoEscenario;
    }

    public int getViento() {
        return viento;
    }

    public void setViento(int viento) {
        this.viento = viento;
    }

    public String getNombreCcaa() {
        return nombreCcaa;
    }

    public void setNombreCcaa(String nombreCcaa) {
        this.nombreCcaa = nombreCcaa;
    }

    public String getNombreAgencia() {
        return nombreAgencia;
    }

    public void setNombreAgencia(String nombreAgencia) {
        this.nombreAgencia = nombreAgencia;
    }

}
