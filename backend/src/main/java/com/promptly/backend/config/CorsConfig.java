package com.promptly.backend.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Autorise le frontend Angular (port 4200) a appeler l'API (port 8080).
 *
 * Sans cette configuration, le navigateur bloque les requetes car les deux
 * applications n'ont pas la meme origine (politique "same-origin").
 *
 * <p><b>Modification apportee au module 1.</b> En phase 0, cette classe
 * implementait {@code WebMvcConfigurer}. Depuis l'ajout de Spring Security, la
 * configuration CORS doit etre exposee sous forme de bean
 * {@link CorsConfigurationSource} : la chaine de filtres de securite s'execute
 * <em>avant</em> Spring MVC, et une requete rejetee par la securite ne
 * recevrait jamais les en-tetes CORS definis au niveau MVC. Le navigateur
 * afficherait alors une erreur CORS trompeuse au lieu du veritable code 401.</p>
 */
@Configuration
public class CorsConfig {

    private final List<String> allowedOrigins;

    public CorsConfig(@Value("${promptly.cors.allowed-origins}") String[] allowedOrigins) {
        this.allowedOrigins = Arrays.asList(allowedOrigins);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(
                List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}
