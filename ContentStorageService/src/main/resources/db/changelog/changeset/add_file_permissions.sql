--liquibase formatted sql
--changeset alexgls:add_permissions_columns

alter table files add column security_type varchar(16) default 'public';
alter table files add column chat_id integer;


create index chat_id_index on files using hash(chat_id)