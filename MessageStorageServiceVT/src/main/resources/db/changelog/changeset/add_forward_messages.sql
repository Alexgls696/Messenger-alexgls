--liquibae formatted sql
--changeset alexgls:add_forward_message_columns
--comment Добавление столбцов

alter table messages add column reply_to_message_id bigint;
alter table messages add column forward_from_user_id integer;
alter table messages add column is_forwarded boolean default false;