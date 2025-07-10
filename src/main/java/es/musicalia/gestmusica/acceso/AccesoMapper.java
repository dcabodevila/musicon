package es.musicalia.gestmusica.acceso;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface AccesoMapper {
    AccesoMapper INSTANCE = Mappers.getMapper(AccesoMapper.class);

    @Mapping(source = "usuario.id", target = "idUsuario")
    @Mapping(source = "usuario.username", target = "nombreUsuario")
    @Mapping(source = "agencia.id", target = "idAgencia")
    @Mapping(source = "agencia.nombre", target = "agencia")
    @Mapping(source = "rol.id", target = "idRol")
    @Mapping(source = "rol.nombre", target = "rol")
    @Mapping(source = "artista.id", target = "idArtista")
    AccesoDto toAccesoDto(Acceso acceso);

    @Mapping(source = "idUsuario", target = "usuario.id")
    @Mapping(source = "idAgencia", target = "agencia.id")
    @Mapping(source = "idRol", target = "rol.id")
    @Mapping(source = "idArtista", target = "artista.id")
    Acceso toAcceso(AccesoDto accesoDto);
}

