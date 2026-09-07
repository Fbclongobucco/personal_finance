package br.com.longobucco.personal_finance_app.infra.rest.security;

public record TokenResponse(String accessToken, String refreshToken) {}
