package es.musicalia.gestmusica.reactivacion;

import es.musicalia.gestmusica.artista.ArtistaRepository;
import es.musicalia.gestmusica.agencia.AgenciaRepository;
import es.musicalia.gestmusica.mail.EmailDto;
import es.musicalia.gestmusica.mail.EmailService;
import es.musicalia.gestmusica.usuario.EnvioEmailException;
import es.musicalia.gestmusica.usuario.Usuario;
import es.musicalia.gestmusica.usuario.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReactivacionEmailService {

    private static final String TEMPLATE_TIBIO = "reactivacion-tibio";
    private static final String TEMPLATE_FRIO  = "reactivacion-frio";

    private static final long DELAY_ENTRE_EMAILS_MS = 500L;

    @Value("${reactivacion.inactivo.tibio.min:60}")
    private int diasInactivoTibioMin;

    @Value("${reactivacion.inactivo.tibio.max:89}")
    private int diasInactivoTibioMax;

    @Value("${reactivacion.inactivo.frio.max:365}")
    private int diasInactivoFrioMax;

    @Value("${reactivacion.cooldown.dias:90}")
    private int diasCooldown;

    @Value("${app.base-url:https://festia.es}")
    private String baseUrl;

    @Value("${app.mail.enabled:true}")
    private boolean mailEnabled;

    private final EmailReactivacionLogRepository logRepository;
    private final UsuarioRepository usuarioRepository;
    private final ArtistaRepository artistaRepository;
    private final AgenciaRepository agenciaRepository;
    private final SpringTemplateEngine templateEngine;
    private final EmailService emailService;
    private final ReactivacionTokenService reactivacionTokenService;

    /**
     * Punto de entrada del job. Calcula candidatos, segmenta y envía.
     * La lectura de candidatos se realiza en su propia transacción de solo lectura.
     * El loop de envío (sleep + llamada HTTP a Mailgun) ocurre fuera de transacción.
     * El registro de cada envío se persiste en su propia transacción independiente.
     *
     * @return número de emails enviados correctamente.
     */
    public int enviarEmailsReactivacion() {
        if (!mailEnabled) {
            log.info("Envío de correo deshabilitado. El job de reactivación no enviará emails.");
            return 0;
        }

        List<CandidatoReactivacion> candidatos = obtenerCandidatos();
        log.info("Reactivación: {} usuarios elegibles encontrados", candidatos.size());

        int enviados = 0;
        int fallidos = 0;

        for (CandidatoReactivacion candidato : candidatos) {
            SegmentoReactivacion segmento = candidato.segmento();
            try {
                enviarEmailParaUsuario(candidato);
                registrarEnvio(candidato.usuarioId(), segmento, "ENVIADO");
                enviados++;
                aplicarDelay();
            } catch (EnvioEmailException e) {
                fallidos++;
                log.error("Error enviando reactivación a usuario {}: {}", candidato.usuarioId(), e.getMessage());
                registrarEnvio(candidato.usuarioId(), segmento, "ERROR");
            } catch (Exception e) {
                fallidos++;
                log.error("Error inesperado procesando usuario {} para reactivación", candidato.usuarioId(), e);
            }
        }

        log.info("Reactivación completada. Enviados: {}, Fallidos: {}", enviados, fallidos);
        return enviados;
    }

    /**
     * Lee candidatos y prepara los datos necesarios para el envío.
     * Transacción de solo lectura, sin locks largos.
     */
    @Transactional(readOnly = true)
    public List<CandidatoReactivacion> obtenerCandidatos() {
        OffsetDateTime ahora         = OffsetDateTime.now();
        OffsetDateTime limite60      = ahora.minusDays(diasInactivoTibioMin);
        OffsetDateTime limite365     = ahora.minusDays(diasInactivoFrioMax);
        OffsetDateTime limiteCooldown = ahora.minusDays(diasCooldown);

        List<Usuario> usuarios = logRepository.findUsuariosElegibles(limite60, limite365, limiteCooldown);

        long totalArtistas       = artistaRepository.count();
        long totalAgencias       = agenciaRepository.count();
        long totalRepresentantes = usuarioRepository.countByRolGeneralCodigoIn(List.of("REPRE", "AGENTE"));

        return usuarios.stream()
                .map(u -> new CandidatoReactivacion(
                        u.getId(),
                        u.getNombre(),
                        u.getEmail(),
                        u.getEmailBajaToken(),
                        determinarSegmento(u, ahora),
                        calcularDiasInactivo(u, ahora),
                        totalArtistas,
                        totalAgencias,
                        totalRepresentantes
                ))
                .toList();
    }

    /**
     * Procesa la baja de un usuario por token. Devuelve true si el token era válido.
     * El token se anula tras el primer uso para evitar reutilización.
     */
    @Transactional
    public boolean procesarBajaPorToken(String token) {
        Optional<Usuario> optUsuario = usuarioRepository.findByEmailBajaToken(token);

        if (optUsuario.isEmpty()) {
            log.warn("Intento de baja con token inválido: {}", token);
            return false;
        }

        Usuario usuario = optUsuario.get();
        usuario.setEmailBaja(true);
        usuario.setEmailBajaFecha(OffsetDateTime.now());
        usuario.setEmailBajaToken(null);
        usuarioRepository.save(usuario);
        log.info("Baja de email procesada para usuario {}", usuario.getId());
        return true;
    }

    // -------------------------------------------------------------------------
    // Métodos privados
    // -------------------------------------------------------------------------

    private void enviarEmailParaUsuario(CandidatoReactivacion candidato) throws EnvioEmailException {
        String token   = resolverToken(candidato);
        String urlBaja = baseUrl + "/baja/email/" + token;

        ReactivacionContextDto ctx = new ReactivacionContextDto(
                candidato.nombre(),
                candidato.diasInactivo(),
                candidato.totalArtistas(),
                candidato.totalAgencias(),
                candidato.totalRepresentantes(),
                urlBaja
        );

        String template   = candidato.segmento() == SegmentoReactivacion.TIBIO ? TEMPLATE_TIBIO : TEMPLATE_FRIO;
        String asunto     = candidato.segmento() == SegmentoReactivacion.TIBIO
                ? construirAsuntoTibio(candidato.nombre(), candidato.totalArtistas())
                : construirAsuntoFrio(candidato.nombre());
        String plainText  = candidato.segmento() == SegmentoReactivacion.TIBIO
                ? "Tienes " + candidato.totalArtistas() + " artistas esperándote en festia.es"
                : "La temporada está a la vuelta de la esquina. Mantené tus presupuestos al día en festia.es";

        String html = renderizarTemplate(template, asunto, ctx);

        emailService.sendMailgunEmail(
                EmailDto.builder()
                        .to(candidato.email())
                        .subject(asunto)
                        .content(html)
                        .plainContent(plainText)
                        .isHtml(true)
                        .build()
        );

        log.debug("Email de reactivación ({}) enviado a usuario {}", candidato.segmento(), candidato.usuarioId());
    }

    /**
     * Genera o recupera el token de baja del usuario.
     * Si el candidato ya trae token no es necesario tocar la BD.
     * Si no tiene, se genera en su propia transacción.
     */
    private String resolverToken(CandidatoReactivacion candidato) {
        if (candidato.emailBajaToken() != null) {
            return candidato.emailBajaToken();
        }
        return reactivacionTokenService.generarYPersistirToken(candidato.usuarioId());
    }

    @Transactional
    public void registrarEnvio(Long usuarioId, SegmentoReactivacion segmento, String estado) {
        Usuario usuario = usuarioRepository.getReferenceById(usuarioId);
        String template = segmento == SegmentoReactivacion.TIBIO ? TEMPLATE_TIBIO : TEMPLATE_FRIO;
        EmailReactivacionLog entry = EmailReactivacionLog.builder()
                .usuario(usuario)
                .segmento(segmento)
                .fechaEnvio(OffsetDateTime.now())
                .estado(estado)
                .template(template)
                .build();
        logRepository.save(entry);
    }

    private String renderizarTemplate(String templateName, String asunto, ReactivacionContextDto ctx) {
        Context context = new Context();
        context.setVariable("asunto",              asunto);
        context.setVariable("nombre",              ctx.nombre());
        context.setVariable("diasInactivo",        ctx.diasInactivo());
        context.setVariable("totalArtistas",       ctx.totalArtistas());
        context.setVariable("totalAgencias",       ctx.totalAgencias());
        context.setVariable("totalRepresentantes", ctx.totalRepresentantes());
        context.setVariable("urlBaja",             ctx.urlBaja());
        return templateEngine.process(templateName, context);
    }

    private SegmentoReactivacion determinarSegmento(Usuario usuario, OffsetDateTime ahora) {
        long dias = calcularDiasInactivo(usuario, ahora);
        return dias <= diasInactivoTibioMax ? SegmentoReactivacion.TIBIO : SegmentoReactivacion.FRIO;
    }

    private long calcularDiasInactivo(Usuario usuario, OffsetDateTime ahora) {
        return java.time.temporal.ChronoUnit.DAYS.between(usuario.getFechaUltimoAcceso(), ahora);
    }

    private String construirAsuntoTibio(String nombre, long totalArtistas) {
        return String.format("%s, tienes %d artistas esperándote en festia.es", nombre, totalArtistas);
    }

    private String construirAsuntoFrio(String nombre) {
        return String.format("%s, la temporada está a la vuelta de la esquina y no tienes los presupuestos al día", nombre);
    }

    private void aplicarDelay() {
        try {
            Thread.sleep(DELAY_ENTRE_EMAILS_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Delay entre emails interrumpido");
        }
    }
}
