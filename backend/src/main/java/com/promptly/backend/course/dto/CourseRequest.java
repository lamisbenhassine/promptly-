package com.promptly.backend.course.dto;

import com.promptly.backend.course.CourseCategory;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Creation ou modification d'un cours.
 *
 * {@code instructorId} n'est pris en compte que pour un administrateur : un
 * enseignant est toujours rattache a ses propres cours, meme s'il transmet
 * un autre identifiant.
 */
public record CourseRequest(

        @NotBlank(message = "Le titre est obligatoire")
        @Size(min = 3, max = 120, message = "Le titre doit contenir entre 3 et 120 caracteres")
        String title,

        @NotBlank(message = "La description est obligatoire")
        @Size(min = 10, max = 2000, message = "La description doit contenir entre 10 et 2000 caracteres")
        String description,

        @NotBlank(message = "Les objectifs d'apprentissage sont obligatoires")
        @Size(min = 10, max = 2000, message = "Les objectifs doivent contenir entre 10 et 2000 caracteres")
        String learningObjectives,

        @NotNull(message = "La categorie est obligatoire")
        CourseCategory category,

        @NotNull(message = "Le statut de publication est obligatoire")
        Boolean published,

        Long instructorId
) {
}
