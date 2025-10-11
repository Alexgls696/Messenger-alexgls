--liquibase formatted sql
--changeset alexgls:initialize
--comment Инициализация схемы базы данных

create table user_details
(
    id       integer primary key generated always as identity,
    user_id  integer not null unique,
    birthday date,
    status   varchar(256)
);

create table user_images
(
    id         integer primary key generated always as identity,
    user_id    integer not null,
    image_id   integer not null,
    created_at timestamp
);

create table user_avatars
(
    id            integer primary key generated always as identity,
    user_image_id integer not null references user_images (id),
    user_id       integer,
    constraint unique_user_image_and_user_id unique (user_image_id, user_id)
);

create index user_details_index on user_details using hash (user_id);
create index user_images_index on user_images using hash (user_id);