package br.com.longobucco.personal_finance_app.infra.rest.controllers;

import tools.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserControllerIntegrationTest extends AbstractControllerIntegrationTest {

    @Test
    void createUserAsAdminReturnsCreated() throws Exception {
        String token = adminAccessToken();
        String email = "john-%s@example.com".formatted(uniqueSuffix());

        mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"John Doe","email":"%s","password":"secret123","phone":"11987654321","initialBalance":100.00}
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void createUserWithoutTokenIsForbidden() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"John Doe","email":"noone-%s@example.com","password":"secret123","phone":"11987654321","initialBalance":0}
                                """.formatted(uniqueSuffix())))
                .andExpect(status().isForbidden());
    }

    @Test
    void createUserAsNonAdminIsForbidden() throws Exception {
        String adminToken = adminAccessToken();
        String regularEmail = "regular-%s@example.com".formatted(uniqueSuffix());
        createUser(adminToken, "Regular User", regularEmail);
        String regularToken = login(regularEmail, "secret123");

        mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + regularToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Jane Doe","email":"jane-%s@example.com","password":"secret123","phone":"11987654321","initialBalance":0}
                                """.formatted(uniqueSuffix())))
                .andExpect(status().isForbidden());
    }

    @Test
    void createUserWithBlankNameReturnsBadRequest() throws Exception {
        String token = adminAccessToken();

        mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"","email":"blank-%s@example.com","password":"secret123","phone":"11987654321","initialBalance":0}
                                """.formatted(uniqueSuffix())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createUserWithDuplicateEmailReturnsConflict() throws Exception {
        String token = adminAccessToken();
        String email = "dup-%s@example.com".formatted(uniqueSuffix());
        createUser(token, "First", email);

        mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Second","email":"%s","password":"secret123","phone":"11987654321","initialBalance":0}
                                """.formatted(email)))
                .andExpect(status().isConflict());
    }

    @Test
    void getByIdWithoutTokenIsForbidden() throws Exception {
        mockMvc.perform(get("/api/users/00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getByIdReturnsUserWhenAuthenticated() throws Exception {
        String token = adminAccessToken();
        JsonNode user = createUser(token, "Findable", "findable-%s@example.com".formatted(uniqueSuffix()));

        mockMvc.perform(get("/api/users/" + user.get("id").asText())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.get("id").asText()));
    }

    @Test
    void getByIdReturnsNotFoundWhenMissing() throws Exception {
        String token = adminAccessToken();

        mockMvc.perform(get("/api/users/00000000-0000-0000-0000-000000000000")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void getByEmailReturnsUserWhenAuthenticated() throws Exception {
        String token = adminAccessToken();
        String email = "byemail-%s@example.com".formatted(uniqueSuffix());
        createUser(token, "By Email", email);

        mockMvc.perform(get("/api/users").param("email", email)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));
    }

    @Test
    void deleteUserRemovesItAndSubsequentGetIsNotFound() throws Exception {
        String token = adminAccessToken();
        JsonNode user = createUser(token, "Deletable", "deletable-%s@example.com".formatted(uniqueSuffix()));
        String userId = user.get("id").asText();

        mockMvc.perform(delete("/api/users/" + userId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/users/" + userId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteUserWithoutTokenIsForbidden() throws Exception {
        mockMvc.perform(delete("/api/users/00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getTransactionsForNewUserReturnsEmptyList() throws Exception {
        String token = adminAccessToken();
        JsonNode user = createUser(token, "No Transactions", "notx-%s@example.com".formatted(uniqueSuffix()));

        mockMvc.perform(get("/api/users/" + user.get("id").asText() + "/transactions")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
