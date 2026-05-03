package es.musicalia.gestmusica.ocupacion;

import es.musicalia.gestmusica.acceso.AccesoRepository;
import es.musicalia.gestmusica.agencia.Agencia;
import es.musicalia.gestmusica.artista.Artista;
import es.musicalia.gestmusica.artista.ArtistaRepository;
import es.musicalia.gestmusica.auth.model.CustomAuthenticatedUser;
import es.musicalia.gestmusica.excel.ExcelExportService;
import es.musicalia.gestmusica.informe.InformeService;
import es.musicalia.gestmusica.localizacion.Municipio;
import es.musicalia.gestmusica.localizacion.MunicipioRepository;
import es.musicalia.gestmusica.localizacion.Provincia;
import es.musicalia.gestmusica.localizacion.ProvinciaRepository;
import es.musicalia.gestmusica.mail.EmailService;
import es.musicalia.gestmusica.mail.EmailTemplateEnum;
import es.musicalia.gestmusica.mensaje.MensajeService;
import es.musicalia.gestmusica.orquestasdegalicia.ActuacionExterna;
import es.musicalia.gestmusica.orquestasdegalicia.OrquestasDeGaliciaService;
import es.musicalia.gestmusica.permiso.PermisoAgenciaEnum;
import es.musicalia.gestmusica.permiso.PermisoArtistaEnum;
import es.musicalia.gestmusica.permiso.PermisoService;
import es.musicalia.gestmusica.tarifa.Tarifa;
import es.musicalia.gestmusica.tarifa.TarifaRepository;
import es.musicalia.gestmusica.usuario.EnvioEmailException;
import es.musicalia.gestmusica.usuario.UserService;
import es.musicalia.gestmusica.usuario.Usuario;
import es.musicalia.gestmusica.util.DefaultResponseBody;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios completos para OcupacionServiceImpl.
 * Cubre todos los métodos públicos del servicio con casos de éxito y error.
 */
@ExtendWith(MockitoExtension.class)
class OcupacionServiceImplTest {

    @Mock
    private OcupacionRepository ocupacionRepository;

    @Mock
    private ArtistaRepository artistaRepository;

    @Mock
    private ProvinciaRepository provinciaRepository;

    @Mock
    private MunicipioRepository municipioRepository;

    @Mock
    private TipoOcupacionRepository tipoOcupacionRepository;

    @Mock
    private OcupacionEstadoRepository ocupacionEstadoRepository;

    @Mock
    private TarifaRepository tarifaRepository;

    @Mock
    private UserService userService;

    @Mock
    private PermisoService permisoService;

    @Mock
    private AccesoRepository accesoRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private OcupacionMapper ocupacionMapper;

    @Mock
    private MensajeService mensajeService;

    @Mock
    private OrquestasDeGaliciaService orquestasDeGaliciaService;

    @Mock
    private ExcelExportService excelExportService;

    @Mock
    private InformeService informeService;

    @InjectMocks
    private OcupacionServiceImpl ocupacionService;

    // =========================================================================
    // Fixtures y Helpers
    // =========================================================================

    private Artista crearArtista(Long id) {
        Artista artista = new Artista();
        artista.setId(id);
        artista.setNombre("Artista Test");
        return artista;
    }

    private Artista crearArtistaConAgencia(Long id, Long agenciaId) {
        Artista artista = crearArtista(id);
        Agencia agencia = new Agencia();
        agencia.setId(agenciaId);
        agencia.setNombre("Agencia Test");
        artista.setAgencia(agencia);
        return artista;
    }

    private Usuario crearUsuario(Long id, String username, String email) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setUsername(username);
        usuario.setEmail(email);
        usuario.setNombre("Nombre");
        usuario.setApellidos("Apellidos");
        usuario.setNombreComercial("Comercial Test");
        usuario.setTelefono("666123456");
        return usuario;
    }

    private Provincia crearProvincia(Long id, String nombre) {
        Provincia provincia = new Provincia();
        provincia.setId(id);
        provincia.setNombre(nombre);
        provincia.setNombreOrquestasdegalicia(nombre);
        return provincia;
    }

    private Municipio crearMunicipio(Long id, String nombre) {
        Municipio municipio = new Municipio();
        municipio.setId(id);
        municipio.setNombre(nombre);
        return municipio;
    }

    private TipoOcupacion crearTipoOcupacion(Long id, String nombre) {
        TipoOcupacion tipo = new TipoOcupacion();
        tipo.setId(id);
        tipo.setNombre(nombre);
        return tipo;
    }

    private OcupacionEstado crearOcupacionEstado(Long id, String nombre) {
        OcupacionEstado estado = new OcupacionEstado();
        estado.setId(id);
        estado.setNombre(nombre);
        return estado;
    }

    private Tarifa crearTarifa(Long id, BigDecimal importe) {
        Tarifa tarifa = new Tarifa();
        tarifa.setId(id);
        tarifa.setImporte(importe);
        tarifa.setActivo(true);
        tarifa.setFecha(LocalDateTime.now());
        tarifa.setFechaCreacion(LocalDateTime.now());
        tarifa.setUsuarioCreacion("test");
        return tarifa;
    }

    private Ocupacion crearOcupacion(Long id, Artista artista) {
        Ocupacion ocupacion = new Ocupacion();
        ocupacion.setId(id);
        ocupacion.setArtista(artista);
        ocupacion.setFecha(LocalDateTime.of(2025, 6, 15, 12, 0));
        ocupacion.setImporte(new BigDecimal("1000"));
        ocupacion.setPorcentajeRepre(new BigDecimal("10"));
        ocupacion.setIva(new BigDecimal("21"));
        ocupacion.setProvincia(crearProvincia(1L, "A Coruña"));
        ocupacion.setMunicipio(crearMunicipio(1L, "Santiago"));
        ocupacion.setPoblacion("Santiago de Compostela");
        ocupacion.setUsuario(crearUsuario(1L, "test", "test@test.com"));
        ocupacion.setTarifa(crearTarifa(1L, new BigDecimal("1000")));
        ocupacion.setActivo(true);
        ocupacion.setEventoVisible(true);
        ocupacion.setPublicadoOdg(false);
        return ocupacion;
    }

    private OcupacionSaveDto crearOcupacionSaveDto() {
        OcupacionSaveDto dto = new OcupacionSaveDto();
        dto.setIdArtista(1L);
        dto.setIdTipoOcupacion(TipoOcupacionEnum.OCUPADO.getId());
        dto.setIdCcaa(1L);
        dto.setIdProvincia(1L);
        dto.setIdMunicipio(1L);
        dto.setLocalidad("Santiago de Compostela");
        dto.setLugar("Plaza Mayor");
        dto.setImporte(new BigDecimal("1000"));
        dto.setPorcentajeRepre(new BigDecimal("10"));
        dto.setIva(new BigDecimal("21"));
        dto.setFecha(LocalDateTime.of(2025, 6, 15, 12, 0));
        dto.setMatinal(false);
        dto.setSoloMatinal(false);
        dto.setProvisional(false);
        dto.setObservaciones("Test");
        return dto;
    }

    // =========================================================================
    // Tests para listarTiposOcupacion
    // =========================================================================

    @Nested
    @DisplayName("listarTiposOcupacion - Listado de tipos de ocupación")
    class ListarTiposOcupacionTest {

        @Test
        @DisplayName("Sin permiso RESERVAR_OCUPACION: solo devuelve OCUPADO")
        void sinPermisoReservar_soloDevuelveOcupado() {
            // Given
            Long idArtista = 1L;
            when(permisoService.existePermisoUsuarioArtista(idArtista, PermisoArtistaEnum.RESERVAR_OCUPACION.name()))
                    .thenReturn(false);

            // When
            var result = ocupacionService.listarTiposOcupacion(idArtista);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isEqualTo(TipoOcupacionEnum.OCUPADO.getId());
            verify(permisoService).existePermisoUsuarioArtista(idArtista, PermisoArtistaEnum.RESERVAR_OCUPACION.name());
        }

        @Test
        @DisplayName("Con permiso RESERVAR_OCUPACION: devuelve OCUPADO y RESERVADO")
        void conPermisoReservar_devuelveOcupadoYReservado() {
            // Given
            Long idArtista = 1L;
            when(permisoService.existePermisoUsuarioArtista(idArtista, PermisoArtistaEnum.RESERVAR_OCUPACION.name()))
                    .thenReturn(true);

            // When
            var result = ocupacionService.listarTiposOcupacion(idArtista);

            // Then
            assertThat(result).hasSize(2);
            assertThat(result).extracting("id").containsExactlyInAnyOrder(
                    TipoOcupacionEnum.OCUPADO.getId(),
                    TipoOcupacionEnum.RESERVADO.getId()
            );
        }
    }

    // =========================================================================
    // Tests para anularOcupacion
    // =========================================================================

    @Nested
    @DisplayName("anularOcupacion - Anulación de ocupaciones")
    class AnularOcupacionTest {

        @Test
        @DisplayName("Anulación exitosa con envío de email exitoso → success")
        void anulacionExitosaConEmail_success() {
            // Given
            Long idOcupacion = 1L;
            Ocupacion ocupacion = crearOcupacion(idOcupacion, crearArtista(1L));
            ocupacion.setPublicadoOdg(false);

            when(ocupacionRepository.findById(idOcupacion)).thenReturn(Optional.of(ocupacion));
            when(ocupacionEstadoRepository.findById(OcupacionEstadoEnum.ANULADO.getId()))
                    .thenReturn(Optional.of(crearOcupacionEstado(OcupacionEstadoEnum.ANULADO.getId(), "Anulado")));
            when(ocupacionRepository.save(any(Ocupacion.class))).thenReturn(ocupacion);
            when(userService.obtenerUsuarioAutenticado()).thenReturn(Optional.of(crearUsuario(1L, "test", "test@test.com")));
            // emailService mock void por defecto no hace nada (éxito)

            // When
            DefaultResponseBody response = ocupacionService.anularOcupacion(idOcupacion);

            // Then
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getMessageType()).isEqualTo("success");
            assertThat(response.getMessage()).contains("anulada correctamente");
            verify(ocupacionRepository, times(1)).save(ocupacion);
        }

        @Test
        @DisplayName("Anulación exitosa con error de email → warning")
        void anulacionExitosaConErrorEmail_warning() throws EnvioEmailException {
            // Given
            Long idOcupacion = 1L;
            Ocupacion ocupacion = crearOcupacion(idOcupacion, crearArtista(1L));
            ocupacion.setPublicadoOdg(false);

            when(ocupacionRepository.findById(idOcupacion)).thenReturn(Optional.of(ocupacion));
            when(ocupacionEstadoRepository.findById(OcupacionEstadoEnum.ANULADO.getId()))
                    .thenReturn(Optional.of(crearOcupacionEstado(OcupacionEstadoEnum.ANULADO.getId(), "Anulado")));
            when(ocupacionRepository.save(any(Ocupacion.class))).thenReturn(ocupacion);
            when(userService.obtenerUsuarioAutenticado()).thenReturn(Optional.of(crearUsuario(1L, "test", "test@test.com")));
            doThrow(new EnvioEmailException("Error")).when(emailService).enviarMensajePorEmail(anyString(), any(EmailTemplateEnum.class));

            // When
            DefaultResponseBody response = ocupacionService.anularOcupacion(idOcupacion);

            // Then
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getMessageType()).isEqualTo("warning");
            assertThat(response.getMessage()).contains("error enviando");
        }

        @Test
        @DisplayName("Ocupación publicada en ODG → intenta eliminar de ODG y limpia flag")
        void ocupacionPublicadaODG_eliminaDeODG() {
            // Given
            Long idOcupacion = 1L;
            Ocupacion ocupacion = crearOcupacion(idOcupacion, crearArtista(1L));
            ocupacion.setPublicadoOdg(true);

            when(ocupacionRepository.findById(idOcupacion)).thenReturn(Optional.of(ocupacion));
            when(ocupacionEstadoRepository.findById(OcupacionEstadoEnum.ANULADO.getId()))
                    .thenReturn(Optional.of(crearOcupacionEstado(OcupacionEstadoEnum.ANULADO.getId(), "Anulado")));
            when(ocupacionRepository.save(any(Ocupacion.class))).thenReturn(ocupacion);
            when(userService.obtenerUsuarioAutenticado()).thenReturn(Optional.of(crearUsuario(1L, "test", "test@test.com")));
            when(orquestasDeGaliciaService.eliminarActuacion(idOcupacion.intValue()))
                    .thenReturn(DefaultResponseBody.builder().success(true).build());

            // When
            DefaultResponseBody response = ocupacionService.anularOcupacion(idOcupacion);

            // Then
            assertThat(response.isSuccess()).isTrue();
            assertThat(ocupacion.isPublicadoOdg()).isFalse();
            verify(orquestasDeGaliciaService).eliminarActuacion(idOcupacion.intValue());
            verify(ocupacionRepository, times(2)).save(ocupacion);
        }
    }

    // =========================================================================
    // Tests para confirmarOcupacion
    // =========================================================================

    @Nested
    @DisplayName("confirmarOcupacion - Confirmación de ocupaciones")
    class ConfirmarOcupacionTest {

        @Test
        @DisplayName("Confirmación exitosa → success")
        void confirmacionExitosa_success() {
            // Given
            Long idOcupacion = 1L;
            Ocupacion ocupacion = crearOcupacion(idOcupacion, crearArtista(1L));
            Usuario usuarioConfirmacion = crearUsuario(2L, "confirmador", "confirm@test.com");

            when(ocupacionRepository.findById(idOcupacion)).thenReturn(Optional.of(ocupacion));
            when(ocupacionEstadoRepository.findById(OcupacionEstadoEnum.OCUPADO.getId()))
                    .thenReturn(Optional.of(crearOcupacionEstado(OcupacionEstadoEnum.OCUPADO.getId(), "Ocupado")));
            when(tipoOcupacionRepository.findById(TipoOcupacionEnum.OCUPADO.getId()))
                    .thenReturn(Optional.of(crearTipoOcupacion(TipoOcupacionEnum.OCUPADO.getId(), "Ocupado")));
            when(userService.obtenerUsuarioAutenticado()).thenReturn(Optional.of(usuarioConfirmacion));
            when(ocupacionRepository.save(any(Ocupacion.class))).thenReturn(ocupacion);

            // When
            DefaultResponseBody response = ocupacionService.confirmarOcupacion(idOcupacion);

            // Then
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getMessageType()).isEqualTo("success");
            assertThat(ocupacion.getUsuarioConfirmacion()).isEqualTo(usuarioConfirmacion);
        }

        @Test
        @DisplayName("Confirmación con error de email → warning")
        void confirmacionConErrorEmail_warning() throws EnvioEmailException {
            // Given
            Long idOcupacion = 1L;
            Ocupacion ocupacion = crearOcupacion(idOcupacion, crearArtista(1L));

            when(ocupacionRepository.findById(idOcupacion)).thenReturn(Optional.of(ocupacion));
            when(ocupacionEstadoRepository.findById(OcupacionEstadoEnum.OCUPADO.getId()))
                    .thenReturn(Optional.of(crearOcupacionEstado(OcupacionEstadoEnum.OCUPADO.getId(), "Ocupado")));
            when(tipoOcupacionRepository.findById(TipoOcupacionEnum.OCUPADO.getId()))
                    .thenReturn(Optional.of(crearTipoOcupacion(TipoOcupacionEnum.OCUPADO.getId(), "Ocupado")));
            when(userService.obtenerUsuarioAutenticado()).thenReturn(Optional.of(crearUsuario(2L, "confirm", "confirm@test.com")));
            when(ocupacionRepository.save(any(Ocupacion.class))).thenReturn(ocupacion);
            when(userService.obtenerUsuarioAutenticado()).thenReturn(Optional.of(crearUsuario(1L, "test", "test@test.com")));
            doThrow(new EnvioEmailException("Error")).when(emailService).enviarMensajePorEmail(anyString(), any(EmailTemplateEnum.class));

            // When
            DefaultResponseBody response = ocupacionService.confirmarOcupacion(idOcupacion);

            // Then
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getMessageType()).isEqualTo("warning");
        }
    }

    // =========================================================================
    // Tests para saveOcupacion
    // =========================================================================

    @Nested
    @DisplayName("saveOcupacion - Guardar ocupación")
    class SaveOcupacionTest {

        @Test
        @DisplayName("Usuario sin permiso confirmar → guarda como PENDIENTE + notificación")
        void sinPermisoConfirmar_guardaPendienteYNotifica() throws ModificacionOcupacionException, EnvioEmailException {
            // Given
            OcupacionSaveDto dto = crearOcupacionSaveDto();
            dto.setId(null); // Nueva ocupación
            Artista artista = crearArtistaConAgencia(1L, 1L);
            Agencia agencia = artista.getAgencia();
            Usuario usuarioAgencia = crearUsuario(10L, "agencia", "agencia@test.com");
            agencia.setUsuario(usuarioAgencia);

            Ocupacion ocupacionGuardada = crearOcupacion(1L, artista);

            when(artistaRepository.findById(dto.getIdArtista())).thenReturn(Optional.of(artista));
            when(permisoService.existePermisoUsuarioAgencia(agencia.getId(), PermisoAgenciaEnum.CONFIRMAR_OCUPACION.name()))
                    .thenReturn(false);
            when(userService.obtenerUsuarioAutenticado()).thenReturn(Optional.of(crearUsuario(1L, "user", "user@test.com")));
            when(tipoOcupacionRepository.findById(dto.getIdTipoOcupacion()))
                    .thenReturn(Optional.of(crearTipoOcupacion(dto.getIdTipoOcupacion(), "Ocupado")));
            when(ocupacionEstadoRepository.findById(OcupacionEstadoEnum.PENDIENTE.getId()))
                    .thenReturn(Optional.of(crearOcupacionEstado(OcupacionEstadoEnum.PENDIENTE.getId(), "Pendiente")));
            when(provinciaRepository.findById(dto.getIdProvincia()))
                    .thenReturn(Optional.of(crearProvincia(dto.getIdProvincia(), "A Coruña")));
            when(municipioRepository.findById(dto.getIdMunicipio()))
                    .thenReturn(Optional.of(crearMunicipio(dto.getIdMunicipio(), "Santiago")));
            when(tarifaRepository.findTarifasByArtistaIdAndDates(anyLong(), any(), any())).thenReturn(Collections.emptyList());
            when(tarifaRepository.save(any(Tarifa.class))).thenAnswer(inv -> inv.getArgument(0));
            when(ocupacionRepository.save(any(Ocupacion.class))).thenReturn(ocupacionGuardada);

            // When
            DefaultResponseBody response = ocupacionService.saveOcupacion(dto);

            // Then
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getMessageType()).isEqualTo("success");
            verify(emailService).enviarMensajePorEmail(eq("agencia@test.com"), eq(EmailTemplateEnum.EMAIL_NOTIFICACION_CONFIRMACION_PENDIENTE));
        }

        @Test
        @DisplayName("Usuario con permiso confirmar → guarda directamente sin notificación")
        void conPermisoConfirmar_guardaDirectamente() throws ModificacionOcupacionException {
            // Given
            OcupacionSaveDto dto = crearOcupacionSaveDto();
            dto.setId(null);
            Artista artista = crearArtistaConAgencia(1L, 1L);
            Ocupacion ocupacionGuardada = crearOcupacion(1L, artista);

            when(artistaRepository.findById(dto.getIdArtista())).thenReturn(Optional.of(artista));
            when(permisoService.existePermisoUsuarioAgencia(artista.getAgencia().getId(), PermisoAgenciaEnum.CONFIRMAR_OCUPACION.name()))
                    .thenReturn(true);
            when(userService.obtenerUsuarioAutenticado()).thenReturn(Optional.of(crearUsuario(1L, "user", "user@test.com")));
            when(tipoOcupacionRepository.findById(dto.getIdTipoOcupacion()))
                    .thenReturn(Optional.of(crearTipoOcupacion(dto.getIdTipoOcupacion(), "Ocupado")));
            when(ocupacionEstadoRepository.findById(OcupacionEstadoEnum.OCUPADO.getId()))
                    .thenReturn(Optional.of(crearOcupacionEstado(OcupacionEstadoEnum.OCUPADO.getId(), "Ocupado")));
            when(provinciaRepository.findById(dto.getIdProvincia()))
                    .thenReturn(Optional.of(crearProvincia(dto.getIdProvincia(), "A Coruña")));
            when(municipioRepository.findById(dto.getIdMunicipio()))
                    .thenReturn(Optional.of(crearMunicipio(dto.getIdMunicipio(), "Santiago")));
            when(tarifaRepository.findTarifasByArtistaIdAndDates(anyLong(), any(), any())).thenReturn(Collections.emptyList());
            when(tarifaRepository.save(any(Tarifa.class))).thenAnswer(inv -> inv.getArgument(0));
            when(ocupacionRepository.save(any(Ocupacion.class))).thenReturn(ocupacionGuardada);

            // When
            DefaultResponseBody response = ocupacionService.saveOcupacion(dto);

            // Then
            assertThat(response.isSuccess()).isTrue();
            verifyNoInteractions(emailService);
        }

        @Test
        @DisplayName("Error de email → warning message")
        void errorEmail_warning() throws ModificacionOcupacionException, EnvioEmailException {
            // Given
            OcupacionSaveDto dto = crearOcupacionSaveDto();
            dto.setId(null);
            Artista artista = crearArtistaConAgencia(1L, 1L);
            Agencia agencia = artista.getAgencia();
            Usuario usuarioAgencia = crearUsuario(10L, "agencia", "agencia@test.com");
            agencia.setUsuario(usuarioAgencia);
            Ocupacion ocupacionGuardada = crearOcupacion(1L, artista);

            when(artistaRepository.findById(dto.getIdArtista())).thenReturn(Optional.of(artista));
            when(permisoService.existePermisoUsuarioAgencia(agencia.getId(), PermisoAgenciaEnum.CONFIRMAR_OCUPACION.name()))
                    .thenReturn(false);
            when(userService.obtenerUsuarioAutenticado()).thenReturn(Optional.of(crearUsuario(1L, "user", "user@test.com")));
            when(tipoOcupacionRepository.findById(dto.getIdTipoOcupacion()))
                    .thenReturn(Optional.of(crearTipoOcupacion(dto.getIdTipoOcupacion(), "Ocupado")));
            when(ocupacionEstadoRepository.findById(OcupacionEstadoEnum.PENDIENTE.getId()))
                    .thenReturn(Optional.of(crearOcupacionEstado(OcupacionEstadoEnum.PENDIENTE.getId(), "Pendiente")));
            when(provinciaRepository.findById(dto.getIdProvincia()))
                    .thenReturn(Optional.of(crearProvincia(dto.getIdProvincia(), "A Coruña")));
            when(municipioRepository.findById(dto.getIdMunicipio()))
                    .thenReturn(Optional.of(crearMunicipio(dto.getIdMunicipio(), "Santiago")));
            when(tarifaRepository.findTarifasByArtistaIdAndDates(anyLong(), any(), any())).thenReturn(Collections.emptyList());
            when(tarifaRepository.save(any(Tarifa.class))).thenAnswer(inv -> inv.getArgument(0));
            when(ocupacionRepository.save(any(Ocupacion.class))).thenReturn(ocupacionGuardada);
            doThrow(new EnvioEmailException("Error")).when(emailService).enviarMensajePorEmail(anyString(), any(EmailTemplateEnum.class));

            // When
            DefaultResponseBody response = ocupacionService.saveOcupacion(dto);

            // Then
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getMessageType()).isEqualTo("warning");
            assertThat(response.getMessage()).contains("no se ha podido enviar");
        }
    }

    // =========================================================================
    // Tests para guardarOcupacion (método más complejo)
    // =========================================================================

    @Nested
    @DisplayName("guardarOcupacion - Método complejo de guardado")
    class GuardarOcupacionTest {

        @Test
        @DisplayName("Nueva ocupación normal")
        void nuevaOcupacionNormal() throws ModificacionOcupacionException {
            // Given
            OcupacionSaveDto dto = crearOcupacionSaveDto();
            dto.setId(null);
            Artista artista = crearArtistaConAgencia(1L, 1L);

            when(artistaRepository.findById(dto.getIdArtista())).thenReturn(Optional.of(artista));
            when(userService.obtenerUsuarioAutenticado()).thenReturn(Optional.of(crearUsuario(1L, "user", "user@test.com")));
            when(tipoOcupacionRepository.findById(dto.getIdTipoOcupacion()))
                    .thenReturn(Optional.of(crearTipoOcupacion(dto.getIdTipoOcupacion(), "Ocupado")));
            when(ocupacionEstadoRepository.findById(anyLong()))
                    .thenReturn(Optional.of(crearOcupacionEstado(OcupacionEstadoEnum.OCUPADO.getId(), "Ocupado")));
            when(provinciaRepository.findById(dto.getIdProvincia()))
                    .thenReturn(Optional.of(crearProvincia(dto.getIdProvincia(), "A Coruña")));
            when(municipioRepository.findById(dto.getIdMunicipio()))
                    .thenReturn(Optional.of(crearMunicipio(dto.getIdMunicipio(), "Santiago")));
            when(tarifaRepository.findTarifasByArtistaIdAndDates(anyLong(), any(), any())).thenReturn(Collections.emptyList());
            when(tarifaRepository.save(any(Tarifa.class))).thenAnswer(inv -> inv.getArgument(0));
            when(ocupacionRepository.save(any(Ocupacion.class))).thenAnswer(inv -> {
                Ocupacion o = inv.getArgument(0);
                o.setId(1L);
                return o;
            });

            // When
            Ocupacion result = ocupacionService.guardarOcupacion(dto, true, false);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getArtista()).isEqualTo(artista);
            assertThat(result.getUsuarioCreacion()).isEqualTo("user");
        }

        @Test
        @DisplayName("Modificación de ocupación propia")
        void modificacionOcupacionPropia() throws ModificacionOcupacionException {
            // Given
            Long idOcupacion = 1L;
            OcupacionSaveDto dto = crearOcupacionSaveDto();
            dto.setId(idOcupacion);

            Artista artista = crearArtistaConAgencia(1L, 1L);
            Usuario usuarioActual = crearUsuario(1L, "user", "user@test.com");

            Ocupacion ocupacionExistente = crearOcupacion(idOcupacion, artista);
            ocupacionExistente.setUsuario(usuarioActual);

            when(ocupacionRepository.findById(idOcupacion)).thenReturn(Optional.of(ocupacionExistente));
            when(artistaRepository.findById(dto.getIdArtista())).thenReturn(Optional.of(artista));
            when(userService.obtenerUsuarioAutenticado()).thenReturn(Optional.of(usuarioActual));
            when(tipoOcupacionRepository.findById(dto.getIdTipoOcupacion()))
                    .thenReturn(Optional.of(crearTipoOcupacion(dto.getIdTipoOcupacion(), "Ocupado")));
            when(ocupacionEstadoRepository.findById(anyLong()))
                    .thenReturn(Optional.of(crearOcupacionEstado(OcupacionEstadoEnum.OCUPADO.getId(), "Ocupado")));
            when(provinciaRepository.findById(dto.getIdProvincia()))
                    .thenReturn(Optional.of(crearProvincia(dto.getIdProvincia(), "A Coruña")));
            when(municipioRepository.findById(dto.getIdMunicipio()))
                    .thenReturn(Optional.of(crearMunicipio(dto.getIdMunicipio(), "Santiago")));
            when(tarifaRepository.findTarifasByArtistaIdAndDates(anyLong(), any(), any())).thenReturn(Collections.emptyList());
            when(tarifaRepository.save(any(Tarifa.class))).thenAnswer(inv -> inv.getArgument(0));
            when(ocupacionRepository.save(any(Ocupacion.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            Ocupacion result = ocupacionService.guardarOcupacion(dto, true, false);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getUsuarioModificacion()).isEqualTo("user");
        }

        @Test
        @DisplayName("Modificación de ocupación ajena SIN permiso → ModificacionOcupacionException")
        void modificacionOcupacionAjenaSinPermiso_lanzaExcepcion() {
            // Given
            Long idOcupacion = 1L;
            OcupacionSaveDto dto = crearOcupacionSaveDto();
            dto.setId(idOcupacion);

            Artista artista = crearArtistaConAgencia(1L, 1L);
            Usuario usuarioOriginal = crearUsuario(1L, "original", "original@test.com");
            Usuario usuarioActual = crearUsuario(2L, "otro", "otro@test.com");

            Ocupacion ocupacionExistente = crearOcupacion(idOcupacion, artista);
            ocupacionExistente.setUsuario(usuarioOriginal);

            when(ocupacionRepository.findById(idOcupacion)).thenReturn(Optional.of(ocupacionExistente));
            lenient().when(artistaRepository.findById(dto.getIdArtista())).thenReturn(Optional.of(artista));
            when(userService.obtenerUsuarioAutenticado()).thenReturn(Optional.of(usuarioActual));
            when(permisoService.existePermisoUsuarioAgencia(artista.getAgencia().getId(), PermisoAgenciaEnum.MODIFICAR_OCUPACION_OTROS.name()))
                    .thenReturn(false);

            // When/Then
            assertThatThrownBy(() -> ocupacionService.guardarOcupacion(dto, true, false))
                    .isInstanceOf(ModificacionOcupacionException.class)
                    .hasMessageContaining("No tiene permisos");
        }

        @Test
        @DisplayName("Sincronización creación → usuario USUARIO_SINCRONIZACION")
        void sincronizacionCreacion_usuarioSincronizacion() throws ModificacionOcupacionException {
            // Given
            OcupacionSaveDto dto = crearOcupacionSaveDto();
            dto.setId(null);
            Artista artista = crearArtistaConAgencia(1L, 1L);

            when(artistaRepository.findById(dto.getIdArtista())).thenReturn(Optional.of(artista));
            when(userService.findUsuarioById(49L)).thenReturn(crearUsuario(49L, "sync", "sync@test.com"));
            when(tipoOcupacionRepository.findById(dto.getIdTipoOcupacion()))
                    .thenReturn(Optional.of(crearTipoOcupacion(dto.getIdTipoOcupacion(), "Ocupado")));
            when(ocupacionEstadoRepository.findById(anyLong()))
                    .thenReturn(Optional.of(crearOcupacionEstado(OcupacionEstadoEnum.OCUPADO.getId(), "Ocupado")));
            when(provinciaRepository.findById(dto.getIdProvincia()))
                    .thenReturn(Optional.of(crearProvincia(dto.getIdProvincia(), "A Coruña")));
            when(municipioRepository.findById(dto.getIdMunicipio()))
                    .thenReturn(Optional.of(crearMunicipio(dto.getIdMunicipio(), "Santiago")));
            when(tarifaRepository.findTarifasByArtistaIdAndDates(anyLong(), any(), any())).thenReturn(Collections.emptyList());
            when(tarifaRepository.save(any(Tarifa.class))).thenAnswer(inv -> inv.getArgument(0));
            when(ocupacionRepository.save(any(Ocupacion.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            Ocupacion result = ocupacionService.guardarOcupacion(dto, true, true);

            // Then
            assertThat(result.getUsuarioCreacion()).isEqualTo("usuario_sincronizacion");
        }

        @Test
        @DisplayName("Municipio null → usa municipio provisional")
        void municipioNull_usaMunicipioProvisional() throws ModificacionOcupacionException {
            // Given
            OcupacionSaveDto dto = crearOcupacionSaveDto();
            dto.setId(null);
            dto.setIdMunicipio(null); // Municipio null
            Artista artista = crearArtistaConAgencia(1L, 1L);

            Municipio municipioProvisional = crearMunicipio(8117L, "PROVISIONAL");

            when(artistaRepository.findById(dto.getIdArtista())).thenReturn(Optional.of(artista));
            when(userService.obtenerUsuarioAutenticado()).thenReturn(Optional.of(crearUsuario(1L, "user", "user@test.com")));
            when(tipoOcupacionRepository.findById(dto.getIdTipoOcupacion()))
                    .thenReturn(Optional.of(crearTipoOcupacion(dto.getIdTipoOcupacion(), "Ocupado")));
            when(ocupacionEstadoRepository.findById(anyLong()))
                    .thenReturn(Optional.of(crearOcupacionEstado(OcupacionEstadoEnum.OCUPADO.getId(), "Ocupado")));
            when(provinciaRepository.findById(dto.getIdProvincia()))
                    .thenReturn(Optional.of(crearProvincia(dto.getIdProvincia(), "A Coruña")));
            when(municipioRepository.findById(8117L)).thenReturn(Optional.of(municipioProvisional));
            when(tarifaRepository.findTarifasByArtistaIdAndDates(anyLong(), any(), any())).thenReturn(Collections.emptyList());
            when(tarifaRepository.save(any(Tarifa.class))).thenAnswer(inv -> inv.getArgument(0));
            when(ocupacionRepository.save(any(Ocupacion.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            Ocupacion result = ocupacionService.guardarOcupacion(dto, true, false);

            // Then
            assertThat(result.getMunicipio()).isEqualTo(municipioProvisional);
        }
    }

    // =========================================================================
    // Tests para existeOcupacionFecha
    // =========================================================================

    @Nested
    @DisplayName("existeOcupacionFecha - Verificar existencia de ocupación")
    class ExisteOcupacionFechaTest {

        @Test
        @DisplayName("No existe ocupación → false")
        void noExisteOcupacion_devuelveFalse() {
            // Given
            OcupacionSaveDto dto = crearOcupacionSaveDto();
            dto.setId(null);

            when(ocupacionRepository.findOcupacionesDtoByArtistaIdAndDates(anyLong(), any(), any()))
                    .thenReturn(Collections.emptyList());

            // When
            boolean result = ocupacionService.existeOcupacionFecha(dto);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Existe ocupación con misma configuración matinal → true")
        void existeOcupacionMismaConfiguracionMatinal_devuelveTrue() {
            // Given
            OcupacionSaveDto dto = crearOcupacionSaveDto();
            dto.setId(null);
            dto.setMatinal(true);
            dto.setSoloMatinal(false);

            OcupacionRecord record = new OcupacionRecord(1L, dto.getFecha(), dto.getIdArtista(), "Artista Test",
                    "1000", true, "Ocupado", "A Coruña", "Santiago", "Santiago de Compostela",
                    true, false, "Ocupado", 1L, "Usuario Test");

            when(ocupacionRepository.findOcupacionesDtoByArtistaIdAndDates(anyLong(), any(), any()))
                    .thenReturn(List.of(record));

            // When
            boolean result = ocupacionService.existeOcupacionFecha(dto);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Existe ocupación con diferente configuración matinal → false")
        void existeOcupacionDiferenteConfiguracionMatinal_devuelveFalse() {
            // Given
            OcupacionSaveDto dto = crearOcupacionSaveDto();
            dto.setId(null);
            dto.setMatinal(true);
            dto.setSoloMatinal(false);

            // Ocupación existente con configuración diferente
            OcupacionRecord record = new OcupacionRecord(1L, dto.getFecha(), dto.getIdArtista(), "Artista Test",
                    "1000", true, "Ocupado", "A Coruña", "Santiago", "Santiago de Compostela",
                    false, true, "Ocupado", 1L, "Usuario Test");

            when(ocupacionRepository.findOcupacionesDtoByArtistaIdAndDates(anyLong(), any(), any()))
                    .thenReturn(List.of(record));

            // When
            boolean result = ocupacionService.existeOcupacionFecha(dto);

            // Then
            assertThat(result).isFalse();
        }
    }

    // =========================================================================
    // Tests para findOcupacionesDtoByAgenciaPendientes
    // =========================================================================

    @Nested
    @DisplayName("findOcupacionesDtoByAgenciaPendientes - Ocupaciones pendientes por agencia")
    class FindOcupacionesDtoByAgenciaPendientesTest {

        @Test
        @DisplayName("Hay pendientes → devuelve lista")
        void hayPendientes_devuelveLista() {
            // Given
            Set<Long> idsAgencia = Set.of(1L, 2L);
            OcupacionRecord record = new OcupacionRecord(1L, LocalDateTime.now(), 1L, "Artista Test",
                    "1000", true, "Ocupado", "A Coruña", "Santiago", "Santiago de Compostela",
                    true, false, "Ocupado", 1L, "Usuario Test");

            when(ocupacionRepository.findOcupacionesDtoByAgenciaPendientes(idsAgencia))
                    .thenReturn(Optional.of(List.of(record)));

            // When
            List<OcupacionRecord> result = ocupacionService.findOcupacionesDtoByAgenciaPendientes(idsAgencia);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).id()).isEqualTo(1L);
        }

        @Test
        @DisplayName("No hay pendientes → devuelve lista vacía")
        void noHayPendientes_devuelveListaVacia() {
            // Given
            Set<Long> idsAgencia = Set.of(1L, 2L);

            when(ocupacionRepository.findOcupacionesDtoByAgenciaPendientes(idsAgencia))
                    .thenReturn(Optional.empty());

            // When
            List<OcupacionRecord> result = ocupacionService.findOcupacionesDtoByAgenciaPendientes(idsAgencia);

            // Then
            assertThat(result).isEmpty();
        }
    }

    // =========================================================================
    // Tests para getOcupacionSaveDto
    // =========================================================================

    @Nested
    @DisplayName("getOcupacionSaveDto - Obtener DTO de ocupación")
    class GetOcupacionSaveDtoTest {

        @Test
        @DisplayName("Existe ocupación → devuelve DTO mapeado")
        void existeOcupacion_devuelveDto() {
            // Given
            Long idOcupacion = 1L;
            Ocupacion ocupacion = crearOcupacion(idOcupacion, crearArtista(1L));
            OcupacionSaveDto dtoEsperado = crearOcupacionSaveDto();
            dtoEsperado.setId(idOcupacion);

            when(ocupacionRepository.findById(idOcupacion)).thenReturn(Optional.of(ocupacion));
            when(ocupacionMapper.toDto(ocupacion)).thenReturn(dtoEsperado);

            // When
            OcupacionSaveDto result = ocupacionService.getOcupacionSaveDto(idOcupacion);

            // Then
            assertThat(result).isEqualTo(dtoEsperado);
            verify(ocupacionMapper).toDto(ocupacion);
        }
    }

    // =========================================================================
    // Tests para buscarPorIdOcupacionLegacy
    // =========================================================================

    @Nested
    @DisplayName("buscarPorIdOcupacionLegacy - Búsqueda por ID legacy")
    class BuscarPorIdOcupacionLegacyTest {

        @Test
        @DisplayName("Existe → Optional con ocupación")
        void existe_devuelveOptionalConOcupacion() {
            // Given
            Integer idLegacy = 12345;
            Ocupacion ocupacion = crearOcupacion(1L, crearArtista(1L));

            when(ocupacionRepository.findOcupacionByIdOcupacionLegacy(idLegacy))
                    .thenReturn(Optional.of(ocupacion));

            // When
            Optional<Ocupacion> result = ocupacionService.buscarPorIdOcupacionLegacy(idLegacy);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("No existe → Optional vacío")
        void noExiste_devuelveOptionalVacio() {
            // Given
            Integer idLegacy = 12345;

            when(ocupacionRepository.findOcupacionByIdOcupacionLegacy(idLegacy))
                    .thenReturn(Optional.empty());

            // When
            Optional<Ocupacion> result = ocupacionService.buscarPorIdOcupacionLegacy(idLegacy);

            // Then
            assertThat(result).isEmpty();
        }
    }

    // =========================================================================
    // Tests para publicarOcupacionOrquestasDeGalicia
    // =========================================================================

    @Nested
    @DisplayName("publicarOcupacionOrquestasDeGalicia - Publicar en ODG")
    class PublicarOcupacionOrquestasDeGaliciaTest {

        @Test
        @DisplayName("No está en estado OCUPADO → error")
        void noEstaOcupado_devuelveError() {
            // Given
            Long idOcupacion = 1L;
            Ocupacion ocupacion = crearOcupacion(idOcupacion, crearArtista(1L));
            ocupacion.setTipoOcupacion(crearTipoOcupacion(TipoOcupacionEnum.RESERVADO.getId(), "Reservado"));

            when(ocupacionRepository.findById(idOcupacion)).thenReturn(Optional.of(ocupacion));

            // When
            DefaultResponseBody response = ocupacionService.publicarOcupacionOrquestasDeGalicia(idOcupacion);

            // Then
            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getMessageType()).isEqualTo("danger");
            assertThat(response.getMessage()).contains("Solo se pueden publicar en estado ocupada");
        }

        @Test
        @DisplayName("Provincia provisional → error")
        void provinciaProvisional_devuelveError() {
            // Given
            Long idOcupacion = 1L;
            Ocupacion ocupacion = crearOcupacion(idOcupacion, crearArtista(1L));
            ocupacion.setTipoOcupacion(crearTipoOcupacion(TipoOcupacionEnum.OCUPADO.getId(), "Ocupado"));
            Provincia provinciaProvisional = crearProvincia(53L, "PROVISIONAL");
            ocupacion.setProvincia(provinciaProvisional);

            when(ocupacionRepository.findById(idOcupacion)).thenReturn(Optional.of(ocupacion));

            // When
            DefaultResponseBody response = ocupacionService.publicarOcupacionOrquestasDeGalicia(idOcupacion);

            // Then
            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getMessage()).contains("Es necesario especificar la provincia");
        }

        @Test
        @DisplayName("Municipio provisional → error")
        void municipioProvisional_devuelveError() {
            // Given
            Long idOcupacion = 1L;
            Ocupacion ocupacion = crearOcupacion(idOcupacion, crearArtista(1L));
            ocupacion.setTipoOcupacion(crearTipoOcupacion(TipoOcupacionEnum.OCUPADO.getId(), "Ocupado"));
            Municipio municipioProvisional = crearMunicipio(8117L, "PROVISIONAL");
            ocupacion.setMunicipio(municipioProvisional);

            when(ocupacionRepository.findById(idOcupacion)).thenReturn(Optional.of(ocupacion));

            // When
            DefaultResponseBody response = ocupacionService.publicarOcupacionOrquestasDeGalicia(idOcupacion);

            // Then
            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getMessage()).contains("Es necesario especificar el municipio");
        }

        @Test
        @DisplayName("Publicación exitosa → success + flag actualizado")
        void publicacionExitosa_success() {
            // Given
            Long idOcupacion = 1L;
            Ocupacion ocupacion = crearOcupacion(idOcupacion, crearArtista(1L));
            ocupacion.setTipoOcupacion(crearTipoOcupacion(TipoOcupacionEnum.OCUPADO.getId(), "Ocupado"));
            ocupacion.setPublicadoOdg(false);

            ActuacionExterna actuacion = new ActuacionExterna();
            actuacion.setIdActuacionExterno(idOcupacion.intValue());

            when(ocupacionRepository.findById(idOcupacion)).thenReturn(Optional.of(ocupacion));
            when(orquestasDeGaliciaService.crearActuacion(any(ActuacionExterna.class)))
                    .thenReturn(DefaultResponseBody.builder().success(true).build());
            when(ocupacionRepository.save(any(Ocupacion.class))).thenReturn(ocupacion);

            // When
            DefaultResponseBody response = ocupacionService.publicarOcupacionOrquestasDeGalicia(idOcupacion);

            // Then
            assertThat(response.isSuccess()).isTrue();
            assertThat(ocupacion.isPublicadoOdg()).isTrue();
            verify(ocupacionRepository).save(ocupacion);
        }
    }

    // =========================================================================
    // Tests para actualizarOcupacionOrquestasDeGalicia
    // =========================================================================

    @Nested
    @DisplayName("actualizarOcupacionOrquestasDeGalicia - Actualizar en ODG")
    class ActualizarOcupacionOrquestasDeGaliciaTest {

        @Test
        @DisplayName("No publicado → error")
        void noPublicado_devuelveError() throws OrquestasDeGaliciaException {
            // Given
            Long idOcupacion = 1L;
            Ocupacion ocupacion = crearOcupacion(idOcupacion, crearArtista(1L));
            ocupacion.setTipoOcupacion(crearTipoOcupacion(TipoOcupacionEnum.OCUPADO.getId(), "Ocupado"));
            ocupacion.setPublicadoOdg(false);

            when(ocupacionRepository.findById(idOcupacion)).thenReturn(Optional.of(ocupacion));

            // When
            DefaultResponseBody response = ocupacionService.actualizarOcupacionOrquestasDeGalicia(idOcupacion);

            // Then
            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getMessage()).contains("no ha sido publicada");
        }

        @Test
        @DisplayName("No encontrado en ODG → desactiva flag")
        void noEncontradoEnODG_desactivaFlag() throws OrquestasDeGaliciaException {
            // Given
            Long idOcupacion = 1L;
            Ocupacion ocupacion = crearOcupacion(idOcupacion, crearArtista(1L));
            ocupacion.setTipoOcupacion(crearTipoOcupacion(TipoOcupacionEnum.OCUPADO.getId(), "Ocupado"));
            ocupacion.setPublicadoOdg(true);

            when(ocupacionRepository.findById(idOcupacion)).thenReturn(Optional.of(ocupacion));
            when(orquestasDeGaliciaService.obtenerActuacion(idOcupacion.intValue()))
                    .thenReturn(Optional.empty());
            when(ocupacionRepository.save(any(Ocupacion.class))).thenReturn(ocupacion);

            // When
            DefaultResponseBody response = ocupacionService.actualizarOcupacionOrquestasDeGalicia(idOcupacion);

            // Then
            assertThat(response.isSuccess()).isFalse();
            assertThat(ocupacion.isPublicadoOdg()).isFalse();
        }

        @Test
        @DisplayName("Actualización exitosa → success")
        void actualizacionExitosa_success() throws OrquestasDeGaliciaException {
            // Given
            Long idOcupacion = 1L;
            Ocupacion ocupacion = crearOcupacion(idOcupacion, crearArtista(1L));
            ocupacion.setTipoOcupacion(crearTipoOcupacion(TipoOcupacionEnum.OCUPADO.getId(), "Ocupado"));
            ocupacion.setPublicadoOdg(true);

            ActuacionExterna actuacion = new ActuacionExterna();
            actuacion.setIdActuacionExterno(idOcupacion.intValue());

            when(ocupacionRepository.findById(idOcupacion)).thenReturn(Optional.of(ocupacion));
            when(orquestasDeGaliciaService.obtenerActuacion(idOcupacion.intValue()))
                    .thenReturn(Optional.of(actuacion));
            when(orquestasDeGaliciaService.modificarActuacion(anyInt(), any(ActuacionExterna.class)))
                    .thenReturn(DefaultResponseBody.builder().success(true).build());
            when(ocupacionRepository.save(any(Ocupacion.class))).thenReturn(ocupacion);

            // When
            DefaultResponseBody response = ocupacionService.actualizarOcupacionOrquestasDeGalicia(idOcupacion);

            // Then
            assertThat(response.isSuccess()).isTrue();
            verify(ocupacionRepository, times(1)).save(ocupacion);
        }
    }

    // =========================================================================
    // Tests para eliminarOcupacionOrquestasDeGalicia
    // =========================================================================

    @Nested
    @DisplayName("eliminarOcupacionOrquestasDeGalicia - Eliminar de ODG")
    class EliminarOcupacionOrquestasDeGaliciaTest {

        @Test
        @DisplayName("No publicado → error")
        void noPublicado_devuelveError() {
            // Given
            Long idOcupacion = 1L;
            Ocupacion ocupacion = crearOcupacion(idOcupacion, crearArtista(1L));
            ocupacion.setPublicadoOdg(false);

            when(ocupacionRepository.findById(idOcupacion)).thenReturn(Optional.of(ocupacion));

            // When
            DefaultResponseBody response = ocupacionService.eliminarOcupacionOrquestasDeGalicia(idOcupacion);

            // Then
            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getMessage()).contains("no ha sido publicada");
        }

        @Test
        @DisplayName("Eliminación exitosa → success + flag limpiado")
        void eliminacionExitosa_success() {
            // Given
            Long idOcupacion = 1L;
            Ocupacion ocupacion = crearOcupacion(idOcupacion, crearArtista(1L));
            ocupacion.setPublicadoOdg(true);

            when(ocupacionRepository.findById(idOcupacion)).thenReturn(Optional.of(ocupacion));
            when(orquestasDeGaliciaService.eliminarActuacion(idOcupacion.intValue()))
                    .thenReturn(DefaultResponseBody.builder().success(true).build());
            when(ocupacionRepository.save(any(Ocupacion.class))).thenReturn(ocupacion);

            // When
            DefaultResponseBody response = ocupacionService.eliminarOcupacionOrquestasDeGalicia(idOcupacion);

            // Then
            assertThat(response.isSuccess()).isTrue();
            assertThat(ocupacion.isPublicadoOdg()).isFalse();
            assertThat(ocupacion.isExcluirSincronizacionOdg()).isTrue();
        }
    }

    // =========================================================================
    // Tests para toggleVisibilidadEvento
    // =========================================================================

    @Nested
    @DisplayName("toggleVisibilidadEvento - Cambiar visibilidad")
    class ToggleVisibilidadEventoTest {

        @Test
        @DisplayName("Toggle a visible → success")
        void toggleAVisible_success() {
            // Given
            Long idOcupacion = 1L;
            Ocupacion ocupacion = crearOcupacion(idOcupacion, crearArtista(1L));
            ocupacion.setEventoVisible(false);

            when(ocupacionRepository.findById(idOcupacion)).thenReturn(Optional.of(ocupacion));
            when(ocupacionRepository.save(any(Ocupacion.class))).thenReturn(ocupacion);

            // When
            DefaultResponseBody response = ocupacionService.toggleVisibilidadEvento(idOcupacion);

            // Then
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getMessageType()).isEqualTo("success");
            assertThat(ocupacion.isEventoVisible()).isTrue();
        }

        @Test
        @DisplayName("Toggle a oculto → warning")
        void toggleAOculto_warning() {
            // Given
            Long idOcupacion = 1L;
            Ocupacion ocupacion = crearOcupacion(idOcupacion, crearArtista(1L));
            ocupacion.setEventoVisible(true);

            when(ocupacionRepository.findById(idOcupacion)).thenReturn(Optional.of(ocupacion));
            when(ocupacionRepository.save(any(Ocupacion.class))).thenReturn(ocupacion);

            // When
            DefaultResponseBody response = ocupacionService.toggleVisibilidadEvento(idOcupacion);

            // Then
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getMessageType()).isEqualTo("warning");
            assertThat(ocupacion.isEventoVisible()).isFalse();
        }
    }

    // =========================================================================
    // Tests para exportOcupacionesToExcel
    // =========================================================================

    @Nested
    @DisplayName("exportOcupacionesToExcel - Exportar a Excel")
    class ExportOcupacionesToExcelTest {

        @Test
        @DisplayName("Exportación exitosa → llama a ExcelExportService")
        void exportacionExitosa_llamaServicio() {
            // Given - Configurar SecurityContext
            CustomAuthenticatedUser user = mock(CustomAuthenticatedUser.class);
            when(user.getUserId()).thenReturn(1L);
            Map<Long, Set<String>> permisos = new HashMap<>();
            permisos.put(1L, Set.of(PermisoArtistaEnum.OCUPACIONES.name()));
            when(user.getMapPermisosArtista()).thenReturn(permisos);

            Authentication authentication = mock(Authentication.class);
            when(authentication.getPrincipal()).thenReturn(user);
            SecurityContext securityContext = mock(SecurityContext.class);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            SecurityContextHolder.setContext(securityContext);

            OcupacionListFilterDto filterDto = new OcupacionListFilterDto();
            filterDto.setFechaDesde(LocalDate.now());
            filterDto.setFechaHasta(LocalDate.now().plusDays(30));

            Usuario usuario = crearUsuario(1L, "user", "user@test.com");

            // Mocks para findOcupacionesByArtistasListAndDatesActivoPaginado
            lenient().when(accesoRepository.findAccesoByIdUsuarioAndIdAgenciaAndCodigoRol(anyLong(), any(), any()))
                    .thenReturn(Optional.empty());
            when(ocupacionRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(org.springframework.data.domain.Pageable.class)))
                    .thenReturn(org.springframework.data.domain.Page.empty());
            lenient().when(userService.findUsuarioById(anyLong())).thenReturn(usuario);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            when(excelExportService.exportToExcel(anyList(), eq(OcupacionExcelDto.class), anyString()))
                    .thenReturn(baos);

            // When
            ByteArrayOutputStream result = ocupacionService.exportOcupacionesToExcel(user, filterDto);

            // Then
            assertThat(result).isNotNull();
            verify(excelExportService).exportToExcel(anyList(), eq(OcupacionExcelDto.class), eq("Ocupaciones"));

            // Cleanup
            SecurityContextHolder.clearContext();
        }
    }

    // =========================================================================
    // Tests para exportOcupacionesToPDF
    // =========================================================================

    @Nested
    @DisplayName("exportOcupacionesToPDF - Exportar a PDF")
    class ExportOcupacionesToPDFTest {

        @Test
        @DisplayName("Exportación exitosa → llama a InformeService")
        void exportacionExitosa_llamaServicio() {
            // Given - Configurar SecurityContext
            CustomAuthenticatedUser user = mock(CustomAuthenticatedUser.class);
            when(user.getUserId()).thenReturn(1L);
            Map<Long, Set<String>> permisos = new HashMap<>();
            permisos.put(1L, Set.of(PermisoArtistaEnum.OCUPACIONES.name()));
            when(user.getMapPermisosArtista()).thenReturn(permisos);

            Authentication authentication = mock(Authentication.class);
            when(authentication.getPrincipal()).thenReturn(user);
            SecurityContext securityContext = mock(SecurityContext.class);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            SecurityContextHolder.setContext(securityContext);

            OcupacionListFilterDto filterDto = new OcupacionListFilterDto();
            filterDto.setFechaDesde(LocalDate.now());

            Usuario usuario = crearUsuario(1L, "user", "user@test.com");

            // Mocks para findOcupacionesByArtistasListAndDatesActivoPaginado
            lenient().when(accesoRepository.findAccesoByIdUsuarioAndIdAgenciaAndCodigoRol(anyLong(), any(), any()))
                    .thenReturn(Optional.empty());
            when(ocupacionRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(org.springframework.data.domain.Pageable.class)))
                    .thenReturn(org.springframework.data.domain.Page.empty());
            lenient().when(userService.findUsuarioById(anyLong())).thenReturn(usuario);

            byte[] pdfBytes = new byte[]{0x25, 0x50, 0x44, 0x46}; // PDF header
            when(informeService.imprimirInformeConDataSource(anyMap(), anyString(), anyString(), any()))
                    .thenReturn(pdfBytes);

            // When
            byte[] result = ocupacionService.exportOcupacionesToPDF(user, filterDto);

            // Then
            assertThat(result).isNotNull();
            verify(informeService).imprimirInformeConDataSource(anyMap(), anyString(), eq("listado_ocupaciones.jrxml"), any());

            // Cleanup
            SecurityContextHolder.clearContext();
        }
    }
}
