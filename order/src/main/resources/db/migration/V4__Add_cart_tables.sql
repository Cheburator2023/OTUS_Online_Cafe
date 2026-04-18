CREATE TABLE IF NOT EXISTS carts (
                                     id BIGSERIAL PRIMARY KEY,
                                     user_id BIGINT NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS cart_items (
                                          id BIGSERIAL PRIMARY KEY,
                                          cart_id BIGINT NOT NULL,
                                          product_id BIGINT NOT NULL,
                                          quantity BIGINT NOT NULL,
                                          CONSTRAINT fk_cart_items_cart FOREIGN KEY (cart_id) REFERENCES carts(id) ON DELETE CASCADE
    );

CREATE INDEX idx_cart_items_cart_id ON cart_items(cart_id);