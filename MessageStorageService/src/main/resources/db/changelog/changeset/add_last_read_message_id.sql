--liquibase formatted sql
--changeset alexgls:last_read_message_id_location_change
--comment Изменено расположение last_read_message_id

alter table participants add column last_read_message_id bigint