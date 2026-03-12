package com.alexgls.springboot.messagestorageservicevt.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Entity
@Table(name = "message_tokens")
@Getter
@Setter
@NoArgsConstructor
public class MessageToken {

    @EmbeddedId
    private MessageTokenId id;

    public MessageToken(long messageId, String tokenHash) {
        this.id = new MessageTokenId(messageId, tokenHash);
    }
}