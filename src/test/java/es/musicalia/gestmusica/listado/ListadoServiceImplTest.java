package es.musicalia.gestmusica.listado;

import es.musicalia.gestmusica.accesoartista.AccesoArtista;
import es.musicalia.gestmusica.accesoartista.AccesoArtistaRepository;
import es.musicalia.gestmusica.agencia.Agencia;
import es.musicalia.gestmusica.agencia.AgenciaRepository;
import es.musicalia.gestmusica.artista.Artista;
import es.musicalia.gestmusica.artista.ArtistaRepository;
import es.musicalia.gestmusica.auth.model.CustomAuthenticatedUser;
import es.musicalia.gestmusica.informe.InformeService;
import es.musicalia.gestmusica.localizacion.Ccaa;
import es.musicalia.gestmusica.localizacion.CodigoNombreDto;
import es.musicalia.gestmusica.localizacion.Municipio;
import es.musicalia.gestmusica.localizacion.MunicipioRepository;
import es.musicalia.gestmusica.localizacion.Provincia;
import es.musicalia.gestmusica.localizacion.ProvinciaRepository;
import es.musicalia.gestmusica.permiso.PermisoArtistaEnum;
import es.musicalia.gestmusica.permiso.PermisoRecord;
import es.musicalia.gestmusica.permiso.PermisoRepository;
import es.musicalia.gestmusica.usuario.Usuario;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListadoServiceImplTest {

    private static final Long ID_USUARIO = 7L;
    private static final long ID_PERMISO_RESTRINGIDOS = 33L;
    private static final byte[] PDF = {1, 2, 3};

    @Mock
    private InformeService informeService;
    @Mock
    private ProvinciaRepository provinciaRepository;
    @Mock
    private MunicipioRepository municipioRepository;
    @Mock
    private ListadoRepository listadoRepository;
    @Mock
    private AgenciaRepository agenciaRepository;
    @Mock
    private ArtistaRepository artistaRepository;
    @Mock
    private AccesoArtistaRepository accesoArtistaRepository;
    @Mock
    private PermisoRepository permisoRepository;
    @Mock
    private EntityManager entityManager;

    private ListadoServiceImpl service;
    private Usuario usuarioAutenticado;

    @BeforeEach
    void setUp() {
        service = new ListadoServiceImpl(informeService, provinciaRepository, municipioRepository, new ListadoMapperImpl(),
                listadoRepository, agenciaRepository, artistaRepository, accesoArtistaRepository, permisoRepository, entityManager);

        usuarioAutenticado = new Usuario();
        usuarioAutenticado.setId(ID_USUARIO);
        usuarioAutenticado.setUsername("representante");
        usuarioAutenticado.setNombre("Repre");
        usuarioAutenticado.setPassword("{noop}x");
        CustomAuthenticatedUser principal = new CustomAuthenticatedUser(usuarioAutenticado, true, true, true, true,
                List.of(new SimpleGrantedAuthority("ROLE_USER")), Map.of(), Map.of());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void generarInformeListado_rangoDeSieteDiasConOcupacion_usaReportVerticalYGuardaListado() {
        Provincia provincia = provincia(15L, "A Coruña");
        Agencia agencia = new Agencia();
        agencia.setId(3L);
        when(provinciaRepository.findById(15L)).thenReturn(Optional.of(provincia));
        when(agenciaRepository.findAllById(Set.of(3L))).thenReturn(List.of(agencia));
        sinArtistasRestringidos();
        when(informeService.imprimirInforme(anyMap(), anyString(), anyString())).thenReturn(PDF);

        ListadoDto dto = listadoValido(TipoOcupacionEnum.CON_OCUPACION);
        dto.setFechaDesde(LocalDate.of(2026, 3, 2));
        dto.setFechaHasta(LocalDate.of(2026, 3, 8));

        byte[] resultado = service.generarInformeListado(dto, ID_USUARIO);

        assertThat(resultado).isSameAs(PDF);
        Map<String, Object> parametros = capturarInforme(TipoReportEnum.LISTADO_CON_OCUPACION_VERTICAL, "Listado_Con ocupación");
        assertThat(parametros)
                .containsEntry("titulo", "Presupuesto con ocupación")
                .containsEntry("idProvincia", 15)
                .containsEntry("idCcaa", 12)
                .containsEntry("provincia", "A Coruña")
                .containsEntry("municipio", "")
                .containsEntry("lugar", "")
                .containsEntry("solicitante", "Cliente Fiestas")
                .containsEntry("observaciones", "Comentario")
                .containsEntry("idsAgencias", "3")
                .containsEntry("idsComunidades", "11")
                .containsEntry("idsArtistaRestringidos", "-1")
                .containsEntry("fechaListIn", "''2026-03-02'', ''2026-03-03'', ''2026-03-04'', ''2026-03-05'', "
                        + "''2026-03-06'', ''2026-03-07'', ''2026-03-08''")
                .containsEntry("dia1", "02/03")
                .containsEntry("dia7", "08/03")
                .containsEntry("fecha1", "''2026-03-02''")
                .containsEntry("fecha15", "''2026-03-16''")
                .doesNotContainKeys("dia8", "fecha16");
        assertThat(splitIds(parametros.get("idsTipoArtista"))).containsExactlyInAnyOrder("1", "2");
        assertThat((String) parametros.get("colNames"))
                .startsWith("\"Artista\" text, \"Agencia\" text, \"Telefono\" text")
                .endsWith("\"dia15val\" text");

        ArgumentCaptor<Listado> guardado = ArgumentCaptor.forClass(Listado.class);
        verify(listadoRepository).save(guardado.capture());
        Listado listado = guardado.getValue();
        assertThat(listado.getId()).isNull();
        assertThat(listado.isActivo()).isTrue();
        assertThat(listado.getUsuario()).isSameAs(usuarioAutenticado);
        assertThat(listado.getSolicitadoPara()).isEqualTo("Cliente Fiestas");
        assertThat(listado.getComentario()).isEqualTo("Comentario");
        assertThat(listado.getTipoOcupacion()).isEqualTo(TipoOcupacionEnum.CON_OCUPACION);
        assertThat(listado.getProvincia()).isSameAs(provincia);
        assertThat(listado.getMunicipio()).isNull();
        assertThat(listado.getAgencias()).containsExactly(agencia);
        assertThat(listado.getIdsTipoArtista()).isEqualTo("1,2");
        assertThat(listado.getIdsComunidades()).isEqualTo("11");
        assertThat(listado.getFechaDesde()).isEqualTo(LocalDate.of(2026, 3, 2));
        assertThat(listado.getFechaHasta()).isEqualTo(LocalDate.of(2026, 3, 8));
        assertThat(listado.getFecha1()).isNull();
    }

    @Test
    void generarInformeListado_rangoDeOchoDiasSinOcupacion_usaReportHorizontalConDiecisieisDias() {
        prepararGeneracionBasica();

        ListadoDto dto = listadoValido(TipoOcupacionEnum.SIN_OCUPACION);
        dto.setFechaDesde(LocalDate.of(2026, 3, 2));
        dto.setFechaHasta(LocalDate.of(2026, 3, 9));

        service.generarInformeListado(dto, ID_USUARIO);

        Map<String, Object> parametros = capturarInforme(TipoReportEnum.LISTADO_SIN_OCUPACION_HORIZONTAL, "Listado_Sin ocupación");
        assertThat(parametros)
                .containsEntry("titulo", "Presupuesto sin ocupación")
                .containsEntry("dia8", "09/03")
                .containsEntry("dia9", "")
                .containsEntry("dia16", "")
                .doesNotContainKey("dia17");
        verify(listadoRepository).save(any(Listado.class));
    }

    @Test
    void generarInformeListado_rangoDeOchoDiasConOcupacion_usaReportHorizontalConOcupacion() {
        prepararGeneracionBasica();

        ListadoDto dto = listadoValido(TipoOcupacionEnum.CON_OCUPACION);
        dto.setFechaDesde(LocalDate.of(2026, 3, 2));
        dto.setFechaHasta(LocalDate.of(2026, 3, 9));

        service.generarInformeListado(dto, ID_USUARIO);

        assertThat(capturarInforme(TipoReportEnum.LISTADO_CON_OCUPACION_HORIZONTAL, "Listado_Con ocupación"))
                .containsEntry("titulo", "Presupuesto con ocupación");
    }

    @Test
    void generarInformeListado_fechasSueltasDesordenadasSinOcupacion_ordenaFechasYUsaReportVertical() {
        Provincia provincia = provincia(15L, "A Coruña");
        Municipio municipio = new Municipio();
        municipio.setId(150L);
        municipio.setNombre("Betanzos");
        when(provinciaRepository.findById(15L)).thenReturn(Optional.of(provincia));
        when(municipioRepository.findById(150L)).thenReturn(Optional.of(municipio));
        when(agenciaRepository.findAllById(Set.of(3L))).thenReturn(List.of());
        sinArtistasRestringidos();
        when(informeService.imprimirInforme(anyMap(), anyString(), anyString())).thenReturn(PDF);

        ListadoDto dto = listadoValido(TipoOcupacionEnum.SIN_OCUPACION);
        dto.setIdMunicipio(150L);
        dto.setLocalidad("Plaza Mayor");
        dto.setFecha1(LocalDate.of(2026, 5, 10));
        dto.setFecha2(LocalDate.of(2026, 5, 3));

        service.generarInformeListado(dto, ID_USUARIO);

        Map<String, Object> parametros = capturarInforme(TipoReportEnum.LISTADO_SIN_OCUPACION_VERTICAL, "Listado_Sin ocupación");
        assertThat(parametros)
                .containsEntry("municipio", "Betanzos")
                .containsEntry("lugar", "Plaza Mayor")
                .containsEntry("fechaListIn", "''2026-05-03'', ''2026-05-10''")
                .containsEntry("dia1", "03/05")
                .containsEntry("dia2", "10/05")
                .containsEntry("dia3", "")
                .containsEntry("dia7", "")
                .containsEntry("fecha1", "''2026-05-03''")
                .containsEntry("fecha2", "''2026-05-10''")
                .containsEntry("fecha3", "''2026-05-11''")
                .containsEntry("fecha15", "''2026-05-23''")
                .doesNotContainKeys("dia8", "fecha16");

        ArgumentCaptor<Listado> guardado = ArgumentCaptor.forClass(Listado.class);
        verify(listadoRepository).save(guardado.capture());
        assertThat(guardado.getValue().getMunicipio()).isSameAs(municipio);
        assertThat(guardado.getValue().getLocalidad()).isEqualTo("Plaza Mayor");
        assertThat(guardado.getValue().getFecha1()).isEqualTo(LocalDate.of(2026, 5, 10));
        assertThat(guardado.getValue().getFecha2()).isEqualTo(LocalDate.of(2026, 5, 3));
        assertThat(guardado.getValue().getTipoOcupacion()).isEqualTo(TipoOcupacionEnum.SIN_OCUPACION);
    }

    @Test
    void generarInformeListado_conArtistasRestringidos_pasaSusIdsAlInforme() {
        when(provinciaRepository.findById(15L)).thenReturn(Optional.of(provincia(15L, "A Coruña")));
        when(agenciaRepository.findAllById(Set.of(3L))).thenReturn(List.of());
        when(permisoRepository.findPermisoRecordByCodigo(PermisoArtistaEnum.PRESUPUESTOS_RESTRINGIDOS.name()))
                .thenReturn(new PermisoRecord(ID_PERMISO_RESTRINGIDOS, "PRESUPUESTOS_RESTRINGIDOS", "Restringidos"));
        when(accesoArtistaRepository.findAllAccesosArtistaByIdUsuarioIdPermiso(ID_USUARIO, ID_PERMISO_RESTRINGIDOS))
                .thenReturn(Optional.of(List.of(accesoArtista(5L), accesoArtista(9L))));
        when(informeService.imprimirInforme(anyMap(), anyString(), anyString())).thenReturn(PDF);

        ListadoDto dto = listadoValido(TipoOcupacionEnum.CON_OCUPACION);
        dto.setFecha1(LocalDate.of(2026, 5, 3));

        service.generarInformeListado(dto, ID_USUARIO);

        Map<String, Object> parametros = capturarInforme(TipoReportEnum.LISTADO_CON_OCUPACION_VERTICAL, "Listado_Con ocupación");
        assertThat(splitIds(parametros.get("idsArtistaRestringidos"))).containsExactlyInAnyOrder("5", "9");
    }

    @Test
    void generarInformeListado_errorDelInforme_propagaExcepcionSinGuardarListado() {
        when(provinciaRepository.findById(15L)).thenReturn(Optional.of(provincia(15L, "A Coruña")));
        sinArtistasRestringidos();
        when(informeService.imprimirInforme(anyMap(), anyString(), anyString()))
                .thenThrow(new IllegalStateException("Jasper caído"));

        ListadoDto dto = listadoValido(TipoOcupacionEnum.CON_OCUPACION);
        dto.setFecha1(LocalDate.of(2026, 5, 3));

        assertThatThrownBy(() -> service.generarInformeListado(dto, ID_USUARIO))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Jasper caído");
        verify(listadoRepository, never()).save(any());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("peticionesInvalidas")
    void generarInformeListado_peticionInvalida_lanzaIllegalArgumentSinInformeNiGuardado(
            String caso, Consumer<ListadoDto> modificacion, String mensajeEsperado) {
        ListadoDto dto = listadoValido(TipoOcupacionEnum.CON_OCUPACION);
        dto.setFecha1(LocalDate.of(2026, 5, 3));
        modificacion.accept(dto);

        assertThatThrownBy(() -> service.generarInformeListado(dto, ID_USUARIO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(mensajeEsperado);
        verifyNoInteractions(informeService, listadoRepository, agenciaRepository);
    }

    static Stream<Arguments> peticionesInvalidas() {
        return Stream.of(
                Arguments.of("sin agencias", (Consumer<ListadoDto>) d -> d.setIdsAgencias(Set.of()),
                        "Selecciona al menos una agencia."),
                Arguments.of("agencias null", (Consumer<ListadoDto>) d -> d.setIdsAgencias(null),
                        "Selecciona al menos una agencia."),
                Arguments.of("sin tipos de artista", (Consumer<ListadoDto>) d -> d.setIdsTipoArtista(Set.of()),
                        "Selecciona al menos un tipo de artista."),
                Arguments.of("sin comunidades", (Consumer<ListadoDto>) d -> d.setIdsComunidades(Set.of()),
                        "Selecciona al menos una comunidad del artista."),
                Arguments.of("rango incompleto", (Consumer<ListadoDto>) d -> {
                    d.setFecha1(null);
                    d.setFechaDesde(LocalDate.of(2026, 5, 1));
                }, "Completa fecha inicial y fecha final, o borra el rango y usa fechas sueltas."),
                Arguments.of("sin fechas", (Consumer<ListadoDto>) d -> d.setFecha1(null),
                        "Introduce fecha inicial y final, o al menos una fecha suelta."),
                Arguments.of("rango y fechas sueltas", (Consumer<ListadoDto>) d -> {
                    d.setFechaDesde(LocalDate.of(2026, 5, 1));
                    d.setFechaHasta(LocalDate.of(2026, 5, 2));
                }, "No mezcles rango de fechas con fechas sueltas. Borra uno de los dos bloques."),
                Arguments.of("fecha final anterior a la inicial", (Consumer<ListadoDto>) d -> {
                    d.setFecha1(null);
                    d.setFechaDesde(LocalDate.of(2026, 5, 10));
                    d.setFechaHasta(LocalDate.of(2026, 5, 9));
                }, "La fecha de fin no puede ser anterior a la fecha de inicio.")
        );
    }

    @Test
    void obtenerListadoEntreFechas_mapeaEntidadesARecordsOrdenadosPorFechaCreacionDesc() {
        Ccaa ccaa = new Ccaa();
        ccaa.setNombre("Galicia");
        Provincia provincia = provincia(15L, "A Coruña");
        provincia.setCcaa(ccaa);
        Municipio municipio = new Municipio();
        municipio.setNombre("Betanzos");
        municipio.setProvincia(provincia);

        Usuario conNombreComercial = new Usuario();
        conNombreComercial.setNombre("Ana");
        conNombreComercial.setApellidos("Pérez");
        conNombreComercial.setNombreComercial("Orquesta Ana");
        Usuario sinNombreComercial = new Usuario();
        sinNombreComercial.setNombre("Luis");
        sinNombreComercial.setApellidos("Gómez");

        Listado completo = new Listado();
        completo.setId(1L);
        completo.setSolicitadoPara("Concello");
        completo.setUsuario(conNombreComercial);
        completo.setMunicipio(municipio);
        completo.setProvincia(provincia);
        completo.setLocalidad("Centro");
        completo.setTipoOcupacion(TipoOcupacionEnum.SIN_OCUPACION);
        completo.setFechaCreacion(LocalDateTime.of(2026, 4, 1, 10, 0));
        completo.setFecha1(LocalDate.of(2026, 6, 1));
        completo.setFechaDesde(LocalDate.of(2026, 7, 1));
        completo.setFechaHasta(LocalDate.of(2026, 7, 5));

        Listado minimo = new Listado();
        minimo.setId(2L);
        minimo.setUsuario(sinNombreComercial);
        minimo.setFechaCreacion(LocalDateTime.of(2026, 3, 1, 9, 0));

        when(listadoRepository.findAll(any(Specification.class), eq(Sort.by(Sort.Direction.DESC, "fechaCreacion"))))
                .thenReturn(List.of(completo, minimo));

        List<ListadoRecord> records = service.obtenerListadoEntreFechas(
                new ListadoAudienciasDto(4L, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 4, 30)));

        assertThat(records).containsExactly(
                new ListadoRecord(1L, "Concello", "Orquesta Ana", "Centro", "Betanzos", "Galicia", "A Coruña",
                        "Ana", "Pérez", "SIN_OCUPACION", LocalDateTime.of(2026, 4, 1, 10, 0),
                        LocalDate.of(2026, 6, 1), null, null, null, null, null, null,
                        LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 5)),
                new ListadoRecord(2L, null, "Luis Gómez", null, "", "", "",
                        "Luis", "Gómez", null, LocalDateTime.of(2026, 3, 1, 9, 0),
                        null, null, null, null, null, null, null, null, null));
    }

    @Test
    void obtenerListadosPorPeriodo_porMes_agrupaConNombreEspanolYOrdenaCronologicamente() {
        List<ListadoRecord> listados = List.of(
                record(LocalDateTime.of(2026, 3, 5, 10, 0)),
                record(LocalDateTime.of(2025, 12, 1, 8, 0)),
                record(LocalDateTime.of(2026, 1, 20, 12, 0)),
                record(LocalDateTime.of(2025, 12, 31, 23, 59)));

        List<ListadosPorMesDto> resultado = service.obtenerListadosPorPeriodo(listados, false);

        assertThat(resultado).extracting(ListadosPorMesDto::getMes, ListadosPorMesDto::getCantidad)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("Diciembre 2025", 2L),
                        org.assertj.core.groups.Tuple.tuple("Enero 2026", 1L),
                        org.assertj.core.groups.Tuple.tuple("Marzo 2026", 1L));
    }

    @Test
    void obtenerListadosPorPeriodo_porDia_agrupaPorFechaOrdenadaConFormatoDiaMesAno() {
        List<ListadoRecord> listados = List.of(
                record(LocalDateTime.of(2026, 3, 1, 23, 0)),
                record(LocalDateTime.of(2026, 2, 15, 8, 0)),
                record(LocalDateTime.of(2026, 3, 1, 0, 5)));

        List<ListadosPorMesDto> resultado = service.obtenerListadosPorPeriodo(listados, true);

        assertThat(resultado).extracting(ListadosPorMesDto::getMes, ListadosPorMesDto::getCantidad)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("15/02/2026", 1L),
                        org.assertj.core.groups.Tuple.tuple("01/03/2026", 2L));
    }

    @Test
    void obtenerListadosPorPeriodo_sinListados_devuelveListaVacia() {
        assertThat(service.obtenerListadosPorPeriodo(List.of(), false)).isEmpty();
        assertThat(service.obtenerListadosPorPeriodo(List.of(), true)).isEmpty();
    }

    @Test
    void findAllTiposOcupacion_devuelveConYSinOcupacion() {
        assertThat(service.findAllTiposOcupacion())
                .extracting(CodigoNombreDto::getId, CodigoNombreDto::getNombre)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(1L, "Con ocupación"),
                        org.assertj.core.groups.Tuple.tuple(2L, "Sin ocupación"));
    }

    private void prepararGeneracionBasica() {
        when(provinciaRepository.findById(15L)).thenReturn(Optional.of(provincia(15L, "A Coruña")));
        when(agenciaRepository.findAllById(Set.of(3L))).thenReturn(List.of());
        sinArtistasRestringidos();
        when(informeService.imprimirInforme(anyMap(), anyString(), anyString())).thenReturn(PDF);
    }

    private void sinArtistasRestringidos() {
        when(permisoRepository.findPermisoRecordByCodigo(PermisoArtistaEnum.PRESUPUESTOS_RESTRINGIDOS.name()))
                .thenReturn(new PermisoRecord(ID_PERMISO_RESTRINGIDOS, "PRESUPUESTOS_RESTRINGIDOS", "Restringidos"));
        when(accesoArtistaRepository.findAllAccesosArtistaByIdUsuarioIdPermiso(ID_USUARIO, ID_PERMISO_RESTRINGIDOS))
                .thenReturn(Optional.of(List.of()));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> capturarInforme(TipoReportEnum reportEsperado, String prefijoFichero) {
        ArgumentCaptor<Map<String, Object>> parametros = ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<String> fichero = ArgumentCaptor.forClass(String.class);
        verify(informeService).imprimirInforme(parametros.capture(), fichero.capture(), eq(reportEsperado.getNombreFicheroReport()));
        assertThat(fichero.getValue()).matches(java.util.regex.Pattern.quote(prefijoFichero) + "\\d{14}\\.pdf");
        return parametros.getValue();
    }

    private static Set<String> splitIds(Object valor) {
        return Arrays.stream(valor.toString().split(", ")).collect(Collectors.toSet());
    }

    private static ListadoDto listadoValido(TipoOcupacionEnum tipo) {
        ListadoDto dto = new ListadoDto();
        dto.setSolicitadoPara("Cliente Fiestas");
        dto.setIdCcaa(12L);
        dto.setIdProvincia(15L);
        dto.setComentario("Comentario");
        dto.setIdTipoOcupacion(tipo.getId());
        dto.setIdsAgencias(Set.of(3L));
        dto.setIdsTipoArtista(Set.of(2L, 1L));
        dto.setIdsComunidades(Set.of(11L));
        return dto;
    }

    private static Provincia provincia(Long id, String nombre) {
        Provincia provincia = new Provincia();
        provincia.setId(id);
        provincia.setNombre(nombre);
        return provincia;
    }

    private static AccesoArtista accesoArtista(Long idArtista) {
        Artista artista = new Artista();
        artista.setId(idArtista);
        AccesoArtista acceso = new AccesoArtista();
        acceso.setArtista(artista);
        return acceso;
    }

    private static ListadoRecord record(LocalDateTime fechaCreacion) {
        return new ListadoRecord(null, null, null, null, null, null, null, null, null, null, fechaCreacion,
                null, null, null, null, null, null, null, null, null);
    }
}
