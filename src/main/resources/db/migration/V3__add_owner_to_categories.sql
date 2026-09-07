ALTER TABLE categories_tb ADD COLUMN user_id UUID NOT NULL;

ALTER TABLE categories_tb
    ADD CONSTRAINT fk_categories_user FOREIGN KEY (user_id) REFERENCES users_tb (id);

ALTER TABLE categories_tb
    ADD CONSTRAINT uk_categories_user_name_type UNIQUE (user_id, name, type);

CREATE INDEX idx_transactions_user ON transactions_tb (user_id);
