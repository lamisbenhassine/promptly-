package com.promptly.backend.security;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Filtre executé une fois par requete, qui authentifie l'appelant a partir du
 * jeton JWT present dans l'en-tete {@code Authorization}.
 *
 * Deroulement :
 * <ol>
 *   <li>lecture de l'en-tete {@code Authorization: Bearer <jeton>} ;</li>
 *   <li>verification de la signature et de l'expiration du jeton ;</li>
 *   <li>chargement de l'utilisateur depuis la base ;</li>
 *   <li>enregistrement de l'authentification dans le contexte de securite.</li>
 * </ol>
 *
 * En l'absence de jeton, le filtre ne fait rien : la requete poursuit son
 * chemin et c'est la configuration de securite qui decidera de l'accepter
 * (route publique) ou de la rejeter (route protegee).
 *
 * <p>Cette classe n'est volontairement <b>pas</b> annotee {@code @Component} :
 * elle est instanciee explicitement dans {@link SecurityConfig}. Deux raisons :
 * un {@code Filter} declare comme bean serait automatiquement enregistre par
 * Spring Boot dans la chaine de filtres du servlet, en plus de celle de Spring
 * Security, donc execute deux fois ; et {@code @WebMvcTest} inclut les beans de
 * type {@code Filter} dans son contexte reduit, ce qui ferait echouer les tests
 * de controleur faute de pouvoir injecter {@link JwtService}.</p>
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final AppUserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService,
                                   AppUserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String token = extractToken(request);

        // Si aucune authentification n'est encore etablie et qu'un jeton est
        // present, on tente de l'exploiter.
        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            String email = jwtService.extractEmailIfValid(token);

            if (email != null) {
                try {
                    // L'utilisateur est recharge depuis la base a chaque requete :
                    // un changement de role ou une desactivation de compte prend
                    // ainsi effet immediatement, sans attendre l'expiration du jeton.
                    UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                    if (userDetails.isEnabled()) {
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(
                                        userDetails, null, userDetails.getAuthorities());
                        authentication.setDetails(
                                new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                } catch (UsernameNotFoundException e) {
                    // Jeton valide mais compte supprime depuis son emission :
                    // la requete reste non authentifiee.
                    logger.debug("Jeton valide pour un compte inexistant : " + email);
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader(AUTH_HEADER);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            String token = header.substring(BEARER_PREFIX.length()).trim();
            return token.isEmpty() ? null : token;
        }
        return null;
    }
}
