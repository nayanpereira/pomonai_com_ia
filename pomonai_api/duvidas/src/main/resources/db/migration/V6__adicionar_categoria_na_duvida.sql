ALTER TABLE duvida
    ADD COLUMN categoria_id BIGINT NULL;

ALTER TABLE duvida
    ADD CONSTRAINT fk_duvida_categoria
    FOREIGN KEY (categoria_id) REFERENCES categoria (id)
    ON DELETE SET NULL;