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

class TransactionControllerIntegrationTest extends AbstractControllerIntegrationTest {

    @Test
    void createTransactionWhenAuthenticatedReturnsCreated() throws Exception {
        String token = adminAccessToken();
        UUID categoryId = createCategory(token, "Salary-%s".formatted(uniqueSuffix()), "INCOME");
        JsonNode user = createUser(token, "Payer", "payer-%s@example.com".formatted(uniqueSuffix()));

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"Salary","categoryId":"%s","amount":1500.00,"userId":"%s","paymentMethod":"PIX"}
                                """.formatted(categoryId, user.get("id").asText())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description").value("Salary"))
                .andExpect(jsonPath("$.category.id").value(categoryId.toString()))
                .andExpect(jsonPath("$.userId").value(user.get("id").asText()));
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
    void createTransactionWithNegativeAmountReturnsBadRequest() throws Exception {
        String token = adminAccessToken();

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"Bad","categoryId":"00000000-0000-0000-0000-000000000000","amount":-10.00,"userId":"00000000-0000-0000-0000-000000000000","paymentMethod":"CASH"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createTransactionWithNonexistentUserReturnsNotFound() throws Exception {
        String token = adminAccessToken();
        UUID categoryId = createCategory(token, "Rent-%s".formatted(uniqueSuffix()), "EXPENSE");

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"Rent","categoryId":"%s","amount":30.00,"userId":"00000000-0000-0000-0000-000000000000","paymentMethod":"CASH"}
                                """.formatted(categoryId)))
                .andExpect(status().isNotFound());
    }

    @Test
    void createTransactionWithNonexistentCategoryReturnsNotFound() throws Exception {
        String token = adminAccessToken();
        JsonNode user = createUser(token, "Payer2", "payer2-%s@example.com".formatted(uniqueSuffix()));

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + token)
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
    void listByUserReturnsCreatedTransaction() throws Exception {
        String token = adminAccessToken();
        UUID categoryId = createCategory(token, "Salary-%s".formatted(uniqueSuffix()), "INCOME");
        JsonNode user = createUser(token, "Lister", "lister-%s@example.com".formatted(uniqueSuffix()));
        String userId = user.get("id").asText();

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"Salary","categoryId":"%s","amount":1500.00,"userId":"%s","paymentMethod":"PIX"}
                                """.formatted(categoryId, userId)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/transactions").param("userId", userId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].userId").value(userId));
    }

    @Test
    void listByUserWithoutTokenIsForbidden() throws Exception {
        mockMvc.perform(get("/api/transactions").param("userId", "00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteTransactionRemovesItAndSubsequentGetIsNotFound() throws Exception {
        String token = adminAccessToken();
        UUID categoryId = createCategory(token, "Rent-%s".formatted(uniqueSuffix()), "EXPENSE");
        JsonNode user = createUser(token, "Deletable", "deltx-%s@example.com".formatted(uniqueSuffix()));

        String body = mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"Rent","categoryId":"%s","amount":30.00,"userId":"%s","paymentMethod":"CASH"}
                                """.formatted(categoryId, user.get("id").asText())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String transactionId = objectMapper.readTree(body).get("id").asText();

        mockMvc.perform(delete("/api/transactions/" + transactionId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/transactions/" + transactionId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }
}
