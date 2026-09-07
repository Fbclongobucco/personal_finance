package br.com.longobucco.personal_finance_app.infra.rest.controllers;

import tools.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CategoryControllerIntegrationTest extends AbstractControllerIntegrationTest {

    @Test
    void createCategoryWhenAuthenticatedReturnsCreated() throws Exception {
        String adminToken = adminAccessToken();
        JsonNode user = createUser(adminToken, "Creator", "creator-%s@example.com".formatted(uniqueSuffix()));
        String userToken = login(user.get("email").asText(), "secret123");
        String name = "Salary-%s".formatted(uniqueSuffix());

        mockMvc.perform(post("/api/categories")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s","type":"INCOME"}
                                """.formatted(name)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(name))
                .andExpect(jsonPath("$.type").value("INCOME"))
                .andExpect(jsonPath("$.userId").value(user.get("id").asText()));
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
    void getByIdAsOwnerReturnsCategory() throws Exception {
        String adminToken = adminAccessToken();
        JsonNode user = createUser(adminToken, "Finder", "finder-%s@example.com".formatted(uniqueSuffix()));
        String userToken = login(user.get("email").asText(), "secret123");
        UUID id = createCategory(userToken, "Findable-%s".formatted(uniqueSuffix()), "EXPENSE");

        mockMvc.perform(get("/api/categories/" + id)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    void getByIdAsAdminIsAllowedViewOnly() throws Exception {
        String adminToken = adminAccessToken();
        JsonNode user = createUser(adminToken, "Finder2", "finder2-%s@example.com".formatted(uniqueSuffix()));
        String userToken = login(user.get("email").asText(), "secret123");
        UUID id = createCategory(userToken, "Findable2-%s".formatted(uniqueSuffix()), "EXPENSE");

        mockMvc.perform(get("/api/categories/" + id)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    void getByIdForAnotherRegularUsersCategoryIsNotFound() throws Exception {
        String adminToken = adminAccessToken();
        JsonNode owner = createUser(adminToken, "Owner", "catowner-%s@example.com".formatted(uniqueSuffix()));
        String ownerToken = login(owner.get("email").asText(), "secret123");
        UUID id = createCategory(ownerToken, "Private-%s".formatted(uniqueSuffix()), "EXPENSE");
        JsonNode intruder = createUser(adminToken, "Intruder", "catintruder-%s@example.com".formatted(uniqueSuffix()));
        String intruderToken = login(intruder.get("email").asText(), "secret123");

        mockMvc.perform(get("/api/categories/" + id)
                        .header("Authorization", "Bearer " + intruderToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteAnotherRegularUsersCategoryIsNotFound() throws Exception {
        String adminToken = adminAccessToken();
        JsonNode owner = createUser(adminToken, "Owner4", "catowner4-%s@example.com".formatted(uniqueSuffix()));
        String ownerToken = login(owner.get("email").asText(), "secret123");
        UUID id = createCategory(ownerToken, "Hidden-%s".formatted(uniqueSuffix()), "EXPENSE");
        JsonNode intruder = createUser(adminToken, "Intruder4", "catintruder4-%s@example.com".formatted(uniqueSuffix()));
        String intruderToken = login(intruder.get("email").asText(), "secret123");

        mockMvc.perform(delete("/api/categories/" + id)
                        .header("Authorization", "Bearer " + intruderToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void newUserStartsWithDefaultCategoriesOfBothTypes() throws Exception {
        String adminToken = adminAccessToken();
        JsonNode user = createUser(adminToken, "Starter", "starter-%s@example.com".formatted(uniqueSuffix()));
        String userToken = login(user.get("email").asText(), "secret123");

        mockMvc.perform(get("/api/categories").param("userId", user.get("id").asText())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.type=='INCOME')]").exists())
                .andExpect(jsonPath("$[?(@.type=='EXPENSE')]").exists());
    }

    @Test
    void createCategoryWithADuplicateNameAndTypeReturnsConflict() throws Exception {
        String adminToken = adminAccessToken();
        JsonNode user = createUser(adminToken, "Duplicator", "dupcat-%s@example.com".formatted(uniqueSuffix()));
        String userToken = login(user.get("email").asText(), "secret123");
        String name = "Repeated-%s".formatted(uniqueSuffix());
        createCategory(userToken, name, "EXPENSE");

        mockMvc.perform(post("/api/categories")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s","type":"EXPENSE"}
                                """.formatted(name)))
                .andExpect(status().isConflict());
    }

    @Test
    void twoUsersMayEachHaveACategoryWithTheSameName() throws Exception {
        String adminToken = adminAccessToken();
        String name = "Shared-%s".formatted(uniqueSuffix());
        String firstToken = createUserAndLogin(adminToken, "First",
                "sharedcat1-%s@example.com".formatted(uniqueSuffix()));
        String secondToken = createUserAndLogin(adminToken, "Second",
                "sharedcat2-%s@example.com".formatted(uniqueSuffix()));
        createCategory(firstToken, name, "EXPENSE");

        mockMvc.perform(post("/api/categories")
                        .header("Authorization", "Bearer " + secondToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s","type":"EXPENSE"}
                                """.formatted(name)))
                .andExpect(status().isCreated());
    }

    @Test
    void renameCategoryAsOwnerUpdatesTheName() throws Exception {
        String adminToken = adminAccessToken();
        JsonNode user = createUser(adminToken, "Renamer", "renamer-%s@example.com".formatted(uniqueSuffix()));
        String userToken = login(user.get("email").asText(), "secret123");
        UUID id = createCategory(userToken, "OldName-%s".formatted(uniqueSuffix()), "EXPENSE");
        String newName = "NewName-%s".formatted(uniqueSuffix());

        mockMvc.perform(put("/api/categories/" + id)
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s"}
                                """.formatted(newName)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(newName))
                .andExpect(jsonPath("$.type").value("EXPENSE"));

        mockMvc.perform(get("/api/categories/" + id)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(newName));
    }

    @Test
    void renameCategoryWithBlankNameReturnsBadRequest() throws Exception {
        String adminToken = adminAccessToken();
        String userToken = createUserAndLogin(adminToken, "Renamer2",
                "renamer2-%s@example.com".formatted(uniqueSuffix()));
        UUID id = createCategory(userToken, "Renameable-%s".formatted(uniqueSuffix()), "EXPENSE");

        mockMvc.perform(put("/api/categories/" + id)
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":""}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void renameAnotherUsersCategoryIsForbiddenEvenForAdmin() throws Exception {
        String adminToken = adminAccessToken();
        JsonNode owner = createUser(adminToken, "Owner5", "catowner5-%s@example.com".formatted(uniqueSuffix()));
        String ownerToken = login(owner.get("email").asText(), "secret123");
        UUID id = createCategory(ownerToken, "Untouchable-%s".formatted(uniqueSuffix()), "EXPENSE");

        mockMvc.perform(put("/api/categories/" + id)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Renamed-%s"}
                                """.formatted(uniqueSuffix())))
                .andExpect(status().isForbidden());
    }

    @Test
    void listCategoriesFiltersByType() throws Exception {
        String adminToken = adminAccessToken();
        JsonNode user = createUser(adminToken, "Lister", "catlister-%s@example.com".formatted(uniqueSuffix()));
        String userToken = login(user.get("email").asText(), "secret123");
        String userId = user.get("id").asText();
        String incomeName = "Income-%s".formatted(uniqueSuffix());
        createCategory(userToken, incomeName, "INCOME");
        String expenseName = "Expense-%s".formatted(uniqueSuffix());
        createCategory(userToken, expenseName, "EXPENSE");

        mockMvc.perform(get("/api/categories").param("userId", userId).param("type", "INCOME")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name=='" + incomeName + "')]").exists())
                .andExpect(jsonPath("$[?(@.name=='" + expenseName + "')]").doesNotExist());
    }

    @Test
    void listCategoriesAsAnotherRegularUserIsForbidden() throws Exception {
        String adminToken = adminAccessToken();
        JsonNode owner = createUser(adminToken, "Owner2", "catowner2-%s@example.com".formatted(uniqueSuffix()));
        JsonNode intruder = createUser(adminToken, "Intruder2", "catintruder2-%s@example.com".formatted(uniqueSuffix()));
        String intruderToken = login(intruder.get("email").asText(), "secret123");

        mockMvc.perform(get("/api/categories").param("userId", owner.get("id").asText())
                        .header("Authorization", "Bearer " + intruderToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteCategoryRemovesItAndSubsequentGetIsNotFound() throws Exception {
        String adminToken = adminAccessToken();
        JsonNode user = createUser(adminToken, "Deleter", "catdeleter-%s@example.com".formatted(uniqueSuffix()));
        String userToken = login(user.get("email").asText(), "secret123");
        UUID id = createCategory(userToken, "Deletable-%s".formatted(uniqueSuffix()), "EXPENSE");

        mockMvc.perform(delete("/api/categories/" + id)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/categories/" + id)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteCategoryWithoutTokenIsForbidden() throws Exception {
        mockMvc.perform(delete("/api/categories/00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteAnotherUsersCategoryIsForbiddenEvenForAdmin() throws Exception {
        String adminToken = adminAccessToken();
        JsonNode owner = createUser(adminToken, "Owner3", "catowner3-%s@example.com".formatted(uniqueSuffix()));
        String ownerToken = login(owner.get("email").asText(), "secret123");
        UUID id = createCategory(ownerToken, "Protected-%s".formatted(uniqueSuffix()), "EXPENSE");

        mockMvc.perform(delete("/api/categories/" + id)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteCategoryStillReferencedByATransactionReturnsConflict() throws Exception {
        String adminToken = adminAccessToken();
        JsonNode user = createUser(adminToken, "CategoryUser", "categoryuser-%s@example.com".formatted(uniqueSuffix()));
        String userToken = login(user.get("email").asText(), "secret123");
        UUID categoryId = createCategory(userToken, "InUse-%s".formatted(uniqueSuffix()), "EXPENSE");
        createTransaction(userToken, categoryId, "Rent", "30.00", UUID.fromString(user.get("id").asText()), "CASH");

        mockMvc.perform(delete("/api/categories/" + categoryId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isConflict());
    }

    @Test
    void searchWithoutUserIdReturnsTheCallersOwnCategories() throws Exception {
        String adminToken = adminAccessToken();
        JsonNode user = createUser(adminToken, "Selfie", "selfcat-%s@example.com".formatted(uniqueSuffix()));
        String userToken = login(user.get("email").asText(), "secret123");
        String own = "Mine-%s".formatted(uniqueSuffix());
        createCategory(userToken, own, "EXPENSE");

        mockMvc.perform(get("/api/categories")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name=='" + own + "')]").exists())
                .andExpect(jsonPath("$[?(@.userId!='" + user.get("id").asText() + "')]").doesNotExist());
    }

    @Test
    void searchByNameMatchesAFragmentIgnoringCase() throws Exception {
        String adminToken = adminAccessToken();
        JsonNode user = createUser(adminToken, "Searcher", "searchcat-%s@example.com".formatted(uniqueSuffix()));
        String userToken = login(user.get("email").asText(), "secret123");
        String suffix = uniqueSuffix();
        String wanted = "Mercado-%s".formatted(suffix);
        String other = "Cinema-%s".formatted(suffix);
        createCategory(userToken, wanted, "EXPENSE");
        createCategory(userToken, other, "EXPENSE");

        mockMvc.perform(get("/api/categories")
                        .param("userId", user.get("id").asText())
                        .param("name", "mercado-" + suffix)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value(wanted));
    }

    @Test
    void searchByNameCombinesWithTheTypeFilter() throws Exception {
        String adminToken = adminAccessToken();
        JsonNode user = createUser(adminToken, "Combiner", "combocat-%s@example.com".formatted(uniqueSuffix()));
        String userToken = login(user.get("email").asText(), "secret123");
        String suffix = uniqueSuffix();
        createCategory(userToken, "Bonus-%s".formatted(suffix), "INCOME");
        createCategory(userToken, "Bonus-%s-gasto".formatted(suffix), "EXPENSE");

        mockMvc.perform(get("/api/categories")
                        .param("userId", user.get("id").asText())
                        .param("name", "bonus-" + suffix)
                        .param("type", "INCOME")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].type").value("INCOME"));
    }

    @Test
    void searchByNameThatMatchesNothingReturnsAnEmptyList() throws Exception {
        String adminToken = adminAccessToken();
        String userToken = createUserAndLogin(adminToken, "Empty",
                "emptycat-%s@example.com".formatted(uniqueSuffix()));

        mockMvc.perform(get("/api/categories")
                        .param("name", "nao-existe-%s".formatted(uniqueSuffix()))
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void adminCanSearchSeveralUsersAtOnce() throws Exception {
        String adminToken = adminAccessToken();
        String suffix = uniqueSuffix();
        JsonNode first = createUser(adminToken, "Multi1", "multicat1-%s@example.com".formatted(suffix));
        JsonNode second = createUser(adminToken, "Multi2", "multicat2-%s@example.com".formatted(suffix));
        String firstToken = login(first.get("email").asText(), "secret123");
        String secondToken = login(second.get("email").asText(), "secret123");
        createCategory(firstToken, "Compartilhada-%s".formatted(suffix), "EXPENSE");
        createCategory(secondToken, "Compartilhada-%s".formatted(suffix), "EXPENSE");

        mockMvc.perform(get("/api/categories")
                        .param("userId", first.get("id").asText())
                        .param("userId", second.get("id").asText())
                        .param("name", "compartilhada-" + suffix)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[?(@.userId=='" + first.get("id").asText() + "')]").exists())
                .andExpect(jsonPath("$[?(@.userId=='" + second.get("id").asText() + "')]").exists());
    }

    @Test
    void regularUserCannotSearchAListThatIncludesAnotherUser() throws Exception {
        String adminToken = adminAccessToken();
        JsonNode owner = createUser(adminToken, "Owner6", "catowner6-%s@example.com".formatted(uniqueSuffix()));
        JsonNode intruder = createUser(adminToken, "Intruder6", "catintruder6-%s@example.com".formatted(uniqueSuffix()));
        String intruderToken = login(intruder.get("email").asText(), "secret123");

        mockMvc.perform(get("/api/categories")
                        .param("userId", intruder.get("id").asText())
                        .param("userId", owner.get("id").asText())
                        .header("Authorization", "Bearer " + intruderToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void allUsersSearchIsAllowedForAdmin() throws Exception {
        String adminToken = adminAccessToken();
        String suffix = uniqueSuffix();
        String userToken = createUserAndLogin(adminToken, "Global", "globalcat-%s@example.com".formatted(suffix));
        String name = "Global-%s".formatted(suffix);
        createCategory(userToken, name, "EXPENSE");

        mockMvc.perform(get("/api/categories")
                        .param("allUsers", "true")
                        .param("name", name)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name=='" + name + "')]").exists());
    }

    @Test
    void allUsersSearchIsForbiddenForARegularUser() throws Exception {
        String adminToken = adminAccessToken();
        String userToken = createUserAndLogin(adminToken, "NotAdmin",
                "notadmincat-%s@example.com".formatted(uniqueSuffix()));

        mockMvc.perform(get("/api/categories")
                        .param("allUsers", "true")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void categoriesAreOrderedByTypeThenName() throws Exception {
        String adminToken = adminAccessToken();
        JsonNode user = createUser(adminToken, "Sorter", "sortcat-%s@example.com".formatted(uniqueSuffix()));
        String userToken = login(user.get("email").asText(), "secret123");

        String body = mockMvc.perform(get("/api/categories")
                        .param("userId", user.get("id").asText())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode categories = objectMapper.readTree(body);
        String previous = null;
        for (JsonNode category : categories) {
            String key = category.get("type").asText() + "\u0000" + category.get("name").asText();
            if (previous != null) {
                assertTrue(previous.compareTo(key) <= 0,
                        "out of order: " + previous + " came before " + key);
            }
            previous = key;
        }
    }
}
