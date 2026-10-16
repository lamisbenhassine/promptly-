package com.promptly.backend.forum;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.promptly.backend.user.Role;
import com.promptly.backend.user.User;
import com.promptly.backend.user.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ForumRulesIntegrationTest {

    private static final String STUDENT_EMAIL = "etudiant-forum@test.local";
    private static final String STUDENT_PASSWORD = "Etudiant1";
    private static final String OTHER_EMAIL = "autre-forum@test.local";
    private static final String OTHER_PASSWORD = "Etudiant1";
    private static final String TEACHER_EMAIL = "enseignant-forum@test.local";
    private static final String TEACHER_PASSWORD = "Enseignant1";

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
        save(STUDENT_EMAIL, STUDENT_PASSWORD, Role.ETUDIANT);
        save(OTHER_EMAIL, OTHER_PASSWORD, Role.ETUDIANT);
        save(TEACHER_EMAIL, TEACHER_PASSWORD, Role.ENSEIGNANT);
    }

    @Test
    @DisplayName("Un etudiant cree une publication")
    void studentCreatesPost() throws Exception {
        mockMvc.perform(post("/api/forum/posts")
                        .header("Authorization", bearer(login(STUDENT_EMAIL, STUDENT_PASSWORD)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Comment boucler sur une List ?",
                                  "content": "Je cherche la difference entre for et forEach en Java.",
                                  "category": "JAVA"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Comment boucler sur une List ?"))
                .andExpect(jsonPath("$.category").value("JAVA"))
                .andExpect(jsonPath("$.locked").value(false));
    }

    @Test
    @DisplayName("Un etudiant commente une publication ouverte")
    void studentComments() throws Exception {
        long postId = createPost();

        mockMvc.perform(post("/api/forum/posts/" + postId + "/comments")
                        .header("Authorization", bearer(login(OTHER_EMAIL, OTHER_PASSWORD)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "content": "Utilisez une boucle for-each : for (String s : list)" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(
                        "Utilisez une boucle for-each : for (String s : list)"));
    }

    @Test
    @DisplayName("Un etudiant ne peut pas supprimer la publication d'un autre (403)")
    void studentCannotDeleteOthersPost() throws Exception {
        long postId = createPost();

        mockMvc.perform(delete("/api/forum/posts/" + postId)
                        .header("Authorization", bearer(login(OTHER_EMAIL, OTHER_PASSWORD))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Un enseignant peut verrouiller un sujet")
    void teacherLocksPost() throws Exception {
        long postId = createPost();

        mockMvc.perform(put("/api/forum/posts/" + postId + "/lock")
                        .header("Authorization", bearer(login(TEACHER_EMAIL, TEACHER_PASSWORD)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"locked\": true }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.locked").value(true));

        mockMvc.perform(post("/api/forum/posts/" + postId + "/comments")
                        .header("Authorization", bearer(login(OTHER_EMAIL, OTHER_PASSWORD)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "content": "Ce commentaire ne doit pas passer." }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("La liste des publications est accessible aux utilisateurs connectes")
    void listPosts() throws Exception {
        createPost();

        mockMvc.perform(get("/api/forum/posts")
                        .header("Authorization", bearer(login(STUDENT_EMAIL, STUDENT_PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Comment boucler sur une List ?"));
    }

    @Test
    @DisplayName("Un utilisateur peut reagir puis retirer sa reaction")
    void studentReactsLikeFacebook() throws Exception {
        long postId = createPost();
        String token = bearer(login(OTHER_EMAIL, OTHER_PASSWORD));

        mockMvc.perform(put("/api/forum/posts/" + postId + "/reaction")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"type\": \"LOVE\" }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reactions.total").value(1))
                .andExpect(jsonPath("$.reactions.myReaction").value("LOVE"))
                .andExpect(jsonPath("$.reactions.counts.LOVE").value(1));

        mockMvc.perform(put("/api/forum/posts/" + postId + "/reaction")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"type\": \"LOVE\" }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reactions.total").value(0))
                .andExpect(jsonPath("$.reactions.myReaction").doesNotExist());
    }

    private long createPost() throws Exception {
        String body = mockMvc.perform(post("/api/forum/posts")
                        .header("Authorization", bearer(login(STUDENT_EMAIL, STUDENT_PASSWORD)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Comment boucler sur une List ?",
                                  "content": "Je cherche la difference entre for et forEach en Java.",
                                  "category": "JAVA"
                                }
                                """))
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
