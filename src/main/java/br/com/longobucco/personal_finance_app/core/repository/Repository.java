package br.com.longobucco.personal_finance_app.core.repository;

import java.util.Optional;

public interface Repository<T, ID> {

    T save(T entity);

    Optional<T> findById(ID id);

    void delete(T entity);
}
