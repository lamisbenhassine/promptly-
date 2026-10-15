package com.promptly.backend.common.exception;

/** Levee lorsqu'une ressource demandee n'existe pas (HTTP 404). */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resource, Object identifier) {
        super(resource + " introuvable : " + identifier);
    }
}
