--liquibase formatted sql

--changeset alexgls:init_schema
-- Создаем схему и расширение
CREATE SCHEMA IF NOT EXISTS partman;
CREATE EXTENSION IF NOT EXISTS pg_partman SCHEMA partman;

--changeset alexgls:create_logs_table
-- Создаем основную таблицу
CREATE TABLE logs (
                      id BIGINT GENERATED ALWAYS AS IDENTITY,
                      ts TIMESTAMP NOT NULL,
                      level VARCHAR(10),
                      service_name VARCHAR(50),
                      message TEXT,
                      trace_id VARCHAR(64),
                      context JSONB,
                      PRIMARY KEY (ts, id)
) PARTITION BY RANGE (ts);

--changeset alexgls:create_logs_indices
-- Индексы создаем на мастере, они сами разойдутся по партициям
CREATE INDEX idx_logs_ts ON logs (ts);
CREATE INDEX idx_logs_trace_id ON logs (trace_id);
CREATE INDEX idx_logs_service ON logs (service_name);

--changeset alexgls:setup_partman
-- Настраиваем партиционирование
-- В pg_partman 5.x:
-- 1. Удален параметр p_type (теперь всегда native)
-- 2. p_interval должен быть строкой интервала Postgres ('1 week')
SELECT partman.create_parent(
               p_parent_table => 'public.logs',
               p_control => 'ts',
               p_interval => '1 week',
               p_premake => 4
       );

--changeset alexgls:setup_retention
-- Настройка очистки старых данных
UPDATE partman.part_config
SET retention = '8 weeks',
    retention_keep_table = false
WHERE parent_table = 'public.logs';