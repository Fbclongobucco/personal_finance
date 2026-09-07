package br.com.longobucco.personal_finance_app.infra.rest.adapters;

import br.com.longobucco.personal_finance_app.core.domain.User;
import br.com.longobucco.personal_finance_app.core.repository.UserRepository;
import br.com.longobucco.personal_finance_app.infra.rest.entities.UserEntity;
import br.com.longobucco.personal_finance_app.infra.rest.jpa_repository.UserJpaRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class UserRepositoryAdapter implements UserRepository {

    private final UserJpaRepository userJpaRepository;

    public UserRepositoryAdapter(UserJpaRepository userJpaRepository) {
        this.userJpaRepository = userJpaRepository;
    }

    @Override
    public User save(User user) {
        UserEntity saved = userJpaRepository.save(toEntity(user));
        return toDomain(saved);
    }

    @Override
    public Optional<User> findById(UUID id) {
        return userJpaRepository.findById(id).map(UserRepositoryAdapter::toDomain);
    }

    @Override
    public void delete(User user) {
        userJpaRepository.deleteById(user.getId());
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userJpaRepository.findByEmail(email).map(UserRepositoryAdapter::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userJpaRepository.existsByEmail(email);
    }

    static User toDomain(UserEntity entity) {
        return User.recover(entity.getId(), entity.getName(), entity.getEmail(), entity.getPhone(),
                entity.getPassword(), toDomainRole(entity.getRole()), entity.getBalance(),
                entity.getCreatedAt(), entity.getUpdatedAt());
    }

    static UserEntity toEntity(User user) {
        return new UserEntity(user.getId(), user.getName(), user.getEmail(), user.getPhone(), user.getPassword(),
                toEntityRole(user.getRole()), user.getBalance(), user.getCreatedAt(), user.getUpdatedAt());
    }

    private static User.Role toDomainRole(UserEntity.Role role) {
        return User.Role.valueOf(role.name());
    }

    private static UserEntity.Role toEntityRole(User.Role role) {
        return UserEntity.Role.valueOf(role.name());
    }
}
