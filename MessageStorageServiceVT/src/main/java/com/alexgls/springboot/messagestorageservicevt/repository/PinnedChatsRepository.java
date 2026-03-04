package com.alexgls.springboot.messagestorageservicevt.repository;

import com.alexgls.springboot.messagestorageservicevt.entity.PinnedChat;
import com.alexgls.springboot.messagestorageservicevt.entity.PinnedChatId;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PinnedChatsRepository extends CrudRepository<PinnedChat, PinnedChatId> {

    @Modifying
    void deleteById_ChatIdAndId_UserId(long chatId, long userId);

    boolean existsById_ChatIdAndId_UserId(long chatId, long userId);

    Integer findCountOfPinnedChatsById_ChatIdAndId_UserId(long chatId, long userId);

    List<PinnedChat>findAllById_UserId(long userId);

}
