package com.promptly.backend.security;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.promptly.backend.common.ApiError;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Reponse renvoyee lorsqu'une route protegee est appelee sans authentification.
 *
 * Sans cette classe, Spring Security redirigerait vers une page de connexion
 * HTML, ce qui n'a pas de sens pour une API consommee par Angular. On renvoie
 * donc un code 401 accompagne du meme format JSON que les autres erreurs.
 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public RestAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ApiError error = ApiError.of(401, "Unauthorized",
                "Authentification requise pour acceder a cette ressource",
                request.getRequestURI());

        objectMapper.writeValue(response.getOutputStream(), error);
    }
}
