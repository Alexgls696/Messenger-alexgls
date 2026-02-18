--liquibase formatted sql
--changeset alexgls:initialization
--comment Создание схемы БД

create table notifications
(
    id          bigint primary key generated always as identity,
    title       varchar(256), -- Заголовок уведомления
    content     text,         -- Основной текст (varchar(1024) может быть мало)
    type        varchar(32),  -- SYSTEM, MESSAGE, INVITE, etc.
    image_id    integer,
    metadata    jsonb,         --{"chatId": 1, "link": "/chats/1"}
    created_at  timestamp default now()
);

create table users_notifications
(
    notification_id bigint references notifications(id) on delete cascade,
    user_id         integer,
    is_read         boolean not null default false,
    read_at         timestamp, -- Полезно знать, когда именно прочитали
    primary key (notification_id, user_id)
);

create index idx_user_notifications_user_id on users_notifications(user_id);
-- Индекс для фильтрации только непрочитанных
create index idx_user_notifications_unread on users_notifications(user_id) where is_read = false;