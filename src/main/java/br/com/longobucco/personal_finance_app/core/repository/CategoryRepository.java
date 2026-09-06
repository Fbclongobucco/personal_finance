package br.com.longobucco.personal_finance_app.core.repository;

import br.com.longobucco.personal_finance_app.core.domain.Category;

import java.util.List;
import java.util.UUID;

public interface CategoryRepository extends Repository<Category, UUID> {

    List<Category> findAll();

    List<Category> findByType(Category.Type type);
}
