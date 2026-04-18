ALTER TABLE stock_items
    ADD COLUMN IF NOT EXISTS preparation_time_minutes INT DEFAULT 0,
    ADD COLUMN IF NOT EXISTS price DECIMAL(19,2) NOT NULL DEFAULT 0.00;

-- Обновляем product 1 (в наличии, без готовки)
UPDATE stock_items SET price = 25.50, preparation_time_minutes = 0 WHERE product_id = 1;

-- Для product 2 устанавливаем количество = 0 (нет в наличии), время готовки = 10 минут
UPDATE stock_items SET quantity = 0, price = 25.50, preparation_time_minutes = 10 WHERE product_id = 2;;