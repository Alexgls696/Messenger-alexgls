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
    id       integer primary key generated always as identity,
    name     varchar(128),
    surname  varchar(128),
    username varchar(64) unique,
    password varchar(256),
    email    varchar(128) unique
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

create table user_details
(
    id       integer primary key generated always as identity,
    user_id  integer references users (id) unique,
    birthday date,
    status   varchar(256)
);

create table user_images
(
    id         integer primary key generated always as identity,
    user_id    integer references users (id),
    image_id   integer not null,
    created_at timestamp
);

create table user_avatars
(
    id            integer primary key generated always as identity,
    user_image_id integer not null references user_images (id),
    user_id       integer references users (id),
    constraint unique_user_image_and_user_id unique (user_image_id, user_id)
);

create index user_details_index on user_details using hash (user_id);
create index user_images_index on user_images using hash (user_id);

insert into roles(name)
values ('ROLE_USER');

CREATE TABLE oauth2_registered_client (
                                          id varchar(100) NOT NULL,
                                          client_id varchar(100) NOT NULL,
                                          client_id_issued_at timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,
                                          client_secret varchar(200) DEFAULT NULL,
                                          client_secret_expires_at timestamp DEFAULT NULL,
                                          client_name varchar(200) NOT NULL,
                                          client_authentication_methods varchar(1000) NOT NULL,
                                          authorization_grant_types varchar(1000) NOT NULL,
                                          redirect_uris varchar(1000) DEFAULT NULL,
                                          post_logout_redirect_uris varchar(1000) DEFAULT NULL,
                                          scopes varchar(1000) NOT NULL,
                                          client_settings varchar(2000) NOT NULL,
                                          token_settings varchar(2000) NOT NULL,
                                          PRIMARY KEY (id)
);
