package es.musicalia.gestmusica.config;

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
            .securityMatcher("/eventos/**", "/baja/**", "/robots.txt")
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/eventos/**", "/baja/**")
            )
            .headers(headers -> headers
                .contentSecurityPolicy(csp -> csp.policyDirectives(
                    "default-src 'self'; " +
                        "img-src 'self' data: https: http://res.cloudinary.com; " +
                        "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com https://cdnjs.cloudflare.com; " +
                        "script-src 'self' 'unsafe-inline' 'unsafe-eval' https://cdnjs.cloudflare.com; " +
                        "font-src 'self' data: https://fonts.gstatic.com; " +
                        "connect-src 'self'; " +
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
                .anyRequest().permitAll()
            )
            .addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain appChain(HttpSecurity http) throws Exception {
	        http
	            .csrf(Customizer.withDefaults())
	            .authorizeHttpRequests(authz -> authz
	                .requestMatchers("/auth/**").permitAll()
                    .requestMatchers("/info**","/info").permitAll()
					.requestMatchers("/manifest.json").permitAll()
					.requestMatchers("/robots.txt").permitAll()
					.requestMatchers("/release-notes/**").permitAll()
					// Permitir acceso público a eventos para indexación de Google
					.requestMatchers("/eventos/**").permitAll()

					.requestMatchers("/fragments/**", "/static/**", "/adminkit/**", "/img/**", "/logo/**", "/favicon.ico", "/js/**", "/css/**").permitAll()
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
                .defaultSuccessUrl("/", true)
            )

	            .logout(logout -> logout
	                .logoutUrl("/logout")
	                .invalidateHttpSession(true)
	                .clearAuthentication(true)
	                .deleteCookies("JSESSIONID")
	                .logoutSuccessUrl("/auth/login?logout")
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
