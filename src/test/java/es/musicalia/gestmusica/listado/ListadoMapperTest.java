package es.musicalia.gestmusica.listado;

import es.musicalia.gestmusica.agencia.Agencia;
import es.musicalia.gestmusica.localizacion.Municipio;
import es.musicalia.gestmusica.localizacion.Provincia;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ListadoMapperTest {

    private final ListadoMapper mapper = new ListadoMapperImpl();

    @Test
    void toEntity_ordenaIdsComoCadenaYResuelveTipoOcupacion() {
        ListadoDto dto = new ListadoDto();
        dto.setSolicitadoPara("Concello");
        dto.setIdMunicipio(150L);
        dto.setIdProvincia(15L);
        dto.setLocalidad("Centro");
        dto.setComentario("Notas");
        dto.setIdTipoOcupacion(2L);
        dto.setFechaDesde(LocalDate.of(2026, 7, 1));
        dto.setFechaHasta(LocalDate.of(2026, 7, 5));
        dto.setFecha3(LocalDate.of(2026, 8, 1));
        dto.setIdsTipoArtista(Set.of(10L, 2L, 7L));
        dto.setIdsComunidades(Set.of(12L));
        dto.setIdsAgencias(Set.of(3L));

        Listado listado = mapper.toEntity(dto);

        assertThat(listado.getId()).isNull();
        assertThat(listado.getSolicitadoPara()).isEqualTo("Concello");
        assertThat(listado.getLocalidad()).isEqualTo("Centro");
        assertThat(listado.getComentario()).isEqualTo("Notas");
        assertThat(listado.getTipoOcupacion()).isEqualTo(TipoOcupacionEnum.SIN_OCUPACION);
        assertThat(listado.getIdsTipoArtista()).isEqualTo("2,7,10");
        assertThat(listado.getIdsComunidades()).isEqualTo("12");
        assertThat(listado.getFechaDesde()).isEqualTo(LocalDate.of(2026, 7, 1));
        assertThat(listado.getFechaHasta()).isEqualTo(LocalDate.of(2026, 7, 5));
        assertThat(listado.getFecha3()).isEqualTo(LocalDate.of(2026, 8, 1));
        assertThat(listado.getMunicipio()).isNull();
        assertThat(listado.getAgencias()).isNull();
        assertThat(listado.getUsuario()).isNull();
        assertThat(listado.getFechaCreacion()).isNull();
    }

    @Test
    void toEntity_idsVaciosYTipoDesconocido_quedanANull() {
        ListadoDto dto = new ListadoDto();
        dto.setIdTipoOcupacion(99L);
        dto.setIdsTipoArtista(Set.of());
        dto.setIdsComunidades(null);

        Listado listado = mapper.toEntity(dto);

        assertThat(listado.getTipoOcupacion()).isNull();
        assertThat(listado.getIdsTipoArtista()).isNull();
        assertThat(listado.getIdsComunidades()).isNull();
    }

    @Test
    void toDto_convierteCadenasYRelacionesEnIds() {
        Municipio municipio = new Municipio();
        municipio.setId(150L);
        Provincia provincia = new Provincia();
        provincia.setId(15L);
        Agencia agencia = new Agencia();
        agencia.setId(3L);

        Listado listado = new Listado();
        listado.setSolicitadoPara("Concello");
        listado.setMunicipio(municipio);
        listado.setProvincia(provincia);
        listado.setTipoOcupacion(TipoOcupacionEnum.CON_OCUPACION);
        listado.setAgencias(Set.of(agencia));
        listado.setIdsTipoArtista("1, 2,,3");
        listado.setIdsComunidades(" ");

        ListadoDto dto = mapper.toDto(listado);

        assertThat(dto.getSolicitadoPara()).isEqualTo("Concello");
        assertThat(dto.getIdMunicipio()).isEqualTo(150L);
        assertThat(dto.getIdProvincia()).isEqualTo(15L);
        assertThat(dto.getIdCcaa()).isNull();
        assertThat(dto.getIdTipoOcupacion()).isEqualTo(1L);
        assertThat(dto.getIdsAgencias()).containsExactly(3L);
        assertThat(dto.getIdsTipoArtista()).containsExactlyInAnyOrder(1L, 2L, 3L);
        assertThat(dto.getIdsComunidades()).isEmpty();
    }

    @Test
    void tipoOcupacionEnum_buscaPorIdYDevuelveNullSiNoExiste() {
        assertThat(TipoOcupacionEnum.getById(1L)).isEqualTo(TipoOcupacionEnum.CON_OCUPACION);
        assertThat(TipoOcupacionEnum.getById(2L)).isEqualTo(TipoOcupacionEnum.SIN_OCUPACION);
        assertThat(TipoOcupacionEnum.getById(3L)).isNull();
        assertThat(TipoOcupacionEnum.getDescripcionById(1L)).isEqualTo("Con ocupación");
        assertThat(TipoOcupacionEnum.getDescripcionById(2L)).isEqualTo("Sin ocupación");
        assertThat(TipoOcupacionEnum.getDescripcionById(3L)).isNull();
    }
}
