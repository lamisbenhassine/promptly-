package com.promptly.backend.course;

/**
 * Technologies et themes selon lesquels les cours sont classes.
 *
 * Une enumeration est preferee a une table dediee : la liste est fixe, connue
 * a la conception, et le compilateur detecte toute valeur invalide.
 */
public enum CourseCategory {

    JAVA("Java"),
    SPRING_BOOT("Spring Boot"),
    ANGULAR("Angular"),
    TYPESCRIPT("TypeScript"),
    ALGORITHMES("Algorithmes et structures de donnees"),
    GENIE_LOGICIEL("Genie logiciel");

    private final String label;

    CourseCategory(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
