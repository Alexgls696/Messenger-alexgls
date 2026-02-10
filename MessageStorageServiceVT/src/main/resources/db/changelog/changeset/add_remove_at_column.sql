--liquibase formatted sql
--changeset alexgls:add_remove_at_column
--comment Добавление нового столбца remove_at в participants

alter table participants
    add column remove_at timestamp;