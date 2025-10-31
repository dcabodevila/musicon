package es.musicalia.gestmusica.artista;

import es.musicalia.gestmusica.tipoartista.TipoArtista;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", uses = {})
public interface ArtistaMapper {

    @Mapping(target = "id", source = "id")
    @Mapping(target = "idCcaa", source = "ccaa.id")
    @Mapping(target = "nombreCcaa", source = "ccaa.nombre")
    @Mapping(target = "idUsuario", source = "usuario.id")
    @Mapping(target = "nombreUsuario", source = "usuario.nombreCompleto")
    @Mapping(target = "idAgencia", source = "agencia.id")
    @Mapping(target = "nombreAgencia", source = "agencia.nombre")
    @Mapping(target = "idTipoEscenario", source = "tipoEscenario.id")
    @Mapping(target = "nombreTipoEscenario", source = "tipoEscenario.nombre")
    @Mapping(target = "idsTipoArtista", source = "tiposArtista", qualifiedByName = "tipoArtistaToIdList")
    @Mapping(target = "email", source = "contacto.email")
    @Mapping(target = "facebook", source = "contacto.facebook")
    @Mapping(target = "web", source = "contacto.web")
    @Mapping(target = "instagram", source = "contacto.instagram")
    @Mapping(target = "telefono", source = "contacto.telefono")
    @Mapping(target = "telefono2", source = "contacto.telefono2")
    @Mapping(target = "telefono3", source = "contacto.telefono3")
    @Mapping(target = "youtube", source = "contacto.youtube")
    ArtistaDto toDto(Artista artista);

    @Named("tipoArtistaToIdList")
    default List<Long> mapTiposArtistaToIds(Set<TipoArtista> tiposArtista) {
        if (tiposArtista == null || tiposArtista.isEmpty()) {
            return List.of();
        }
        return tiposArtista.stream()
                .map(TipoArtista::getId)
                .collect(Collectors.toList());
    }
}