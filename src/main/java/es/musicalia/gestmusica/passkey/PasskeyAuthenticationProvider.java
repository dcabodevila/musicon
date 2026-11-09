package es.musicalia.gestmusica.passkey;

import org.springframework.security.authentication.AccountStatusUserDetailsChecker;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsChecker;
import org.springframework.security.web.webauthn.api.Bytes;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialUserEntity;
import org.springframework.security.web.webauthn.authentication.WebAuthnAuthenticationRequestToken;
import org.springframework.security.web.webauthn.management.WebAuthnRelyingPartyOperations;

import java.util.function.Function;

/**
 * Verifies a passkey assertion and authenticates the application user it belongs to.
 * <p>
 * Spring Security's own provider returns a {@code WebAuthnAuthentication} whose principal is the WebAuthn user
 * entity; the application expects a {@code CustomAuthenticatedUser} principal (permissions, user id), exactly as
 * after a password login. The user is resolved by the owner of the verified credential (its user handle), never by
 * login name.
 */
public class PasskeyAuthenticationProvider implements AuthenticationProvider {

    private final WebAuthnRelyingPartyOperations relyingPartyOperations;
    private final Function<Bytes, UserDetails> ownerLoader;
    private final UserDetailsChecker userDetailsChecker = new AccountStatusUserDetailsChecker();

    public PasskeyAuthenticationProvider(WebAuthnRelyingPartyOperations relyingPartyOperations,
                                         Function<Bytes, UserDetails> ownerLoader) {
        this.relyingPartyOperations = relyingPartyOperations;
        this.ownerLoader = ownerLoader;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        WebAuthnAuthenticationRequestToken request = (WebAuthnAuthenticationRequestToken) authentication;
        PublicKeyCredentialUserEntity userEntity;
        try {
            userEntity = relyingPartyOperations.authenticate(request.getWebAuthnRequest());
        } catch (RuntimeException ex) {
            throw new BadCredentialsException(ex.getMessage(), ex);
        }
        UserDetails userDetails = ownerLoader.apply(userEntity.getId());
        userDetailsChecker.check(userDetails);
        UsernamePasswordAuthenticationToken result =
                UsernamePasswordAuthenticationToken.authenticated(userDetails, null, userDetails.getAuthorities());
        result.setDetails(request.getDetails());
        return result;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return WebAuthnAuthenticationRequestToken.class.isAssignableFrom(authentication);
    }
}
