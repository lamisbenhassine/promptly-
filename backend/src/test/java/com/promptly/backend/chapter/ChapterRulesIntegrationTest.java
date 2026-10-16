package com.promptly.backend.chapter;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.promptly.backend.user.Role;
import com.promptly.backend.user.User;
import com.promptly.backend.user.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ChapterRulesIntegrationTest {

    private static final String TEACHER_EMAIL = "enseignant-ch@test.local";
    private static final String TEACHER_PASSWORD = "Enseignant1";
    private static final String STUDENT_EMAIL = "etudiant-ch@test.local";
    private static final String STUDENT_PASSWORD = "Etudiant1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        save(TEACHER_EMAIL, TEACHER_PASSWORD, Role.ENSEIGNANT);
        save(STUDENT_EMAIL, STUDENT_PASSWORD, Role.ETUDIANT);
    }

    @Test
    @DisplayName("Un enseignant cree un chapitre dans son cours")
    void teacherCreatesChapter() throws Exception {
        long courseId = createPublishedCourse();

        mockMvc.perform(post("/api/courses/" + courseId + "/chapters")
                        .header("Authorization", bearer(login(TEACHER_EMAIL, TEACHER_PASSWORD)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "title": "Variables et types",
                                  "content": "Les variables stockent des valeurs en memoire.",
                                  "codeExample": "int x = 5;",
                                  "position": 1 }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Variables et types"))
                .andExpect(jsonPath("$.position").value(1))
                .andExpect(jsonPath("$.courseId").value(courseId));
    }

    @Test
    @DisplayName("Un etudiant lit les chapitres d'un cours publie")
    void studentReadsChapters() throws Exception {
        long courseId = createPublishedCourse();
        createChapter(courseId);

        mockMvc.perform(get("/api/courses/" + courseId + "/chapters")
                        .header("Authorization", bearer(login(STUDENT_EMAIL, STUDENT_PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Variables et types"));
    }

    @Test
    @DisplayName("Un etudiant ne peut pas creer un chapitre (403)")
    void studentCannotCreateChapter() throws Exception {
        long courseId = createPublishedCourse();

        mockMvc.perform(post("/api/courses/" + courseId + "/chapters")
                        .header("Authorization", bearer(login(STUDENT_EMAIL, STUDENT_PASSWORD)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "title": "Pirate",
                                  "content": "Contenu suffisamment long pour passer.",
                                  "position": 1 }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("La suppression d'un chapitre renvoie 204")
    void teacherDeletesChapter() throws Exception {
        long courseId = createPublishedCourse();
        long chapterId = createChapter(courseId);

        mockMvc.perform(delete("/api/courses/" + courseId + "/chapters/" + chapterId)
                        .header("Authorization", bearer(login(TEACHER_EMAIL, TEACHER_PASSWORD))))
                .andExpect(status().isNoContent());
    }

    private void save(String email, String password, Role role) {
        if (!userRepository.existsByEmail(email)) {
            userRepository.saveAndFlush(new User("Prenom", "Nom", email,
                    passwordEncoder.encode(password), role));
        }
    }

    private long createPublishedCourse() throws Exception {
        String body = mockMvc.perform(post("/api/courses")
                        .header("Authorization", bearer(login(TEACHER_EMAIL, TEACHER_PASSWORD)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "title": "Cours Java",
                                  "description": "Les bases du langage Java pour debuter.",
                                  "learningObjectives": "Ecrire une classe simple et compiler.",
                                  "category": "JAVA",
                                  "published": true }
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    private long createChapter(long courseId) throws Exception {
        String body = mockMvc.perform(post("/api/courses/" + courseId + "/chapters")
                        .header("Authorization", bearer(login(TEACHER_EMAIL, TEACHER_PASSWORD)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "title": "Variables et types",
                                  "content": "Les variables stockent des valeurs en memoire.",
                                  "codeExample": "int x = 5;",
                                  "position": 1 }
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    private String login(String email, String password) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "email": "%s", "password": "%s" }
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(body);
        return json.get("token").asText();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
