--liquibase formatted sql
--changeset alexgls:add_description_update
--comment Добавление описания для чата (группы) и добавление ролей пользователей в participants

alter table chats
    add column description text;

alter table participants add column role varchar(32) not null default 'MEMBER';

create index ROLE_INDEX on participants(role);