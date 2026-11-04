CREATE TABLE categoria (
    id BIGINT NOT NULL AUTO_INCREMENT,
    nome VARCHAR(100) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_categoria_nome (nome)
);