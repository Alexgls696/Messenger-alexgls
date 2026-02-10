package com.alexgls.springboot.messagestorageservicevt.entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.ColumnDefault;


import java.sql.Timestamp;

@Entity
@Table(name = "participants")
@Getter
@Setter
@NoArgsConstructor
@ToString
public class Participants {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @JoinColumn(name = "chat_id")
    @ManyToOne
    private Chat chat;

    private int userId;

    @Column(name = "joined_at")
    private Timestamp joinedAt;

    @Column(name = "is_deleted_by_user")
    private boolean isDeletedByUser;

    @Column(name = "last_read_message_id")
    private Long lastReadMessageId;

    @Column(name = "unread_count")
    private int unreadCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    private ChatRole role;

    @Column(name = "is_leave")
    private boolean leave;

    @Column(name = "is_removed")
    private boolean removed;

    @Column(name = "remove_at")
    private Timestamp removeAt;
}
