package com.promptly.backend.user;

/**
 * Roles disponibles sur la plateforme.
 *
 * Une enumeration est preferee a une table dediee car les roles sont en nombre
 * fixe et connus a la compilation : le compilateur detecte alors toute faute de
 * frappe, ce qu'une chaine de caracteres libre ne permettrait pas.
 *
 * Spring Security attend des autorisations prefixees par "ROLE_" ; la
 * conversion est assuree par {@link #asAuthority()}.
 */
public enum Role {

    /** Apprenant : consulte les cours, passe les quiz, participe au forum. */
    ETUDIANT,

    /** Auteur de contenu pedagogique : cree cours, chapitres et quiz. */
    ENSEIGNANT,

    /** Responsable de la plateforme : gere les comptes et les reclamations. */
    ADMIN;

    public String asAuthority() {
        return "ROLE_" + name();
    }
}
