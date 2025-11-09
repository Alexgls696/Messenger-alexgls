--liquibase formatted sql
--changeset alexgls:add_message_tokens_table
--comment Создание таблицы message_tokens для хранения токенов для поиска слов

CREATE TABLE message_tokens
(
    message_id  BIGINT NOT NULL REFERENCES messages(message_id) ON DELETE CASCADE,
    token_hash  VARCHAR(64) NOT NULL,
    PRIMARY KEY (message_id, token_hash)
);

CREATE INDEX idx_message_tokens_hash ON message_tokens (token_hash);