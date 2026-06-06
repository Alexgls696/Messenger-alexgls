package ru.alexgls.springboot.dto;


/**
 * Класс является сообщением для blacklist-topic в kafka. Рассылка обоим пользователям.
 * @param currentUser - кто отправляет запрос
 * @param targetUser - кого затрагивает запрос
 * @param lock - заблокировал или разблокировал, true - заблокировал.
 */
public record BlacklistKafkaMessage(
        int currentUser,
        int targetUser,
        boolean lock
) {
}
