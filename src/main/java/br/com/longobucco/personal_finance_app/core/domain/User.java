package br.com.longobucco.personal_finance_app.core.domain;

import br.com.longobucco.personal_finance_app.core.exception.InvalidPeriodException;
import br.com.longobucco.personal_finance_app.core.exception.InvalidUserException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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
    private final List<Transaction> transactions = new ArrayList<>();
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

    public BigDecimal getBalance() {
        return balance;
    }

    public List<Transaction> getTransactions() {
        return List.copyOf(transactions);
    }

    public List<Transaction> getTransactionsBetween(LocalDateTime start, LocalDateTime end) {
        validatePeriod(start, end);
        return transactions.stream()
                .filter(transaction -> !transaction.getCreatedAt().isBefore(start) && !transaction.getCreatedAt().isAfter(end))
                .toList();
    }

    public List<Transaction> getTransactionsByTypeBetween(Category.Type type, LocalDateTime start, LocalDateTime end) {
        return getTransactionsBetween(start, end).stream()
                .filter(transaction -> transaction.getCategory().getType() == type)
                .toList();
    }

    public List<Transaction> getExpensesBetween(LocalDateTime start, LocalDateTime end) {
        return getTransactionsByTypeBetween(Category.Type.EXPENSE, start, end);
    }

    public List<Transaction> getIncomesBetween(LocalDateTime start, LocalDateTime end) {
        return getTransactionsByTypeBetween(Category.Type.INCOME, start, end);
    }

    public LocalDate getCreatedAt() {
        return createdAt;
    }

    public LocalDate getUpdatedAt() {
        return updatedAt;
    }

    public void addTransaction(Transaction transaction) {
        this.transactions.add(transaction);
        this.balance = transaction.getCategory().getType().apply(balance, transaction.getAmount());
    }

    public static User createUser(String name, String email, String phone, String password, BigDecimal initialBalance,
                                 LocalDate createdAt, LocalDate updatedAt) {
        UUID id = UUID.randomUUID();
        Role role = Role.USER;
        return new User(id, name, email, phone, password, role, initialBalance, createdAt, updatedAt);
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

    private static void validatePeriod(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            throw InvalidPeriodException.nullBounds();
        }
        if (start.isAfter(end)) {
            throw InvalidPeriodException.startAfterEnd(start, end);
        }
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
