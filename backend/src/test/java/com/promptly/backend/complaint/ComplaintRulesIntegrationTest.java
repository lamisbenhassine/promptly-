package com.promptly.backend.complaint;

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
class ComplaintRulesIntegrationTest {

    private static final String STUDENT_EMAIL = "etudiant-rec@test.local";
    private static final String STUDENT_PASSWORD = "Etudiant1";
    private static final String OTHER_EMAIL = "autre-rec@test.local";
    private static final String OTHER_PASSWORD = "Etudiant1";
    private static final String ADMIN_EMAIL = "admin-rec@test.local";
    private static final String ADMIN_PASSWORD = "Admin1234";

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
        save(ADMIN_EMAIL, ADMIN_PASSWORD, Role.ADMIN);
    }

    @Test
    @DisplayName("Un etudiant depose une reclamation")
    void studentCreatesComplaint() throws Exception {
        mockMvc.perform(post("/api/complaints")
                        .header("Authorization", bearer(login(STUDENT_EMAIL, STUDENT_PASSWORD)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "subject": "Quiz inaccessible",
                                  "description": "Le quiz du cours Java renvoie une erreur 500.",
                                  "category": "TECHNIQUE"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.subject").value("Quiz inaccessible"))
                .andExpect(jsonPath("$.status").value("OUVERTE"));
    }

    @Test
    @DisplayName("Un etudiant ne voit pas la reclamation d'un autre")
    void studentCannotSeeOthers() throws Exception {
        long id = createComplaint();

        mockMvc.perform(get("/api/complaints/" + id)
                        .header("Authorization", bearer(login(OTHER_EMAIL, OTHER_PASSWORD))))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Un administrateur traite et cloture une reclamation")
    void adminResolvesComplaint() throws Exception {
        long id = createComplaint();

        mockMvc.perform(put("/api/complaints/" + id + "/status")
                        .header("Authorization", bearer(login(ADMIN_EMAIL, ADMIN_PASSWORD)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "RESOLUE",
                                  "adminResponse": "Le probleme a ete corrige cote serveur."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLUE"))
                .andExpect(jsonPath("$.adminResponse").value(
                        "Le probleme a ete corrige cote serveur."))
                .andExpect(jsonPath("$.handledBy.email").value(ADMIN_EMAIL));
    }

    @Test
    @DisplayName("Un etudiant ne peut pas changer le statut (403)")
    void studentCannotUpdateStatus() throws Exception {
        long id = createComplaint();

        mockMvc.perform(put("/api/complaints/" + id + "/status")
                        .header("Authorization", bearer(login(STUDENT_EMAIL, STUDENT_PASSWORD)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "status": "RESOLUE", "adminResponse": "Pirate" }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("L'auteur peut supprimer une reclamation encore ouverte")
    void authorDeletesOpenComplaint() throws Exception {
        long id = createComplaint();

        mockMvc.perform(delete("/api/complaints/" + id)
                        .header("Authorization", bearer(login(STUDENT_EMAIL, STUDENT_PASSWORD))))
                .andExpect(status().isNoContent());
    }

    private long createComplaint() throws Exception {
        String body = mockMvc.perform(post("/api/complaints")
                        .header("Authorization", bearer(login(STUDENT_EMAIL, STUDENT_PASSWORD)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "subject": "Quiz inaccessible",
                                  "description": "Le quiz du cours Java renvoie une erreur 500.",
                                  "category": "TECHNIQUE"
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
