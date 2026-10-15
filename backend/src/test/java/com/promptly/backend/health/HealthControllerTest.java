package com.promptly.backend.health;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Tests de l'endpoint /api/health.
 *
 * @WebMvcTest ne demarre que la couche web : aucune connexion MySQL reelle
 * n'est necessaire, le DataSource est remplace par un mock. Les tests peuvent
 * donc s'executer meme si le serveur MySQL est arrete.
 */
@WebMvcTest(HealthController.class)
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DataSource dataSource;

    @Test
    @DisplayName("Renvoie UP quand la connexion a la base est valide")
    void shouldReturnUpWhenDatabaseIsReachable() throws Exception {
        Connection connection = org.mockito.Mockito.mock(Connection.class);
        given(dataSource.getConnection()).willReturn(connection);
        given(connection.isValid(anyInt())).willReturn(true);

        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.application").value("Promptly"))
                .andExpect(jsonPath("$.database").value("UP"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("Renvoie DOWN pour la base sans faire echouer l'API")
    void shouldReportDatabaseDownWithoutFailing() throws Exception {
        given(dataSource.getConnection()).willThrow(new SQLException("Connection refused"));

        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.database").value("DOWN: Connection refused"));
    }
}
