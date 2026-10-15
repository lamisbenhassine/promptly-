package com.promptly.backend.course;

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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.promptly.backend.user.Role;
import com.promptly.backend.user.User;
import com.promptly.backend.user.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CourseRulesIntegrationTest {

    private static final String ADMIN_EMAIL = "admin@test.local";
    private static final String ADMIN_PASSWORD = "AdminTest123";
    private static final String TEACHER_EMAIL = "enseignant@test.local";
    private static final String TEACHER_PASSWORD = "Enseignant1";
    private static final String OTHER_TEACHER_EMAIL = "collegue@test.local";
    private static final String OTHER_TEACHER_PASSWORD = "Collegue1";
    private static final String STUDENT_EMAIL = "etudiant@test.local";
    private static final String STUDENT_PASSWORD = "Etudiant1";

    private static final String COURSE_JSON = """
            { "title": "Introduction a Java",
              "description": "Les bases du langage Java pour debuter.",
              "learningObjectives": "Ecrire une classe simple et compiler un programme.",
              "category": "JAVA",
              "published": %s }
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUpUsers() {
        saveUser("Admin", "Test", ADMIN_EMAIL, ADMIN_PASSWORD, Role.ADMIN);
        saveUser("Anne", "Enseignante", TEACHER_EMAIL, TEACHER_PASSWORD, Role.ENSEIGNANT);
        saveUser("Paul", "Collegue", OTHER_TEACHER_EMAIL, OTHER_TEACHER_PASSWORD, Role.ENSEIGNANT);
        saveUser("Lea", "Etudiante", STUDENT_EMAIL, STUDENT_PASSWORD, Role.ETUDIANT);
    }

    @Test
    @DisplayName("La liste des cours exige une authentification (401)")
    void listRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/courses")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Un etudiant ne peut pas creer un cours (403)")
    void studentCannotCreate() throws Exception {
        mockMvc.perform(post("/api/courses")
                        .header("Authorization", bearer(login(STUDENT_EMAIL, STUDENT_PASSWORD)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(COURSE_JSON.formatted(true)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Un enseignant cree un cours rattache a lui-meme")
    void teacherCreatesOwnCourse() throws Exception {
        mockMvc.perform(post("/api/courses")
                        .header("Authorization", bearer(login(TEACHER_EMAIL, TEACHER_PASSWORD)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(COURSE_JSON.formatted(true)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Introduction a Java"))
                .andExpect(jsonPath("$.category").value("JAVA"))
                .andExpect(jsonPath("$.instructor.email").value(TEACHER_EMAIL))
                .andExpect(jsonPath("$.published").value(true));
    }

    @Test
    @DisplayName("Un etudiant ne voit pas un brouillon (404)")
    void studentCannotSeeDraft() throws Exception {
        long draftId = createCourse(TEACHER_EMAIL, TEACHER_PASSWORD, false);

        mockMvc.perform(get("/api/courses/" + draftId)
                        .header("Authorization", bearer(login(STUDENT_EMAIL, STUDENT_PASSWORD))))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Un etudiant voit un cours publie")
    void studentSeesPublishedCourse() throws Exception {
        long id = createCourse(TEACHER_EMAIL, TEACHER_PASSWORD, true);

        mockMvc.perform(get("/api/courses/" + id)
                        .header("Authorization", bearer(login(STUDENT_EMAIL, STUDENT_PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));
    }

    @Test
    @DisplayName("La liste d'un etudiant n'inclut pas les brouillons")
    void studentListHidesDrafts() throws Exception {
        createCourse(TEACHER_EMAIL, TEACHER_PASSWORD, false);
        long publishedId = createCourse(TEACHER_EMAIL, TEACHER_PASSWORD, true);

        mockMvc.perform(get("/api/courses")
                        .header("Authorization", bearer(login(STUDENT_EMAIL, STUDENT_PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(publishedId));
    }

    @Test
    @DisplayName("Un enseignant voit son propre brouillon dans la liste")
    void teacherSeesOwnDraftInList() throws Exception {
        long draftId = createCourse(TEACHER_EMAIL, TEACHER_PASSWORD, false);

        mockMvc.perform(get("/api/courses")
                        .header("Authorization", bearer(login(TEACHER_EMAIL, TEACHER_PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(draftId));
    }

    @Test
    @DisplayName("Un enseignant ne peut pas modifier le cours d'un collegue (403)")
    void teacherCannotEditColleague() throws Exception {
        long id = createCourse(OTHER_TEACHER_EMAIL, OTHER_TEACHER_PASSWORD, true);

        mockMvc.perform(put("/api/courses/" + id)
                        .header("Authorization", bearer(login(TEACHER_EMAIL, TEACHER_PASSWORD)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(COURSE_JSON.formatted(true)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Un administrateur peut modifier le cours d'un enseignant")
    void adminCanEditAnyCourse() throws Exception {
        long id = createCourse(TEACHER_EMAIL, TEACHER_PASSWORD, true);

        mockMvc.perform(put("/api/courses/" + id)
                        .header("Authorization", bearer(login(ADMIN_EMAIL, ADMIN_PASSWORD)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "title": "Java avance",
                                  "description": "Les bases du langage Java pour debuter.",
                                  "learningObjectives": "Ecrire une classe simple et compiler un programme.",
                                  "category": "SPRING_BOOT",
                                  "published": true }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Java avance"))
                .andExpect(jsonPath("$.category").value("SPRING_BOOT"));
    }

    @Test
    @DisplayName("Un administrateur peut supprimer un cours (204)")
    void adminCanDelete() throws Exception {
        long id = createCourse(TEACHER_EMAIL, TEACHER_PASSWORD, true);

        mockMvc.perform(delete("/api/courses/" + id)
                        .header("Authorization", bearer(login(ADMIN_EMAIL, ADMIN_PASSWORD))))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/courses/" + id)
                        .header("Authorization", bearer(login(ADMIN_EMAIL, ADMIN_PASSWORD))))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Un titre trop court est refuse (400)")
    void rejectsShortTitle() throws Exception {
        mockMvc.perform(post("/api/courses")
                        .header("Authorization", bearer(login(TEACHER_EMAIL, TEACHER_PASSWORD)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "title": "J",
                                  "description": "Les bases du langage Java pour debuter.",
                                  "learningObjectives": "Ecrire une classe simple et compiler un programme.",
                                  "category": "JAVA",
                                  "published": true }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.title").exists());
    }

    @Test
    @DisplayName("Le filtre par categorie ne renvoie que les cours correspondants")
    void filterByCategory() throws Exception {
        createCourse(TEACHER_EMAIL, TEACHER_PASSWORD, true);
        mockMvc.perform(post("/api/courses")
                        .header("Authorization", bearer(login(TEACHER_EMAIL, TEACHER_PASSWORD)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "title": "Composants Angular",
                                  "description": "Construire une interface avec Angular.",
                                  "learningObjectives": "Creer un composant et le relier a un service.",
                                  "category": "ANGULAR",
                                  "published": true }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/courses").param("category", "ANGULAR")
                        .header("Authorization", bearer(login(STUDENT_EMAIL, STUDENT_PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].category").value("ANGULAR"));
    }

    private void saveUser(String first, String last, String email, String password, Role role) {
        if (!userRepository.existsByEmail(email)) {
            userRepository.saveAndFlush(new User(first, last, email, passwordEncoder.encode(password), role));
        }
    }

    private long createCourse(String email, String password, boolean published) throws Exception {
        String body = mockMvc.perform(post("/api/courses")
                        .header("Authorization", bearer(login(email, password)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(COURSE_JSON.formatted(published)))
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
