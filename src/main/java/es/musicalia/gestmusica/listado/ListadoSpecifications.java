package es.musicalia.gestmusica.listado;

import es.musicalia.gestmusica.agencia.Agencia;
import es.musicalia.gestmusica.localizacion.Municipio;
import es.musicalia.gestmusica.localizacion.Provincia;
import es.musicalia.gestmusica.usuario.Usuario;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ListadoSpecifications {

    public static Specification<Listado> hasAgencia(Long idAgencia) {
        return (root, query, criteriaBuilder) -> {
            if (idAgencia == null) {
                return criteriaBuilder.conjunction();
            }
            
            Join<Listado, Agencia> agenciaJoin = root.join("agencias", JoinType.INNER);
            return criteriaBuilder.equal(agenciaJoin.get("id"), idAgencia);
        };
    }

    public static Specification<Listado> hasAgencias(Set<Long> idsAgencias) {
        return (root, query, criteriaBuilder) -> {
            if (idsAgencias == null || idsAgencias.isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            
            Join<Listado, Agencia> agenciaJoin = root.join("agencias", JoinType.INNER);
            return agenciaJoin.get("id").in(idsAgencias);
        };
    }

    public static Specification<Listado> hasFechaCreacionDesde(LocalDateTime fechaDesde) {
        return (root, query, criteriaBuilder) -> {
            if (fechaDesde == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.greaterThanOrEqualTo(root.get("fechaCreacion"), fechaDesde);
        };
    }

    public static Specification<Listado> hasFechaCreacionHasta(LocalDateTime fechaHasta) {
        return (root, query, criteriaBuilder) -> {
            if (fechaHasta == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.lessThanOrEqualTo(root.get("fechaCreacion"), fechaHasta);
        };
    }

    public static Specification<Listado> hasFechaCreacionEntre(LocalDateTime fechaDesde, LocalDateTime fechaHasta) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            if (fechaDesde != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("fechaCreacion"), fechaDesde));
            }
            
            if (fechaHasta != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("fechaCreacion"), fechaHasta));
            }
            
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<Listado> hasSolicitadoPara(String solicitadoPara) {
        return (root, query, criteriaBuilder) -> {
            if (solicitadoPara == null || solicitadoPara.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(
                criteriaBuilder.lower(root.get("solicitadoPara")), 
                "%" + solicitadoPara.toLowerCase() + "%"
            );
        };
    }

    public static Specification<Listado> hasLocalidad(String localidad) {
        return (root, query, criteriaBuilder) -> {
            if (localidad == null || localidad.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(
                criteriaBuilder.lower(root.get("localidad")), 
                "%" + localidad.toLowerCase() + "%"
            );
        };
    }

    public static Specification<Listado> hasMunicipio(Long idMunicipio) {
        return (root, query, criteriaBuilder) -> {
            if (idMunicipio == null) {
                return criteriaBuilder.conjunction();
            }
            
            Join<Listado, Municipio> municipioJoin = root.join("municipio", JoinType.INNER);
            return criteriaBuilder.equal(municipioJoin.get("id"), idMunicipio);
        };
    }

    public static Specification<Listado> hasMunicipioNombre(String nombreMunicipio) {
        return (root, query, criteriaBuilder) -> {
            if (nombreMunicipio == null || nombreMunicipio.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            
            Join<Listado, Municipio> municipioJoin = root.join("municipio", JoinType.INNER);
            return criteriaBuilder.like(
                criteriaBuilder.lower(municipioJoin.get("nombre")), 
                "%" + nombreMunicipio.toLowerCase() + "%"
            );
        };
    }

    public static Specification<Listado> hasProvincia(Long idProvincia) {
        return (root, query, criteriaBuilder) -> {
            if (idProvincia == null) {
                return criteriaBuilder.conjunction();
            }
            
            Join<Listado, Municipio> municipioJoin = root.join("municipio", JoinType.INNER);
            Join<Municipio, Provincia> provinciaJoin = municipioJoin.join("provincia", JoinType.INNER);
            return criteriaBuilder.equal(provinciaJoin.get("id"), idProvincia);
        };
    }

    public static Specification<Listado> hasUsuario(Long idUsuario) {
        return (root, query, criteriaBuilder) -> {
            if (idUsuario == null) {
                return criteriaBuilder.conjunction();
            }
            
            Join<Listado, Usuario> usuarioJoin = root.join("usuario", JoinType.INNER);
            return criteriaBuilder.equal(usuarioJoin.get("id"), idUsuario);
        };
    }

    public static Specification<Listado> hasUsuarioNombre(String nombreUsuario) {
        return (root, query, criteriaBuilder) -> {
            if (nombreUsuario == null || nombreUsuario.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            
            Join<Listado, Usuario> usuarioJoin = root.join("usuario", JoinType.INNER);
            return criteriaBuilder.or(
                criteriaBuilder.like(
                    criteriaBuilder.lower(usuarioJoin.get("nombre")), 
                    "%" + nombreUsuario.toLowerCase() + "%"
                ),
                criteriaBuilder.like(
                    criteriaBuilder.lower(usuarioJoin.get("apellidos")), 
                    "%" + nombreUsuario.toLowerCase() + "%"
                )
            );
        };
    }

    public static Specification<Listado> hasTipoOcupacion(TipoOcupacionEnum tipoOcupacion) {
        return (root, query, criteriaBuilder) -> {
            if (tipoOcupacion == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("tipoOcupacion"), tipoOcupacion);
        };
    }

    public static Specification<Listado> hasFechaDesde(LocalDate fechaDesde) {
        return (root, query, criteriaBuilder) -> {
            if (fechaDesde == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("fechaDesde"), fechaDesde);
        };
    }

    public static Specification<Listado> hasFechaHasta(LocalDate fechaHasta) {
        return (root, query, criteriaBuilder) -> {
            if (fechaHasta == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("fechaHasta"), fechaHasta);
        };
    }

    public static Specification<Listado> hasFechaEnRango(LocalDate fechaDesde, LocalDate fechaHasta) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            if (fechaDesde != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("fechaDesde"), fechaDesde));
            }
            
            if (fechaHasta != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("fechaHasta"), fechaHasta));
            }
            
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<Listado> hasComentario(String comentario) {
        return (root, query, criteriaBuilder) -> {
            if (comentario == null || comentario.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(
                criteriaBuilder.lower(root.get("comentario")), 
                "%" + comentario.toLowerCase() + "%"
            );
        };
    }

    // Método de conveniencia para combinar criterios similares a findListadosByAgenciaAndFechasOpcionales
    public static Specification<Listado> findListadosByAgenciaAndFechas(Long idAgencia, LocalDateTime fechaDesde, LocalDateTime fechaHasta) {
        return Specification.where(hasAgencia(idAgencia))
                .and(hasFechaCreacionEntre(fechaDesde, fechaHasta));
    }

    // Método de conveniencia para búsquedas complejas
    public static Specification<Listado> findListadosCompleto(
            Long idAgencia, 
            LocalDateTime fechaCreacionDesde, 
            LocalDateTime fechaCreacionHasta,
            String solicitadoPara,
            String localidad,
            Long idMunicipio,
            Long idProvincia,
            Long idUsuario,
            TipoOcupacionEnum tipoOcupacion) {
        
        return Specification.where(hasAgencia(idAgencia))
                .and(hasFechaCreacionEntre(fechaCreacionDesde, fechaCreacionHasta))
                .and(hasSolicitadoPara(solicitadoPara))
                .and(hasLocalidad(localidad))
                .and(hasMunicipio(idMunicipio))
                .and(hasProvincia(idProvincia))
                .and(hasUsuario(idUsuario))
                .and(hasTipoOcupacion(tipoOcupacion));
    }
}