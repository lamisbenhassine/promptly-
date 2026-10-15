package com.promptly.backend.common.exception;

/** Levee lorsqu'une inscription utilise une adresse deja enregistree (HTTP 409). */
public class EmailAlreadyUsedException extends RuntimeException {

    public EmailAlreadyUsedException(String email) {
        super("Un compte existe deja avec l'adresse " + email);
    }
}
