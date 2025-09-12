--liquibase formatted sql
--changeset alexgls:initialize
--comment Создание схемы базы данных

create table chats
(
    chat_id         integer primary key generated always as identity,
    name            varchar(256),
    is_group        boolean default false,
    type            varchar(32),
    created_at      timestamp,
    updated_at      timestamp,
    last_message_id integer
);

create table participants
(
    id        integer primary key generated always as identity,
    user_id   integer,
    chat_id   integer references chats (chat_id),
    joined_at timestamp
);

create table messages
(
    message_id          bigint primary key generated always as identity,
    chat_id             integer references chats (chat_id),
    sender_id           integer,
    content             text,
    created_at          timestamp,
    updated_at          timestamp,
    is_read             boolean,
    read_at             timestamp,
    message_type        varchar(32),
    deleted_for_user_id integer
);

create table attachments
(
    attachment_id bigint primary key generated always as identity,
    message_id    integer references messages (message_id) not null,
    chat_id       integer references chats (chat_id)       not null,
    file_id       bigint,
    mime_type     varchar(256),
    logic_type    varchar(256),
    filename      varchar(256)
);

create index message_type_index ON messages using hash (message_type);


--Создание триггера и триггерной функции