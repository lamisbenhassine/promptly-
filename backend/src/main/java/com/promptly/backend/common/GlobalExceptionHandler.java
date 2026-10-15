package com.promptly.backend.common;

import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.promptly.backend.common.exception.BusinessRuleException;
import com.promptly.backend.common.exception.EmailAlreadyUsedException;
import com.promptly.backend.common.exception.ResourceNotFoundException;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Traduit les exceptions en reponses HTTP au format {@link ApiError}.
 *
 * Centraliser ce traitement evite de repeter des blocs try/catch dans chaque
 * controleur et garantit que l'API ne divulgue jamais de trace d'execution.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Echec de Bean Validation : detaille les erreurs champ par champ. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException e,
                                                     HttpServletRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError fieldError : e.getBindingResult().getFieldErrors()) {
            fieldErrors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return ResponseEntity.badRequest().body(
                ApiError.validation("Donnees invalides", request.getRequestURI(), fieldErrors));
    }

    @ExceptionHandler(EmailAlreadyUsedException.class)
    public ResponseEntity<ApiError> handleEmailAlreadyUsed(EmailAlreadyUsedException e,
                                                          HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                ApiError.of(409, "Conflict", e.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException e,
                                                   HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ApiError.of(404, "Not Found", e.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ApiError> handleBusinessRule(BusinessRuleException e,
                                                       HttpServletRequest request) {
        return ResponseEntity.badRequest().body(
                ApiError.of(400, "Bad Request", e.getMessage(), request.getRequestURI()));
    }

    /**
     * Identifiants invalides. Le message reste volontairement generique : preciser
     * si c'est l'adresse ou le mot de passe qui est erronee permettrait a un
     * attaquant d'enumerer les comptes existants.
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiError> handleBadCredentials(HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                ApiError.of(401, "Unauthorized",
                        "Adresse electronique ou mot de passe incorrect",
                        request.getRequestURI()));
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ApiError> handleDisabled(HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                ApiError.of(401, "Unauthorized",
                        "Ce compte est desactive. Contactez un administrateur.",
                        request.getRequestURI()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                ApiError.of(403, "Forbidden",
                        "Vous n'avez pas les droits necessaires pour cette action",
                        request.getRequestURI()));
    }

    /**
     * Filet de securite. L'exception est journalisee cote serveur, mais le
     * client ne recoit qu'un message generique, sans detail technique.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception e, HttpServletRequest request) {
        log.error("Erreur inattendue sur {}", request.getRequestURI(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiError.of(500, "Internal Server Error",
                        "Une erreur interne est survenue", request.getRequestURI()));
    }
}
