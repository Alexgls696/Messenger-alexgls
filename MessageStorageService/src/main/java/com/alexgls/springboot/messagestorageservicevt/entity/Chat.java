package com.alexgls.springboot.messagestorageservicevt.entity;

import jakarta.persistence.*;
import lombok.*;

import java.sql.Timestamp;
import java.util.Set;

@Entity
@Table(name = "chats")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"lastMessage", "participants"})
@EqualsAndHashCode(exclude = {"lastMessage", "participants"})
public class Chat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chat_id")
    private long id;

    @Column(name = "name")
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "is_group")
    private boolean isGroup;

    @Column(name = "type")
    private String type;

    @Column(name = "created_at")
    private Timestamp createdAt;

    @Column(name = "updated_at")
    private Timestamp updatedAt;

    @OneToMany(mappedBy = "chat")
    private Set<Participants> participants;

    @JoinColumn(name = "last_message_id")
    @OneToOne(fetch = FetchType.LAZY)
    private Message lastMessage;
}
