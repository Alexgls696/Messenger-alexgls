--liquibase formatted sql
--changeset alexgls:add_unread_count_column
--comment Добавлен столбец с числом непрочитанных сообщений

alter table participants add column unread_count int default 0;