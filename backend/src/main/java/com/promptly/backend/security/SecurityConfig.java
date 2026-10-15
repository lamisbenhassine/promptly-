package com.promptly.backend.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Configuration de la securite de l'API.
 *
 * Trois choix structurants sont appliques ici :
 *
 * <ul>
 *   <li><b>Sessions desactivees</b> (STATELESS) : l'identite est portee par le
 *       jeton JWT a chaque requete, le serveur ne conserve aucun etat.</li>
 *   <li><b>Protection CSRF desactivee</b> : l'attaque CSRF repose sur l'envoi
 *       automatique des cookies par le navigateur. Le jeton etant transmis dans
 *       l'en-tete Authorization, et non dans un cookie, il n'est jamais joint
 *       automatiquement a une requete forgee par un site tiers. Ce raisonnement
 *       ne vaudrait plus si le jeton etait stocke dans un cookie.</li>
 *   <li><b>Autorisation en deux niveaux</b> : les regles d'acces aux URL sont
 *       declarees ici, et les restrictions par role sont exprimees au plus pres
 *       du code metier avec {@code @PreAuthorize} (active par
 *       {@code @EnableMethodSecurity}).</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtService jwtService;
    private final AppUserDetailsService userDetailsService;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;
    private final CorsConfigurationSource corsConfigurationSource;

    public SecurityConfig(JwtService jwtService,
                          AppUserDetailsService userDetailsService,
                          RestAuthenticationEntryPoint authenticationEntryPoint,
                          RestAccessDeniedHandler accessDeniedHandler,
                          CorsConfigurationSource corsConfigurationSource) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
        this.corsConfigurationSource = corsConfigurationSource;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Requetes preliminaires CORS : le navigateur les envoie
                        // sans en-tete Authorization, elles doivent donc passer.
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Routes publiques
                        .requestMatchers("/api/health").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/register").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()

                        // Toute autre route exige une authentification. Les
                        // restrictions par role sont appliquees par @PreAuthorize.
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .addFilterBefore(
                        new JwtAuthenticationFilter(jwtService, userDetailsService),
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * BCrypt applique un sel aleatoire et un cout de calcul volontairement eleve.
     * Deux hachages du meme mot de passe sont donc differents, et une attaque par
     * force brute reste couteuse. Un algorithme rapide comme SHA-256 serait
     * inadapte a cet usage, precisement parce qu'il est rapide.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(
            AppUserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }
}
