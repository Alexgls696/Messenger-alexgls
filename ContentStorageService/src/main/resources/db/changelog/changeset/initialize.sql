--liquibase formatted sql
--changeset alexgls:initialize
--comment Создание схемы БД

create table files
(
    id   integer primary key generated always as identity,
    path varchar(256),
    filename varchar(256)
);

create table avatars
(
    id   integer primary key generated always as identity,
    path varchar(256)
);

create table sticker_packages
(
    id   integer primary key generated always as identity,
    name varchar(128)
);

create table stickers
(
    id         integer primary key generated always as identity,
    path       varchar(256),
    package_id integer references sticker_packages (id)
)