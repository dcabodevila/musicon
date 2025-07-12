package es.musicalia.gestmusica.acceso;

import es.musicalia.gestmusica.usuario.Usuario;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface AccesoMapper {

    @Mapping(source = "usuario.id", target = "idUsuario")
    @Mapping(source = "usuario", target = "nombreUsuario", qualifiedByName = "mapUsuarioToNombreUsuario")
    @Mapping(source = "agencia.id", target = "idAgencia")
    @Mapping(source = "agencia.nombre", target = "agencia")
    @Mapping(source = "rol.id", target = "idRol")
    @Mapping(source = "rol.nombre", target = "rol")
    @Mapping(source = "artista.id", target = "idArtista")
    @Mapping(source = "artista.nombre", target = "nombreArtista")
    AccesoDto toAccesoDto(Acceso acceso);

    @Mapping(source = "idUsuario", target = "usuario.id")
    @Mapping(source = "idAgencia", target = "agencia.id")
    @Mapping(source = "idRol", target = "rol.id")
    @Mapping(source = "idArtista", target = "artista.id")
    Acceso toAcceso(AccesoDto accesoDto);

    @Named("mapUsuarioToNombreUsuario")
    default String mapUsuarioToNombreUsuario(Usuario usuario) {
        if (usuario == null) return null;
        return usuario.getNombre() + " " + usuario.getApellidos();
    }
}