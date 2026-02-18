package com.alexgls.springboot.notificationsservice.entity;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.sql.Timestamp;

@Entity
@Table(name = "users_notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = "notification" )
@ToString(exclude = "notification")
public class UserNotification {

    @EmbeddedId
    private UserNotificationId id;

    @Column(name = "is_read")
    private boolean read;

    private Timestamp readAt;

    @JoinColumn(name = "notification_id", insertable = false, updatable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private Notification notification;

}
