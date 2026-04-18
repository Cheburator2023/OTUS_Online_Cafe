CREATE TABLE delivery_slots (
                                id BIGSERIAL PRIMARY KEY,
                                time_slot TIMESTAMP WITHOUT TIME ZONE NOT NULL,
                                courier_id BIGINT NOT NULL,
                                order_id BIGINT,
                                reserved BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_delivery_slots_time_slot ON delivery_slots(time_slot);
CREATE INDEX idx_delivery_slots_order_id ON delivery_slots(order_id);

-- Функция для генерации слотов на 7 дней вперёд с шагом 1 час
DO $$
DECLARE
start_date TIMESTAMP := NOW();
    end_date TIMESTAMP := NOW() + INTERVAL '7 days';
    slot_time TIMESTAMP;
    courier_ids BIGINT[] := ARRAY[1, 2, 3];
    c_id BIGINT;
BEGIN
    slot_time := DATE_TRUNC('hour', start_date + INTERVAL '1 hour');
    WHILE slot_time <= end_date LOOP
        FOREACH c_id IN ARRAY courier_ids LOOP
            INSERT INTO delivery_slots (time_slot, courier_id) VALUES (slot_time, c_id);
END LOOP;
        slot_time := slot_time + INTERVAL '1 hour';
END LOOP;
END $$;