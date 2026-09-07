package br.com.longobucco.personal_finance_app.infra.rest.controllers;

import tools.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.UUID;

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
    void getByIdAsAnotherUserIsForbidden() throws Exception {
        String adminToken = adminAccessToken();
        JsonNode owner = createUser(adminToken, "Owner", "owner-%s@example.com".formatted(uniqueSuffix()));
        String intruderEmail = "intruder-%s@example.com".formatted(uniqueSuffix());
        createUser(adminToken, "Intruder", intruderEmail);
        String intruderToken = login(intruderEmail, "secret123");

        mockMvc.perform(get("/api/users/" + owner.get("id").asText())
                        .header("Authorization", "Bearer " + intruderToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void getByIdAsSelfIsAllowed() throws Exception {
        String adminToken = adminAccessToken();
        String selfEmail = "self-%s@example.com".formatted(uniqueSuffix());
        JsonNode self = createUser(adminToken, "Self", selfEmail);
        String selfToken = login(selfEmail, "secret123");

        mockMvc.perform(get("/api/users/" + self.get("id").asText())
                        .header("Authorization", "Bearer " + selfToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(self.get("id").asText()));
    }

    @Test
    void deleteUserAsAnotherUserIsForbidden() throws Exception {
        String adminToken = adminAccessToken();
        JsonNode owner = createUser(adminToken, "Owner2", "owner2-%s@example.com".formatted(uniqueSuffix()));
        String intruderEmail = "intruder2-%s@example.com".formatted(uniqueSuffix());
        createUser(adminToken, "Intruder2", intruderEmail);
        String intruderToken = login(intruderEmail, "secret123");

        mockMvc.perform(delete("/api/users/" + owner.get("id").asText())
                        .header("Authorization", "Bearer " + intruderToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void getTransactionsForAnotherUserIsForbidden() throws Exception {
        String adminToken = adminAccessToken();
        JsonNode owner = createUser(adminToken, "Owner3", "owner3-%s@example.com".formatted(uniqueSuffix()));
        String intruderEmail = "intruder3-%s@example.com".formatted(uniqueSuffix());
        createUser(adminToken, "Intruder3", intruderEmail);
        String intruderToken = login(intruderEmail, "secret123");

        mockMvc.perform(get("/api/users/" + owner.get("id").asText() + "/transactions")
                        .header("Authorization", "Bearer " + intruderToken))
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

    @Test
    void balanceReflectsIncomeTransactionAfterPersisting() throws Exception {
        String adminToken = adminAccessToken();
        UUID categoryId = createCategory(adminToken, "Salary-%s".formatted(uniqueSuffix()), "INCOME");
        JsonNode user = createUser(adminToken, "Earner", "earner-%s@example.com".formatted(uniqueSuffix()));
        String userToken = login(user.get("email").asText(), "secret123");
        UUID userId = UUID.fromString(user.get("id").asText());

        createTransaction(userToken, categoryId, "Salary", "50.00", userId, "PIX");

        mockMvc.perform(get("/api/users/" + userId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(150.00));
    }

    @Test
    void balanceReflectsExpenseTransactionAfterPersisting() throws Exception {
        String adminToken = adminAccessToken();
        UUID categoryId = createCategory(adminToken, "Rent-%s".formatted(uniqueSuffix()), "EXPENSE");
        JsonNode user = createUser(adminToken, "Payer3", "payer3-%s@example.com".formatted(uniqueSuffix()));
        String userToken = login(user.get("email").asText(), "secret123");
        UUID userId = UUID.fromString(user.get("id").asText());

        createTransaction(userToken, categoryId, "Rent", "30.00", userId, "CASH");

        mockMvc.perform(get("/api/users/" + userId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(70.00));
    }

    @Test
    void balanceIsReversedAndTransactionsSurviveAfterDeletingOneOfSeveral() throws Exception {
        String adminToken = adminAccessToken();
        UUID categoryId = createCategory(adminToken, "Salary-%s".formatted(uniqueSuffix()), "INCOME");
        JsonNode user = createUser(adminToken, "MultiTx", "multitx-%s@example.com".formatted(uniqueSuffix()));
        String userToken = login(user.get("email").asText(), "secret123");
        UUID userId = UUID.fromString(user.get("id").asText());

        createTransaction(userToken, categoryId, "Salary 1", "50.00", userId, "PIX");
        JsonNode second = createTransaction(userToken, categoryId, "Salary 2", "20.00", userId, "PIX");

        mockMvc.perform(get("/api/users/" + userId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(170.00));

        mockMvc.perform(delete("/api/transactions/" + second.get("id").asText())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/users/" + userId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(150.00));

        mockMvc.perform(get("/api/users/" + userId + "/transactions")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }
}
