package es.musicalia.gestmusica.listado;

import es.musicalia.gestmusica.agencia.Agencia;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface ListadoMapper {

    @Mapping(target = "idMunicipio", source = "municipio.id")
    @Mapping(target = "idTipoOcupacion", source = "tipoOcupacion", qualifiedByName = "tipoOcupacionEnumToLong")
    @Mapping(target = "idsAgencias", source = "agencias", qualifiedByName = "agenciasToIds")
    @Mapping(target = "idsTipoArtista", source = "idsTipoArtista", qualifiedByName = "stringToSetLong")
    @Mapping(target = "idsComunidades", source = "idsComunidades", qualifiedByName = "stringToSetLong")
    @Mapping(target = "idCcaa", ignore = true)
    @Mapping(target = "idProvincia", ignore = true)
    ListadoDto toDto(Listado listado);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "municipio", ignore = true)
    @Mapping(target = "usuario", ignore = true)
    @Mapping(target = "tipoOcupacion", source = "idTipoOcupacion", qualifiedByName = "longToTipoOcupacionEnum")
    @Mapping(target = "agencias", ignore = true)
    @Mapping(target = "artistas", ignore = true)
    @Mapping(target = "idsTipoArtista", source = "idsTipoArtista", qualifiedByName = "setLongToString")
    @Mapping(target = "idsComunidades", source = "idsComunidades", qualifiedByName = "setLongToString")
    @Mapping(target = "fechaCreacion", ignore = true)
    Listado toEntity(ListadoDto dto);

    @Named("tipoOcupacionEnumToLong")
    default Long tipoOcupacionEnumToLong(TipoOcupacionEnum tipoOcupacion) {
        return tipoOcupacion != null ? tipoOcupacion.getId() : null;
    }

    @Named("longToTipoOcupacionEnum")
    default TipoOcupacionEnum longToTipoOcupacionEnum(Long idTipoOcupacion) {
        if (idTipoOcupacion == null) {
            return null;
        }
        return TipoOcupacionEnum.getById(idTipoOcupacion);
    }

    @Named("setLongToString")
    default String setLongToString(Set<Long> value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        return value.stream()
                .sorted()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }

    @Named("stringToSetLong")
    default Set<Long> stringToSetLong(String value) {
        if (!StringUtils.hasText(value)) {
            return new HashSet<>();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::valueOf)
                .collect(Collectors.toSet());
    }

    @Named("agenciasToIds")
    default Set<Long> agenciasToIds(Set<Agencia> agencias) {
        if (agencias == null || agencias.isEmpty()) {
            return new HashSet<>();
        }
        return agencias.stream()
                .map(Agencia::getId)
                .collect(Collectors.toSet());
    }

    // Mapeo para fechas
    default LocalDate map(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.toLocalDate() : null;
    }

    default LocalDateTime map(LocalDate date) {
        return date != null ? date.atTime(LocalTime.MIN) : null;
    }
}