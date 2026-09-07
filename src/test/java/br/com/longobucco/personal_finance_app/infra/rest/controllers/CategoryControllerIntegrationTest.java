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

class CategoryControllerIntegrationTest extends AbstractControllerIntegrationTest {

    @Test
    void createCategoryWhenAuthenticatedReturnsCreated() throws Exception {
        String token = adminAccessToken();
        String name = "Salary-%s".formatted(uniqueSuffix());

        mockMvc.perform(post("/api/categories")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s","type":"INCOME"}
                                """.formatted(name)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(name))
                .andExpect(jsonPath("$.type").value("INCOME"));
    }

    @Test
    void createCategoryWithoutTokenIsForbidden() throws Exception {
        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"NoToken-%s","type":"INCOME"}
                                """.formatted(uniqueSuffix())))
                .andExpect(status().isForbidden());
    }

    @Test
    void createCategoryAsNonAdminIsForbidden() throws Exception {
        String adminToken = adminAccessToken();
        String regularToken = createUserAndLogin(adminToken, "Regular", "regular-%s@example.com".formatted(uniqueSuffix()));

        mockMvc.perform(post("/api/categories")
                        .header("Authorization", "Bearer " + regularToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"NotAllowed-%s","type":"INCOME"}
                                """.formatted(uniqueSuffix())))
                .andExpect(status().isForbidden());
    }

    @Test
    void createCategoryWithBlankNameReturnsBadRequest() throws Exception {
        String token = adminAccessToken();

        mockMvc.perform(post("/api/categories")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"","type":"INCOME"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createCategoryWithNullTypeReturnsBadRequest() throws Exception {
        String token = adminAccessToken();

        mockMvc.perform(post("/api/categories")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"NoType-%s","type":null}
                                """.formatted(uniqueSuffix())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getByIdWithoutTokenIsForbidden() throws Exception {
        mockMvc.perform(get("/api/categories/00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getByIdReturnsNotFoundWhenMissing() throws Exception {
        String token = adminAccessToken();

        mockMvc.perform(get("/api/categories/00000000-0000-0000-0000-000000000000")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void getByIdReturnsCategoryWhenFound() throws Exception {
        String token = adminAccessToken();
        UUID id = createCategory(token, "Findable-%s".formatted(uniqueSuffix()), "EXPENSE");

        mockMvc.perform(get("/api/categories/" + id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    void listCategoriesFiltersByType() throws Exception {
        String token = adminAccessToken();
        String incomeName = "Income-%s".formatted(uniqueSuffix());
        createCategory(token, incomeName, "INCOME");
        String expenseName = "Expense-%s".formatted(uniqueSuffix());
        createCategory(token, expenseName, "EXPENSE");

        mockMvc.perform(get("/api/categories").param("type", "INCOME")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name=='" + incomeName + "')]").exists())
                .andExpect(jsonPath("$[?(@.name=='" + expenseName + "')]").doesNotExist());
    }

    @Test
    void deleteCategoryRemovesItAndSubsequentGetIsNotFound() throws Exception {
        String token = adminAccessToken();
        UUID id = createCategory(token, "Deletable-%s".formatted(uniqueSuffix()), "EXPENSE");

        mockMvc.perform(delete("/api/categories/" + id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/categories/" + id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteCategoryWithoutTokenIsForbidden() throws Exception {
        mockMvc.perform(delete("/api/categories/00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteCategoryAsNonAdminIsForbidden() throws Exception {
        String adminToken = adminAccessToken();
        UUID id = createCategory(adminToken, "Protected-%s".formatted(uniqueSuffix()), "EXPENSE");
        String regularToken = createUserAndLogin(adminToken, "Regular2", "regular2-%s@example.com".formatted(uniqueSuffix()));

        mockMvc.perform(delete("/api/categories/" + id)
                        .header("Authorization", "Bearer " + regularToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteCategoryStillReferencedByATransactionReturnsConflict() throws Exception {
        String adminToken = adminAccessToken();
        UUID categoryId = createCategory(adminToken, "InUse-%s".formatted(uniqueSuffix()), "EXPENSE");
        JsonNode user = createUser(adminToken, "CategoryUser", "categoryuser-%s@example.com".formatted(uniqueSuffix()));
        String userToken = login(user.get("email").asText(), "secret123");
        createTransaction(userToken, categoryId, "Rent", "30.00", UUID.fromString(user.get("id").asText()), "CASH");

        mockMvc.perform(delete("/api/categories/" + categoryId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isConflict());
    }
}
