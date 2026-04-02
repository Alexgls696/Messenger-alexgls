package com.alexgls.springboot.messagestorageservicevt.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Entity
@Table(name = "attachments")
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(value = "message")
@EqualsAndHashCode(exclude = "message")
public class Attachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attachment_id")
    private Long id;

    @Column(name = "chat_id")
    private Long chatId;

    @Column(name = "message_id")
    private Long messageId;

    @Column(name = "file_id")
    private Long fileId;

    @Column(name = "mime_type")
    private String mimeType;

    @Enumerated(EnumType.STRING)
    @Column(name = "logic_type")
    private MessageType logicType;

    @Column(name = "filename")
    private String fileName;

    @Column(name = "has_analysis")
    private Boolean hasAnalysis;

    @JoinColumn(name = "message_id", insertable = false, updatable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private Message message;

}
