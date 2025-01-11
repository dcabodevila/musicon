package es.musicalia.gestmusica.contacto;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "contacto", schema="gestmusica")
@Getter
@Setter
public class Contacto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "email")
    private String email;

    @Column(name = "fax")
    private String fax;

    @Column(name = "web")
    private String web;

    @Column(name = "instagram")
    private String instagram;
    @Column(name = "telefono")
    private String telefono;
    @Column(name = "telefono2")
    private String telefono2;
    @Column(name = "telefono3")
    private String telefono3;
    @Column(name = "twitter")
    private String twitter;

    @Column(name = "facebook")
    private String facebook;

    @Column(name = "youtube")
    private String youtube;

}
