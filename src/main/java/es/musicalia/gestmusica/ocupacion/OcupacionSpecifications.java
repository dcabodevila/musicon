package es.musicalia.gestmusica.ocupacion;

import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.Set;

public class OcupacionSpecifications {

    public static Specification<Ocupacion> hasArtistaIdsIn(Set<Long> idsArtistas) {
        return (root, query, criteriaBuilder) -> root.get("artista").get("id").in(idsArtistas);
    }

    public static Specification<Ocupacion> hasFechaAfter(LocalDateTime startDate) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.greaterThanOrEqualTo(root.get("fecha"), startDate);
    }

    public static Specification<Ocupacion> isActivo() {
        return (root, query, criteriaBuilder) -> criteriaBuilder.isTrue(root.get("activo"));
    }

    public static Specification<Ocupacion> orderByIdDesc() {
        return (root, query, criteriaBuilder) -> {
            query.orderBy(criteriaBuilder.desc(root.get("id")));
            return null;
        };
    }

    public static Specification<Ocupacion> hasAgenciaId(Long agenciaId) {
        if (agenciaId == null) {
            return null;
        }
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("artista").get("agencia").get("id"), agenciaId);
    }


}
