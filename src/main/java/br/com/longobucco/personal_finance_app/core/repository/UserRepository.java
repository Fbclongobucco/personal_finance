package br.com.longobucco.personal_finance_app.core.repository;

import br.com.longobucco.personal_finance_app.core.domain.User;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends Repository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
