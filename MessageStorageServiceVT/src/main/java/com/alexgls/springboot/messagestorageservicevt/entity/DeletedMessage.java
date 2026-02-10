package com.alexgls.springboot.messagestorageservicevt.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "deleted_messages")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeletedMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private long messageId;
    private int userId;
}
