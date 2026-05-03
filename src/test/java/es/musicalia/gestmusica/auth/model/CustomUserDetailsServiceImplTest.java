package es.musicalia.gestmusica.auth.model;

import es.musicalia.gestmusica.accesoartista.AccesoArtistaService;
import es.musicalia.gestmusica.permiso.Permiso;
import es.musicalia.gestmusica.permiso.PermisoService;
import es.musicalia.gestmusica.registrologin.RegistroLoginService;
import es.musicalia.gestmusica.rol.Rol;
import es.musicalia.gestmusica.usuario.Usuario;
import es.musicalia.gestmusica.usuario.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceImplTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PermisoService permisoService;
    @Mock
    private AccesoArtistaService accesoArtistaService;
    @Mock
    private RegistroLoginService registroLoginService;

    @InjectMocks
    private CustomUserDetailsServiceImpl customUserDetailsService;

    private Usuario crearUsuarioConRolYPermisos(Long id, String username, String email) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setUsername(username);
        usuario.setNombre("Nombre");
        usuario.setApellidos("Apellidos");
        usuario.setEmail(email);
        usuario.setPassword("password");
        usuario.setActivo(true);

        Permiso permiso1 = new Permiso();
        permiso1.setId(1L);
        permiso1.setCodigo("PERMISO_1");
        permiso1.setDescripcion("Permiso 1");

        Permiso permiso2 = new Permiso();
        permiso2.setId(2L);
        permiso2.setCodigo("PERMISO_2");
        permiso2.setDescripcion("Permiso 2");

        Set<Permiso> permisos = new HashSet<>();
        permisos.add(permiso1);
        permisos.add(permiso2);

        Rol rol = new Rol();
        rol.setId(1L);
        rol.setNombre("ROL_TEST");
        rol.setCodigo("ROL_TEST");
        rol.setPermisos(permisos);

        usuario.setRolGeneral(rol);

        return usuario;
    }

    // ==================== loadUserByUsername con email ====================

    @Test
    void loadUserByUsername_emailValido_retornaCustomAuthenticatedUserConPermisos() {
        Usuario usuario = crearUsuarioConRolYPermisos(1L, "testuser", "test@test.com");

        when(usuarioRepository.findUsuarioActivoByMail("test@test.com")).thenReturn(Optional.of(usuario));
        when(accesoArtistaService.obtenerMapPermisosArtista(1L)).thenReturn(Collections.emptyMap());
        when(permisoService.obtenerMapPermisosAgencia(1L)).thenReturn(Collections.emptyMap());
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuario);
        when(registroLoginService.registrarLogin(1L)).thenReturn(null);

        UserDetails result = customUserDetailsService.loadUserByUsername("test@test.com");

        assertNotNull(result);
        assertInstanceOf(CustomAuthenticatedUser.class, result);
        CustomAuthenticatedUser customUser = (CustomAuthenticatedUser) result;
        assertEquals(1L, customUser.getUserId());
        assertEquals(2, customUser.getAuthorities().size());
        assertTrue(customUser.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("PERMISO_1")));
        assertTrue(customUser.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("PERMISO_2")));
    }

    // ==================== loadUserByUsername con username ====================

    @Test
    void loadUserByUsername_usernameValido_retornaCustomAuthenticatedUser() {
        Usuario usuario = crearUsuarioConRolYPermisos(1L, "testuser", "test@test.com");

        when(usuarioRepository.findByUsername("testuser")).thenReturn(Optional.of(usuario));
        when(accesoArtistaService.obtenerMapPermisosArtista(1L)).thenReturn(Collections.emptyMap());
        when(permisoService.obtenerMapPermisosAgencia(1L)).thenReturn(Collections.emptyMap());
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuario);
        when(registroLoginService.registrarLogin(1L)).thenReturn(null);

        UserDetails result = customUserDetailsService.loadUserByUsername("testuser");

        assertNotNull(result);
        assertInstanceOf(CustomAuthenticatedUser.class, result);
        assertEquals(1L, ((CustomAuthenticatedUser) result).getUserId());
    }

    // ==================== loadUserByUsername email no existe ====================

    @Test
    void loadUserByUsername_emailNoExiste_lanzaUsernameNotFoundException() {
        when(usuarioRepository.findUsuarioActivoByMail("inexistente@test.com")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () ->
            customUserDetailsService.loadUserByUsername("inexistente@test.com")
        );
    }

    // ==================== loadUserByUsername username no existe ====================

    @Test
    void loadUserByUsername_usernameNoExiste_lanzaUsernameNotFoundException() {
        when(usuarioRepository.findByUsername("inexistente")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () ->
            customUserDetailsService.loadUserByUsername("inexistente")
        );
    }

    // ==================== Verifica actualización fecha último acceso ====================

    @Test
    void loadUserByUsername_actualizaFechaUltimoAcceso() {
        Usuario usuario = crearUsuarioConRolYPermisos(1L, "testuser", "test@test.com");

        when(usuarioRepository.findUsuarioActivoByMail("test@test.com")).thenReturn(Optional.of(usuario));
        when(accesoArtistaService.obtenerMapPermisosArtista(1L)).thenReturn(Collections.emptyMap());
        when(permisoService.obtenerMapPermisosAgencia(1L)).thenReturn(Collections.emptyMap());
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuario);
        when(registroLoginService.registrarLogin(1L)).thenReturn(null);

        customUserDetailsService.loadUserByUsername("test@test.com");

        verify(usuarioRepository).save(argThat(u -> u.getFechaUltimoAcceso() != null));
    }

    // ==================== Verifica registro de login ====================

    @Test
    void loadUserByUsername_llamaARegistrarLogin() {
        Usuario usuario = crearUsuarioConRolYPermisos(1L, "testuser", "test@test.com");

        when(usuarioRepository.findUsuarioActivoByMail("test@test.com")).thenReturn(Optional.of(usuario));
        when(accesoArtistaService.obtenerMapPermisosArtista(1L)).thenReturn(Collections.emptyMap());
        when(permisoService.obtenerMapPermisosAgencia(1L)).thenReturn(Collections.emptyMap());
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuario);
        when(registroLoginService.registrarLogin(1L)).thenReturn(null);

        customUserDetailsService.loadUserByUsername("test@test.com");

        verify(registroLoginService).registrarLogin(1L);
    }
}
