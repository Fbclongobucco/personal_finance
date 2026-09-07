package br.com.longobucco.personal_finance_app.infra.rest.controllers;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Boots the full Spring context (real JPA repositories against the h2 profile's in-memory
 * database, real security filter chain) and drives every endpoint exclusively through MockMvc, the
 * same way a real HTTP client would. The h2 database is shared across all test classes in this
 * run (Spring caches the context), so every test creates its own uniquely-named fixtures instead
 * of assuming a clean database.
 */
@SpringBootTest
@AutoConfigureMockMvc
abstract class AbstractControllerIntegrationTest {

    private static final String ADMIN_EMAIL = "admin@personalfinance.local";
    private static final String ADMIN_PASSWORD = "admin123";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    protected String uniqueSuffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    protected String adminAccessToken() throws Exception {
        return login(ADMIN_EMAIL, ADMIN_PASSWORD);
    }

    protected String login(String email, String password) throws Exception {
        String body = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, password))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("accessToken").asText();
    }

    protected UUID createCategory(String token, String name, String type) throws Exception {
        String body = mockMvc.perform(post("/api/categories")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"%s\",\"type\":\"%s\"}".formatted(name, type)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(objectMapper.readTree(body).get("id").asText());
    }

    protected JsonNode createUser(String token, String name, String email) throws Exception {
        String body = mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s","email":"%s","password":"secret123","phone":"11987654321","initialBalance":100.00}
                                """.formatted(name, email)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body);
    }

    /** Creates a regular user (as admin) and returns their own access token. */
    protected String createUserAndLogin(String adminToken, String name, String email) throws Exception {
        createUser(adminToken, name, email);
        return login(email, "secret123");
    }

    protected JsonNode createTransaction(String token, UUID categoryId, String description, String amount,
                                         UUID userId, String paymentMethod) throws Exception {
        String body = mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"%s","categoryId":"%s","amount":%s,"userId":"%s","paymentMethod":"%s"}
                                """.formatted(description, categoryId, amount, userId, paymentMethod)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body);
    }

    private record LoginRequest(String email, String password) {
    }
}
