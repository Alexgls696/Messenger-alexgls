--liquibase formatted sql
--changeset alexgls:update_pacticipants_table
--comment столбец - флаг об удалении чата пользователем

alter table participants add column is_deleted_by_user boolean;
update participants set is_deleted_by_user = false;

