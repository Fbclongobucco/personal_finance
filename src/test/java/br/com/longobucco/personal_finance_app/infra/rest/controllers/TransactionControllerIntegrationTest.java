package br.com.longobucco.personal_finance_app.infra.rest.controllers;

import tools.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TransactionControllerIntegrationTest extends AbstractControllerIntegrationTest {

    @Test
    void createTransactionForSelfReturnsCreated() throws Exception {
        String adminToken = adminAccessToken();
        JsonNode user = createUser(adminToken, "Payer", "payer-%s@example.com".formatted(uniqueSuffix()));
        String userToken = login(user.get("email").asText(), "secret123");
        UUID categoryId = createCategory(userToken, "Salary-%s".formatted(uniqueSuffix()), "INCOME");

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"Salary","categoryId":"%s","amount":1500.00,"userId":"%s","paymentMethod":"PIX"}
                                """.formatted(categoryId, user.get("id").asText())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description").value("Salary"))
                .andExpect(jsonPath("$.category.id").value(categoryId.toString()))
                .andExpect(jsonPath("$.userId").value(user.get("id").asText()))
                .andExpect(jsonPath("$.paid").value(true));
    }

    @Test
    void createExpenseTransactionStartsUnpaid() throws Exception {
        String adminToken = adminAccessToken();
        JsonNode user = createUser(adminToken, "Payer5", "payer5-%s@example.com".formatted(uniqueSuffix()));
        String userToken = login(user.get("email").asText(), "secret123");
        UUID categoryId = createCategory(userToken, "Rent-%s".formatted(uniqueSuffix()), "EXPENSE");

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"Rent","categoryId":"%s","amount":900.00,"userId":"%s","paymentMethod":"PIX"}
                                """.formatted(categoryId, user.get("id").asText())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paid").value(false));
    }

    @Test
    void createTransactionWithoutTokenIsForbidden() throws Exception {
        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"NoToken","categoryId":"00000000-0000-0000-0000-000000000000","amount":10.00,"userId":"00000000-0000-0000-0000-000000000000","paymentMethod":"CASH"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void createTransactionForAnotherUserIsForbiddenEvenForAdmin() throws Exception {
        String adminToken = adminAccessToken();
        UUID categoryId = createCategory(adminToken, "Salary-%s".formatted(uniqueSuffix()), "INCOME");
        JsonNode owner = createUser(adminToken, "Owner", "owner-%s@example.com".formatted(uniqueSuffix()));

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"Salary","categoryId":"%s","amount":1500.00,"userId":"%s","paymentMethod":"PIX"}
                                """.formatted(categoryId, owner.get("id").asText())))
                .andExpect(status().isForbidden());
    }

    @Test
    void createTransactionWithNegativeAmountReturnsBadRequest() throws Exception {
        String adminToken = adminAccessToken();
        JsonNode user = createUser(adminToken, "Neg", "neg-%s@example.com".formatted(uniqueSuffix()));
        String userToken = login(user.get("email").asText(), "secret123");

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"Bad","categoryId":"00000000-0000-0000-0000-000000000000","amount":-10.00,"userId":"%s","paymentMethod":"CASH"}
                                """.formatted(user.get("id").asText())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createTransactionWithNonexistentCategoryReturnsNotFound() throws Exception {
        String adminToken = adminAccessToken();
        JsonNode user = createUser(adminToken, "Payer2", "payer2-%s@example.com".formatted(uniqueSuffix()));
        String userToken = login(user.get("email").asText(), "secret123");

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"Rent","categoryId":"00000000-0000-0000-0000-000000000000","amount":30.00,"userId":"%s","paymentMethod":"CASH"}
                                """.formatted(user.get("id").asText())))
                .andExpect(status().isNotFound());
    }

    @Test
    void getByIdWithoutTokenIsForbidden() throws Exception {
        mockMvc.perform(get("/api/transactions/00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getByIdReturnsNotFoundWhenMissing() throws Exception {
        String token = adminAccessToken();

        mockMvc.perform(get("/api/transactions/00000000-0000-0000-0000-000000000000")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void getByIdAsOwnerIsAllowed() throws Exception {
        String adminToken = adminAccessToken();
        JsonNode user = createUser(adminToken, "Owner2", "owner2-%s@example.com".formatted(uniqueSuffix()));
        String userToken = login(user.get("email").asText(), "secret123");
        UUID categoryId = createCategory(userToken, "Salary-%s".formatted(uniqueSuffix()), "INCOME");
        JsonNode transaction = createTransaction(userToken, categoryId, "Salary", "1500.00",
                UUID.fromString(user.get("id").asText()), "PIX");

        mockMvc.perform(get("/api/transactions/" + transaction.get("id").asText())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(transaction.get("id").asText()));
    }

    @Test
    void getByIdAsAdminIsAllowedViewOnly() throws Exception {
        String adminToken = adminAccessToken();
        JsonNode user = createUser(adminToken, "Owner6", "owner6-%s@example.com".formatted(uniqueSuffix()));
        String userToken = login(user.get("email").asText(), "secret123");
        UUID categoryId = createCategory(userToken, "Salary-%s".formatted(uniqueSuffix()), "INCOME");
        JsonNode transaction = createTransaction(userToken, categoryId, "Salary", "1500.00",
                UUID.fromString(user.get("id").asText()), "PIX");

        mockMvc.perform(get("/api/transactions/" + transaction.get("id").asText())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(transaction.get("id").asText()));
    }

    @Test
    void getByIdForAnotherRegularUsersTransactionIsNotFound() throws Exception {
        String adminToken = adminAccessToken();
        JsonNode owner = createUser(adminToken, "Owner3", "owner3-%s@example.com".formatted(uniqueSuffix()));
        String ownerToken = login(owner.get("email").asText(), "secret123");
        UUID categoryId = createCategory(ownerToken, "Salary-%s".formatted(uniqueSuffix()), "INCOME");
        JsonNode intruder = createUser(adminToken, "Intruder3", "intruder3-%s@example.com".formatted(uniqueSuffix()));
        String intruderToken = login(intruder.get("email").asText(), "secret123");
        JsonNode transaction = createTransaction(ownerToken, categoryId, "Salary", "1500.00",
                UUID.fromString(owner.get("id").asText()), "PIX");

        mockMvc.perform(get("/api/transactions/" + transaction.get("id").asText())
                        .header("Authorization", "Bearer " + intruderToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void createTransactionWithAnotherUsersCategoryReturnsNotFound() throws Exception {
        String adminToken = adminAccessToken();
        JsonNode owner = createUser(adminToken, "Owner9", "owner9-%s@example.com".formatted(uniqueSuffix()));
        String ownerToken = login(owner.get("email").asText(), "secret123");
        UUID othersCategoryId = createCategory(ownerToken, "Private-%s".formatted(uniqueSuffix()), "INCOME");
        JsonNode intruder = createUser(adminToken, "Intruder9", "intruder9-%s@example.com".formatted(uniqueSuffix()));
        String intruderToken = login(intruder.get("email").asText(), "secret123");

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + intruderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"Salary","categoryId":"%s","amount":100.00,"userId":"%s","paymentMethod":"PIX"}
                                """.formatted(othersCategoryId, intruder.get("id").asText())))
                .andExpect(status().isNotFound());
    }

    @Test
    void listByUserWithStartAfterEndReturnsBadRequest() throws Exception {
        String adminToken = adminAccessToken();
        JsonNode user = createUser(adminToken, "Ranger", "ranger-%s@example.com".formatted(uniqueSuffix()));
        String userToken = login(user.get("email").asText(), "secret123");

        mockMvc.perform(get("/api/transactions")
                        .param("userId", user.get("id").asText())
                        .param("start", "2026-02-01T00:00:00")
                        .param("end", "2026-01-01T00:00:00")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void settlingAnExpenseMovesItIntoTheSettledBalance() throws Exception {
        String adminToken = adminAccessToken();
        JsonNode user = createUser(adminToken, "Settler3", "settler3-%s@example.com".formatted(uniqueSuffix()));
        String userToken = login(user.get("email").asText(), "secret123");
        UUID userId = UUID.fromString(user.get("id").asText());
        UUID categoryId = createCategory(userToken, "Rent-%s".formatted(uniqueSuffix()), "EXPENSE");
        JsonNode transaction = createTransaction(userToken, categoryId, "Rent", "30.00", userId, "CASH");

        mockMvc.perform(get("/api/users/" + userId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(70.00))
                .andExpect(jsonPath("$.settledBalance").value(100.00));

        mockMvc.perform(patch("/api/transactions/" + transaction.get("id").asText() + "/settle")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/users/" + userId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(70.00))
                .andExpect(jsonPath("$.settledBalance").value(70.00));
    }

    @Test
    void listByUserReturnsCreatedTransaction() throws Exception {
        String adminToken = adminAccessToken();
        JsonNode user = createUser(adminToken, "Lister", "lister-%s@example.com".formatted(uniqueSuffix()));
        String userToken = login(user.get("email").asText(), "secret123");
        String userId = user.get("id").asText();
        UUID categoryId = createCategory(userToken, "Salary-%s".formatted(uniqueSuffix()), "INCOME");
        createTransaction(userToken, categoryId, "Salary", "1500.00", UUID.fromString(userId), "PIX");

        mockMvc.perform(get("/api/transactions").param("userId", userId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].userId").value(userId));
    }

    @Test
    void listByUserAsAdminIsAllowedViewOnly() throws Exception {
        String adminToken = adminAccessToken();
        JsonNode user = createUser(adminToken, "Lister2", "lister2-%s@example.com".formatted(uniqueSuffix()));
        String userToken = login(user.get("email").asText(), "secret123");
        String userId = user.get("id").asText();
        UUID categoryId = createCategory(userToken, "Salary-%s".formatted(uniqueSuffix()), "INCOME");
        createTransaction(userToken, categoryId, "Salary", "1500.00", UUID.fromString(userId), "PIX");

        mockMvc.perform(get("/api/transactions").param("userId", userId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void listByUserWithoutTokenIsForbidden() throws Exception {
        mockMvc.perform(get("/api/transactions").param("userId", "00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isForbidden());
    }

    @Test
    void listByUserAsAnotherRegularUserIsForbidden() throws Exception {
        String adminToken = adminAccessToken();
        JsonNode owner = createUser(adminToken, "Owner4", "owner4-%s@example.com".formatted(uniqueSuffix()));
        JsonNode intruder = createUser(adminToken, "Intruder4", "intruder4-%s@example.com".formatted(uniqueSuffix()));
        String intruderToken = login(intruder.get("email").asText(), "secret123");

        mockMvc.perform(get("/api/transactions").param("userId", owner.get("id").asText())
                        .header("Authorization", "Bearer " + intruderToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteTransactionRemovesItAndSubsequentGetIsNotFound() throws Exception {
        String adminToken = adminAccessToken();
        JsonNode user = createUser(adminToken, "Deletable", "deltx-%s@example.com".formatted(uniqueSuffix()));
        String userToken = login(user.get("email").asText(), "secret123");
        UUID categoryId = createCategory(userToken, "Rent-%s".formatted(uniqueSuffix()), "EXPENSE");
        JsonNode transaction = createTransaction(userToken, categoryId, "Rent", "30.00",
                UUID.fromString(user.get("id").asText()), "CASH");
        String transactionId = transaction.get("id").asText();

        mockMvc.perform(delete("/api/transactions/" + transactionId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/transactions/" + transactionId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteAnotherUsersTransactionIsForbiddenEvenForAdmin() throws Exception {
        String adminToken = adminAccessToken();
        JsonNode owner = createUser(adminToken, "Owner7", "owner7-%s@example.com".formatted(uniqueSuffix()));
        String ownerToken = login(owner.get("email").asText(), "secret123");
        UUID categoryId = createCategory(ownerToken, "Rent-%s".formatted(uniqueSuffix()), "EXPENSE");
        JsonNode transaction = createTransaction(ownerToken, categoryId, "Rent", "30.00",
                UUID.fromString(owner.get("id").asText()), "CASH");

        mockMvc.perform(delete("/api/transactions/" + transaction.get("id").asText())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void settleExpenseMarksItAsPaid() throws Exception {
        String adminToken = adminAccessToken();
        JsonNode user = createUser(adminToken, "Settler", "settler-%s@example.com".formatted(uniqueSuffix()));
        String userToken = login(user.get("email").asText(), "secret123");
        UUID categoryId = createCategory(userToken, "Rent-%s".formatted(uniqueSuffix()), "EXPENSE");
        JsonNode transaction = createTransaction(userToken, categoryId, "Rent", "30.00",
                UUID.fromString(user.get("id").asText()), "CASH");

        mockMvc.perform(patch("/api/transactions/" + transaction.get("id").asText() + "/settle")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paid").value(true));
    }

    @Test
    void settleExpenseAsAnotherUserIsForbiddenEvenForAdmin() throws Exception {
        String adminToken = adminAccessToken();
        JsonNode owner = createUser(adminToken, "Owner8", "owner8-%s@example.com".formatted(uniqueSuffix()));
        String ownerToken = login(owner.get("email").asText(), "secret123");
        UUID categoryId = createCategory(ownerToken, "Rent-%s".formatted(uniqueSuffix()), "EXPENSE");
        JsonNode transaction = createTransaction(ownerToken, categoryId, "Rent", "30.00",
                UUID.fromString(owner.get("id").asText()), "CASH");

        mockMvc.perform(patch("/api/transactions/" + transaction.get("id").asText() + "/settle")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void settleIncomeTransactionReturnsBadRequest() throws Exception {
        String adminToken = adminAccessToken();
        JsonNode user = createUser(adminToken, "Settler2", "settler2-%s@example.com".formatted(uniqueSuffix()));
        String userToken = login(user.get("email").asText(), "secret123");
        UUID categoryId = createCategory(userToken, "Salary-%s".formatted(uniqueSuffix()), "INCOME");
        JsonNode transaction = createTransaction(userToken, categoryId, "Salary", "1500.00",
                UUID.fromString(user.get("id").asText()), "PIX");

        mockMvc.perform(patch("/api/transactions/" + transaction.get("id").asText() + "/settle")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void settleWithoutTokenIsForbidden() throws Exception {
        mockMvc.perform(patch("/api/transactions/00000000-0000-0000-0000-000000000000/settle"))
                .andExpect(status().isForbidden());
    }
}
