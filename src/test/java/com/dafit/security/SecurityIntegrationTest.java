package com.dafit.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Seguridad con la cadena de filtros real y JWT real.
 *
 * <p>Este es el unico test que enciende {@code dafit.security.enabled}; el resto
 * de la suite corre con el API abierto, igual que durante la integracion del
 * frontend.
 */
@SpringBootTest(properties = {
        "dafit.security.enabled=true",
        "dafit.security.jwt-secret=secreto-de-pruebas-con-mas-de-32-bytes-2026",
        "dafit.security.jwt-ttl-minutes=60"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void sinTokenElApiResponde401() throws Exception {
        mockMvc.perform(get("/api/exercises"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void elFlujoCompletoDeLoginYAccesoFunciona() throws Exception {
        UUID userId = registerUser();
        String email = uniqueEmail();

        mockMvc.perform(post("/api/users/{id}/credentials", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"secreto-123"}
                                """.formatted(email.toUpperCase())))
                .andExpect(status().isNoContent());

        MvcResult login = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"secreto-123"}
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andReturn();

        JsonNode json = objectMapper.readTree(login.getResponse().getContentAsString());
        String token = json.get("token").asText();
        assertThat(token).isNotBlank();

        mockMvc.perform(get("/api/users/{id}", userId))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/users/{id}", userId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Ana"));
    }

    @Test
    void unaContrasenaIncorrectaDevuelve401SinDistinguirElMotivo() throws Exception {
        UUID userId = registerUser();
        String email = uniqueEmail();
        setCredentials(userId, email, "secreto-123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"otra-cosa"}
                                """.formatted(email)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("credenciales invalidas"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"nadie-%s@example.com","password":"secreto-123"}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("credenciales invalidas"));
    }

    @Test
    void unTokenManipuladoNoAutentica() throws Exception {
        UUID userId = registerUser();
        setCredentials(userId, uniqueEmail(), "secreto-123");

        mockMvc.perform(get("/api/users/{id}", userId)
                        .header("Authorization", "Bearer token.inventado.falso"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void noSePuedenRegistrarDosCredencialesParaElMismoEmail() throws Exception {
        UUID first = registerUser();
        UUID second = registerUser();
        String email = uniqueEmail();
        setCredentials(first, email, "secreto-123");

        mockMvc.perform(post("/api/users/{id}/credentials", second)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"secreto-456"}
                                """.formatted(email)))
                .andExpect(status().isConflict());
    }

    @Test
    void unaContrasenaCortaSeRechazaAntesDeGuardar() throws Exception {
        UUID userId = registerUser();

        mockMvc.perform(post("/api/users/{id}/credentials", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"123"}
                                """.formatted(uniqueEmail())))
                .andExpect(status().isBadRequest());
    }

    // --- helpers -----------------------------------------------------------

    private UUID registerUser() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"displayName":"Ana","role":"ATHLETE","sex":"MALE",
                                 "bodyweightKg":74.20,"benchmarkConsent":true}
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return UUID.fromString(json.get("id").asText());
    }

    private void setCredentials(UUID userId, String email, String password) throws Exception {
        mockMvc.perform(post("/api/users/{id}/credentials", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(email, password)))
                .andExpect(status().isNoContent());
    }

    private static String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@example.com";
    }
}
