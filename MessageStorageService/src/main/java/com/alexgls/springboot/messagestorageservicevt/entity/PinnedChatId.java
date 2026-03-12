package com.alexgls.springboot.messagestorageservicevt.entity;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class PinnedChatId implements Serializable {
    private long userId;
    private long chatId;
}
