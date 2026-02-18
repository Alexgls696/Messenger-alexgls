package com.alexgls.springboot.notificationsservice.controller;

import com.alexgls.springboot.notificationsservice.dto.CreateNotificationRequest;
import com.alexgls.springboot.notificationsservice.dto.NotificationDto;
import com.alexgls.springboot.notificationsservice.service.NotificationsService;
import com.alexgls.springboot.notificationsservice.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationsController {

    private final NotificationsService notificationsService;

    @GetMapping
    public List<NotificationDto> findAllByUserId(Authentication authentication, @RequestParam("page") int page, @RequestParam("size") int size) {
        int id = SecurityUtils.getSenderId(authentication);
        log.info("Find all notifications by user id {}", id);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt")));
        return notificationsService.findAllByUserId(id, pageable);
    }

    @GetMapping("/unread-count")
    public Integer getUnreadCount(Authentication authentication) {
        int id = SecurityUtils.getSenderId(authentication);
        log.info("Find unread count notifications by user id {}", id);
        return notificationsService.findUnreadCountForUser(id);
    }

    /*@PostMapping
    public NotificationDto addNotification(@RequestBody CreateNotificationRequest createNotificationRequest) {
        log.info("Add notification {}", createNotificationRequest);
        return notificationsService.save(createNotificationRequest);
    }*/

    @KafkaListener(topics = "create-notifications-topic", groupId = "notifications-consumer", containerFactory = "notificationsKafkaListenerContainerFactory")
    public void asyncAddNotification(CreateNotificationRequest createNotificationRequest) {
        log.info("Add notification {} from kafka", createNotificationRequest);
        notificationsService.save(createNotificationRequest);
    }

    @PostMapping("/{id}/read")
    public void readNotification(@PathVariable("id") int id, Authentication authentication) {
        log.info("Read notification {}", id);
        int userId = SecurityUtils.getSenderId(authentication);
        notificationsService.readOneNotification(id, userId);
    }

    @PostMapping("/read-all")
    public void readNotifications(Authentication authentication) {
        int id = SecurityUtils.getSenderId(authentication);
        log.info("Read all notifications by user id {}", id);
        notificationsService.readAllNotifications(id);
    }
}
