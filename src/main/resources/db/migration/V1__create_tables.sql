CREATE TABLE users_tb (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    phone VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    balance DECIMAL(19, 2) NOT NULL,
    created_at DATE NOT NULL,
    updated_at DATE NOT NULL
);

CREATE TABLE categories_tb (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE transactions_tb (
    id UUID PRIMARY KEY,
    description VARCHAR(255) NOT NULL,
    category_id UUID NOT NULL REFERENCES categories_tb (id),
    amount DECIMAL(19, 2) NOT NULL,
    user_id UUID NOT NULL REFERENCES users_tb (id),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    payment_method VARCHAR(20) NOT NULL
);
