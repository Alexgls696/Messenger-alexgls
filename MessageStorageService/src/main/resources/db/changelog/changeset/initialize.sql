--liquibase formatted sql
--changeset alexgls:initialize
--comment Создание схемы базы данных

create table chats
(
    chat_id         integer primary key generated always as identity,
    name            varchar(256),
    description     text,
    is_group        boolean default false,
    type            varchar(32),
    created_at      timestamp,
    updated_at      timestamp,
    last_message_id integer
);

create table participants
(
    id                   integer primary key generated always as identity,
    user_id              integer,
    chat_id              integer references chats (chat_id),
    joined_at            timestamp,
    is_deleted_by_user   boolean              default false,
    last_read_message_id bigint,
    unread_count         int                  default 0,
    role                 varchar(32) not null default 'MEMBER',
    is_leave boolean default false,
    is_removed boolean default false,
    remove_at timestamp,
    unique (user_id, chat_id)
);

create table messages
(
    message_id   bigint primary key generated always as identity,
    chat_id      integer references chats (chat_id),
    sender_id    integer,
    content      text,
    created_at   timestamp,
    updated_at   timestamp,
    is_read      boolean,
    read_at      timestamp,
    is_service boolean default false,
    reply_to_message_id bigint,
    forward_from_user_id integer,
    is_forwarded boolean default false,
    message_type varchar(32)
);

create table attachments
(
    attachment_id bigint primary key generated always as identity,
    message_id    integer references messages (message_id) not null,
    chat_id       integer references chats (chat_id)       not null,
    file_id       bigint,
    mime_type     varchar(256),
    logic_type    varchar(256),
    filename      varchar(256),
    has_analysis  boolean
);

create table deleted_messages
(
    id         integer primary key generated always as identity,
    message_id integer references messages (message_id),
    user_id    integer,
    constraint unique_message_user unique (message_id, user_id)
);

CREATE TABLE message_tokens
(
    message_id BIGINT      NOT NULL REFERENCES messages (message_id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL,
    PRIMARY KEY (message_id, token_hash)
);

CREATE INDEX idx_message_tokens_hash ON message_tokens (token_hash);
create index message_type_index ON messages using hash (message_type);
create index idx_deleted_messages_user ON deleted_messages using hash (user_id);
create index idx_deleted_messages_message ON deleted_messages (message_id);
