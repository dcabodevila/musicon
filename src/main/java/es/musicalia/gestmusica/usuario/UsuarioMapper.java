package es.musicalia.gestmusica.usuario;

import es.musicalia.gestmusica.acceso.AccesoMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface UsuarioMapper {
    UsuarioMapper INSTANCE = Mappers.getMapper(UsuarioMapper.class);

    UsuarioEdicionDTO toUsuarioEdicionDTO(Usuario usuario);
    Usuario toUsuario(UsuarioEdicionDTO usuarioEdicionDTO);
}