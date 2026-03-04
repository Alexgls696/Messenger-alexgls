package com.alexgls.springboot.messagestorageservicevt.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "pinned_chats")
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class PinnedChat {

    @EmbeddedId
    private PinnedChatId id;
}
