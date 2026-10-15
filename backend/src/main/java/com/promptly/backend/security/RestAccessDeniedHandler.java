package com.promptly.backend.security;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.promptly.backend.common.ApiError;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Reponse renvoyee lorsqu'un utilisateur authentifie n'a pas le role requis.
 *
 * La distinction avec le code 401 est importante : 401 signifie "je ne sais pas
 * qui vous etes", 403 signifie "je sais qui vous etes, mais vous n'avez pas le
 * droit". Le frontend exploite cette difference pour decider s'il faut
 * rediriger vers la page de connexion ou afficher un message de refus.
 */
@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public RestAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {

        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ApiError error = ApiError.of(403, "Forbidden",
                "Vous n'avez pas les droits necessaires pour cette action",
                request.getRequestURI());

        objectMapper.writeValue(response.getOutputStream(), error);
    }
}
