package com.promptly.backend.quiz;

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
class QuizRulesIntegrationTest {

    private static final String TEACHER_EMAIL = "enseignant-quiz@test.local";
    private static final String TEACHER_PASSWORD = "Enseignant1";
    private static final String STUDENT_EMAIL = "etudiant-quiz@test.local";
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
    @DisplayName("Un enseignant cree un quiz publie avec questions")
    void teacherCreatesQuiz() throws Exception {
        long courseId = createPublishedCourse();

        mockMvc.perform(post("/api/quizzes")
                        .header("Authorization", bearer(login(TEACHER_EMAIL, TEACHER_PASSWORD)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(quizJson(courseId, true)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Quiz Java bases"))
                .andExpect(jsonPath("$.questionCount").value(1))
                .andExpect(jsonPath("$.questions[0].choices[0].correct").value(true));
    }

    @Test
    @DisplayName("Un etudiant voit un quiz publie sans les bonnes reponses")
    void studentSeesQuizWithoutAnswers() throws Exception {
        long courseId = createPublishedCourse();
        long quizId = createQuiz(courseId);

        mockMvc.perform(get("/api/quizzes/" + quizId)
                        .header("Authorization", bearer(login(STUDENT_EMAIL, STUDENT_PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Quiz Java bases"))
                .andExpect(jsonPath("$.questions[0].choices[0].correct").doesNotExist());
    }

    @Test
    @DisplayName("Un etudiant ne peut pas creer un quiz (403)")
    void studentCannotCreateQuiz() throws Exception {
        long courseId = createPublishedCourse();

        mockMvc.perform(post("/api/quizzes")
                        .header("Authorization", bearer(login(STUDENT_EMAIL, STUDENT_PASSWORD)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(quizJson(courseId, true)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Un etudiant soumet une tentative et obtient un score")
    void studentSubmitsAttempt() throws Exception {
        long courseId = createPublishedCourse();
        long quizId = createQuiz(courseId);

        String quizBody = mockMvc.perform(get("/api/quizzes/" + quizId)
                        .header("Authorization", bearer(login(TEACHER_EMAIL, TEACHER_PASSWORD))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode quiz = objectMapper.readTree(quizBody);
        long correctChoiceId = -1;
        for (JsonNode choice : quiz.path("questions").get(0).path("choices")) {
            if (choice.path("correct").asBoolean(false)) {
                correctChoiceId = choice.path("id").asLong();
                break;
            }
        }

        mockMvc.perform(post("/api/quizzes/" + quizId + "/attempts")
                        .header("Authorization", bearer(login(STUDENT_EMAIL, STUDENT_PASSWORD)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"selectedChoiceIds\":[" + correctChoiceId + "],\"questionTimings\":[],\"timedOut\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scorePercent").value(100))
                .andExpect(jsonPath("$.correctCount").value(1))
                .andExpect(jsonPath("$.totalQuestions").value(1));
    }

    private String quizJson(long courseId, boolean published) {
        return """
                {
                  "title": "Quiz Java bases",
                  "description": "Verifier les notions de variables et types.",
                  "courseId": %d,
                  "chapterId": null,
                  "published": %s,
                  "questions": [
                    {
                      "text": "Quel mot-cle declare une constante ?",
                      "position": 1,
                      "choices": [
                        { "text": "final", "correct": true },
                        { "text": "const", "correct": false },
                        { "text": "static", "correct": false }
                      ]
                    }
                  ]
                }
                """.formatted(courseId, published);
    }

    private long createPublishedCourse() throws Exception {
        String body = mockMvc.perform(post("/api/courses")
                        .header("Authorization", bearer(login(TEACHER_EMAIL, TEACHER_PASSWORD)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Cours quiz",
                                  "description": "Description suffisamment longue pour le cours.",
                                  "learningObjectives": "Objectifs suffisamment longs pour le cours.",
                                  "category": "JAVA",
                                  "published": true
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).path("id").asLong();
    }

    private long createQuiz(long courseId) throws Exception {
        String body = mockMvc.perform(post("/api/quizzes")
                        .header("Authorization", bearer(login(TEACHER_EMAIL, TEACHER_PASSWORD)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(quizJson(courseId, true)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).path("id").asLong();
    }

    private void save(String email, String password, Role role) {
        if (!userRepository.existsByEmail(email)) {
            userRepository.saveAndFlush(new User("Prenom", "Nom", email,
                    passwordEncoder.encode(password), role));
        }
    }

    private String login(String email, String password) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "email": "%s", "password": "%s" }
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).path("token").asText();
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
