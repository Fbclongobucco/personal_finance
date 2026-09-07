package br.com.longobucco.personal_finance_app.infra.rest.config;

import br.com.longobucco.personal_finance_app.application.usecase.CategoryUseCase;
import br.com.longobucco.personal_finance_app.application.usecase.TransactionUseCase;
import br.com.longobucco.personal_finance_app.application.usecase.UserUseCase;
import br.com.longobucco.personal_finance_app.core.repository.CategoryRepository;
import br.com.longobucco.personal_finance_app.core.repository.TransactionRepository;
import br.com.longobucco.personal_finance_app.core.repository.UserRepository;
import br.com.longobucco.personal_finance_app.core.security.PasswordHasher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UseCaseConfig {

    @Bean
    public UserUseCase userUseCase(UserRepository userRepository, TransactionRepository transactionRepository,
                                    PasswordHasher passwordHasher) {
        return new UserUseCase(userRepository, transactionRepository, passwordHasher);
    }

    @Bean
    public CategoryUseCase categoryUseCase(CategoryRepository categoryRepository) {
        return new CategoryUseCase(categoryRepository);
    }

    @Bean
    public TransactionUseCase transactionUseCase(TransactionRepository transactionRepository,
                                                  UserRepository userRepository,
                                                  CategoryRepository categoryRepository) {
        return new TransactionUseCase(transactionRepository, userRepository, categoryRepository);
    }
}
