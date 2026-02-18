package com.alexgls.springboot.notificationsservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class UserNotificationId implements Serializable {

    @Column(name = "notification_id", insertable = false, updatable = false)
    private long notificationId;

    @Column(name = "user_id")
    private int userId;
}
