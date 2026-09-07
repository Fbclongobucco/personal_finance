package br.com.longobucco.personal_finance_app.core.domain;

import br.com.longobucco.personal_finance_app.core.exception.InvalidUserException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

public class User {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[\\w.+-]+@[\\w-]+\\.[a-zA-Z]{2,}$");

    private final UUID id;
    private final String name;
    private final String email;
    private final String phone;
    private final String password;
    private final Role role;
    private BigDecimal balance;
    private final LocalDate createdAt;
    private final LocalDate updatedAt;
    public enum Role {
        ADMIN, USER;
    }

    private User(UUID id, String name, String email, String phone, String password, Role role, BigDecimal initialBalance,
                 LocalDate createdAt, LocalDate updatedAt) {
        validate(name, email, phone, password, initialBalance);
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.password = password;
        this.role = role;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.balance = initialBalance;
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

    public String getPhone() {
        return phone;
    }

    public String getPassword() {
        return password;
    }

    public Role getRole() {
        return role;
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public BigDecimal settledBalance(BigDecimal pendingExpenseTotal) {
        if (pendingExpenseTotal == null || pendingExpenseTotal.compareTo(BigDecimal.ZERO) < 0) {
            throw InvalidUserException.invalidPendingExpenseTotal(pendingExpenseTotal);
        }
        return balance.add(pendingExpenseTotal);
    }

    public LocalDate getCreatedAt() {
        return createdAt;
    }

    public LocalDate getUpdatedAt() {
        return updatedAt;
    }

    public void applyTransaction(Transaction transaction) {
        requireOwnTransaction(transaction);
        this.balance = transaction.getCategory().getType().apply(balance, transaction.getAmount());
    }

    public void reverseTransaction(Transaction transaction) {
        requireOwnTransaction(transaction);
        this.balance = transaction.getCategory().getType().reverse(balance, transaction.getAmount());
    }

    public static User createUser(String name, String email, String phone, String password, BigDecimal initialBalance) {
        UUID id = UUID.randomUUID();
        Role role = Role.USER;
        LocalDate createdAt = LocalDate.now();
        return new User(id, name, email, phone, password, role, initialBalance, createdAt, createdAt);
    }

    public static User createAdmin(String name, String email, String phone, String password, BigDecimal initialBalance,
                                   LocalDate createdAt, LocalDate updatedAt){
        UUID id = UUID.randomUUID();
        Role role = Role.ADMIN;
        return new User(id, name, email, phone, password, role, initialBalance, createdAt, updatedAt);
    }

    public static User recover(UUID id, String name, String email, String phone, String password, Role role, BigDecimal initialBalance,
                               LocalDate createdAt, LocalDate updatedAt){
        return new User(id, name, email, phone, password, role, initialBalance, createdAt, updatedAt);
    }

    private void requireOwnTransaction(Transaction transaction) {
        if (transaction == null || !id.equals(transaction.getUserId())) {
            throw InvalidUserException.foreignTransaction();
        }
    }

    private static void validate(String name, String email, String phone, String password, BigDecimal initialBalance) {
        if (name == null || name.isBlank()) {
            throw InvalidUserException.blankName();
        }
        if (email == null || !EMAIL_PATTERN.matcher(email).matches()) {
            throw InvalidUserException.invalidEmail(email);
        }
        if (phone == null || !isValidPhone(phone)) {
            throw InvalidUserException.invalidPhone(phone);
        }
        if (password == null || password.isBlank()) {
            throw InvalidUserException.blankPassword();
        }
        if (initialBalance == null) {
            throw InvalidUserException.nullInitialBalance();
        }
    }

    private static boolean isValidPhone(String phone) {
        String digits = phone.replaceAll("\\D", "");
        return digits.length() == 10 || digits.length() == 11;
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
