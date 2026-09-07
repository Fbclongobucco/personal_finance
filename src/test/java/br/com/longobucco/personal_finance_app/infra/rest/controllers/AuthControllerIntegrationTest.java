package br.com.longobucco.personal_finance_app.infra.rest.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerIntegrationTest extends AbstractControllerIntegrationTest {

    @Test
    void loginWithValidAdminCredentialsReturnsTokenPair() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"admin@personalfinance.local","password":"admin123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty());
    }

    @Test
    void loginWithWrongPasswordReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"admin@personalfinance.local","password":"wrong-password"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginWithUnknownEmailReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"nobody-%s@example.com","password":"whatever"}
                                """.formatted(uniqueSuffix())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginWithInvalidEmailFormatReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"not-an-email","password":"admin123"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void refreshWithValidRefreshTokenReturnsNewAccessToken() throws Exception {
        String loginBody = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"admin@personalfinance.local","password":"admin123"}
                                """))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String refreshToken = objectMapper.readTree(loginBody).get("refreshToken").asText();

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshRequestBody(refreshToken))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").value(refreshToken));
    }

    @Test
    void refreshWithAnAccessTokenInsteadOfRefreshTokenReturnsUnauthorized() throws Exception {
        String accessToken = adminAccessToken();

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshRequestBody(accessToken))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshWithGarbageTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"not.a.jwt"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    private record RefreshRequestBody(String refreshToken) {
    }
}
