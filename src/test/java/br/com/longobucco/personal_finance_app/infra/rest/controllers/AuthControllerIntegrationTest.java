package br.com.longobucco.personal_finance_app.infra.rest.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerIntegrationTest extends AbstractControllerIntegrationTest {

    @Test
    void registerWithoutTokenCreatesRegularUser() throws Exception {
        String email = "signup-%s@example.com".formatted(uniqueSuffix());

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Self Signup","email":"%s","password":"secret123","phone":"11987654321","initialBalance":0}
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.password").doesNotExist());

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"secret123"}
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test
    void registerWithDuplicateEmailReturnsConflict() throws Exception {
        String email = "dupsignup-%s@example.com".formatted(uniqueSuffix());
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"First","email":"%s","password":"secret123","phone":"11987654321","initialBalance":0}
                                """.formatted(email)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Second","email":"%s","password":"secret123","phone":"11987654321","initialBalance":0}
                                """.formatted(email)))
                .andExpect(status().isConflict());
    }

    @Test
    void registerWithBlankNameReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"","email":"blanksignup-%s@example.com","password":"secret123","phone":"11987654321","initialBalance":0}
                                """.formatted(uniqueSuffix())))
                .andExpect(status().isBadRequest());
    }

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
