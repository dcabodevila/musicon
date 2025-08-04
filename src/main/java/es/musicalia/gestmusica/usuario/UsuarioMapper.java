package es.musicalia.gestmusica.usuario;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UsuarioMapper {


    @Mapping(source = "provincia.id", target = "idProvincia")
    @Mapping(source = "provincia.nombre", target = "provincia")
    UsuarioEdicionDTO toUsuarioEdicionDTO(Usuario usuario);

}