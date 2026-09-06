package br.com.longobucco.personal_finance_app.core.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class User {

    private UUID id;
    private String name;
    private String email;
    private String password;
    private Role role;
    private BigDecimal balance;
    private final List<Transaction> transactions = new ArrayList<>();
    private LocalDate createdAt;
    private LocalDate updatedAt;
    public enum Role {
        ADMIN, USER;
    }

    private User(UUID id, String name, String email, String password, Role role, BigDecimal balance,
                 LocalDate createdAt, LocalDate updatedAt) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = role;
        this.balance = balance;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public Role getRole() {
        return role;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }

    public LocalDate getCreatedAt() {
        return createdAt;
    }

    public LocalDate getUpdatedAt() {
        return updatedAt;
    }

    public void addTransaction(Transaction transaction) {
        this.transactions.add(transaction);
    }

    public static User createUser(String name, String email, String password, BigDecimal balance,
                                 LocalDate createdAt, LocalDate updatedAt) {
        UUID id = UUID.randomUUID();
        Role role = Role.USER;
        return new User(id, name, email, password, role, balance, createdAt, updatedAt);
    }


    public static User createAdmin(String name, String email, String password, BigDecimal balance,
                                   LocalDate createdAt, LocalDate updatedAt){
        UUID id = UUID.randomUUID();
        Role role = Role.ADMIN;
        return new User(id, name, email, password, role, balance, createdAt, updatedAt);
    }

    public static User recover(UUID id, String name, String email, String password, Role role, BigDecimal balance,
                               LocalDate createdAt, LocalDate updatedAt){
        return new User(id, name, email, password, role, balance, createdAt, updatedAt);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(id, user.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
