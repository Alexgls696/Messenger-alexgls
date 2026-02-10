--liquibase formatted sql
--changeset alexgls:add_has_analysis

alter table attachments add column has_analysis boolean;