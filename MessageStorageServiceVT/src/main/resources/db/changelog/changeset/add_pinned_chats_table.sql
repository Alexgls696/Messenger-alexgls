--liquibase formatted sql
--changeset alexgls:add_pinned_table
--comment Добавление таблицы закрепленных чатов

create table pinned_chats
(
    user_id integer,
    chat_id integer,
    primary key (user_id, chat_id)
);