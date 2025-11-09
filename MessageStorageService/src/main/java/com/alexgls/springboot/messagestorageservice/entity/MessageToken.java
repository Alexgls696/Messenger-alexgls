package com.alexgls.springboot.messagestorageservice.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("message_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MessageToken {

    @Column(value = "message_id")
    private long messageId;

    @Column(value = "token_hash")
    private String tokenHash;
}
