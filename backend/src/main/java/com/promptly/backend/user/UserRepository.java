package com.promptly.backend.user;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acces aux donnees des utilisateurs.
 *
 * Spring Data genere l'implementation a partir du nom des methodes : aucune
 * requete SQL n'est a ecrire pour ces cas simples.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    /** Utilise pour empecher la suppression du dernier administrateur actif. */
    long countByRoleAndEnabledTrue(Role role);
}
