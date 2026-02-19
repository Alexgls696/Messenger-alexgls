--liquibase formatted sql
--changeset alexgls:initialize-security-schema
--comment Инициализация схемы БД для сервиса аутентификации и авторизации

create table roles
(
    id   integer primary key generated always as identity,
    name varchar(32)
);

create table users
(
    id               integer primary key generated always as identity,
    name             varchar(128),
    surname          varchar(128),
    username         varchar(64) unique,
    password         varchar(256),
    email            varchar(128) unique
);

create table users_roles
(
    id      integer primary key generated always as identity,
    role_id integer references roles (id),
    user_id integer references users (id),
    constraint role_and_user_unique unique (role_id, user_id)
);

create table refresh_tokens
(
    id           bigint primary key generated always as identity,
    user_id      integer,
    token        varchar(512),
    expires_date timestamp
);

insert into roles(name)
values ('ROLE_USER');
