package es.musicalia.gestmusica.auth.model;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

/**
 * Loads application users. Password-less re-authentication (remember-me cookie, passkey) must resolve the user by
 * id: a login name can be ambiguous, because a username may look like another user's email.
 */
public interface UsuarioUserDetailsService extends UserDetailsService {

    /**
     * @param registrarAcceso whether this counts as a new access (last access date and login record)
     * @throws UsernameNotFoundException when the user does not exist or is not active
     */
    UserDetails loadUserByUsuarioId(Long usuarioId, boolean registrarAcceso) throws UsernameNotFoundException;
}
