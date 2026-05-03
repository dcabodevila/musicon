package es.musicalia.gestmusica.config;

import es.musicalia.gestmusica.observabilidad.FunctionalEventTracker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
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
import org.springframework.security.web.session.HttpSessionEventPublisher;
import jakarta.servlet.DispatcherType;


@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class WebSecurityConfig {
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

	@Bean
	public AuthenticationProvider authProvider(){
		DaoAuthenticationProvider daoAuthenticationProvider = new DaoAuthenticationProvider();
		daoAuthenticationProvider.setUserDetailsService(this.userDetailsService);
		daoAuthenticationProvider.setPasswordEncoder(bCryptPasswordEncoder());
		return daoAuthenticationProvider;
	}
	@Bean
	public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
		return http.getSharedObject(AuthenticationManagerBuilder.class)
				.build();
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
                        "connect-src 'self' https://www.googletagmanager.com https://www.google-analytics.com https://cdn.jsdelivr.net; " +
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
    public SecurityFilterChain appChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
            .authorizeHttpRequests(authz -> authz
	                .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
	                .requestMatchers("/error", "/403").permitAll()
	                .requestMatchers("/auth/**").permitAll()
					.requestMatchers("/manifest.json").permitAll()
					.requestMatchers("/robots.txt").permitAll()
					.requestMatchers("/release-notes/**").permitAll()

					.requestMatchers("/fragments/**", "/static/**", "/adminkit/**", "/img/**", "/logo/**", "/favicon.ico", "/js/**", "/css/**", "/leaflet/**", "/geojson/**").permitAll()
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
	            .exceptionHandling(exceptionHandling -> exceptionHandling
	                .accessDeniedPage("/403")
	            )

	            .logout(logout -> logout
	                .logoutUrl("/logout")
	                .invalidateHttpSession(true)
	                .clearAuthentication(true)
	                .deleteCookies("JSESSIONID")
	                .logoutSuccessHandler(new FunctionalLogoutSuccessHandler(functionalEventTracker))
	                .permitAll()
	            )
	            .addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
	}
	@Bean
	public MethodSecurityExpressionHandler methodSecurityExpressionHandler(CustomPermissionEvaluator permissionEvaluator) {
		DefaultMethodSecurityExpressionHandler expressionHandler = new DefaultMethodSecurityExpressionHandler();
		expressionHandler.setPermissionEvaluator(permissionEvaluator);
		return expressionHandler;
	}

}
