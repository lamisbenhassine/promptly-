package com.promptly.backend.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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

/**
 * Tests d'integration de l'authentification et des regles d'autorisation.
 *
 * Contrairement aux tests {@code @WebMvcTest}, ceux-ci demarrent le contexte
 * Spring complet avec la configuration de securite reelle et une base H2 en
 * memoire. Ils verifient donc le comportement effectif de la chaine de filtres.
 *
 * <p>Les cas d'<b>acces refuse</b> sont testes au moins autant que les cas de
 * succes : dans un module de securite, une regle qui autorise par erreur est
 * plus grave qu'une regle qui refuse par erreur.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SecurityRulesIntegrationTest {

    private static final String ADMIN_EMAIL = "admin@test.local";
    private static final String ADMIN_PASSWORD = "AdminTest123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Cree l'unique administrateur utilise par les tests.
     *
     * L'etat initial est ainsi explicite plutot que dependant de
     * {@code AdminInitializer}, lui-meme desactive sous le profil "test". La
     * transaction de test etant annulee a la fin de chaque methode, chaque test
     * repart d'une base identique contenant exactement un administrateur actif.
     */
    @BeforeEach
    void createAdminAccount() {
        if (userRepository.existsByEmail(ADMIN_EMAIL)) {
            return;
        }
        userRepository.saveAndFlush(new User(
                "Admin", "Test", ADMIN_EMAIL,
                passwordEncoder.encode(ADMIN_PASSWORD), Role.ADMIN));
    }

    // ------------------------------------------------------------------
    //  Outils
    // ------------------------------------------------------------------

    private String registerAndLogin(String email, String password) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "firstName": "Jean", "lastName": "Dupont",
                                  "email": "%s", "password": "%s" }
                                """.formatted(email, password)))
                .andExpect(status().isCreated());
        return login(email, password);
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

    // ------------------------------------------------------------------
    //  Routes publiques
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("Routes publiques")
    class PublicRoutes {

        @Test
        @DisplayName("/api/health reste accessible sans authentification")
        void healthIsPublic() throws Exception {
            mockMvc.perform(get("/api/health"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("UP"));
        }
    }

    // ------------------------------------------------------------------
    //  Inscription
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("Inscription")
    class Registration {

        @Test
        @DisplayName("Cree un compte avec le role ETUDIANT et ne renvoie pas le mot de passe")
        void shouldCreateStudentAccount() throws Exception {
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "firstName": "Lamis", "lastName": "Ben Hassine",
                                      "email": "lamis@promptly.local", "password": "MotDePasse1" }
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.email").value("lamis@promptly.local"))
                    .andExpect(jsonPath("$.role").value("ETUDIANT"))
                    .andExpect(jsonPath("$.enabled").value(true))
                    .andExpect(jsonPath("$.password").doesNotExist());
        }

        @Test
        @DisplayName("Ignore un role transmis par le client (prevention d'elevation de privileges)")
        void shouldIgnoreClientSuppliedRole() throws Exception {
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "firstName": "Pirate", "lastName": "Malveillant",
                                      "email": "pirate@promptly.local", "password": "MotDePasse1",
                                      "role": "ADMIN" }
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.role").value("ETUDIANT"));

            User created = userRepository.findByEmail("pirate@promptly.local").orElseThrow();
            assertThat(created.getRole()).isEqualTo(Role.ETUDIANT);
        }

        @Test
        @DisplayName("Refuse une adresse deja utilisee avec le code 409")
        void shouldRejectDuplicateEmail() throws Exception {
            String payload = """
                    { "firstName": "Jean", "lastName": "Dupont",
                      "email": "doublon@promptly.local", "password": "MotDePasse1" }
                    """;

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON).content(payload))
                    .andExpect(status().isCreated());

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON).content(payload))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409));
        }

        @Test
        @DisplayName("Refuse un mot de passe sans chiffre et detaille l'erreur par champ")
        void shouldRejectWeakPassword() throws Exception {
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "firstName": "Jean", "lastName": "Dupont",
                                      "email": "faible@promptly.local", "password": "sanschiffre" }
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.password").exists());
        }

        @Test
        @DisplayName("Refuse une adresse electronique mal formee")
        void shouldRejectInvalidEmail() throws Exception {
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "firstName": "Jean", "lastName": "Dupont",
                                      "email": "pas-une-adresse", "password": "MotDePasse1" }
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.email").exists());
        }
    }

    // ------------------------------------------------------------------
    //  Connexion
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("Connexion")
    class Login {

        @Test
        @DisplayName("Renvoie un jeton JWT pour des identifiants valides")
        void shouldReturnToken() throws Exception {
            String token = registerAndLogin("connexion@promptly.local", "MotDePasse1");
            assertThat(token).isNotBlank();
            // Un JWT comporte trois segments separes par des points.
            assertThat(token.split("\\.")).hasSize(3);
        }

        @Test
        @DisplayName("Refuse un mot de passe incorrect avec le code 401")
        void shouldRejectWrongPassword() throws Exception {
            registerAndLogin("mauvais@promptly.local", "MotDePasse1");

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "email": "mauvais@promptly.local", "password": "MauvaisPass1" }
                                    """))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Refuse la connexion d'un compte inexistant avec le code 401")
        void shouldRejectUnknownAccount() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "email": "fantome@promptly.local", "password": "MotDePasse1" }
                                    """))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Refuse la connexion d'un compte desactive")
        void shouldRejectDisabledAccount() throws Exception {
            registerAndLogin("desactive@promptly.local", "MotDePasse1");

            User user = userRepository.findByEmail("desactive@promptly.local").orElseThrow();
            user.setEnabled(false);
            userRepository.saveAndFlush(user);

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "email": "desactive@promptly.local", "password": "MotDePasse1" }
                                    """))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ------------------------------------------------------------------
    //  Acces aux routes protegees
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("Protection des routes")
    class ProtectedRoutes {

        @Test
        @DisplayName("Refuse l'acces a /api/auth/me sans jeton (401)")
        void shouldRejectMissingToken() throws Exception {
            mockMvc.perform(get("/api/auth/me"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401));
        }

        @Test
        @DisplayName("Refuse un jeton falsifie (401)")
        void shouldRejectForgedToken() throws Exception {
            mockMvc.perform(get("/api/auth/me")
                            .header("Authorization", "Bearer jeton.completement.invalide"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Autorise /api/auth/me avec un jeton valide")
        void shouldAcceptValidToken() throws Exception {
            String token = registerAndLogin("profil@promptly.local", "MotDePasse1");

            mockMvc.perform(get("/api/auth/me").header("Authorization", bearer(token)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value("profil@promptly.local"))
                    .andExpect(jsonPath("$.role").value("ETUDIANT"));
        }
    }

    // ------------------------------------------------------------------
    //  Restrictions par role
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("Restrictions par role")
    class RoleRestrictions {

        @Test
        @DisplayName("Un ETUDIANT ne peut pas lister les utilisateurs (403)")
        void studentCannotListUsers() throws Exception {
            String token = registerAndLogin("etudiant@promptly.local", "MotDePasse1");

            mockMvc.perform(get("/api/users").header("Authorization", bearer(token)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(403));
        }

        @Test
        @DisplayName("Un ETUDIANT ne peut pas modifier le role d'un autre utilisateur (403)")
        void studentCannotChangeRoles() throws Exception {
            String token = registerAndLogin("etudiant2@promptly.local", "MotDePasse1");
            Long adminId = userRepository.findByEmail(ADMIN_EMAIL).orElseThrow().getId();

            mockMvc.perform(put("/api/users/" + adminId + "/role")
                            .header("Authorization", bearer(token))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{ \"role\": \"ETUDIANT\" }"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Un ADMIN peut lister les utilisateurs (200)")
        void adminCanListUsers() throws Exception {
            String token = login(ADMIN_EMAIL, ADMIN_PASSWORD);

            mockMvc.perform(get("/api/users").header("Authorization", bearer(token)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray());
        }

        @Test
        @DisplayName("Un ADMIN peut promouvoir un etudiant en enseignant")
        void adminCanPromoteStudent() throws Exception {
            registerAndLogin("promu@promptly.local", "MotDePasse1");
            Long id = userRepository.findByEmail("promu@promptly.local").orElseThrow().getId();
            String adminToken = login(ADMIN_EMAIL, ADMIN_PASSWORD);

            mockMvc.perform(put("/api/users/" + id + "/role")
                            .header("Authorization", bearer(adminToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{ \"role\": \"ENSEIGNANT\" }"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.role").value("ENSEIGNANT"));
        }

        @Test
        @DisplayName("Un ADMIN ne peut pas se desactiver s'il est le dernier administrateur (400)")
        void adminCannotDisableLastAdmin() throws Exception {
            String adminToken = login(ADMIN_EMAIL, ADMIN_PASSWORD);
            Long adminId = userRepository.findByEmail(ADMIN_EMAIL).orElseThrow().getId();

            mockMvc.perform(patch("/api/users/" + adminId + "/status")
                            .header("Authorization", bearer(adminToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{ \"enabled\": false }"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(
                            org.hamcrest.Matchers.containsString("dernier administrateur")));
        }
    }

    // ------------------------------------------------------------------
    //  Modification de son propre compte
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("Gestion de son propre compte")
    class OwnAccount {

        @Test
        @DisplayName("Un utilisateur peut modifier son profil")
        void shouldUpdateOwnProfile() throws Exception {
            String token = registerAndLogin("maj@promptly.local", "MotDePasse1");

            mockMvc.perform(put("/api/users/me")
                            .header("Authorization", bearer(token))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{ \"firstName\": \"Nouveau\", \"lastName\": \"Nom\" }"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.firstName").value("Nouveau"))
                    .andExpect(jsonPath("$.email").value("maj@promptly.local"));
        }

        @Test
        @DisplayName("Le changement de mot de passe exige l'ancien mot de passe (400)")
        void shouldRequireCurrentPassword() throws Exception {
            String token = registerAndLogin("motdepasse@promptly.local", "MotDePasse1");

            mockMvc.perform(put("/api/users/me/password")
                            .header("Authorization", bearer(token))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "currentPassword": "MauvaisActuel1",
                                      "newPassword": "NouveauPass1" }
                                    """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Le mot de passe est change puis permet de se reconnecter")
        void shouldChangePasswordAndAllowLogin() throws Exception {
            String token = registerAndLogin("rotation@promptly.local", "MotDePasse1");

            mockMvc.perform(put("/api/users/me/password")
                            .header("Authorization", bearer(token))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "currentPassword": "MotDePasse1",
                                      "newPassword": "NouveauPass2" }
                                    """))
                    .andExpect(status().isNoContent());

            // L'ancien mot de passe ne fonctionne plus.
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "email": "rotation@promptly.local", "password": "MotDePasse1" }
                                    """))
                    .andExpect(status().isUnauthorized());

            // Le nouveau fonctionne.
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "email": "rotation@promptly.local", "password": "NouveauPass2" }
                                    """))
                    .andExpect(status().isOk());
        }
    }
}

