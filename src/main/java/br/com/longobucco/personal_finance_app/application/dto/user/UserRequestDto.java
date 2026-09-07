package br.com.longobucco.personal_finance_app.application.dto.user;

import java.math.BigDecimal;

public record UserRequestDto(String name, String email, String password, String phone, BigDecimal initialBalance) {}
