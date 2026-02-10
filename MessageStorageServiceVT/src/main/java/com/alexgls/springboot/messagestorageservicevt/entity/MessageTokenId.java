package com.alexgls.springboot.messagestorageservicevt.entity;


import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.IdClass;
import lombok.*;

import java.io.Serializable;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class MessageTokenId implements Serializable {
    @Column(name = "message_id")
    private long messageId;

    @Column(name = "token_hash")
    private String tokenHash;
}