package es.musicalia.gestmusica.ocupacion;


import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface OcupacionMapper {

    @Mapping(source = "artista.agencia.id", target = "idAgencia")
    @Mapping(source = "artista.id", target = "idArtista")
    @Mapping(source = "tipoOcupacion.id", target = "idTipoOcupacion")
    @Mapping(source = "provincia.ccaa.id", target = "idCcaa")
    @Mapping(source = "provincia.id", target = "idProvincia")
    @Mapping(source = "municipio.id", target = "idMunicipio")
    @Mapping(source = "poblacion", target = "localidad")
    @Mapping(source = "ocupacionEstado.nombre", target = "estado")
    @Mapping(source = "usuario.id", target = "idUsuario")
    OcupacionSaveDto toDto(Ocupacion ocupacion);



}
