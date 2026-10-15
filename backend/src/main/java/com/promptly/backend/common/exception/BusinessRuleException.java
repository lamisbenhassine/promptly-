package com.promptly.backend.common.exception;

/**
 * Levee lorsqu'une regle metier est violee (HTTP 400).
 *
 * Exemples : mot de passe actuel incorrect, retrait du role du dernier
 * administrateur actif.
 */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
