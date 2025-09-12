package com.alexgls.springboot.messagestorageservice.entity;


import lombok.*;
import org.springframework.data.relational.core.mapping.Table;

@Table(name = "deleted_messages")
@Data
@AllArgsConstructor
public class DeletedMessage {
    private Long id;
    private long messageId;
    private int userId;

}
