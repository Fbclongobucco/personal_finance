package br.com.longobucco.personal_finance_app.infra.rest.security;

import br.com.longobucco.personal_finance_app.core.domain.User;
import br.com.longobucco.personal_finance_app.core.repository.UserRepository;
import br.com.longobucco.personal_finance_app.core.security.PasswordHasher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

@Component
public class AdminBootstrap implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final String adminName;
    private final String adminEmail;
    private final String adminPhone;
    private final String adminPassword;

    public AdminBootstrap(UserRepository userRepository, PasswordHasher passwordHasher,
                          @Value("${app.admin.name}") String adminName,
                          @Value("${app.admin.email}") String adminEmail,
                          @Value("${app.admin.phone}") String adminPhone,
                          @Value("${app.admin.password}") String adminPassword) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.adminName = adminName;
        this.adminEmail = adminEmail;
        this.adminPhone = adminPhone;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.existsByEmail(adminEmail)) {
            return;
        }
        LocalDate today = LocalDate.now();
        User admin = User.createAdmin(adminName, adminEmail, adminPhone, passwordHasher.hash(adminPassword),
                BigDecimal.ZERO, today, today);
        userRepository.save(admin);
    }
}
