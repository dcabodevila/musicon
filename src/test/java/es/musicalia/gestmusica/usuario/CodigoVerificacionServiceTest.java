package es.musicalia.gestmusica.usuario;

import es.musicalia.gestmusica.mail.EmailService;
import es.musicalia.gestmusica.mail.EmailTemplateEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Characterization tests of the e-mail verification codes used by registration
 * ({@link EmailTemplateEnum#REGISTRO}): generation, validation, expiry, resend throttling
 * and the e-mail-verified flag of the user.
 */
@ExtendWith(MockitoExtension.class)
class CodigoVerificacionServiceTest {

    private static final String EMAIL = "nuevo@test.com";
    private static final int MINUTOS_EXPIRACION = 15;

    @Mock
    private CodigoVerificacionRepository codigoRepository;
    @Mock
    private EmailService emailService;
    @Mock
    private UsuarioRepository usuarioRepository;

    private CodigoVerificacionService service;

    @BeforeEach
    void setUp() {
        service = new CodigoVerificacionService(codigoRepository, emailService, usuarioRepository);
        // @Value("${app.verificacion.expiracion-minutos:15}") is not resolved outside Spring
        ReflectionTestUtils.setField(service, "minutosExpiracion", MINUTOS_EXPIRACION);
    }

    private static CodigoVerificacion codigo(String valor, LocalDateTime creacion, LocalDateTime expiracion) {
        return CodigoVerificacion.builder()
                .email(EMAIL)
                .codigo(valor)
                .tipo(EmailTemplateEnum.REGISTRO)
                .fechaCreacion(creacion)
                .fechaExpiracion(expiracion)
                .build();
    }

    // ==================== generarYEnviarCodigo ====================

    @Test
    void generarYEnviarCodigo_savesActiveFourDigitCodeAndEmailsTheSameCode() throws Exception {
        LocalDateTime antes = LocalDateTime.now();

        service.generarYEnviarCodigo(EMAIL, EmailTemplateEnum.REGISTRO);

        ArgumentCaptor<CodigoVerificacion> captor = ArgumentCaptor.forClass(CodigoVerificacion.class);
        verify(codigoRepository).save(captor.capture());
        CodigoVerificacion guardado = captor.getValue();
        assertThat(guardado.getEmail()).isEqualTo(EMAIL);
        assertThat(guardado.getCodigo()).matches("\\d{4}");
        assertThat(guardado.getTipo()).isEqualTo(EmailTemplateEnum.REGISTRO);
        assertThat(guardado.getUsado()).isFalse();
        assertThat(guardado.getActivo()).isTrue();
        assertThat(guardado.getFechaCreacion()).isBetween(antes, LocalDateTime.now());
        assertThat(Duration.between(guardado.getFechaCreacion(), guardado.getFechaExpiracion()))
                .isCloseTo(Duration.ofMinutes(MINUTOS_EXPIRACION), Duration.ofSeconds(1));
        verify(emailService).enviarCodigoPorEmail(EMAIL, guardado.getCodigo(), EmailTemplateEnum.REGISTRO);
    }

    @Test
    void generarYEnviarCodigo_emailFailure_propagatesAfterSavingCode() throws Exception {
        doThrow(new EnvioEmailException("mailgun down"))
                .when(emailService).enviarCodigoPorEmail(eq(EMAIL), anyString(), eq(EmailTemplateEnum.REGISTRO));

        assertThrows(EnvioEmailException.class, () -> service.generarYEnviarCodigo(EMAIL, EmailTemplateEnum.REGISTRO));

        verify(codigoRepository).save(any(CodigoVerificacion.class));
    }

    // ==================== verificarCodigo ====================

    @Test
    void verificarCodigo_unknownCode_returnsFalseWithoutChanges() {
        when(codigoRepository.findByEmailAndCodigoAndTipoAndActivoTrueAndUsado(EMAIL, "0000", EmailTemplateEnum.REGISTRO, false))
                .thenReturn(Optional.empty());

        assertThat(service.verificarCodigo(EMAIL, "0000", EmailTemplateEnum.REGISTRO, false)).isFalse();

        verify(codigoRepository, never()).save(any());
        verifyNoInteractions(usuarioRepository);
    }

    @Test
    void verificarCodigo_expiredCode_returnsFalseAndLeavesCodeUnused() {
        LocalDateTime now = LocalDateTime.now();
        CodigoVerificacion expirado = codigo("1234", now.minusMinutes(20), now.minusMinutes(5));
        when(codigoRepository.findByEmailAndCodigoAndTipoAndActivoTrueAndUsado(EMAIL, "1234", EmailTemplateEnum.REGISTRO, false))
                .thenReturn(Optional.of(expirado));

        assertThat(service.verificarCodigo(EMAIL, "1234", EmailTemplateEnum.REGISTRO, false)).isFalse();

        assertThat(expirado.getUsado()).isFalse();
        verify(codigoRepository, never()).save(any());
        verifyNoInteractions(usuarioRepository);
    }

    @Test
    void verificarCodigo_validCode_marksCodeUsedAndUserEmailVerified() {
        LocalDateTime now = LocalDateTime.now();
        CodigoVerificacion valido = codigo("1234", now.minusMinutes(1), now.plusMinutes(14));
        when(codigoRepository.findByEmailAndCodigoAndTipoAndActivoTrueAndUsado(EMAIL, "1234", EmailTemplateEnum.REGISTRO, false))
                .thenReturn(Optional.of(valido));
        Usuario usuario = new Usuario();
        usuario.setEmail(EMAIL);
        usuario.setEmailVerified(false);
        when(usuarioRepository.findUsuarioByMail(EMAIL)).thenReturn(Optional.of(usuario));

        assertThat(service.verificarCodigo(EMAIL, "1234", EmailTemplateEnum.REGISTRO, false)).isTrue();

        assertThat(valido.getUsado()).isTrue();
        verify(codigoRepository).save(valido);
        assertThat(usuario.isEmailVerified()).isTrue();
        verify(usuarioRepository).save(usuario);
    }

    @Test
    void verificarCodigo_validCodeWithoutUser_stillReturnsTrue() {
        LocalDateTime now = LocalDateTime.now();
        CodigoVerificacion valido = codigo("1234", now.minusMinutes(1), now.plusMinutes(14));
        when(codigoRepository.findByEmailAndCodigoAndTipoAndActivoTrueAndUsado(EMAIL, "1234", EmailTemplateEnum.REGISTRO, false))
                .thenReturn(Optional.of(valido));
        when(usuarioRepository.findUsuarioByMail(EMAIL)).thenReturn(Optional.empty());

        assertThat(service.verificarCodigo(EMAIL, "1234", EmailTemplateEnum.REGISTRO, false)).isTrue();

        verify(codigoRepository).save(valido);
        verify(usuarioRepository, never()).save(any());
    }

    // ==================== failed attempts (S1) ====================

    @Test
    void generarYEnviarCodigo_invalidatesPreviousCodesBeforeSavingTheNewOne() throws Exception {
        service.generarYEnviarCodigo(EMAIL, EmailTemplateEnum.REGISTRO);

        var inOrder = inOrder(codigoRepository);
        inOrder.verify(codigoRepository).desactivarCodigosPrevios(EMAIL, EmailTemplateEnum.REGISTRO);
        inOrder.verify(codigoRepository).save(any(CodigoVerificacion.class));
    }

    @Test
    void verificarCodigo_wrongCode_countsAFailedAttemptOnTheCurrentCode() {
        LocalDateTime now = LocalDateTime.now();
        CodigoVerificacion vigente = codigo("1234", now.minusMinutes(1), now.plusMinutes(14));
        when(codigoRepository.findByEmailAndCodigoAndTipoAndActivoTrueAndUsado(EMAIL, "0000", EmailTemplateEnum.REGISTRO, false))
                .thenReturn(Optional.empty());
        when(codigoRepository.findCodigosVigentesParaIntentoFallido(eq(EMAIL), eq(EmailTemplateEnum.REGISTRO), any(LocalDateTime.class)))
                .thenReturn(java.util.List.of(vigente));

        assertThat(service.verificarCodigo(EMAIL, "0000", EmailTemplateEnum.REGISTRO, false)).isFalse();

        assertThat(vigente.getIntentosFallidos()).isEqualTo(1);
        assertThat(vigente.getActivo()).isTrue();
        assertThat(vigente.getUsado()).isFalse();
        verify(codigoRepository).save(vigente);
        verifyNoInteractions(usuarioRepository);
    }

    @Test
    void verificarCodigo_fifthWrongCode_invalidatesTheCurrentCode() {
        LocalDateTime now = LocalDateTime.now();
        CodigoVerificacion vigente = codigo("1234", now.minusMinutes(1), now.plusMinutes(14));
        vigente.setIntentosFallidos(4);
        when(codigoRepository.findByEmailAndCodigoAndTipoAndActivoTrueAndUsado(EMAIL, "0000", EmailTemplateEnum.REGISTRO, false))
                .thenReturn(Optional.empty());
        when(codigoRepository.findCodigosVigentesParaIntentoFallido(eq(EMAIL), eq(EmailTemplateEnum.REGISTRO), any(LocalDateTime.class)))
                .thenReturn(java.util.List.of(vigente));

        assertThat(service.verificarCodigo(EMAIL, "0000", EmailTemplateEnum.REGISTRO, false)).isFalse();

        assertThat(vigente.getIntentosFallidos()).isEqualTo(5);
        assertThat(vigente.getActivo()).isFalse();
        assertThat(vigente.isValido()).isFalse();
        verify(codigoRepository).save(vigente);
    }

    @Test
    void verificarCodigo_rightCodeAfterFiveFailedAttempts_isRejectedAndNotUsed() {
        LocalDateTime now = LocalDateTime.now();
        CodigoVerificacion agotado = codigo("1234", now.minusMinutes(1), now.plusMinutes(14));
        agotado.setIntentosFallidos(5);
        when(codigoRepository.findByEmailAndCodigoAndTipoAndActivoTrueAndUsado(EMAIL, "1234", EmailTemplateEnum.REGISTRO, false))
                .thenReturn(Optional.of(agotado));

        assertThat(service.verificarCodigo(EMAIL, "1234", EmailTemplateEnum.REGISTRO, false)).isFalse();

        assertThat(agotado.getUsado()).isFalse();
        assertThat(agotado.isValido()).isFalse();
        verify(codigoRepository, never()).save(any());
        verifyNoInteractions(usuarioRepository);
    }

    // ==================== reenviarCodigo ====================

    @Test
    void reenviarCodigo_validCodeCreatedLessThanTwoMinutesAgo_isThrottled() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        CodigoVerificacion reciente = codigo("1234", now.minusSeconds(90), now.plusMinutes(13));
        when(codigoRepository.findCodigoValidoByEmailAndTipo(eq(EMAIL), eq(EmailTemplateEnum.REGISTRO), any(LocalDateTime.class)))
                .thenReturn(Optional.of(reciente));

        assertThat(service.reenviarCodigo(EMAIL, EmailTemplateEnum.REGISTRO)).isFalse();

        verifyNoInteractions(emailService);
        verify(codigoRepository, never()).save(any());
    }

    @Test
    void reenviarCodigo_validCodeOlderThanTwoMinutes_resendsSameCodeWithoutCreatingOne() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        CodigoVerificacion anterior = codigo("4321", now.minusMinutes(3), now.plusMinutes(12));
        when(codigoRepository.findCodigoValidoByEmailAndTipo(eq(EMAIL), eq(EmailTemplateEnum.REGISTRO), any(LocalDateTime.class)))
                .thenReturn(Optional.of(anterior));

        assertThat(service.reenviarCodigo(EMAIL, EmailTemplateEnum.REGISTRO)).isTrue();

        verify(emailService).enviarCodigoPorEmail(EMAIL, "4321", EmailTemplateEnum.REGISTRO);
        verify(codigoRepository, never()).save(any());
    }

    @Test
    void reenviarCodigo_noValidCode_generatesAndSendsNewCode() throws Exception {
        LocalDateTime antes = LocalDateTime.now();
        when(codigoRepository.findCodigoValidoByEmailAndTipo(eq(EMAIL), eq(EmailTemplateEnum.REGISTRO), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());

        assertThat(service.reenviarCodigo(EMAIL, EmailTemplateEnum.REGISTRO)).isTrue();

        ArgumentCaptor<LocalDateTime> consultadoEn = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(codigoRepository).findCodigoValidoByEmailAndTipo(eq(EMAIL), eq(EmailTemplateEnum.REGISTRO), consultadoEn.capture());
        assertThat(consultadoEn.getValue()).isCloseTo(antes, within(Duration.ofSeconds(5)));
        ArgumentCaptor<CodigoVerificacion> captor = ArgumentCaptor.forClass(CodigoVerificacion.class);
        verify(codigoRepository).save(captor.capture());
        verify(emailService).enviarCodigoPorEmail(EMAIL, captor.getValue().getCodigo(), EmailTemplateEnum.REGISTRO);
    }

    // ==================== CodigoVerificacion validity ====================

    @Test
    void codigoVerificacion_isValidOnlyWhenActiveUnusedAndNotExpired() {
        LocalDateTime now = LocalDateTime.now();
        CodigoVerificacion vigente = codigo("1234", now.minusMinutes(1), now.plusMinutes(1));
        CodigoVerificacion caducado = codigo("1234", now.minusMinutes(20), now.minusSeconds(1));
        CodigoVerificacion usado = codigo("1234", now.minusMinutes(1), now.plusMinutes(1));
        usado.setUsado(true);
        CodigoVerificacion inactivo = codigo("1234", now.minusMinutes(1), now.plusMinutes(1));
        inactivo.setActivo(false);

        assertThat(vigente.isExpirado()).isFalse();
        assertThat(vigente.isValido()).isTrue();
        assertThat(caducado.isExpirado()).isTrue();
        assertThat(caducado.isValido()).isFalse();
        assertThat(usado.isValido()).isFalse();
        assertThat(inactivo.isValido()).isFalse();
    }
}
