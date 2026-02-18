package com.alexgls.springboot.notificationsservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(exclude = "userNotifications")
@ToString(exclude = "userNotifications")
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private String title;

    private String content;

    @Enumerated(EnumType.ORDINAL)
    private NotificationType type;

    private Integer imageId;

    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String,Object> metadata;

    private Timestamp createdAt;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL,mappedBy = "notification")
    private List<UserNotification> userNotifications;
}
