package com.promptly.backend.chapter.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChapterRequest(

        @NotBlank(message = "Le titre est obligatoire")
        @Size(min = 3, max = 150, message = "Le titre doit contenir entre 3 et 150 caracteres")
        String title,

        @NotBlank(message = "Le contenu est obligatoire")
        @Size(min = 10, max = 50000, message = "Le contenu doit contenir au moins 10 caracteres")
        String content,

        @Size(max = 50000, message = "L'exemple de code est trop long")
        String codeExample,

        @Min(value = 1, message = "La position doit etre au moins 1")
        Integer position
) {
}
