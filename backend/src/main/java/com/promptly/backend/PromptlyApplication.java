package com.promptly.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Point d'entree de l'API REST Promptly.
 */
@SpringBootApplication
public class PromptlyApplication {

    public static void main(String[] args) {
        SpringApplication.run(PromptlyApplication.class, args);
    }
}
