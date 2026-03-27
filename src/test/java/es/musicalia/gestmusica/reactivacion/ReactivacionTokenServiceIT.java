package es.musicalia.gestmusica.reactivacion;

import com.cloudinary.Cloudinary;
import es.musicalia.gestmusica.localizacion.Provincia;
import es.musicalia.gestmusica.localizacion.ProvinciaRepository;
import es.musicalia.gestmusica.rol.Rol;
import es.musicalia.gestmusica.rol.RolRepository;
import es.musicalia.gestmusica.usuario.Usuario;
import es.musicalia.gestmusica.usuario.UsuarioRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "mailgun.api-key=test-key",
        "orquestas.api.username=test-user",
        "orquestas.api.password=test-pass"
})
@ActiveProfiles("dev")
class ReactivacionTokenServiceIT {

    @MockBean
    private Cloudinary cloudinary;

    @Autowired
    private ReactivacionTokenService reactivacionTokenService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private ProvinciaRepository provinciaRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void generarYPersistirToken_debe_persistir_token_en_bd() {
        Usuario usuario = crearUsuarioPrueba();

        try {
            String tokenGenerado = reactivacionTokenService.generarYPersistirToken(usuario.getId());

            assertThat(tokenGenerado).isNotBlank();

            entityManager.clear();
            Usuario recargado = usuarioRepository.findById(usuario.getId()).orElseThrow();
            assertThat(recargado.getEmailBajaToken()).isEqualTo(tokenGenerado);
        } finally {
            usuarioRepository.deleteById(usuario.getId());
        }
    }

    @Test
    void generarYPersistirToken_si_ya_existe_no_lo_sobrescribe() {
        Usuario usuario = crearUsuarioPrueba();
        String tokenOriginal = "token-fijo-prueba";
        usuario.setEmailBajaToken(tokenOriginal);
        usuarioRepository.save(usuario);

        try {
            String tokenDevuelto = reactivacionTokenService.generarYPersistirToken(usuario.getId());

            entityManager.clear();
            Usuario recargado = usuarioRepository.findById(usuario.getId()).orElseThrow();

            assertThat(tokenDevuelto).isEqualTo(tokenOriginal);
            assertThat(recargado.getEmailBajaToken()).isEqualTo(tokenOriginal);
        } finally {
            usuarioRepository.deleteById(usuario.getId());
        }
    }

    private Usuario crearUsuarioPrueba() {
        Rol rol = rolRepository.findRolByCodigo("REPRE");
        if (rol == null) {
            rol = rolRepository.findAll().stream().findFirst().orElseThrow();
        }

        Provincia provincia = provinciaRepository.findAll().stream().findFirst().orElseThrow();

        Usuario usuario = new Usuario();
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        usuario.setNombre("Test");
        usuario.setPassword("password");
        usuario.setUsername("react-token-" + suffix);
        usuario.setEmail("react-token-" + suffix + "@test.local");
        usuario.setActivo(true);
        usuario.setEmailVerified(true);
        usuario.setEmailBaja(false);
        usuario.setFechaUltimoAcceso(OffsetDateTime.now().minusDays(70));
        usuario.setRolGeneral(rol);
        usuario.setProvincia(provincia);
        return usuarioRepository.save(usuario);
    }
}
