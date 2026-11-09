package es.musicalia.gestmusica.ocupacion;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Optional listado filters of OcupacionSpecifications: absent values add no restriction,
 * present values restrict on the expected attribute.
 */
class OcupacionSpecificationsTest {

    @SuppressWarnings("unchecked")
    private final Root<Ocupacion> root = mock(Root.class, RETURNS_DEEP_STUBS);
    @SuppressWarnings("unchecked")
    private final CriteriaQuery<Object> query = mock(CriteriaQuery.class);
    private final CriteriaBuilder cb = mock(CriteriaBuilder.class, RETURNS_DEEP_STUBS);

    @Test
    void optionalFilters_withoutValue_addNoRestriction() {
        assertThat(OcupacionSpecifications.hasArtistaId(null)).isNull();
        assertThat(OcupacionSpecifications.hasFechaBefore(null)).isNull();
        assertThat(OcupacionSpecifications.hasAgenciaId(null)).isNull();
        assertThat(OcupacionSpecifications.hasUsuarioId(false, 1L)).isNull();
        assertThat(OcupacionSpecifications.hasUsuarioId(null, 1L)).isNull();
        assertThat(OcupacionSpecifications.hasGlobalSearch(null)).isNull();
        assertThat(OcupacionSpecifications.hasGlobalSearch("   ")).isNull();
    }

    @Test
    void agenciaArtistaAndUsuario_filterByTheirIds() {
        OcupacionSpecifications.hasAgenciaId(10L).toPredicate(root, query, cb);
        OcupacionSpecifications.hasArtistaId(5L).toPredicate(root, query, cb);
        OcupacionSpecifications.hasUsuarioId(true, 1L).toPredicate(root, query, cb);

        verify(cb).equal(root.get("artista").get("agencia").get("id"), 10L);
        verify(cb).equal(root.get("artista").get("id"), 5L);
        verify(cb).equal(root.get("usuario").get("id"), 1L);
    }

    @Test
    void dateRangeIsInclusive() {
        LocalDateTime desde = LocalDateTime.of(2025, 8, 1, 0, 0);
        LocalDateTime hasta = LocalDateTime.of(2025, 8, 31, 23, 59, 59);

        OcupacionSpecifications.hasFechaAfter(desde).toPredicate(root, query, cb);
        OcupacionSpecifications.hasFechaBefore(hasta).toPredicate(root, query, cb);

        Path<LocalDateTime> fecha = root.get("fecha");
        verify(cb).greaterThanOrEqualTo(fecha, desde);
        verify(cb).lessThanOrEqualTo(fecha, hasta);
    }

    @Test
    void anuladasAreExcludedByEstadoName() {
        OcupacionSpecifications.hasEstadoNotAnulado().toPredicate(root, query, cb);

        verify(cb).notEqual(root.get("ocupacionEstado").get("nombre"), "Anulado");
    }

    @Test
    void globalSearch_isTrimmedLowercasedAndAppliedToSevenColumns() {
        OcupacionSpecifications.hasGlobalSearch("  OrQuesta ").toPredicate(root, query, cb);

        verify(cb, times(7)).like(any(Expression.class), eq("%orquesta%"));
        verify(cb).lower(root.get("artista").get("nombre"));
        verify(cb).lower(root.get("poblacion"));
        verify(cb).lower(root.get("municipio").get("nombre"));
        verify(cb).lower(root.get("provincia").get("nombre"));
        verify(cb).lower(root.get("usuario").get("nombre"));
        verify(cb).lower(root.get("usuario").get("apellidos"));
        verify(cb).lower(root.get("ocupacionEstado").get("nombre"));
    }
}
