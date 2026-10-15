package com.promptly.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.promptly.backend.user.Role;
import com.promptly.backend.user.User;
import com.promptly.backend.user.UserRepository;

/**
 * Cree le premier compte administrateur au demarrage, s'il n'en existe aucun.
 *
 * Ce mecanisme resout le probleme de l'amorcage : seul un administrateur peut
 * promouvoir un autre utilisateur, mais aucun administrateur n'existe sur une
 * base vierge.
 *
 * <p>Deux precautions sont appliquees :</p>
 * <ul>
 *   <li>les identifiants sont lus depuis des variables d'environnement, jamais
 *       ecrits dans le code ni versionnes ;</li>
 *   <li>si ces variables ne sont pas definies, aucun compte n'est cree. Creer un
 *       administrateur avec un mot de passe par defaut connu serait une faille
 *       de securite majeure.</li>
 * </ul>
 *
 * <p>{@code @Profile("!test")} desactive cet initialiseur pendant les tests.
 * C'est indispensable : les variables d'environnement ont une priorite
 * <em>superieure</em> aux fichiers application-*.properties dans Spring Boot.
 * Neutraliser {@code promptly.admin.email} dans application-test.properties ne
 * suffirait donc pas, car la variable PROMPTLY_ADMIN_EMAIL du poste de
 * developpement reprendrait le dessus et un administrateur supplementaire
 * serait cree, faussant les tests qui comptent les administrateurs actifs.</p>
 */
@Component
@Profile("!test")
public class AdminInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminPassword;
    private final String adminFirstName;
    private final String adminLastName;

    public AdminInitializer(UserRepository userRepository,
                            PasswordEncoder passwordEncoder,
                            @Value("${promptly.admin.email}") String adminEmail,
                            @Value("${promptly.admin.password}") String adminPassword,
                            @Value("${promptly.admin.first-name}") String adminFirstName,
                            @Value("${promptly.admin.last-name}") String adminLastName) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
        this.adminFirstName = adminFirstName;
        this.adminLastName = adminLastName;
    }

    @Override
    public void run(ApplicationArguments args) {

        if (userRepository.countByRoleAndEnabledTrue(Role.ADMIN) > 0) {
            log.debug("Un administrateur actif existe deja : aucune creation necessaire.");
            return;
        }

        if (adminEmail.isBlank() || adminPassword.isBlank()) {
            log.warn("Aucun administrateur en base et variables PROMPTLY_ADMIN_EMAIL / "
                    + "PROMPTLY_ADMIN_PASSWORD non definies : aucun compte administrateur "
                    + "n'a ete cree.");
            return;
        }

        String email = adminEmail.trim().toLowerCase();

        if (userRepository.existsByEmail(email)) {
            log.warn("Un compte existe deja avec l'adresse {} : promotion en ADMIN.", email);
            userRepository.findByEmail(email).ifPresent(existing -> {
                existing.setRole(Role.ADMIN);
                existing.setEnabled(true);
                userRepository.save(existing);
            });
            return;
        }

        User admin = new User(
                adminFirstName.trim(),
                adminLastName.trim(),
                email,
                passwordEncoder.encode(adminPassword),
                Role.ADMIN);

        userRepository.save(admin);
        log.info("Compte administrateur initial cree : {}", email);
    }
}
