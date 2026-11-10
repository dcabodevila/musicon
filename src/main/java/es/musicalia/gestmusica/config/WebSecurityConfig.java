package es.musicalia.gestmusica.config;

import es.musicalia.gestmusica.observabilidad.FunctionalEventTracker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.authentication.rememberme.TokenBasedRememberMeServices;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialRpEntity;
import org.springframework.security.web.webauthn.authentication.WebAuthnAuthenticationFilter;
import org.springframework.security.web.webauthn.management.PublicKeyCredentialUserEntityRepository;
import org.springframework.security.web.webauthn.management.UserCredentialRepository;
import org.springframework.security.web.webauthn.management.MapPublicKeyCredentialUserEntityRepository;
import org.springframework.security.web.webauthn.management.MapUserCredentialRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.web.webauthn.management.WebAuthnRelyingPartyOperations;
import org.springframework.security.web.webauthn.management.Webauthn4JRelyingPartyOperations;
import es.musicalia.gestmusica.passkey.PasskeyAuthenticationProvider;
import es.musicalia.gestmusica.passkey.PasskeyOwnerLoader;
import es.musicalia.gestmusica.auth.model.UsuarioUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import jakarta.servlet.DispatcherType;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Set;


@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class WebSecurityConfig {
	static final String REMEMBER_ME_COOKIE = "remember-me";
	static final Duration REMEMBER_ME_VALIDITY = Duration.ofDays(30);
	static final String WEBAUTHN_RP_NAME = "Festia";

	@Bean
	public BCryptPasswordEncoder bCryptPasswordEncoder() {
		return new BCryptPasswordEncoder();
	}
	@Autowired
	private UserDetailsService userDetailsService;
    @Autowired
    private RateLimitingFilter rateLimitingFilter;
    @Autowired
    private FunctionalEventTracker functionalEventTracker;
    @Value("${app.security.remember-me.key:}")
    private String rememberMeKey;
    @Value("${server.servlet.session.cookie.secure:false}")
    private boolean secureCookies;
    @Value("${app.security.webauthn.rp-id}")
    private String webauthnRpId;
    @Value("${app.security.webauthn.allowed-origins}")
    private String[] webauthnAllowedOrigins;

	@Bean
	public AuthenticationProvider authProvider(){
		DaoAuthenticationProvider daoAuthenticationProvider = new DaoAuthenticationProvider(this.userDetailsService);
		daoAuthenticationProvider.setPasswordEncoder(bCryptPasswordEncoder());
		return daoAuthenticationProvider;
	}
	@Bean
	public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
		return http.getSharedObject(AuthenticationManagerBuilder.class)
				.build();
	}

    /**
     * Keeps the CSRF token in a cookie instead of the HTTP session, so it lives as long as the remember-me login:
     * a form loaded before the session was lost (timeout, restart, deploy) can still be submitted once remember-me
     * re-authenticates the user. The cookie stays HttpOnly because pages read the token from the {@code _csrf} meta tag.
     */
    private CsrfTokenRepository csrfTokenRepository() {
        CookieCsrfTokenRepository repository = new CookieCsrfTokenRepository();
        repository.setCookieCustomizer(cookie -> cookie
            .maxAge(REMEMBER_ME_VALIDITY)
            .secure(secureCookies)
            .sameSite("Lax"));
        return repository;
    }

    @Bean
    public RememberMeServices rememberMeServices() {
        TokenBasedRememberMeServices services = new UsuarioIdRememberMeServices(resolveRememberMeKey(), usuarioUserDetailsService());
        services.setCookieName(REMEMBER_ME_COOKIE);
        services.setParameter(REMEMBER_ME_COOKIE);
        services.setTokenValiditySeconds((int) REMEMBER_ME_VALIDITY.toSeconds());
        return services;
    }

    /**
     * Id-based loading for password-less re-authentication. Resolved on use, so web slice tests that mock a plain
     * {@link UserDetailsService} can still build this configuration.
     */
    private UsuarioUserDetailsService usuarioUserDetailsService() {
        return new UsuarioUserDetailsService() {
            @Override
            public UserDetails loadUserByUsername(String username) {
                return userDetailsService.loadUserByUsername(username);
            }

            @Override
            public UserDetails loadUserByUsuarioId(Long usuarioId, boolean registrarAcceso) {
                if (userDetailsService instanceof UsuarioUserDetailsService byId) {
                    return byId.loadUserByUsuarioId(usuarioId, registrarAcceso);
                }
                throw new UsernameNotFoundException("Loading users by id is not available");
            }
        };
    }

    private String resolveRememberMeKey() {
        if (rememberMeKey != null && !rememberMeKey.isBlank()) {
            return rememberMeKey;
        }
        log.warn("app.security.remember-me.key is not set: using a random key, remember-me cookies will not survive a restart");
        byte[] random = new byte[32];
        new SecureRandom().nextBytes(random);
        return HexFormat.of().formatHex(random);
    }

    @Bean
    public WebAuthnRelyingPartyOperations webAuthnRelyingPartyOperations(
            ObjectProvider<PublicKeyCredentialUserEntityRepository> userEntityRepository,
            ObjectProvider<UserCredentialRepository> userCredentialRepository) {
        // Same fallback Spring Security applies when no repository bean exists (web slice tests);
        // the application always provides the JPA-backed repositories.
        return new Webauthn4JRelyingPartyOperations(
                userEntityRepository.getIfAvailable(MapPublicKeyCredentialUserEntityRepository::new),
                userCredentialRepository.getIfAvailable(MapUserCredentialRepository::new),
                PublicKeyCredentialRpEntity.builder().id(webauthnRpId).name(WEBAUTHN_RP_NAME).build(),
                Set.of(webauthnAllowedOrigins));
    }

    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

    @Bean
    @Order(1)
    public SecurityFilterChain eventosPublicChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/eventos/**", "/info", "/info/**", "/sitemap.xml", "/baja/**", "/robots.txt")
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .headers(headers -> headers
                .contentSecurityPolicy(csp -> csp.policyDirectives(
                    "default-src 'self'; " +
                        "img-src 'self' data: https: http://res.cloudinary.com; " +
                        "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com https://cdnjs.cloudflare.com https://cdn.jsdelivr.net; " +
                        "script-src 'self' 'unsafe-inline' 'unsafe-eval' https://cdnjs.cloudflare.com https://cdn.jsdelivr.net https://www.googletagmanager.com; " +
                        "font-src 'self' data: https://fonts.gstatic.com https://cdn.jsdelivr.net https://cdnjs.cloudflare.com; " +
                        "connect-src 'self' https://www.googletagmanager.com https://www.google-analytics.com https://region1.google-analytics.com https://cdn.jsdelivr.net; " +
                        "frame-src https://www.googletagmanager.com; " +
                        "frame-ancestors 'none'; " +
                        "form-action 'self'; " +
                        "base-uri 'self'; " +
                        "upgrade-insecure-requests"
                ))
                .referrerPolicy(referrer ->
                    referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                .permissionsPolicy(permissions ->
                    permissions.policy("geolocation=(), camera=(), microphone=(), payment=(), usb=()"))
            )
            .authorizeHttpRequests(authz -> authz
                .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                .requestMatchers("/error", "/403").permitAll()
                .anyRequest().permitAll()
            )
            .addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain appChain(HttpSecurity http, WebAuthnRelyingPartyOperations webAuthnRelyingPartyOperations,
                                        ObjectProvider<PasskeyOwnerLoader> passkeyOwnerLoader) throws Exception {
        http
            .csrf(csrf -> csrf
                .csrfTokenRepository(csrfTokenRepository())
                .ignoringRequestMatchers("/api/**"))
            .authorizeHttpRequests(authz -> authz
	                .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
	                .requestMatchers("/error", "/403").permitAll()
	                .requestMatchers("/auth/**").permitAll()
					.requestMatchers("/manifest.json").permitAll()
					.requestMatchers("/robots.txt").permitAll()
					.requestMatchers("/release-notes/**").permitAll()

					.requestMatchers("/fragments/**", "/static/**", "/adminkit/**", "/img/**", "/logo/**", "/favicon.ico", "/js/**", "/css/**", "/leaflet/**", "/geojson/**", "/vendor/**").permitAll()
					.requestMatchers("/android-icon-*.png", "/apple-icon-*.png", "/ms-icon-*.png").permitAll()

					.requestMatchers("/service-worker.js", "/sw-register.js").permitAll()
					.requestMatchers("/legal/**").permitAll()
					.anyRequest().authenticated()
            )
			.sessionManagement(session -> session
					.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
					.sessionFixation(sessionFixation -> sessionFixation.migrateSession())
					.maximumSessions(1)
					.expiredUrl("/auth/login?expired").sessionRegistry(sessionRegistry())
			)
	            .formLogin(form -> form
                .loginPage("/auth/login")
                .loginProcessingUrl("/auth/login")
                .permitAll()
                .successHandler(new FunctionalLoginSuccessHandler(functionalEventTracker))
                .failureHandler(new FunctionalLoginFailureHandler(functionalEventTracker))
            )
	            .rememberMe(rememberMe -> rememberMe
	                .rememberMeServices(rememberMeServices())
	            )
	            .webAuthn(webAuthn -> webAuthn
	                .rpName(WEBAUTHN_RP_NAME)
	                .rpId(webauthnRpId)
	                .allowedOrigins(webauthnAllowedOrigins)
	                .disableDefaultRegistrationPage(true)
	            )
	            .exceptionHandling(exceptionHandling -> exceptionHandling
	                .accessDeniedPage("/403")
	            )

	            .logout(logout -> logout
	                .logoutUrl("/logout")
	                .invalidateHttpSession(true)
	                .clearAuthentication(true)
	                .deleteCookies("JSESSIONID", REMEMBER_ME_COOKIE)
	                .logoutSuccessHandler(new FunctionalLogoutSuccessHandler(functionalEventTracker))
	                .permitAll()
	            )
	            .addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class);

        DefaultSecurityFilterChain chain = http.build();
        securePasskeyLogin(chain, http.getSharedObject(SessionAuthenticationStrategy.class), webAuthnRelyingPartyOperations,
                passkeyOwnerLoader);
        return chain;
	}

    /**
     * Spring Security's WebAuthn configurer leaves the passkey login filter without session handling
     * (no session fixation protection, no maximum-sessions control) and authenticates a WebAuthn user entity
     * instead of the application user. Align it with the form login.
     */
    private void securePasskeyLogin(DefaultSecurityFilterChain chain, SessionAuthenticationStrategy sessionStrategy,
                                    WebAuthnRelyingPartyOperations webAuthnRelyingPartyOperations,
                                    ObjectProvider<PasskeyOwnerLoader> passkeyOwnerLoader) {
        WebAuthnAuthenticationFilter passkeyLogin = chain.getFilters().stream()
                .filter(WebAuthnAuthenticationFilter.class::isInstance)
                .map(WebAuthnAuthenticationFilter.class::cast)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("WebAuthn login filter not found"));
        if (sessionStrategy == null) {
            throw new IllegalStateException("Session authentication strategy not available for passkey login");
        }
        passkeyLogin.setAuthenticationManager(new ProviderManager(
                new PasskeyAuthenticationProvider(webAuthnRelyingPartyOperations,
                        userHandle -> passkeyOwnerLoader.getObject().loadOwner(userHandle))));
        passkeyLogin.setSessionAuthenticationStrategy(sessionStrategy);
        passkeyLogin.setAuthenticationSuccessHandler(new FunctionalPasskeyLoginSuccessHandler(functionalEventTracker));
        passkeyLogin.setAuthenticationFailureHandler(new FunctionalPasskeyLoginFailureHandler(functionalEventTracker));
    }
	@Bean
	public MethodSecurityExpressionHandler methodSecurityExpressionHandler(CustomPermissionEvaluator permissionEvaluator) {
		DefaultMethodSecurityExpressionHandler expressionHandler = new DefaultMethodSecurityExpressionHandler();
		expressionHandler.setPermissionEvaluator(permissionEvaluator);
		return expressionHandler;
	}

}
