--liquibase formatted sql
--changeset alexgls:leave_group/service_message_update
--comment Добавляет столбцы: вышел ли пользователь из группы, был ли он удален, а также столбец сервисного сообщения

alter table participants add column is_leave boolean default false;
alter table participants add column is_removed boolean default false;
alter table messages add column is_service boolean default false;