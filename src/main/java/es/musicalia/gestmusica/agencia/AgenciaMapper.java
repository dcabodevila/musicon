package es.musicalia.gestmusica.agencia;


import es.musicalia.gestmusica.contacto.Contacto;
import es.musicalia.gestmusica.usuario.Usuario;
import org.mapstruct.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AgenciaMapper {
    @Mapping(source = "id", target = "id")
    @Mapping(source = "usuario", target = "nombreUsuario", qualifiedByName = "mapUsuarioToNombreUsuario")
    @Mapping(source = "usuario.id", target = "idUsuario")
    @Mapping(source = "municipio.id", target = "idMunicipio")
    @Mapping(source = "municipio.provincia.id", target = "idProvincia")
    @Mapping(source = "municipio.provincia.nombre", target = "nombreProvincia")
    @Mapping(source = "agenciaContacto", target = ".", qualifiedByName = "mapContactoToDto")
    AgenciaDto toDto(Agencia agencia);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "activo", ignore = true)
    @Mapping(target = "tarifasPublicas", ignore = true)
    @Mapping(source = ".", target = "agenciaContacto", qualifiedByName = "mapDtoToContacto")
    Agencia toEntity(AgenciaDto agenciaDto);

    @Named("mapUsuarioToNombreUsuario")
    default String mapUsuarioToNombreUsuario(Usuario usuario) {
        if (usuario == null) return null;
        return usuario.getNombre() + " " + usuario.getApellidos();
    }

    @Named("mapContactoToDto")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(source = "email", target = "email")
    @Mapping(source = "fax", target = "fax")
    @Mapping(source = "web", target = "web")
    @Mapping(source = "instagram", target = "instagram")
    @Mapping(source = "facebook", target = "facebook")
    @Mapping(source = "telefono", target = "telefono")
    @Mapping(source = "telefono2", target = "telefono2")
    @Mapping(source = "telefono3", target = "telefono3")
    @Mapping(source = "youtube", target = "youtube")
    void mapContactoToDto(Contacto agenciaContacto, @MappingTarget AgenciaDto agenciaDto);

    @Named("mapDtoToContacto")
    default Contacto mapDtoToContacto(AgenciaDto agenciaDto) {
        if (agenciaDto == null) return null;

        Contacto contacto = new Contacto();
        contacto.setEmail(agenciaDto.getEmail());
        contacto.setFax(agenciaDto.getFax());
        contacto.setWeb(agenciaDto.getWeb());
        contacto.setInstagram(agenciaDto.getInstagram());
        contacto.setFacebook(agenciaDto.getFacebook());
        contacto.setTelefono(agenciaDto.getTelefono());
        contacto.setTelefono2(agenciaDto.getTelefono2());
        contacto.setTelefono3(agenciaDto.getTelefono3());
        contacto.setYoutube(agenciaDto.getYoutube());

        return contacto;
    }
}
